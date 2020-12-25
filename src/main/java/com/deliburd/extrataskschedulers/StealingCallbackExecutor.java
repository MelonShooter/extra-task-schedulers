package com.deliburd.extrataskschedulers;

import java.util.List;
import java.util.Map;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

/**
 * A {@link ThreadPoolExecutor} which has three main features added to it and the {@link Future} it returns,
 * {@link StealingCallbackFuture}
 * <p>
 * First, work can be stolen from this pool via {@link StealingFuture#stealingGet} and its overload. A
 * {@link StealingCallbackFuture}, which is a subinterface of {@link StealingFuture} and {@link CallbackFuture}, is
 * returned from the submit methods provided in this class. Work stealing can be enabled or disabled through
 * {@link #setWorkStealing(boolean)} and {@link #canStealWork}. When work is stolen, the queue is polled to see if there
 * any work to be done. This is enabled by default. However, a cached thread pool created from the static utility class,
 * {@link StealingCallbackExecutors}, will disable this feature by default because each task will start a new thread or
 * use an existing idle thread, which means that in general no opportunities are provided for work stealing.
 * <p>
 * Second, callbacks can be added to the {@link StealingCallbackFuture StealingCallbackFutures} returned in this class
 * as it is a subinterface of {@link CallbackFuture}.
 * <p>
 * Lastly, this thread pool can be made to automatically shut down on JVM shut down which is disabled by default. It can
 * be enabled with {@link #addShutdownOnExit(boolean)} and disabled with {@link #removeShutdownOnExit()}.
 * <p>
 * Note: No interrupts will be sent to tasks actively executing that were stolen from the queue if
 * {@link #shutdownNow()} is called. Also {@link #beforeExecute(Thread, Runnable)} and
 * {@link #afterExecute(Runnable, Throwable)} cannot be overridden because they cannot be properly called for stolen
 * tasks to follow what is done in ThreadPoolExecutor.
 * 
 * @author DELIBURD
 * 
 * @see    StealingCallbackExecutors
 * @see    StealingCallbackFuture
 */
public class StealingCallbackExecutor extends ThreadPoolExecutor implements StealingCallbackExecutorService, AutoShutdownExecutorService {
	private final ReentrantReadWriteLock			mainLock;
	private boolean									canStealWork	= true;			// Externally synchronized
	private volatile Consumer<? super Throwable>	callbackFailureHandler;
	private final AtomicInteger						completedStolenTaskCount;
	private final AtomicInteger						stolenWorkExecutorThreadCount;

	/**
	 * A static class to lazily instantiate a {@link ConcurrentHashMap} used to add, get, and remove
	 * {@link StealingCallbackExecutor StealingCallbackExecutors} to shut down on JVM exit.
	 * 
	 * @author DELIBURD
	 */
	private static class ExecutorShutdownHandler {
		private static final Map<StealingCallbackExecutor, Boolean> shutdownExecutorMap;

		static {
			shutdownExecutorMap = new ConcurrentHashMap<>(2);

			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					ExecutorShutdownHandler.shutdownExecutors();
				}
			});
		}

		private static void shutdownExecutors() {
			for (var executorEntry : shutdownExecutorMap.entrySet()) {
				var executor = executorEntry.getKey();

				if (executorEntry.getValue().booleanValue()) {
					executor.shutdown();
				} else {
					executor.shutdownNow();
				}
			}
		}
	}

	{
		completedStolenTaskCount = new AtomicInteger();
		stolenWorkExecutorThreadCount = new AtomicInteger();
		mainLock = new ReentrantReadWriteLock();
	}

	/**
	 * Creates a {@link StealingCallbackExecutor} with the given core pool size, maximum pool size, keep-alive time, and
	 * work queue.
	 * 
	 * @param corePoolSize        The core pool size. This must be 0 or larger.
	 * @param maximumPoolSize     The maximum pool size. This must be larger than or equal to corePoolSize.
	 * @param keepAliveTime       The keep-alive time of created threads. This must be larger than 0.
	 * @param unit                The unit that the keep-alive time is in. This cannot be null.
	 * @param useSynchronousQueue True to use a {@link SynchronousQueue}. False to use a {@link LinkedBlockingQueue}.
	 */
	public StealingCallbackExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, boolean useSynchronousQueue) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, getQueueType(useSynchronousQueue));
	}

	/**
	 * Creates a {@link StealingCallbackExecutor} with the given core pool size, maximum pool size, keep-alive time,
	 * work queue, and handler to use when a task is rejected.
	 * 
	 * @param corePoolSize        The core pool size. This must be 0 or larger.
	 * @param maximumPoolSize     The maximum pool size. This must be larger than or equal to corePoolSize.
	 * @param keepAliveTime       The keep-alive time of created threads. This must be larger than 0.
	 * @param unit                The unit that the keep-alive time is in. This cannot be null.
	 * @param useSynchronousQueue True to use a {@link SynchronousQueue}. False to use a {@link LinkedBlockingQueue}.
	 * @param rejectedHandler     The handler to use when a task is rejected. This cannot be null.
	 */
	public StealingCallbackExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, boolean useSynchronousQueue, RejectedExecutionHandler rejectedHandler) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, getQueueType(useSynchronousQueue), rejectedHandler);
	}

	/**
	 * Creates a {@link StealingCallbackExecutor} with the given core pool size, maximum pool size, keep-alive time,
	 * work queue, and thread factory.
	 * 
	 * @param corePoolSize        The core pool size. This must be 0 or larger.
	 * @param maximumPoolSize     The maximum pool size. This must be larger than or equal to corePoolSize.
	 * @param keepAliveTime       The keep-alive time of created threads. This must be larger than 0.
	 * @param unit                The unit that the keep-alive time is in. This cannot be null.
	 * @param useSynchronousQueue True to use a {@link SynchronousQueue}. False to use a {@link LinkedBlockingQueue}.
	 * @param threadFactory       The thread factory to use to create new threads. This cannot be null.
	 */
	public StealingCallbackExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, boolean useSynchronousQueue, ThreadFactory threadFactory) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, getQueueType(useSynchronousQueue), threadFactory);
	}

	/**
	 * Creates a {@link StealingCallbackExecutor} with the given core pool size, maximum pool size, keep-alive time,
	 * work queue, thread factory, and handler to use when a task is rejected.
	 * 
	 * @param corePoolSize        The core pool size. This must be 0 or larger.
	 * @param maximumPoolSize     The maximum pool size. This must be larger than or equal to corePoolSize.
	 * @param keepAliveTime       The keep-alive time of created threads. This must be larger than 0.
	 * @param unit                The unit that the keep-alive time is in. This cannot be null.
	 * @param useSynchronousQueue True to use a {@link SynchronousQueue}. False to use a {@link LinkedBlockingQueue}.
	 * @param threadFactory       The thread factory to use to create new threads. This cannot be null.
	 * @param rejectedHandler     The handler to use when a task is rejected. This cannot be null.
	 */
	public StealingCallbackExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, boolean useSynchronousQueue, ThreadFactory threadFactory, RejectedExecutionHandler rejectedHandler) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, getQueueType(useSynchronousQueue), threadFactory, rejectedHandler);
	}

	private static BlockingQueue<Runnable> getQueueType(boolean isSynchronous) {
		if (isSynchronous) {
			return new SynchronousQueue<>();
		} else {
			return new LinkedBlockingQueue<>();
		}
	}

	/**
	 * Steals a task from the work queue that the calling thread can run. Returns null if no task is available for this
	 * thread to run or work stealing is not enabled.
	 *
	 * @return A task from the work queue that the calling thread can run. Null if no task is available for this thread
	 *             to run or work stealing is not enabled.
	 */
	protected Runnable stealTask() {
		var readLock = mainLock.readLock();
		readLock.lock();

		try {
			if (!canStealWork) {
				return null;
			}

			return getQueue().poll();
		} finally {
			readLock.unlock();
		}
	}

	/**
	 * Increments the completed stolen task count. This is to used in {@link StealingCallbackFutureTask} when work is
	 * stolen and done.
	 */
	protected void incrementCompletedStolenTaskCount() {
		completedStolenTaskCount.incrementAndGet();
	}

	/**
	 * Increments the counter for the amount of threads that are executing stolen work. This is to used in
	 * {@link StealingCallbackFutureTask} when work is stolen.
	 */
	protected void incrementStolenWorkExecutorThreadCount() {
		stolenWorkExecutorThreadCount.incrementAndGet();
	}

	/**
	 * Decrements the counter for the amount of threads that are executing stolen work. This is to used in
	 * {@link StealingCallbackFutureTask} when work is stolen.
	 */
	protected void decrementStolenWorkExecutorThreadCount() {
		stolenWorkExecutorThreadCount.decrementAndGet();
	}

	/**
	 * Method invoked prior to executing the given Runnable in the given thread. This implementation does nothing and is
	 * final because it cannot be called properly with stolen tasks in the same way {@link ThreadPoolExecutor} does it.
	 */
	@Override
	protected final void beforeExecute(Thread t, Runnable r) {
		// Final because this can't be properly called before
		// execution of a stolen task.
	}

	/**
	 * Method invoked after executing the given Runnable in the given thread. This implementation does nothing and is
	 * final because it cannot be called properly with stolen tasks in the same way {@link ThreadPoolExecutor} does it.
	 */
	@Override
	protected final void afterExecute(Runnable r, Throwable t) {
		// Final because this can't be properly called after
		// execution of a stolen task.
	}

	/**
	 * Returns a StealingCallbackFuture for the given task.
	 * 
	 * @return A {@link StealingCallbackFuture} which, when executed, will run the task, returning a value of the given
	 *             type parameter when it's done.
	 */
	@Override
	protected <T> StealingCallbackFuture<T> newTaskFor(Callable<T> callable) {
		return new StealingCallbackFutureTask<>(callable, this, callbackFailureHandler);
	}

	/**
	 * Returns a StealingCallbackFuture for the given task.
	 * 
	 * @return A {@link StealingCallbackFuture} which, when executed, will run the task.
	 */
	@Override
	protected <T> StealingCallbackFuture<T> newTaskFor(Runnable runnable, T value) {
		return new StealingCallbackFutureTask<>(runnable, value, this, callbackFailureHandler);
	}

	/**
	 * Returns the task queue of StealingCallbackFutures used by this executor. Access to the task queue is intended
	 * primarily for debugging and monitoring. This queue may be in active use. Retrieving the task queue does not
	 * prevent queued tasks from executing. Adding elements to this queue will result in undefined behavior.
	 */
	@Override
	public BlockingQueue<Runnable> getQueue() {
		return super.getQueue();
	}

	/**
	 * Executes the given command at some time in the future. Exceptions thrown from the Runnable will cause the task to
	 * stop executing and silently fail. For more control over what happens to a submitted task on success or failure,
	 * use {@link #submit(Runnable)} or its overloads.
	 */
	@Override
	public void execute(Runnable command) {
		if (!(command instanceof StealingCallbackFutureTask<?>)) {
			command = newTaskFor(command, null); // Commands going into the queue must be StealingCallbackFutureTasks.
		}

		super.execute(command);
	}

	@Override
	public long getCompletedTaskCount() {
		return super.getCompletedTaskCount() + completedStolenTaskCount.get();
	}

	@Override
	public int getActiveCount() {
		return super.getActiveCount() + stolenWorkExecutorThreadCount.get();
	}

	@Override
	public boolean addShutdownOnExit(boolean regularShutdown) {
		var oldValue = ExecutorShutdownHandler.shutdownExecutorMap.put(this, regularShutdown);
		return oldValue == null || oldValue != regularShutdown;
	}

	@Override
	public boolean removeShutdownOnExit() {
		return ExecutorShutdownHandler.shutdownExecutorMap.remove(this) != null;
	}

	@Override
	public boolean willShutdownOnExit() {
		return ExecutorShutdownHandler.shutdownExecutorMap.containsKey(this);
	}

	@Override
	public boolean willShutdownRegularlyOnExit() {
		return ExecutorShutdownHandler.shutdownExecutorMap.getOrDefault(this, false);
	}

	@Override
	public void shutdown() {
		var writeLock = mainLock.writeLock();
		writeLock.lock();

		try {
			super.shutdown();
			removeShutdownOnExit();
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public List<Runnable> shutdownNow() {
		var writeLock = mainLock.writeLock();
		writeLock.lock();

		try {
			var notExecuted = super.shutdownNow();
			removeShutdownOnExit();

			return notExecuted;
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public void setWorkStealing(boolean enableWorkStealing) {
		var writeLock = mainLock.writeLock();
		writeLock.lock();

		try {
			canStealWork = enableWorkStealing;
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public boolean canStealWork() {
		var readLock = mainLock.readLock();
		readLock.lock();

		try {
			return canStealWork && !isShutdown();
		} finally {
			readLock.unlock();
		}
	}

	@Override
	public <T> StealingCallbackFuture<T> submit(Callable<T> task) {
		return (StealingCallbackFuture<T>) super.submit(task);
	}

	@Override
	public StealingCallbackFuture<?> submit(Runnable task) {
		return (StealingCallbackFuture<?>) super.submit(task);
	}

	@Override
	public <T> StealingCallbackFuture<T> submit(Runnable task, T result) {
		return (StealingCallbackFuture<T>) super.submit(task, result);
	}

	@Override
	public void setCallbackFailureHandler(Consumer<? super Throwable> callbackFailureHandler) {
		this.callbackFailureHandler = callbackFailureHandler;
	}

	@Override
	public Consumer<? super Throwable> getCallbackFailureHandler() {
		return callbackFailureHandler;
	}

	@Override
	public String toString() {
		return super.toString() + "[Active stolen work executor threads = " + stolenWorkExecutorThreadCount +
				", completed stolen tasks = " + completedStolenTaskCount + "]";
	}
}
