package net.openvoxel.utility.async;

import com.lmax.disruptor.*;
import net.openvoxel.utility.debug.UsageAnalyses;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class AsyncExecutorPool implements AsyncTaskPool{

	private static class TaskEvent {
		AsyncTaskPool.Task task;
	}

	private static class NamedThreadFactory implements ThreadFactory {
		private int countdown = 0;
		private String name;
		private NamedThreadFactory(String name) {
			this.name = name;
		}
		@Override
		public Thread newThread(@NotNull Runnable r) {
			Thread thread = new Thread(() -> {
				UsageAnalyses.SetThreadName(Thread.currentThread().getName());
				r.run();
			});
			thread.setDaemon(true);
			thread.setName(name + " #"+countdown);
			countdown += 1;
			return thread;
		}
	}

	private static class TaskHandler implements WorkHandler<TaskEvent> {
		private int threadID;
		private TaskHandler(int ID) {
			threadID = ID;
		}
		@Override
		public void onEvent(TaskEvent event) {
			event.task.execute(threadID);
			event.task = null;
		}
	}

	private final EventTranslatorOneArg<TaskEvent,Task> publishEvent;
	private final ExecutorService executorService;
	private final RingBuffer<TaskEvent> ringBuffer;
	private final WorkerPool<TaskEvent> workerPool;
	private final TaskHandler[] workHandlerList;

	AsyncExecutorPool(String name, int threadCount, int bufferSize) {
		publishEvent = (event,sequence,task) -> event.task = task;
		NamedThreadFactory threadFactory = new NamedThreadFactory(name);
		executorService = Executors.newFixedThreadPool(
				threadCount,
				threadFactory
		);
		ringBuffer = RingBuffer.createSingleProducer(
			TaskEvent::new,
			bufferSize,
			new PhasedBackoffWaitStrategy(
				1,
				10,
				TimeUnit.NANOSECONDS,
				new LiteBlockingWaitStrategy()
			)
		);
		workHandlerList = new TaskHandler[threadCount];
		for(int i = 0; i < threadCount; i++) {
			workHandlerList[i] = new TaskHandler(i);
		}
		workerPool = new WorkerPool<>(
			ringBuffer,
			ringBuffer.newBarrier(),
			new FatalExceptionHandler(),
			workHandlerList
		);
		ringBuffer.addGatingSequences(workerPool.getWorkerSequences());
	}

	@Override
	public void start() {
		workerPool.start(executorService);
	}

	@Override
	public void stop() {
		workerPool.halt();
	}

	@Override
	public void addWork(Task task) {
		ringBuffer.publishEvent(publishEvent,task);
	}

	@Override
	public int getWorkerCount() {
		return workHandlerList.length;
	}
}
