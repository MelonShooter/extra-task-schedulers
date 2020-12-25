package com.deliburd.extrataskschedulers;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

/**
 * An interface that specifies an {@link ExecutorService ExecutorService} that can have tasks submitted to it and
 * returns a {@link CallbackFuture} upon submission. In addition, a consumer to run when a callback fails for the
 * returned {@link CallbackFuture CallbackFutures} can be specified.
 * 
 * @author DELIBURD
 */
public interface CallbackExecutorService extends ExecutorService {
	/**
	 * Sets the callback failure handler which runs when a callback fails for the submission methods' returned futures
	 * due to an exception.
	 *
	 * @param callbackFailureHandler The callback failure handler
	 */
	public void setCallbackFailureHandler(Consumer<? super Throwable> callbackFailureHandler);

	/**
	 * Gets the callback failure handler which runs when a callback fails for the submission methods' returned futures
	 * due to an exception.
	 * 
	 * @return The callback failure handler.
	 */
	public Consumer<? super Throwable> getCallbackFailureHandler();

	/**
	 * Submits a task that returns a value to the returned {@link CallbackFuture} when it's done.
	 */
	@Override
	public <T> CallbackFuture<T> submit(Callable<T> task);

	/**
	 * Submits a task that returns null to the returned {@link CallbackFuture} when it's done.
	 */
	@Override
	public CallbackFuture<?> submit(Runnable task);

	/**
	 * Submits a task that returns the given result to the returned {@link CallbackFuture} when it's done.
	 */
	@Override
	public <T> CallbackFuture<T> submit(Runnable task, T result);
}
