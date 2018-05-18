package net.openvoxel.utility.collection.trove_extended;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TVec2LObjectHashMapTest {

	@Test
	void containsKey() {
		TVec2LObjectHashMap<Integer> testMap = new TVec2LObjectHashMap<>();
		assertFalse(testMap.containsKey(0,0));
		testMap.put(0,0,44);
		assertTrue(testMap.containsKey(0,0));
		testMap.remove(0,0);
		assertFalse(testMap.containsKey(0,0));
		for(int i = 0; i < 100; i++) {
			testMap.put(i*3,i*4-3,i);
		}
		for(int i = 0; i < 100; i++) {
			assertTrue(testMap.containsKey(i*3,i*4-3));
		}
	}

	@Test
	void containsValue() {
		TVec2LObjectHashMap<Integer> testMap = new TVec2LObjectHashMap<>();
		assertFalse(testMap.containsValue(null));
		for(int i = 0; i < 100; i++) {
			testMap.put(i*3,i*4-3,i);
		}
		for(int i = 0; i < 100; i++) {
			assertTrue(testMap.containsValue(i));
		}
	}

	@Test
	void get() {
		TVec2LObjectHashMap<Integer> testMap = new TVec2LObjectHashMap<>();
		for(int i = 0; i < 100; i++) {
			testMap.put(i*3,i*4-3,i);
		}
		for(int i = 0; i < 100; i++) {
			assertEquals(i,(int)testMap.get(i*3,i*4-3));
		}
		assertNull(testMap.get(-100,-100));
	}

	@Test
	void put() {
		//TODO: IMPLEMENT
	}

	@Test
	void putIfAbsent() {
		//TODO: IMPLEMENT
	}

	@Test
	void remove() {
		TVec2LObjectHashMap<Integer> testMap = new TVec2LObjectHashMap<>();
		for(int i = 0; i < 100; i++) {
			testMap.put(i*3,i*4-3,i);
		}
		for(int i = 0; i < 100; i++) {
			assertTrue(testMap.containsKey(i*3,i*4-3));
		}
		for(int j = 0; j < 100; j++) {
			testMap.remove(j*3,j*4-3);
			for(int i = 0; i <= j; i++) {
				assertFalse(testMap.containsKey(j+3,j*4-3));
			}
			for(int i = j+1; i < 100; i++) {
				assertTrue(testMap.containsKey(i*3,i*4-3));
			}
		}
	}

	@Test
	void clear() {
		TVec2LObjectHashMap<Integer> testMap = new TVec2LObjectHashMap<>();
		for(int i = 0; i < 100; i++) {
			testMap.put(i*3,i*4-3,i);
		}
		testMap.clear();
		assertTrue(testMap.isEmpty());
		assertEquals(0,testMap.size());
	}

	@Test
	void forEachValue() {
		//TODO:
	}

	@Test
	void transformValues() {
		//TODO: IMPLEMENT
	}
}