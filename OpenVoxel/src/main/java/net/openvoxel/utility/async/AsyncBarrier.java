package net.openvoxel.utility.async;

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
	public void reset(int numTasks) {
		int old = countdown.getAndSet(numTasks);
		Validate.Condition(old == 0,"Reset a non complete AsyncBarrier");
	}

	/*
	 * Complete a task being waited on
	 */
	public void completeTask() {
		lock.lock();
		int val = countdown.decrementAndGet();
		Validate.Condition(val >= 0,"Called complete on a completed AsyncBarrier");
		if(val == 0) {
			condition.signal();
		}
		lock.unlock();
	}

	/*
	 * Wait until countdown reaches 0
	 */
	public void awaitCompletion() {
		lock.lock();
		while(countdown.get() != 0) {
			try{
				condition.awaitNanos(100000);
			}catch (Exception ignored) {
				//NO OP//
			}
		}
		lock.unlock();
	}

}
