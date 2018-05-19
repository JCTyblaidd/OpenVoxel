package net.openvoxel.utility.async;

import org.jetbrains.annotations.TestOnly;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static java.time.Duration.ofSeconds;
import static org.junit.jupiter.api.Assertions.*;

class AsyncTaskPoolTest {

	@TestOnly
	private final int THREAD_COUNT = 8;

	@Test
	@DisplayName("Calc : AsyncExecutor")
	void testAsyncExecutorCalc() {
		runAsyncCalcTestOn(new AsyncExecutorPool("Debug",THREAD_COUNT,1024),THREAD_COUNT);
	}

	@Test
	@DisplayName("Calc : SyncExecutor")
	void testSyncExecutorCalc() {
		runAsyncCalcTestOn(new SyncExecutorPool(),1);
	}

	@Test
	@DisplayName("Validate : AsyncExecutor")
	void validateAsyncExecutor() {
		validateAsync(new AsyncExecutorPool("Debug",THREAD_COUNT,1024));
	}

	@TestOnly
	private void runAsyncCalcTestOn(AsyncTaskPool pool, int threadSize) {
		AsyncBarrier barrier = new AsyncBarrier();
		AtomicInteger calc = new AtomicInteger(0);
		AtomicInteger calc2 = new AtomicInteger(0);
		final int TASK_SIZE = 3000;
		barrier.reset(TASK_SIZE);

		assertEquals(pool.getWorkerCount(),threadSize);
		assertEquals(0,calc.get());
		pool.start();

		for(int i = 0; i < TASK_SIZE; i++) {
			final int I = i;
			pool.addWork((ignore) -> {
				calc.addAndGet(I);
				calc2.addAndGet((2 * (I % 2)) - 1);
				barrier.completeTask();
			});
		}

		final int expect = (TASK_SIZE * (TASK_SIZE - 1)) / 2;
		assertTimeoutPreemptively(ofSeconds(1), barrier::awaitCompletion);
		assertEquals(expect,calc.get());
		assertEquals(0,calc2.get());

		assertTimeoutPreemptively(ofSeconds(1), pool::stop);
	}

	@TestOnly
	private void validateAsync(AsyncTaskPool pool) {
		AsyncBarrier barrier = new AsyncBarrier();
		AtomicBoolean valid = new AtomicBoolean(true);
		final int WORK_COUNT = THREAD_COUNT*10;

		pool.start();
		barrier.reset(WORK_COUNT);

		for(int i = 0; i < WORK_COUNT; i++) {
			pool.addWork(asyncID -> {
				String name = Thread.currentThread().getName();
				int lastIndex = name.lastIndexOf('#');
				int threadID = Integer.parseInt(name.substring(lastIndex+1));
				if(threadID != asyncID) {
					valid.set(false);
				}
				barrier.completeTask();
			});
		}

		assertTimeoutPreemptively(ofSeconds(1), barrier::awaitCompletion);
		assertTrue(valid.get());
		assertTimeoutPreemptively(ofSeconds(1), pool::stop);
	}
}