package net.openvoxel.utility.async;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static java.time.Duration.ofSeconds;
import static org.junit.jupiter.api.Assertions.*;

class AsyncQueueTest {

	@Test
	@DisplayName("AsyncQueue - Sync")
	void testAsyncQueue_Sync() {
		AsyncQueue<Integer> asyncQueue = new AsyncQueue<>(128,false);
		for(int i = 0; i < 100; i++) {
			asyncQueue.add(i);
		}
		assertTimeoutPreemptively(ofSeconds(1),() -> {
			for (int i = 0; i < 100; i++) {
				assertEquals(i,(int)asyncQueue.awaitNext());
			}
			assertTrue(asyncQueue.isEmpty());
		});
	}

	@Test
	@DisplayName("AsyncQueue - Async(1)")
	void testAsyncQueue_Async() {
		AsyncQueue<Integer> asyncQueue = new AsyncQueue<>(128, false);
		Thread thread = new Thread(() -> {
			for (int i = 0; i < 100; i++) {
				asyncQueue.add(i);
			}
		});
		thread.start();
		assertTimeoutPreemptively(ofSeconds(1), () -> {
			for (int i = 0; i < 100; i++) {
				assertEquals(i, (int) asyncQueue.awaitNext());
			}
			assertTrue(asyncQueue.isEmpty());
		});
	}
	@Test
	@DisplayName("AsyncQueue - Async(2)")
	void testAsyncQueue_Async2() {
		AsyncQueue<Integer> asyncQueue = new AsyncQueue<>(128,false);
		Thread thread = new Thread(() -> {
			for (int i = 0; i < 100; i++) {
				asyncQueue.add(i);
			}
		});
		thread.start();
		assertTimeoutPreemptively(ofSeconds(1),() -> {
			for (int i = 0; i < 100; i++) {
				Integer _int = null;
				while(_int == null) {
					_int = asyncQueue.attemptNext();
				}
				assertEquals(i,(int)_int);
			}
			assertTrue(asyncQueue.isEmpty());
		});
	}

	@Test
	@DisplayName("AsyncQueue - attemptNext")
	void testAsyncQueue_AttemptNext() {
		AsyncQueue<Integer> asyncQueue = new AsyncQueue<>(128,false);
		AsyncBarrier barrier = new AsyncBarrier();
		AtomicBoolean invalidResult = new AtomicBoolean(false);

		for(int i = 0; i < 100; i++) {
			asyncQueue.add(42);
		}

		barrier.reset(50);
		for(int i = 0; i < 50; i++) {
			Thread thread = new Thread(() -> {
				for (int k = 0; k < 4; k++) {
					Integer _val = null;
					while(_val == null) {
						_val = asyncQueue.attemptNext();
					}
					if(_val != 42) invalidResult.set(true);
				}
				barrier.completeTask();
			});
			thread.start();
		}

		for(int i = 0; i < 100; i++) {
			assertTimeoutPreemptively(ofSeconds(1),() -> asyncQueue.add(42));
		}
		assertTimeoutPreemptively(ofSeconds(1),barrier::awaitCompletion);
		assertFalse(invalidResult.get());
		assertTrue(asyncQueue.isEmpty());
	}

	@Test
	@DisplayName("AsyncQueue - awaitNext")
	void testAsyncQueue_AwaitNext() {
		AsyncQueue<Integer> asyncQueue = new AsyncQueue<>(128,false);
		AsyncBarrier barrier = new AsyncBarrier();
		AtomicBoolean invalidState = new AtomicBoolean(false);
		AtomicBoolean interruptException = new AtomicBoolean(false);

		for(int i = 0; i < 100; i++) {
			asyncQueue.add(42);
		}

		barrier.reset(50);
		for(int i = 0; i < 50; i++) {
			Thread thread = new Thread(() -> {
				for (int k = 0; k < 4; k++) {
					try {
						int _val = asyncQueue.awaitNext();
						if (_val != 42) invalidState.set(true);
					}catch(Exception ignored) {
						interruptException.set(true);
					}
				}
				barrier.completeTask();
			});
			thread.start();
		}

		for(int i = 0; i < 100; i++) {
			assertTimeoutPreemptively(ofSeconds(1),() -> asyncQueue.add(42));
		}
		assertTimeoutPreemptively(ofSeconds(1),barrier::awaitCompletion);
		assertFalse(invalidState.get());
		assertFalse(interruptException.get());
		assertTrue(asyncQueue.isEmpty());
	}


}