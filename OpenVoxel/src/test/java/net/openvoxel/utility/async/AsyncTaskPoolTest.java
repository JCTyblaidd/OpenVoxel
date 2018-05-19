package net.openvoxel.utility.async;

import org.jetbrains.annotations.TestOnly;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;

import java.util.concurrent.atomic.AtomicInteger;

import static java.time.Duration.ofSeconds;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class AsyncTaskPoolTest {


	@RepeatedTest(8)
	@DisplayName("Calc : AsyncExecutor")
	void testAsyncExecutorCalc() {
		runAsyncCalcTestOn(new AsyncExecutorPool("Debug",8));
	}

	@TestOnly
	private void runAsyncCalcTestOn(AsyncTaskPool pool) {
		AsyncBarrier barrier = new AsyncBarrier();
		AtomicInteger calc = new AtomicInteger(0);
		AtomicInteger calc2 = new AtomicInteger(0);
		final int TASK_SIZE = 3000;
		barrier.reset(TASK_SIZE);

		assertEquals(pool.getWorkerCount(),8);
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
}