package net.openvoxel.common;

import net.openvoxel.OpenVoxel;
import net.openvoxel.api.util.PerSecondTimer;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by James on 09/04/2017.
 *
 * Thread That Handles Executing Game Logic
 */
public class GameTickThread implements Runnable{

	private PerSecondTimer tickTimer;
	private AtomicBoolean executionFlag;
	private Runnable tickTarget;
	private Thread thread;
	private Runnable onTerminate;

	public GameTickThread(Runnable target,String name,Runnable onTerminate) {
		executionFlag = new AtomicBoolean(true);
		tickTarget = target;
		tickTimer = new PerSecondTimer();
		thread = new Thread(this,name);
		this.onTerminate = onTerminate;
	}

	public void start() {
		thread.start();
	}

	public void terminate() {
		executionFlag.set(false);
	}

	public float getTicksPerSecond() {
		return tickTimer.getPerSecond();
	}

	private static final int TICK_DELAY = 50;//1000 / 20;

	@Override
	public void run() {
		long last_time = System.currentTimeMillis();
		long current_time, time_taken, wait_time;
		while(OpenVoxel.getInstance().isRunning.get() && executionFlag.get()) {
			//Run Game Logic//
			try{
				tickTarget.run();
			}catch(Exception e) {
				e.printStackTrace();
			}
			current_time = tickTimer.notifyEvent();
			time_taken = last_time - current_time;
			if(time_taken < TICK_DELAY) {
				//The Thread Must Sleep//
				wait_time = TICK_DELAY - time_taken;
				try {
					Thread.sleep(wait_time);
				}catch(InterruptedException ignored) {}
			}
			last_time = System.currentTimeMillis();
		}
		onTerminate.run();
	}
}
