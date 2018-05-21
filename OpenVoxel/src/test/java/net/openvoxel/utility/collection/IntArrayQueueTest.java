package net.openvoxel.utility.collection;

import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class IntArrayQueueTest {

	@Test
	void random() {
		IntArrayQueue debugArray = new IntArrayQueue();
		Queue<Integer> compareArray = new ArrayDeque<>();
		Random rnd = new Random();
		for(int i = 0; i < 3000; i++) {
			boolean _add = rnd.nextBoolean();
			if(_add) {
				int val = rnd.nextInt();
				debugArray.add(val);
				compareArray.add(val);
			}else if(!compareArray.isEmpty()){
				assertFalse(debugArray.isEmpty());
				int res1 = debugArray.remove();
				int res2 = compareArray.remove();
				assertEquals(res2,res1);
			}
		}
		while(!compareArray.isEmpty()) {
			assertFalse(debugArray.isEmpty());
			int res = compareArray.remove();
			assertEquals(res,debugArray.remove());
		}
		assertTrue(debugArray.isEmpty());
	}

	@Test
	void add() {
		IntArrayQueue debugArray = new IntArrayQueue();
		assertTrue(debugArray.isEmpty());
		for(int i = 0; i < 10; i++) {
			debugArray.add(i * 3);
		}
		assertEquals(0,debugArray.remove());
		assertEquals(3,debugArray.remove());
		for(int i = 0; i < 100; i++) {
			debugArray.add(i);
		}
		for(int i = 2; i < 10; i++) {
			assertEquals(i*3,debugArray.remove());
		}
		for(int i = 0; i < 100; i++) {
			debugArray.add(i * 7);
		}
		for(int i = 0; i < 100; i++) {
			assertEquals(i,debugArray.remove());
		}
		debugArray.add(42);
		for(int i = 0; i < 100; i++) {
			assertEquals(i * 7,debugArray.remove());
		}
		assertEquals(42,debugArray.remove());
		assertTrue(debugArray.isEmpty());
	}

	@Test
	void remove() {
		IntArrayQueue debugArray = new IntArrayQueue();
		assertTrue(debugArray.isEmpty());
		for(int i = 0; i < 500; i++) {
			debugArray.add(i);
			assertFalse(debugArray.isEmpty());
		}
		for(int i = 0; i < 500; i++) {
			assertFalse(debugArray.isEmpty());
			assertEquals(i,debugArray.remove());
		}
		assertTrue(debugArray.isEmpty());
	}

	@Test
	void isEmpty() {
		IntArrayQueue debugArray = new IntArrayQueue();
		assertTrue(debugArray.isEmpty());
		debugArray.add(10);
		assertFalse(debugArray.isEmpty());
		debugArray.add(42);
		assertFalse(debugArray.isEmpty());
		assertEquals(10,debugArray.remove());
		assertEquals(42,debugArray.remove());
		assertTrue(debugArray.isEmpty());
	}

	@Test
	void clear() {
		IntArrayQueue debugArray = new IntArrayQueue();
		assertTrue(debugArray.isEmpty());
		debugArray.add(42);
		assertFalse(debugArray.isEmpty());
		debugArray.clear();
		assertTrue(debugArray.isEmpty());
	}
}