package net.openvoxel.utility.collection;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IntDequeueTest {

	@Test
	void add() {
		IntDequeue queue = new IntDequeue();
		for(int i = 0; i < 100; i++) {
			queue.add(i);
		}
		for(int i = 0; i < 100; i++) {
			assertEquals(i,queue.remove());
		}
		assertTrue(queue.isEmpty());
	}

	@Test
	void remove() {
		IntDequeue queue = new IntDequeue();
		for(int i = 0; i < 100; i++) {
			queue.add(i);
		}
		for(int i = 0; i < 50; i++) {
			assertEquals(i,queue.remove());
		}
		for(int i = 0; i < 50; i++) {
			queue.add(i);
		}
		for(int i = 50; i < 100; i++) {
			assertEquals(i,queue.remove());
		}
		for(int i = 0; i < 50; i++) {
			assertEquals(i,queue.remove());
		}
	}

	@Test
	void isEmpty() {
		IntDequeue queue = new IntDequeue();
		assertTrue(queue.isEmpty());
		queue.add(0);
		assertFalse(queue.isEmpty());
		assertEquals(0,queue.remove());
		assertTrue(queue.isEmpty());
	}
}