package net.openvoxel.utility.async;

import org.jetbrains.annotations.TestOnly;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static java.time.Duration.ofSeconds;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class BatchEventSubmitterTest {

	@DisplayName("Batching Test: Single")
	@Test
	void testSingleBatching() {
		runAsyncCalcTestOn(AsyncTaskPool.createTaskPool("Unknown",4,8192),4,1);
	}

	@DisplayName("Batching Test: Multiple")
	@Test
	void testMultiBatching() {
		runAsyncCalcTestOn(AsyncTaskPool.createTaskPool("Unknown",8,8192),8,8);
	}

	@TestOnly
	private void runAsyncCalcTestOn(AsyncTaskPool pool, int threadSize,int batching) {
		AsyncBarrier barrier = new AsyncBarrier();
		AtomicInteger calc = new AtomicInteger(0);
		AtomicInteger calc2 = new AtomicInteger(0);
		final int TASK_SIZE = 3000;
		barrier.reset(TASK_SIZE);
		BatchEventSubmitter submit = new BatchEventSubmitter(pool,batching);

		assertEquals(pool.getWorkerCount(),threadSize);
		assertEquals(0,calc.get());
		pool.start();

		for(int i = 0; i < TASK_SIZE; i++) {
			final int I = i;
			submit.addWork((ignore) -> {
				calc2.addAndGet((2 * (I % 2)) - 1);
				calc.addAndGet(I);
				barrier.completeTask();
			});
		}
		submit.flushWork();

		final int expect = (TASK_SIZE * (TASK_SIZE - 1)) / 2;
		assertTimeoutPreemptively(ofSeconds(1), barrier::awaitCompletion);
		assertEquals(expect,calc.get());
		assertEquals(0,calc2.get());

		assertTimeoutPreemptively(ofSeconds(1), pool::stop);
	}
}