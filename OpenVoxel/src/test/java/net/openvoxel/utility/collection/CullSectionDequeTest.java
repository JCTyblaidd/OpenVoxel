package net.openvoxel.utility.collection;

import net.openvoxel.world.client.ClientChunk;
import net.openvoxel.world.client.ClientChunkSection;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CullSectionDequeTest {

	@Test
	void isEmpty() {
		CullSectionDeque testObj = new CullSectionDeque();
		assertTrue(testObj.isEmpty());
		testObj.addLast(1,2,3,4,5,null);
		assertFalse(testObj.isEmpty());
		assertEquals(1,testObj.getFirstX());
		assertEquals(2,testObj.getFirstY());
		assertEquals(3,testObj.getFirstZ());
		assertEquals(4,testObj.getFirstPrev());
		assertEquals(5,testObj.getFirstMask());
		assertNull(testObj.removeFirst());
		assertTrue(testObj.isEmpty());
	}

	@Test
	void addLast() {
		CullSectionDeque testObj = new CullSectionDeque();
		ClientChunk testChunk = new ClientChunk(0,0);
		ClientChunkSection[] testArray = new ClientChunkSection[7];
		for(int i = 0; i < 6; i++) {
			testArray[i] = testChunk.getSectionAt(i);
			assertNotNull(testArray[i]);
		}
		testArray[6] = null;
		for(int i = 0; i < 128; i++) {
			final int shiftI = i * 7;
			testObj.addLast(
					shiftI,
					shiftI+1,
					shiftI+2,
					shiftI+3,
					shiftI+4,
					testArray[i % 7]
			);
		}
		int removedCount = 0;
		while(!testObj.isEmpty()) {
			final int shiftI = removedCount * 7;
			assertEquals(shiftI,testObj.getFirstX());
			assertEquals(shiftI+1,testObj.getFirstY());
			assertEquals(shiftI+2,testObj.getFirstZ());
			assertEquals(shiftI+3,testObj.getFirstPrev());
			assertEquals(shiftI+4,testObj.getFirstMask());
			assertEquals(testArray[removedCount % 7],testObj.removeFirst());
			removedCount += 1;
		}
		assertEquals(128,removedCount);
	}

}