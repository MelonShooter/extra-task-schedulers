package com.deliburd.extrataskschedulers;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

/**
 * An interface that specifies an {@link ExecutorService ExecutorService} that merges the
 * {@link StealingExecutorService} and {@link CallbackExecutorService} interfaces and provides overridden task submission methods that return
 * {@link StealingCallbackFuture StealingCallbackFutures}.
 * 
 * @author DELIBURD
 */
public interface StealingCallbackExecutorService extends StealingExecutorService, CallbackExecutorService {
	/**
	 * Submits a task that returns a value to the returned {@link StealingCallbackFuture} when it's done.
	 */
	@Override
	public <T> StealingCallbackFuture<T> submit(Callable<T> task);

	/**
	 * Submits a task that returns null to the returned {@link StealingCallbackFuture} when it's done.
	 */
	@Override
	public StealingCallbackFuture<?> submit(Runnable task);

	/**
	 * Submits a task that returns the given result to the returned {@link StealingCallbackFuture} when it's done.
	 */
	@Override
	public <T> StealingCallbackFuture<T> submit(Runnable task, T result);
}
