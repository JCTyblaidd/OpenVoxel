package net.openvoxel.utility.async;

import net.openvoxel.OpenVoxel;
import net.openvoxel.api.PublicAPI;

public interface AsyncTaskPool {

	interface Task {
		void execute(int threadID);
	}

	@PublicAPI
	static int getWorkerCount(String ID,int fallback,int minCount) {
		if(OpenVoxel.getLaunchParameters().hasFlag(ID)) {
			return Math.max(OpenVoxel.getLaunchParameters().getIntegerMap(ID),Math.max(1,minCount));
		}else {
			return fallback;
		}
	}

	/**
	 * Add some work to the executor thread
	 *  that takes the thread ID for synchronisation
	 */
	@PublicAPI
	void addWork(Task task);

	/**
	 * Add some work to the executor thread
	 */
	@PublicAPI
	default void addWork(Runnable task) {
		addWork((ignored) -> task.run());
	}

	/**
	 * @return The number of worker threads
	 */
	@PublicAPI
	int getWorkerCount();

	/**
	 * Start the thread pool, if already started does nothing
	 */
	@PublicAPI
	void start();

	/**
	 * Stop the thread pool, if already stopped does nothing
	 */
	@PublicAPI
	void stop();


}
