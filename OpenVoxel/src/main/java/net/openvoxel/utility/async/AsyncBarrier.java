package net.openvoxel.utility.async;

import net.openvoxel.api.PublicAPI;
import net.openvoxel.utility.debug.Validate;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class AsyncBarrier {


	private ReentrantLock lock = new ReentrantLock();
	private Condition condition = lock.newCondition();
	private AtomicInteger countdown = new AtomicInteger(0);

	/*
	 * Reset the task number
	 */
	@PublicAPI
	public void reset(int numTasks) {
		int old = countdown.getAndSet(numTasks);
		Validate.Condition(old == 0,"Reset a non complete AsyncBarrier");
	}

	@PublicAPI
	public int getNumTasks() {
		return countdown.get();
	}

	/*
	 * Add more tasks to be waited on
	 *  - must be called before completing the task being called from
	 */
	@PublicAPI
	public void addNewTasks(int numTasks) {
		Validate.Condition(numTasks > 0,"Can only add tasks to AsyncBarrier");
		int old = countdown.getAndAdd(numTasks);
		Validate.Condition(old > 0,"Can only add tasks to busy AsyncBarrier");
	}

	/*
	 * Complete a task being waited on
	 */
	@PublicAPI
	public void completeTask() {
		int val = countdown.decrementAndGet();
		Validate.Condition(val >= 0,"Called complete on a completed AsyncBarrier");
		if(val == 0) {
			lock.lock();
			condition.signal();
			lock.unlock();
		}
	}

	/*
	 * Wait until countdown reaches 0
	 */
	@PublicAPI
	public void awaitCompletion() {
		lock.lock();
		while(countdown.get() > 0) {
			try{
				condition.awaitNanos(100000);
			}catch (Exception ignored) {
				//NO OP//
			}
		}
		lock.unlock();
	}

	@PublicAPI
	public boolean isComplete() {
		return countdown.get() == 0;
	}


	@Override
	public String toString() {
		return "AsyncBarrier[count="+countdown.get()+"]";
	}
}
