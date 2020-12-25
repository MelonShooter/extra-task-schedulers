package com.deliburd.extrataskschedulers;

import java.util.concurrent.ExecutorService;

/**
 * An interface that specifies an {@link java.util.concurrent.ExecutorService ExecutorService} that can have work stolen
 * from it and provides methods for enabling and disabling work stealing along with a method to check if work stealing
 * is allowed.
 * 
 * @author DELIBURD
 */
public interface StealingExecutorService extends ExecutorService {
	/**
	 * Sets whether to allow work to be stolen and executed.
	 *
	 * @param enableWorkStealing Whether to enable work stealing.
	 */
	public void setWorkStealing(boolean enableWorkStealing);

	/**
	 * Gets whether work is able to be stolen and executed.
	 *
	 * @return Whether work is able to be stolen and executed.
	 */
	public boolean canStealWork();
}
