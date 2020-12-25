package com.deliburd.extrataskschedulers;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * A {@link java.util.concurrent.Future Future} that has methods to do work in the executor linked to it while waiting
 * for a computation's result. It also contains methods to make callbacks that execute after completion of the
 * computation whether it be due to success, failure, or cancellation.
 * 
 * @author DELIBURD
 */
public interface StealingCallbackFuture<V> extends StealingFuture<V>, CallbackFuture<V> {
	/**
	 * Adds a callback to run on success from the thread that executed the computation. If the computation has already
	 * been completed and was successful, the callback runs immediately on the thread calling this method. The callback
	 * should not block or be time-consuming to execute because all other callbacks added before execution or being
	 * added during execution could be delayed. Exceptions thrown from the callback will be printed to System.err, but
	 * will not affect execution of other callbacks. (add logging options?)
	 *
	 * @param onSuccess The callback to run on success, providing the linked {@link StealingCallbackExecutor} and the
	 *                      value of the {@link StealingCallbackFuture}.
	 */
	public void addSuccessCallback(BiConsumer<StealingCallbackExecutor, ? super V> onSuccess);

	/**
	 * Adds a callback to run on failure from the thread that executed the computation. If the computation has already
	 * been completed exceptionally, the callback runs immediately on the thread calling this method. The callback
	 * should not block or be time-consuming to execute because all other callbacks added before execution or being
	 * added during execution could be delayed. Exceptions thrown from the callback will be printed to System.err, but
	 * will not affect execution of other callbacks.
	 *
	 * @param onFailure The callback to run on failure, providing the linked {@link StealingCallbackExecutor} and the
	 *                      value of the {@link java.lang.Throwable Throwable} that caused the failure.
	 */
	public void addFailureCallback(BiConsumer<StealingCallbackExecutor, ? super Throwable> onFailure);

	/**
	 * Adds a callback to run on cancellation from the thread that cancelled the computation. If the computation was
	 * already cancelled, the callback runs immediately on the thread calling this method. The callback should not block
	 * or be time-consuming to execute because all other callbacks added before execution or being added during
	 * execution could be delayed. Exceptions thrown from the callback will be printed to System.err, but will not
	 * affect execution of other callbacks.
	 *
	 * @param onCancellation The callback to run on cancellation, providing the linked {@link StealingCallbackExecutor}.
	 */
	public void addCancellationCallback(Consumer<StealingCallbackExecutor> onCancellation);
}
