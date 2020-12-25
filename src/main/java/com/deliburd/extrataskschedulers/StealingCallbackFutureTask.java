package com.deliburd.extrataskschedulers;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import java.util.concurrent.locks.ReentrantReadWriteLock;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * A {@link FutureTask} that can do work in the executor linked to it if one is provided and can have work stolen from
 * it while waiting for a computation's result. Callbacks can also be added that execute after completion of a callback
 * whether it be due to success, failure, or cancellation. An optional {@link Consumer} can be provided which runs when
 * a callback fails. Exceptions that come from the provided consumer will cause it to silently fail.
 * 
 * @author DELIBURD
 */
public final class StealingCallbackFutureTask<V> extends FutureTask<V> implements StealingCallbackFuture<V> {
	// Has to be final because of set() and setException() which must not be called by overriding classes.

	private enum CallbackReadyState {
		SUCCESSFUL,
		FAILED
	}

	private final ReentrantReadWriteLock									callbackLock;
	private final StealingCallbackExecutor									executor;
	private final Consumer<? super Throwable>								callbackFailureHandler;
	private volatile boolean												isReady;
	private volatile CallbackReadyState										readyState;
	private volatile V														successValue;
	private volatile Throwable												failureValue;
	private List<Consumer<StealingCallbackExecutor>>						cancellationCallbacks;
	private List<BiConsumer<StealingCallbackExecutor, ? super V>>			successCallbacks;
	private List<BiConsumer<StealingCallbackExecutor, ? super Throwable>>	failureCallbacks;

	{
		callbackLock = new ReentrantReadWriteLock();
		cancellationCallbacks = Collections.synchronizedList(new ArrayList<>(1));
		successCallbacks = Collections.synchronizedList(new ArrayList<>(1));
		failureCallbacks = Collections.synchronizedList(new ArrayList<>(1));
	}

	/**
	 * Creates a {@link StealingCallbackFutureTask} which will execute the provided callable when it's run, providing
	 * its returned value to threads waiting after execution. This task will have no executor linked to it or a consumer
	 * to run when a callback fails.
	 *
	 * @param callable The {@link Callable} that can be run.
	 */
	public StealingCallbackFutureTask(Callable<V> callable) {
		this(callable, null, null);
	}

	/**
	 * Creates a {@link StealingCallbackFutureTask} which will execute the provided runnable, giving the provided value
	 * to threads waiting after execution. This task will have no executor linked to it or a consumer to run when a
	 * callback fails.
	 *
	 * @param runnable The {@link Runnable} that can be run.
	 * @param value    The value to provide to waiting threads after the computation is done.
	 */
	public StealingCallbackFutureTask(Runnable runnable, V value) {
		this(runnable, value, null, null);
	}

	/**
	 * Creates a {@link StealingCallbackFutureTask} which will execute the provided callable when it's run, providing
	 * its returned value to threads waiting after execution. This task can have an executor linked to it but no
	 * consumer to run when a callback fails.
	 *
	 * @param callable The {@link Callable} that can be run.
	 * @param executor The {@link StealingCallbackExecutor} to link to this object. This can be null to not link one.
	 */
	public StealingCallbackFutureTask(Callable<V> callable, StealingCallbackExecutor executor) {
		this(callable, executor, null);
	}

	/**
	 * Creates a {@link StealingCallbackFutureTask} which will execute the provided runnable, giving the provided value
	 * to threads waiting after execution. This task can have an executor linked to it but no consumer to run when a
	 * callback fails.
	 *
	 * @param runnable The {@link Runnable} that can be run.
	 * @param value    The value to provide to waiting threads after the computation is done.
	 * @param executor The {@link StealingCallbackExecutor} to link to this object. This can be null to not link one.
	 */
	public StealingCallbackFutureTask(Runnable runnable, V value, StealingCallbackExecutor executor) {
		this(runnable, value, executor, null);
	}

	/**
	 * Creates a {@link StealingCallbackFutureTask} which will execute the provided callable when it's run, providing
	 * its returned value to threads waiting after execution. This task will have no executor linked to it, but can have
	 * a consumer to run when a callback fails.
	 *
	 * @param callable               The {@link Callable} that can be run.
	 * @param callbackFailureHandler The consumer to execute when a callback fails. This can be null to not execute
	 *                                   anything when a callback fails.
	 */
	public StealingCallbackFutureTask(Callable<V> callable, Consumer<? super Throwable> callbackFailureHandler) {
		this(callable, null, callbackFailureHandler);
	}

	/**
	 * Creates a {@link StealingCallbackFutureTask} which will execute the provided runnable, giving the provided value
	 * to threads waiting after execution. This task will have no executor linked to it, but can have a consumer to run
	 * when a callback fails.
	 *
	 * @param runnable               The {@link Runnable} that can be run.
	 * @param value                  The value to provide to waiting threads after the computation is done.
	 * @param callbackFailureHandler The consumer to execute when a callback fails. This can be null to not execute
	 *                                   anything when a callback fails.
	 */
	public StealingCallbackFutureTask(Runnable runnable, V value, Consumer<? super Throwable> callbackFailureHandler) {
		this(runnable, value, null, callbackFailureHandler);
	}

	/**
	 * Creates a {@link StealingCallbackFutureTask} which will execute the provided callable when it's run, providing
	 * its returned value to threads waiting after execution. This task can have an executor linked to it along with a
	 * consumer to run when a callback fails.
	 *
	 * @param callable               The {@link Callable} that can be run.
	 * @param executor               The {@link StealingCallbackExecutor} to link to this object. This can be null to
	 *                                   not link one.
	 * @param callbackFailureHandler The consumer to execute when a callback fails. This can be null to not execute
	 *                                   anything when a callback fails.
	 */
	public StealingCallbackFutureTask(Callable<V> callable, StealingCallbackExecutor executor, Consumer<? super Throwable> callbackFailureHandler) {
		super(callable);

		this.executor = executor;
		this.callbackFailureHandler = callbackFailureHandler;
	}

	/**
	 * Creates a {@link StealingCallbackFutureTask} which will execute the provided runnable, giving the provided value
	 * to threads waiting after execution. This task can have an executor linked to it along with a consumer to run when
	 * a callback fails.
	 *
	 * @param runnable               The {@link Runnable} that can be run.
	 * @param value                  The value to provide to waiting threads after the computation is done.
	 * @param executor               The {@link StealingCallbackExecutor} to link to this object. This can be null to
	 *                                   not link one.
	 * @param callbackFailureHandler The consumer to execute when a callback fails. This can be null to not execute
	 *                                   anything when a callback fails.
	 */
	public StealingCallbackFutureTask(Runnable runnable, V value, StealingCallbackExecutor executor, Consumer<? super Throwable> callbackFailureHandler) {
		super(runnable, value);

		this.executor = executor;
		this.callbackFailureHandler = callbackFailureHandler;
	}

	private <T> void executeCallback(BiConsumer<StealingCallbackExecutor, T> callback, T result) {
		try {
			callback.accept(executor, result);
		} catch (Throwable e) {
			if (callbackFailureHandler != null) {
				try {
					callbackFailureHandler.accept(e);
				} catch (Throwable e2) { // Ignore exceptions thrown from callback failure handler
				}
			}
		}
	}

	private void executeCallback(Consumer<StealingCallbackExecutor> callback) {
		try {
			callback.accept(executor);
		} catch (Throwable e) {
			if (callbackFailureHandler != null) {
				try {
					callbackFailureHandler.accept(e);
				} catch (Throwable e2) { // Ignore exceptions thrown from callback failure handler
				}
			}
		}
	}
	
	/**
	 * Protected method invoked when this task transitions to state isDone (whether normally or via cancellation). This
	 * method should be called by subclasses overriding this method. Subclasses may override this method to invoke
	 * completion callbacks or perform bookkeeping. Note that you can query status inside the implementation of this
	 * method to determine whether this task has been cancelled.
	 */
	@Override
	protected void done() {
		super.done();
		var writeLock = callbackLock.writeLock();
		writeLock.lock();

		try {
			isReady = true;

			if (isCancelled()) {
				synchronized (cancellationCallbacks) {
					for (var callback : cancellationCallbacks) {
						executeCallback(callback);
					}
				}
			} else if (successValue != null) {
				synchronized (successCallbacks) {
					for (var callback : successCallbacks) {
						executeCallback(callback, successValue);
					}
				}
			} else {
				synchronized (failureCallbacks) {
					for (var callback : failureCallbacks) {
						executeCallback(callback, failureValue);
					}
				}
			}

			cancellationCallbacks = null;
			failureCallbacks = null;
			successCallbacks = null;
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public V stealingGet() throws InterruptedException, ExecutionException {
		if (executor != null) {
			while (!isReady) {
				Runnable task = executor.stealTask();

				if (task == null) {
					try {
						return super.get(500, TimeUnit.MILLISECONDS);
					} catch (TimeoutException e) { // Ignore to look for a task again.
					}
				} else {
					executor.incrementStolenWorkExecutorThreadCount();
					task.run();
					executor.decrementStolenWorkExecutorThreadCount();
					executor.incrementCompletedStolenTaskCount();
				}
			}
		}

		return super.get();
	}

	@Override
	public V stealingGet(long timeout,
			TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		if (executor != null) {
			if (timeout <= 0) {
				throw new IllegalArgumentException("The timeout must be larger than 0.");
			}

			timeout = TimeUnit.MILLISECONDS.convert(timeout, unit);
			Instant startTime = Instant.now();

			while (!isReady) {
				long timeLeft = timeout - Instant.now().toEpochMilli() - startTime.toEpochMilli();

				if (timeLeft <= 0) {
					throw new TimeoutException();
				}

				Runnable task = executor.stealTask();

				if (task == null) {
					try {
						return super.get(Math.min(timeLeft, 500), TimeUnit.MILLISECONDS);
					} catch (TimeoutException e) { // Ignore to look for a task again.
					}
				} else {
					executor.incrementStolenWorkExecutorThreadCount();
					task.run();
					executor.decrementStolenWorkExecutorThreadCount();
					executor.incrementCompletedStolenTaskCount();
				}
			}

			return super.get();
		} else {
			return super.get(timeout, unit);
		}
	}

	@Override
	protected void set(V value) {
		successValue = value;
		readyState = CallbackReadyState.SUCCESSFUL;
		super.set(value);
	}

	@Override
	protected void setException(Throwable throwable) {
		failureValue = throwable;
		readyState = CallbackReadyState.FAILED;
		super.setException(throwable);
	}

	@Override
	public void addSuccessCallback(Consumer<? super V> onSuccess) {
		addSuccessCallback((executor, value) -> onSuccess.accept(value));
	}

	@Override
	public void addFailureCallback(Consumer<? super Throwable> onFailure) {
		addFailureCallback((executor, throwable) -> onFailure.accept(throwable));
	}

	@Override
	public void addCancellationCallback(Runnable onCancellation) {
		addCancellationCallback((executor) -> onCancellation.run());
	}

	@Override
	public void addSuccessCallback(BiConsumer<StealingCallbackExecutor, ? super V> onSuccess) {
		var readLock = callbackLock.readLock();
		readLock.lock();

		try {
			if (isReady) {
				if (readyState == CallbackReadyState.SUCCESSFUL) {
					executeCallback(onSuccess, successValue);
				}
			} else {
				successCallbacks.add(onSuccess);
			}
		} finally {
			readLock.unlock();
		}
	}

	@Override
	public void addFailureCallback(BiConsumer<StealingCallbackExecutor, ? super Throwable> onFailure) {
		var readLock = callbackLock.readLock();
		readLock.lock();

		try {
			if (isReady) {
				if (readyState == CallbackReadyState.FAILED) {
					executeCallback(onFailure, failureValue);
				}
			} else {
				failureCallbacks.add(onFailure);
			}
		} finally {
			readLock.unlock();
		}
	}

	@Override
	public void addCancellationCallback(Consumer<StealingCallbackExecutor> onCancellation) {
		var readLock = callbackLock.readLock();
		readLock.lock();

		try {
			if (isReady) {
				if (isCancelled()) {
					onCancellation.accept(executor);
				}
			} else {
				cancellationCallbacks.add(onCancellation);
			}
		} finally {
			readLock.unlock();
		}
	}
}
