package com.deliburd.extrataskschedulers;

/**
 * An interface that specifies an {@link java.util.concurrent.ExecutorService ExecutorService} that can shut down
 * automatically when the JVM terminates gracefully. It contains methods to view the auto-shutdown status and
 * allow/disallow it to shut down automatically.
 * 
 * @author DELIBURD
 */
public interface AutoShutdownExecutorService {

	/**
	 * Makes this executor shut down automatically when the JVM is exiting gracefully.
	 * 
	 * @param  regularShutdown Whether to call {@link #shutdown()} or {@link #shutdownNow()} when exiting. True to call
	 *                             {@link #shutdown()}. False to call {@link #shutdownNow()}.
	 *
	 * @return                 True if done successfully. False if the executor was already made to shut down
	 *                             automatically with the same shutdown option previously.
	 */
	boolean addShutdownOnExit(boolean regularShutdown);

	/**
	 * Stops this executor from shutting down automatically when the JVM is exiting gracefully.
	 *
	 * @return True if stopped successfully. False if the executor was never set to shut down automatically on JVM exit.
	 */
	boolean removeShutdownOnExit();

	/**
	 * Gets whether this executor will shut down automatically when the JVM exits gracefully.
	 *
	 * @return Whether this executor will shut down automatically when the JVM exits gracefully.
	 * 
	 * @see    #addShutdownOnExit(regularShutdown)
	 * @see    #removeShutdownOnExit()
	 */
	boolean willShutdownOnExit();

	/**
	 * Gets whether this executor will shut down regularly (ie: using {@link #shutdown()}) when the JVM exits
	 * gracefully.
	 *
	 * @return Whether this executor will shut down regularly (ie: using {@link #shutdown()}) when the JVM exits
	 *             gracefully. True if the executor will shut down using {@link #shutdown()} on JVM exit. False if it
	 *             will use {@link #shutdownNow()} or the executor won't shut down automatically.
	 */
	boolean willShutdownRegularlyOnExit();

}
