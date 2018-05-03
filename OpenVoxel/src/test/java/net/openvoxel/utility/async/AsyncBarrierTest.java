package net.openvoxel.utility.async;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static java.time.Duration.ofSeconds;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class AsyncBarrierTest {


	@Test
	@DisplayName("AsyncBarrier - Sync")
	void testAsyncBarrier_Synchronous() {
		assertTimeout(ofSeconds(1),() -> {
			AsyncBarrier barrier = new AsyncBarrier();
			barrier.reset(1);
			barrier.completeTask();
			barrier.awaitCompletion();
		});
	}

	@Test
	@DisplayName("AsyncBarrier - Async")
	void testAsyncBarrier_Asynchronous() {
		AsyncBarrier barrier = new AsyncBarrier();
		barrier.reset(1);
		Thread signalThread = new Thread(() -> assertDoesNotThrow(() -> {
			Thread.sleep(10);
			barrier.completeTask();
		}));
		signalThread.start();
		assertTimeoutPreemptively(ofSeconds(5), barrier::awaitCompletion);
	}

	@Test
	@DisplayName("AsyncBarrier - Multiple")
	void testAsyncBarrier_Multiple() {
		AsyncBarrier barrier = new AsyncBarrier();
		barrier.reset(50);
		for(int i = 0; i < 50; i++) {
			final int I = i;
			Thread signalThread = new Thread(() -> assertDoesNotThrow(() -> {
				int sleepTime = (I % 7)*10 + (I % 11)*50 + I;
				Thread.sleep(sleepTime);
				barrier.completeTask();
			}));
			signalThread.start();
		}
		assertTimeoutPreemptively(ofSeconds(5), barrier::awaitCompletion);
	}
}