package net.openvoxel.utility.async;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static java.time.Duration.ofSeconds;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class AsyncBarrierTest {


	@Test
	@DisplayName("AsyncBarrier - sync")
	void testAsyncBarrier_Synchronous() {
		assertTimeoutPreemptively(ofSeconds(1),() -> {
			AsyncBarrier barrier = new AsyncBarrier();
			barrier.reset(1);
			barrier.addNewTasks(2);
			barrier.completeTask();
			barrier.completeTask();
			barrier.completeTask();
			barrier.awaitCompletion();
		});
	}

	@Test
	@DisplayName("AsyncBarrier - async")
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
	@DisplayName("AsyncBarrier - multiple")
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