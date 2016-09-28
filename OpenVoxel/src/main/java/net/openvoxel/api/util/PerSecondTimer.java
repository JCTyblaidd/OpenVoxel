package net.openvoxel.api.util;

/**
 * Created by James on 04/08/2016.
 *
 * Handles a running-average of the rate of a function
 *
 */
public class PerSecondTimer {
	/**Running Average*/
	private long[] array;
	private int currentLoc = 0;
	private long last_notify = 0;
	private long current_total = 0;

	public PerSecondTimer() {
		this(256);
	}

	/**
	 * @param alloc the size of the variable cache
	 */
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

	/**
	 * @return the average time period
	 */
	public float getDelaySecond() {
		return current_total / (array.length * 1000.0F);
	}

	/**
	 * @return the average frequency
	 */
	public float getPerSecond() {
		return (array.length * 1000.0F)/ current_total;
	}

	/**
	 * @return return the frequency of the first value in the timer
	 */
	public float getFirstDelayValue() {
		return 1000.0F / array[0];
	}
}
