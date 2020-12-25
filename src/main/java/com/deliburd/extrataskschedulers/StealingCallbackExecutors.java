package com.deliburd.extrataskschedulers;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.TimeUnit;

/**
 * A static utility class containing methods to get different {@link StealingCallbackExecutor StealingCallbackExecutors}
 * including fixed thread pools, cached thread pools, and single thread executors.
 * 
 * @author DELIBURD
 * 
 * @see    StealingCallbackExecutor
 */
public final class StealingCallbackExecutors {
	private StealingCallbackExecutors() {
	}

	/**
	 * Creates a {@link StealingCallbackExecutor} with a fixed amount of threads in its pool and with work stealing
	 * enabled by default. These threads do not expire and once created, last until the pool is shut down. This kind of
	 * thread pool is good for non-blocking tasks.
	 *
	 * @param  threadCount The amount of threads that the fixed thread pool can have.
	 * 
	 * @return             The new fixed thread pool.
	 * 
	 * @see                StealingCallbackExecutor
	 */
	public static StealingCallbackExecutor newFixedThreadPool(int threadCount) {
		return new StealingCallbackExecutor(threadCount, threadCount, 0, TimeUnit.NANOSECONDS, false);
	}

	/**
	 * Creates a {@link StealingCallbackExecutor} with a fixed amount of threads in its pool and with work stealing
	 * enabled by default. These threads do not expire and once created, last until the pool is shut down. A
	 * {@link RejectedExecutionHandler} can be specified to handle rejected tasks. This kind of thread pool is good for
	 * non-blocking tasks.
	 *
	 * @param  threadCount     The amount of threads that the fixed thread pool can have.
	 * @param  rejectedHandler The handler to use for rejected tasks.
	 * 
	 * @return                 The new fixed thread pool.
	 * 
	 * @see                    StealingCallbackExecutor
	 */
	public static StealingCallbackExecutor newFixedThreadPool(int threadCount, RejectedExecutionHandler rejectedHandler) {
		return new StealingCallbackExecutor(threadCount, threadCount, 0, TimeUnit.NANOSECONDS, false, rejectedHandler);
	}

	/**
	 * Creates a {@link StealingCallbackExecutor} that expands the amount of threads it has dynamically so that each new
	 * task gets its own thread if none is available. Work stealing is disabled by default because a new thread or an
	 * idle thread is used to execute each new task, leaving, in general, no opportunity for work to be stolen. These
	 * threads expire after 60 seconds of inactivity. This kind of thread pool is good for low throughput, blocking task
	 * submission.
	 *
	 * @return The new cached thread pool.
	 * 
	 * @see    StealingCallbackExecutor
	 */
	public static StealingCallbackExecutor newCachedThreadPool() {
		return new StealingCallbackExecutor(0, Integer.MAX_VALUE, 60, TimeUnit.SECONDS, true);
	}

	/**
	 * Creates a {@link StealingCallbackExecutor} that expands the amount of threads it has dynamically so that each new
	 * task gets its own thread if none is available. Work stealing is disabled by default because a new thread or an
	 * idle thread is used to execute each new task, leaving, in general, no opportunity for work to be stolen. These
	 * threads expire after 60 seconds of inactivity. A {@link RejectedExecutionHandler} can be specified to handle
	 * rejected tasks. This kind of thread pool is good for low throughput, blocking tasks.
	 * 
	 * @param  rejectedHandler The handler to use for rejected tasks.
	 * 
	 * @return                 The new cached thread pool.
	 * 
	 * @see                    StealingCallbackExecutor
	 */
	public static StealingCallbackExecutor newCachedThreadPool(RejectedExecutionHandler rejectedHandler) {
		return new StealingCallbackExecutor(0, Integer.MAX_VALUE, 60, TimeUnit.SECONDS, true, rejectedHandler);
	}

	/**
	 * Creates a {@link StealingCallbackExecutor} that can only have 1 thread in it and with work stealing enabled. This
	 * thread will not expire until the pool is shut down.
	 *
	 * @return The new single threaded thread pool.
	 * 
	 * @see    StealingCallbackExecutor
	 */
	public static StealingCallbackExecutor newSingleThreadExecutor() {
		return newFixedThreadPool(1);
	}

	/**
	 * Creates a {@link StealingCallbackExecutor} pool that can only have 1 thread in it and with work stealing enabled.
	 * This thread will not expire until the pool is shut down. A {@link RejectedExecutionHandler} can be specified to
	 * handle rejected tasks.
	 * 
	 * @param  rejectedHandler The handler to use for rejected tasks.
	 * 
	 * @return                 The new single threaded thread pool.
	 * 
	 * @see                    StealingCallbackExecutor
	 */
	public static StealingCallbackExecutor newSingleThreadExecutor(RejectedExecutionHandler rejectedHandler) {
		return newFixedThreadPool(1, rejectedHandler);
	}
}
