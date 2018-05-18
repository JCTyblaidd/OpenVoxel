package net.openvoxel.utility.collection.trove_extended;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TVec3LHashSetTest {

	@Test
	void add() {
		TVec3LHashSet testSet = new TVec3LHashSet();
		testSet.add(0,0,0);
		assertEquals(1,testSet.size());
		assertTrue(testSet.contains(0,0,0));
		assertFalse(testSet.contains(1,0,0));
		assertFalse(testSet.contains(0,1,0));
		assertFalse(testSet.contains(0,0,1));
		testSet.remove(0,0,0);
		assertFalse(testSet.contains(0,0,0));
		assertEquals(0,testSet.size());
		testSet.add(0,0,0);
		testSet.add(1,0,0);
		testSet.add(0,1,0);
		testSet.add(0,0,1);
		testSet.add(1,1,1);
		assertEquals(5,testSet.size());
		assertTrue(testSet.contains(0,0,0));
		assertTrue(testSet.contains(1,0,0));
		assertTrue(testSet.contains(0,1,0));
		assertTrue(testSet.contains(0,0,1));
		assertTrue(testSet.contains(1,1,1));
		testSet.remove(1,1,1);
		assertFalse(testSet.contains(1,1,1));
		testSet.clear();
		assertEquals(0,testSet.size());
		assertTrue(testSet.isEmpty());
		assertFalse(testSet.contains(0,0,0));
		assertFalse(testSet.contains(1,0,0));
		assertFalse(testSet.contains(0,1,0));
		assertFalse(testSet.contains(0,0,1));
		assertFalse(testSet.contains(1,1,1));
	}

	@Test
	void remove() {
		TVec3LHashSet testSet = new TVec3LHashSet(100);
		for(int i = 0; i < 100; i++) {
			testSet.add(i-5,i*2,i*6-32);
		}
		for(int i = 0; i < 100; i++) {
			assertTrue(testSet.contains(i-5,i*2,i*6-32));
		}
		assertEquals(100,testSet.size());
		for(int j = 0; j < 100; j++) {
			testSet.remove(j-5,j*2,j*6-32);
			for(int i = 0; i <= j; i++) {
				assertFalse(testSet.contains(i-5,i*2,i*6-32));
			}
			for(int i = j+1; i < 100; i++) {
				assertTrue(testSet.contains(i-5,i*2,i*6-32));
			}
			assertEquals(99-j,testSet.size());
		}
		assertEquals(0,testSet.size());
	}

	@Test
	void clear() {
		TVec3LHashSet testSet = new TVec3LHashSet(100);
		assertEquals(0,testSet.size());
		testSet.add(42,42,42);
		testSet.trimToSize();
		assertTrue(testSet.contains(42,42,42));
		assertEquals(1,testSet.size());
		testSet.clear();
		assertEquals(0,testSet.size());
		assertTrue(testSet.isEmpty());
		for(int i = 0; i < 100; i++) {
			testSet.add(i-5,i*2,i*6-32);
		}
		for(int i = 0; i < 100; i++) {
			assertTrue(testSet.contains(i-5,i*2,i*6-32));
		}
		testSet.clear();
		assertEquals(0,testSet.size());
		assertTrue(testSet.isEmpty());
		for(int i = 0; i < 100; i++) {
			assertFalse(testSet.contains(i-5,i*2,i*6-32));
		}
	}
}