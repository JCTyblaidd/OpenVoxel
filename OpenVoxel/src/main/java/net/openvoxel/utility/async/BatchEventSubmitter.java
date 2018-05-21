package net.openvoxel.utility.async;

import java.util.Arrays;

public class BatchEventSubmitter {

	private AsyncTaskPool taskPool;
	private AsyncTaskPool.Task[] tasks;
	private int writeIdx;

	public BatchEventSubmitter(AsyncTaskPool taskPool,int batching) {
		this.taskPool = taskPool;
		tasks = new AsyncTaskPool.Task[batching];
		writeIdx = 0;
	}

	public void addWork(AsyncTaskPool.Task task) {
		tasks[writeIdx] = task;
		writeIdx++;
		if(writeIdx == tasks.length) flushWork();
	}

	public void flushWork() {
		final AsyncTaskPool.Task[] writeArray = Arrays.copyOf(tasks,writeIdx);
		taskPool.addWork(ID -> {
			for (AsyncTaskPool.Task writeTask : writeArray) {
				writeTask.execute(ID);
			}
		});
		writeIdx = 0;
	}
}
