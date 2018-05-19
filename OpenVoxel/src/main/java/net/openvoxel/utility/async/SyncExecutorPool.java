package net.openvoxel.utility.async;

public class SyncExecutorPool implements AsyncTaskPool{
	@Override
	public void addWork(Task task) {
		task.execute(0);
	}

	@Override
	public int getWorkerCount() {
		return 1;
	}

	@Override
	public void start() {
		//NO OP
	}

	@Override
	public void stop() {
		//NO OP
	}
}
