package net.openvoxel.utility.collection;

import java.util.Arrays;

public class IntArrayQueue {

	// array[read..write) has values
	private int[] array;
	private int read;
	private int write;

	public IntArrayQueue() {
		array = new int[16];

		read = 0;
		write = 0;
	}

	public void add(int val) {
		int next_write = inc(write);
		if(next_write == read) {
			grow();
			next_write = inc(write);
		}
		array[write] = val;
		write = next_write;
	}

	public int remove() {
		int next_read = inc(read);
		int val = array[read];
		read = next_read;
		return val;
	}


	public boolean isEmpty() {
		return read == write;
	}

	public void clear() {
		read = 0;
		write = 0;
	}

	private int inc(int val) {
		return (val + 1) % array.length;
	}

	private void grow() {
		int old_size = array.length;
		int new_size = old_size + (old_size / 2);
		int[] oldArray = array;
		array = Arrays.copyOf(array,new_size);
		if(read > write) {
			int delta_size = new_size - old_size;
			System.arraycopy(oldArray, read, array, read + delta_size, old_size - read);
			read += delta_size;
		}
	}
}
