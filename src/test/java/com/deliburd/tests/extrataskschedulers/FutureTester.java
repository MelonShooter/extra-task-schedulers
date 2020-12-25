package com.deliburd.tests.extrataskschedulers;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import com.deliburd.extrataskschedulers.StealingCallbackExecutor;
import com.deliburd.extrataskschedulers.StealingCallbackExecutors;
import com.deliburd.extrataskschedulers.StealingCallbackFuture;

import junit.framework.TestCase;

public class FutureTester extends TestCase {
	private StealingCallbackExecutor	executor;
	private Object						obj;
	private AtomicInteger				atomicInt;
	private volatile boolean volatileBoolean;

	@Override
	protected void setUp() throws Exception {
		executor = StealingCallbackExecutors.newSingleThreadExecutor();
		atomicInt = new AtomicInteger();
		obj = null;
		volatileBoolean = false;
	}

	@Override
	protected void tearDown() throws Exception {
		executor.shutdown();
	}

	public void testFutureSuccessCallback() throws InterruptedException, ExecutionException {
		var task = executor.submit(this::returnObject);
		task.addSuccessCallback((obj) -> {
			this.obj = obj;
		});

		Object object = task.get();

		Thread.sleep(50);

		assertEquals(obj, object);
	}

	public void testFutureSuccessCallbackWithExecutor() throws InterruptedException, ExecutionException {
		var task = executor.submit(this::returnObject);
		task.addSuccessCallback((executor, obj) -> {
			this.obj = executor;
		});

		task.get();

		Thread.sleep(50);

		assertEquals(obj, executor);
	}

	public void testFutureFailureCallback() throws InterruptedException {
		var task = executor.submit(this::giveRuntimeError);
		task.addFailureCallback((e) -> {
			obj = e;
		});

		Throwable exception = null;

		try {
			task.get();
		} catch (ExecutionException e) {
			exception = e.getCause();
		}

		Thread.sleep(50);

		assertEquals(obj, exception);
	}

	public void testFutureFailureCallbackWithExecutor() throws InterruptedException {
		var task = executor.submit(this::giveRuntimeError);
		task.addFailureCallback((executor, obj) -> {
			this.obj = executor;
		});

		try {
			task.get();
		} catch (ExecutionException e) {
		}

		Thread.sleep(50);

		assertEquals(obj, executor);
	}

	public void testFutureCancellationCallback() throws InterruptedException, ExecutionException {
		var task = executor.submit(this::wait50MS);
		task.addCancellationCallback((executor) -> {
			obj = new Object();
		});

		task.cancel(true);

		try {
			task.get();
		} catch (CancellationException e) {
		}

		Thread.sleep(50);

		assertNotNull(obj);
	}

	public void testFutureCancellationWithExecutorCallback() throws InterruptedException, ExecutionException {
		var task = executor.submit(this::wait50MS);
		task.addCancellationCallback((executor) -> {
			obj = executor;
		});

		task.cancel(true);

		try {
			task.get();
		} catch (CancellationException e) {
		}

		Thread.sleep(50);

		assertEquals(obj, executor);
	}

	public void testStealingGet() throws InterruptedException, ExecutionException {
		int iterationCount = 10;
		StealingCallbackFuture<?> task = executor.submit(this::wait50MS);
		
		for (int i = 0; i < iterationCount; i++) {
			executor.submit(this::incrementIntegerWithCondition);
		}

		volatileBoolean = true;
		task.stealingGet();
		Thread.sleep(5);
		assertEquals(atomicInt.get() + 1, iterationCount + 1);
	}
	
	public void testCompletedTaskCounter() throws InterruptedException, ExecutionException {
		int iterationCount = 10;
		StealingCallbackFuture<?> task = executor.submit(this::wait50MS);
		
		for (int i = 0; i < iterationCount; i++) {
			executor.submit(this::incrementInteger);
		}
		
		task.stealingGet();
		Thread.sleep(5);
		assertEquals(atomicInt.get() + 1, executor.getCompletedTaskCount());
	}

	// test stealing tasks
	// re-read code for concurrency issues

	private Object returnObject() {
		return new Object();
	}

	private void wait50MS() {
		try {
			Thread.sleep(50);
		} catch (InterruptedException e) {
		}
	}
	
	private void incrementIntegerWithCondition() {
		if(volatileBoolean) {
			incrementInteger();
		}
	}
	
	private void incrementInteger() {
		atomicInt.incrementAndGet();
	}

	private void giveRuntimeError() {
		throw new RuntimeException();
	}
}
