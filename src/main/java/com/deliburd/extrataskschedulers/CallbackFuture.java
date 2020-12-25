package com.deliburd.extrataskschedulers;

import java.util.concurrent.Future;
import java.util.function.Consumer;

/**
 * A {@link java.util.concurrent.Future Future} that has methods to add callbacks that execute after completion of the
 * computation whether it be due to success, failure, or cancellation.
 * 
 * @author DELIBURD
 */
public interface CallbackFuture<V> extends Future<V> {
	/**
	 * Adds a callback to run on success from the thread that executed the computation. If the computation has already
	 * been completed and was successful, the callback runs immediately on the thread calling this method. The callback
	 * should not block or be time-consuming to execute because all other callbacks added before execution or being
	 * added during execution could be delayed. Exceptions thrown from the callback will not affect execution of other
	 * callbacks.
	 *
	 * @param onSuccess The callback to run on success, providing the value of the {@link CallbackFuture}.
	 */
	public void addSuccessCallback(Consumer<? super V> onSuccess);

	/**
	 * Adds a callback to run on failure from the thread that executed the computation. If the computation has already
	 * been completed and failed, the callback runs immediately on the thread calling this method. The callback should
	 * not block or be time-consuming to execute because all other callbacks added before execution or being added
	 * during execution could be delayed. Exceptions thrown from the callback will not affect execution of other
	 * callbacks.
	 *
	 * @param onFailure The callback to run on failure, providing the value of the {@link java.lang.Throwable Throwable}
	 *                      that caused the failure.
	 */
	public void addFailureCallback(Consumer<? super Throwable> onFailure);

	/**
	 * Adds a callback to run on cancellation from the thread that cancelled the computation. If the computation was
	 * already cancelled, the callback runs immediately on the thread calling this method. The callback should not block
	 * or be time-consuming to execute because all other callbacks added before execution or being added during
	 * execution could be delayed. Exceptions thrown from the callback will not affect execution of other callbacks.
	 *
	 * @param onCancellation The callback to run on cancellation.
	 */
	public void addCancellationCallback(Runnable onCancellation);
}
