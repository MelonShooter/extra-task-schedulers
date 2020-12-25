package com.deliburd.tests.extrataskschedulers;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import com.deliburd.extrataskschedulers.StealingCallbackExecutor;
import com.deliburd.extrataskschedulers.StealingCallbackExecutors;

import junit.framework.TestCase;

public class ExecutorTester extends TestCase {
	private StealingCallbackExecutor executor;
	private Object obj;
	
	@Override
	protected void setUp() throws Exception {
		executor = StealingCallbackExecutors.newFixedThreadPool(4);
		obj = null;
	}
	
	@Override
	protected void tearDown() throws Exception {
		executor.shutdown();
	}
	
	public void testShutdownFlagDisabledByDefault() {
		assertFalse(executor.willShutdownOnExit());
	}
	
	public void testShutdownFlagEnabledWhenSet() {
		executor.addShutdownOnExit(true);
		assertTrue(executor.willShutdownOnExit());
	}

	public void testShutdownFlagEnabledWhenDisabledReturn() {
		assertTrue(executor.addShutdownOnExit(true));
	}
	
	public void testShutdownFlagEnabledWithoutDisablingReturn() {
		executor.addShutdownOnExit(true);
		assertFalse(executor.addShutdownOnExit(true));
	}
	
	public void testShutdownFlagDisabledWithoutEnablingReturn() {
		assertFalse(executor.removeShutdownOnExit());
	}
	
	public void testShutdownFlagDisabledWhenEnabledReturn() {
		executor.addShutdownOnExit(true);
		assertTrue(executor.removeShutdownOnExit());
	}
	
	public void testShutdownNowFlag() {
		executor.addShutdownOnExit(false);
		assertFalse(executor.willShutdownRegularlyOnExit());
	}
	
	public void testCallbackFailureHandler() throws InterruptedException, ExecutionException {
		executor.setCallbackFailureHandler(e -> {
			obj = new Object();
		});
		
		var task = executor.submit(this::wait50MS);
		
		task.addCancellationCallback((executor) -> {
			assertNotNull(null); // Intentionally made to fail.
		});
		
		task.cancel(true);

		try {
			task.get();
		} catch(CancellationException e) {
		}

		Thread.sleep(50);
		assertNotNull(obj);
	}
	
	private void wait50MS() {
		try {
			Thread.sleep(50);
		} catch (InterruptedException e) {
		}
	}
}
