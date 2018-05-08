package net.openvoxel.utility.async;

import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import net.openvoxel.OpenVoxel;
import net.openvoxel.api.PublicAPI;
import net.openvoxel.utility.CrashReport;
import net.openvoxel.utility.debug.UsageAnalyses;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class AsyncExecutorPool {

	private class ExecutionEvent {
		private Runnable target;
	}

	private final EventTranslatorOneArg<ExecutionEvent,Runnable> TRANSLATOR =
			(executionEvent, sequence, runnable) -> executionEvent.target = runnable;

	private final class ExecutionWorker implements WorkHandler<ExecutionEvent> { //, LifecycleAware {

		@Override
		public void onEvent(ExecutionEvent event) {
			event.target.run();
		}

		//@Override
		public void onStart() {
			UsageAnalyses.SetThreadName(Thread.currentThread().getName());
		}

		//@Override
		public void onShutdown() {
			//NO OP
		}
	}

	private static final ExceptionHandler<ExecutionEvent> EXCEPTION_HANDLER = new ExceptionHandler<>() {
		@Override
		public void handleEventException(Throwable ex, long sequence, ExecutionEvent event) {
			CrashReport crashReport = new CrashReport("Caught exception processing Runnable");
			crashReport.caughtException(ex);
			OpenVoxel.reportCrash(crashReport);
		}

		@Override
		public void handleOnStartException(Throwable ex) {
			CrashReport crashReport = new CrashReport("Caught exception starting AsyncExecutorPool");
			crashReport.caughtException(ex);
			OpenVoxel.reportCrash(crashReport);
		}

		@Override
		public void handleOnShutdownException(Throwable ex) {
			CrashReport crashReport = new CrashReport("Caught exception stopping AsyncExecutorPool");
			crashReport.caughtException(ex);
			OpenVoxel.reportCrash(crashReport);
		}
	};

	///////////////////////////
	/// Implementation Code ///
	///////////////////////////

	private final Disruptor<ExecutionEvent> disruptor;
	private final ThreadGroup threadGroup;
	private final int workerCount;

	//Naming Counter
	private int threadCount = 0;


	///////////////////
	/// API METHODS ///
	///////////////////

	@PublicAPI
	public static int getWorkerCount(String ID,int fallback,int minCount) {
		if(OpenVoxel.getLaunchParameters().hasFlag(ID)) {
			return Math.max(OpenVoxel.getLaunchParameters().getIntegerMap(ID),Math.max(1,minCount));
		}else {
			return fallback;
		}
	}

	@PublicAPI
	public AsyncExecutorPool(String name,int workerCount) {
		this(name,workerCount,1024);
	}

	@PublicAPI
	public AsyncExecutorPool(String name,int workerCount,int ringBufferSize) {
		this(name,workerCount,ringBufferSize,ProducerType.SINGLE);
	}

	@SuppressWarnings("unchecked")
	@PublicAPI
	public AsyncExecutorPool(String name,int workerCount,int ringBufferSize,ProducerType producerType) {
		threadGroup = new ThreadGroup(name);
		this.workerCount = workerCount;
		ThreadFactory threadFactory = runnable -> {
			String name1 = threadGroup.getName() + " #" + threadCount;
			threadCount += 1;
			Thread thread =  new Thread(
					threadGroup,
					runnable,
					name1
			);
			thread.setDaemon(true);
			return thread;
		};

		disruptor = new Disruptor<>(
			ExecutionEvent::new,
			ringBufferSize,
			threadFactory,//,threadFactory,
			producerType,
			new PhasedBackoffWaitStrategy(
				10,
				100,
				TimeUnit.NANOSECONDS,
				new BlockingWaitStrategy()
			)
		);
		disruptor.setDefaultExceptionHandler(EXCEPTION_HANDLER);

		ExecutionWorker[] handlerList = new ExecutionWorker[workerCount];
		for(int i = 0; i < workerCount; i++) {
			handlerList[i] = new ExecutionWorker();
		}
		disruptor.handleEventsWithWorkerPool(handlerList);
	}


	/**
	 * Add some work to the executor thread
	 */
	@PublicAPI
	public void addWork(Runnable runnable) {
		disruptor.publishEvent(TRANSLATOR,runnable);
	}

	/**
	 * @return The number of worker threads
	 */
	@PublicAPI
	public int getWorkerCount() {
		return workerCount;
	}

	/**
	 * Start the thread pool, if already started does nothing
	 */
	@PublicAPI
	public void start() {
		disruptor.start();
	}

	/**
	 * Stop the thread pool, if already stopped does nothing
	 */
	@PublicAPI
	public void stop() {
		disruptor.shutdown();
	}

}
