package net.openvoxel.api.util;

/**
 * Created by James on 04/08/2016.
 *
 * Handles a running-average of the rate of a function
 *
 */
public class PerSecondTimer {

	private long[] array;
	private int currentLoc = 0;
	private long last_notify = 0;
	private long current_total = 0;

	public PerSecondTimer() {
		array = new long[256];
		_setZero();
	}

	public PerSecondTimer(int alloc) {
		array = new long[alloc];
		_setZero();
	}

	private void _setZero() {
		for(int i = 0; i < array.length; i++) {
			array[i] = 0;
		}
	}

	//Call at the end or the beginning of your task
	public long notifyEvent() {
		long current = System.currentTimeMillis();
		current_total -= array[currentLoc];
		array[currentLoc] = current - last_notify;
		current_total += array[currentLoc];
		last_notify = current;
		currentLoc++;
		if (currentLoc == array.length) currentLoc = 0;//Faster Modulus
		return current;
	}

	public float getDelaySecond() {
		return current_total / (array.length * 1000.0F);
	}

	public float getPerSecond() {
		return (array.length * 1000.0F)/ current_total;
	}

	public float getFirstDelayValue() {
		return 1000.0F / array[0];
	}
}
