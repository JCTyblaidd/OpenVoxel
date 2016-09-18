package net.openvoxel.client.control;

import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import net.openvoxel.OpenVoxel;
import net.openvoxel.common.event.EventListener;

import java.security.DigestException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * Created by James on 28/08/2016.
 *
 * Asynchronously Generate World Data
 *      >+ Asynchronously Generate Render Commands w/ Vulkan
 */
@Deprecated
public class AsyncRenderCacheManager implements EventListener{

	public static class RunnableEvent {
		public Runnable runnable = null;
	}

	//private List<AsyncRenderWorker> threads = new ArrayList<>();
	ThreadGroup threadGroup;
	private int workerCount;
	volatile boolean isRunning = true;
	RingBuffer<RunnableEvent> buffer;
	Disruptor<RunnableEvent> disruptor;
	private static int threadInitCount = 0;

	private static final String WORKER_PARAM_VALUE = "renderWorkerCount";
	private static int _getWorkerCount() {
		if(OpenVoxel.getLaunchParameters().hasFlag(WORKER_PARAM_VALUE)) {
			return OpenVoxel.getLaunchParameters().getIntegerMap(WORKER_PARAM_VALUE);
		}else {
			return 4;
		}
	}

	public void shutdown() {
		isRunning = false;
		disruptor.shutdown();
	}

	public void addWork(Runnable work) {
		buffer.publishEvent((event,sequence) -> event.runnable = work);
	}

	public AsyncRenderCacheManager() {
		this(_getWorkerCount());
	}


	private Thread createSubThread(Runnable r) {
		Thread thread = new Thread(threadGroup,r,"RenderWorkerThread #"+threadInitCount++);
		thread.setDaemon(true);
		return thread;
	}

	private static class RunnableEventHandler implements EventHandler<RunnableEvent> {
		@Override
		public void onEvent(RunnableEvent event, long sequence, boolean endOfBatch) throws Exception {
			event.runnable.run();
		}
	}

	public AsyncRenderCacheManager(int workerThreadCount) {
		disruptor = new Disruptor<>(RunnableEvent::new,512,this::createSubThread,ProducerType.SINGLE,new PhasedBackoffWaitStrategy(100,1000, TimeUnit.NANOSECONDS,new BlockingWaitStrategy()));
		disruptor.handleEventsWith(new RunnableEventHandler());
		disruptor.start();
		buffer = disruptor.getRingBuffer();
		workerCount = workerThreadCount;
		threadGroup = new ThreadGroup("Open Voxel: Async Worker Group");
	}

/**
	private static class AsyncRenderWorker implements Runnable{
		private Thread thread;
		private AsyncRenderCacheManager controller;
		private SequenceBarrier barrier;
		private Sequence sequence;
		public AsyncRenderWorker(AsyncRenderCacheManager master,SequenceBarrier barrier,Sequence sequence) {
			controller = master;
			this.barrier = barrier;
			this.sequence = sequence;
			thread = new Thread(controller.threadGroup,this,"Open Voxel: Async Worker #"+threadInitCount++);
			thread.start();
		}
		@Override
		public void run() {
			try {
				OpenVoxel openVoxel = OpenVoxel.getInstance();
				boolean flag;
				while (openVoxel.isRunning && controller.isRunning) {
					//Attempt To Acquire Some Work
					flag = attemptWork();
					//If No Work Is Found -> Sleep
					if (!flag) {
						Thread.sleep(10);//Wait For about 1/100 of a second
					}
				}
			}catch(InterruptedException e) {
				//STOP THREAD
			}
		}
		private boolean attemptWork() {
			Runnable work = null;

			if(work != null) {
				work.run();
				return true;
			}
			return false;
		}
	}
		**/

}
