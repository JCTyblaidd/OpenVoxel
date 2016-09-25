package net.openvoxel.utility;

import com.lmax.disruptor.*;
import net.openvoxel.OpenVoxel;
import net.openvoxel.api.logger.Logger;
import org.lwjgl.system.MemoryUtil;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by James on 15/09/2016.
 *
 * Asynchronous Worker Pool, Only Callable From ONE THREAD
 */
public class AsyncRunnablePool {

	/**
	 * Runnable Reference Instance For The RingBuffer
	 */
	private static class RunnableRef {
		Runnable pointer;
	}

	/**
	 * Utility Function to get the workers to use
	 * @param ID the parameter to check
	 * @param fallback the fallback amount
	 * @return the number of workers
	 */
	public static int getWorkerCount(String ID,int fallback) {
		if(OpenVoxel.getLaunchParameters().hasFlag(ID)) {
			return OpenVoxel.getLaunchParameters().getIntegerMap(ID);
		}else {
			return fallback;
		}
	}

	RingBuffer<RunnableRef> ringBuffer;
	AtomicBoolean running = new AtomicBoolean(false);
	ConcurrentLinkedQueue<RunnableRefWorker> workerQueue = new ConcurrentLinkedQueue<>();
	int workerCount;
	Sequence sequence = new Sequence(Sequencer.INITIAL_CURSOR_VALUE);
	SequenceBarrier barrier;
	ThreadGroup threadGroup;
	int ID = 0;

	/**
	 * @param name the name of the thread-group + prefix for worker thread names
	 * @param workerCount the number of worker threads to create
	 */
	public AsyncRunnablePool(String name,int workerCount) {
		ringBuffer = RingBuffer.createSingleProducer(RunnableRef::new,512,new PhasedBackoffWaitStrategy(10,100, TimeUnit.NANOSECONDS,new BlockingWaitStrategy()));
		barrier = ringBuffer.newBarrier();
		threadGroup = new ThreadGroup(name);
		for(int i = 0; i < workerCount; i++) {
			workerQueue.add(new RunnableRefWorker(this));
		}
	}

	private static EventTranslatorOneArg<RunnableRef,Runnable> EVENT_TRANSLATOR = (event, sequence, runnable) -> event.pointer = runnable;

	/**
	 * Register a runnable instance for the workers to call at a later time
	 * @param runnable the work, work completed via the {@link Runnable::run}
	 */
	public void addWork(Runnable runnable) {
		ringBuffer.publishEvent(EVENT_TRANSLATOR,runnable);

	}

	public int getWorkerCount() {
		return workerCount;
	}

	/**
	 * Start the thread pool, if already started does nothing
	 */
	public void start() {
		if(!running.get()) {
			running.set(true);
			workerQueue.forEach(RunnableRefWorker::start);
		}
	}

	/**
	 * Stop the thread pool, if already stopped does nothing
	 */
	public void stop() {
		if(running.get()) {
			running.set(false);
			workerQueue.forEach(RunnableRefWorker::stop);
		}
	}

	/**
	 * Basically WorkProcessor w/ some minor tweaks
	 */
	private static class RunnableRefWorker implements Runnable{
		private AsyncRunnablePool this_pool;
		private Thread thread;
		private AtomicBoolean running = new AtomicBoolean(false);
		private SequenceBarrier barrier;
		private Sequence workSequence;
		private Sequence sequence = new Sequence(Sequencer.INITIAL_CURSOR_VALUE);;
		private RingBuffer<RunnableRef> ringBuffer;
		public RunnableRefWorker(AsyncRunnablePool pool) {
			this_pool = pool;
			workSequence = pool.sequence;
			barrier = pool.barrier;
			ringBuffer = pool.ringBuffer;
			thread = new Thread(this_pool.threadGroup,this,this_pool.threadGroup.getName() + " #Worker-"+this_pool.ID++);
			thread.setDaemon(true);
		}
		void start() {
			thread.start();
		}
		void stop() {
			running.set(false);
			barrier.alert();
		}
		@Override
		public void run() {
			if (!running.compareAndSet(false, true))
			{
				throw new IllegalStateException("Thread is already running");
			}
			barrier.clearAlert();
			boolean processedSequence = true;
			long cachedAvailableSequence = Long.MIN_VALUE;
			long nextSequence = sequence.get();
			RunnableRef event;
			while (true)
			{
				try
				{
					if (processedSequence)
					{
						processedSequence = false;
						do
						{
							nextSequence = workSequence.get() + 1L;
							sequence.set(nextSequence - 1L);
						}
						while (!workSequence.compareAndSet(nextSequence - 1L, nextSequence));
					}

					if (cachedAvailableSequence >= nextSequence)
					{
						event = ringBuffer.get(nextSequence);
						//HANDLE//
						try{
							event.pointer.run();
						}catch(Exception e) {
							e.printStackTrace();
						}
						//STOP HANDLE//
						processedSequence = true;
					}
					else
					{
						cachedAvailableSequence = barrier.waitFor(nextSequence);
					}
				}
				catch (final TimeoutException e)
				{
					//IGNORE//
				}
				catch (final AlertException ex)
				{
					if (!running.get())
					{
						break;
					}
				}
				catch (final Throwable ex)
				{
					// handle, mark as processed, unless the exception handler threw an exception
					Logger.getLogger("Worker Thread").StackTrace(ex);
					processedSequence = true;
				}
			}
			running.set(false);
		}
	}

}
