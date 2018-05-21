package net.openvoxel.utility.collection;

import net.openvoxel.world.client.ClientChunkSection;

import java.util.Arrays;

/*
 * Implements a dedicated Dequeue
 *  for the effective class:
 *    ClientChunkSection
 *    int x
 *    int y
 *    int z
 *    int prevFace
 *    int directionMask
 *
 *
 *    Based off implementation of ArrayDeque
 */
public class CullSectionDeque {

	private ClientChunkSection[] sectionArray;
	private int[] intArray;

	private int head;
	private int tail;

	public CullSectionDeque() {
		sectionArray = new ClientChunkSection[16];
		intArray = new int[5*16];
		head = 0;
		tail = 0;
	}

	public boolean isEmpty() {
		return head == tail;
	}

	public void addLast(int x, int y, int z, int prev, int mask, ClientChunkSection ref) {
		final ClientChunkSection[] refArray = sectionArray;
		final int[] refIntArray = intArray;
		int shiftTail = tail * 5;

		refArray[tail] = ref;
		refIntArray[shiftTail] = x;
		refIntArray[shiftTail+1] = y;
		refIntArray[shiftTail+2] = z;
		refIntArray[shiftTail+3] = prev;
		refIntArray[shiftTail+4] = mask;

		if (head == (tail = inc(tail, refArray.length))) {
			grow(1);
		}
	}

	public ClientChunkSection removeFirst() {
		final int h = head;
		ClientChunkSection val = sectionArray[h];
		sectionArray[h] = null;
		head = inc(h, sectionArray.length);
		return val;
	}

	public int getFirstX() {
		return intArray[head*5];
	}

	public int getFirstY() {
		return intArray[head*5+1];
	}

	public int getFirstZ() {
		return intArray[head*5+2];
	}

	public int getFirstPrev() {
		return intArray[head*5+3];
	}

	public int getFirstMask() {
		return intArray[head*5+4];
	}

	///
	/// Private Methods
	///


	private void grow(int needed) {
		final int oldCapacity = sectionArray.length;
		int newCapacity;

		// Double capacity if small; else grow by 50%
		int jump = (oldCapacity < 64) ? (oldCapacity + 2) : (oldCapacity >> 1);
		newCapacity = (oldCapacity + jump);
		int newShiftCapacity = newCapacity * 5;

		//Allocate New Data
		final int[] newInts = intArray = Arrays.copyOf(intArray,newShiftCapacity);
		final ClientChunkSection[] newSections = sectionArray = Arrays.copyOf(sectionArray,newCapacity);

		// Exceptionally, here tail == head needs to be disambiguated
		if (tail < head || (tail == head && newSections[head] != null)) {
			// wrap around; slide first leg forward to end of array
			int newSpace = newCapacity - oldCapacity;
			System.arraycopy(newSections, head,
					newSections, head + newSpace,
					oldCapacity - head);
			int shiftHead = head * 5;
			int shiftNewSpace = newSpace * 5;
			System.arraycopy(newInts, shiftHead,
					newInts,shiftHead+shiftNewSpace,
					newShiftCapacity - shiftHead);
			for (int i = head, to = (head += newSpace); i < to; i++)
				newSections[i] = null;
		}
	}


	private static int inc(int i, int modulus) {
		if (++i >= modulus) i = 0;
		return i;
	}

	/**
	 * Decrements i, mod modulus.
	 * Precondition and postcondition: 0 <= i < modulus.
	 */
	private static int dec(int i, int modulus) {
		if (--i < 0) i = modulus - 1;
		return i;
	}
}
