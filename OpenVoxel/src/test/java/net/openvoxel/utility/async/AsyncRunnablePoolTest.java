package net.openvoxel.utility.async;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static java.time.Duration.ofSeconds;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class AsyncRunnablePoolTest {

	@Test
	@DisplayName("AsyncPool - async calculation")
	void test_Async_Calculation() {
		AsyncRunnablePool pool = new AsyncRunnablePool("Test Pool",8);
		AsyncBarrier barrier = new AsyncBarrier();
		AtomicInteger calc = new AtomicInteger(0);
		AtomicInteger calc2 = new AtomicInteger(0);
		barrier.reset(100);

		assertEquals(pool.getWorkerCount(),8);

		for(int i = 0; i < 100; i++) {
			final int I = i;
			pool.addWork(() -> {
				calc.addAndGet(I);
				calc2.addAndGet((2 * (I % 2)) - 1);
				barrier.completeTask();
			});
		}

		assertEquals(0,calc.get());
		pool.start();

		final int expect = (100 * 99) / 2;
		assertTimeoutPreemptively(ofSeconds(1), barrier::awaitCompletion);
		assertEquals(expect,calc.get());
		assertEquals(0,calc2.get());

		assertTimeoutPreemptively(ofSeconds(1), pool::stop);
	}

}