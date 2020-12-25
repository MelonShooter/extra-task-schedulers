package com.deliburd.extrataskschedulers;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A {@link java.util.concurrent.Future Future} that has methods to do work in the executor linked to it while waiting
 * for a computation's result. These methods act like their counterparts whose contracts are specified in
 * {@link java.util.concurrent.Future Future} if there is no linked executor or the executor cannot have work stolen
 * from it.
 * 
 * @author DELIBURD
 * 
 * @see    StealingExecutorService
 */
public interface StealingFuture<V> extends RunnableFuture<V> {
	/**
	 * Retrieves the results of the computation for this future when they're available. Unlike
	 * {@link java.util.concurrent.Future#get Future.get}, if the computation has not finished yet, this method checks
	 * if the executor linked to this future can have work stolen from it and there are tasks available that this thread
	 * can do from the linked executor and starts executing them if these conditions are met. If the executor cannot
	 * have work stolen from it or no executor is linked, then this method will act just like
	 * {@link java.util.concurrent.Future#get Future.get}. If there is a linked executor but there are no tasks this
	 * thread can execute, it will block for a certain amount of time, checking if the computation has a result. If it
	 * does, then this method returns. Otherwise, it repeats the process.
	 * <p>
	 * Because this method can execute work from the linked executor, the results aren't retrieved as soon as they're
	 * available. Callers of this method should keep this in mind if a result is needed as soon as its available.
	 * 
	 * @return                       The value retrieved from the computation.
	 * 
	 * @throws CancellationException If the future's computation was canelled.
	 * @throws InterruptedException  If the thread was interrupted while waiting when there were no tasks in the queue.
	 * @throws ExecutionException    If the computation threw an exception while it was being completed.
	 */
	public V stealingGet() throws InterruptedException, ExecutionException;

	/**
	 * Retrieves the results of the computation for this future when they're available, throwing an exception if the
	 * timeout was exceeded and the computation hadn't finished. Unlike
	 * {@link java.util.concurrent.Future#get(long timeout, TimeUnit unit) Future.get(long timeout, TimeUnit unit)}, ,
	 * if the computation has not finished yet, this method checks if the executor linked to this future can have work
	 * stolen from it and there are tasks available that this thread can do from the linked executor and starts
	 * executing them if these conditions are met. If this object has no linked executor or the executor cannot have work
	 * stolen from it, this method will act just like
	 * {@link java.util.concurrent.Future#get(long timeout, TimeUnit unit) Future.get(long timeout, TimeUnit unit)}. If
	 * there is a linked executor but there are no tasks this thread can execute, it will block for a certain amount of
	 * time, checking if the computation has a result or the timeout has been exceeded. If it completed successfully,
	 * then this method returns. If not, then this process repeats. If the timeout is exceeded when checked and the
	 * computation has not been completed yet, it will throw a {@link TimeoutException}.
	 * <p>
	 * Because this method can execute work from the linked executor, the results aren't retrieved as soon as they're
	 * available. In addition, this method can only guarantee that <i>at least</i> the specified amount of time passes
	 * before throwing a {@link TimeoutException}, but will not not necessarily throw it as soon as the timeout passes.
	 * Callers of this method should keep this in mind if a result is needed as soon as its available or the method
	 * needs to exit as soon as the timeout passes.
	 *
	 * @param  timeout               The timeout to wait before throwing a TimeoutException. This must be larger than 0.
	 * @param  unit                  The time unit for the timeout specified.
	 * 
	 * @return                       The value retrieved from the computation.
	 * 
	 * @throws CancellationException If the future's computation was canelled.
	 * @throws InterruptedException  If the thread was interrupted while waiting when there were no tasks in the queue.
	 * @throws ExecutionException    If the computation threw an exception while it was being completed.
	 * @throws TimeoutException      If the timeout time was passed.
	 */
	public V stealingGet(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException;
}
