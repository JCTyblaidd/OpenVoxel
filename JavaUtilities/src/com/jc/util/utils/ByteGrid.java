package com.jc.util.utils;

/**
 * Created by James on 07/08/2016.
 */
public class ByteGrid extends AbstractSpecialisedGrid<Byte>{

	private byte[] arr = new byte[1];

	@Override
	protected void _setCurrent(int index, Byte value) {
		arr[index] = value;
	}

	@Override
	protected Byte _getCurrent(int index) {
		return arr[index];
	}

	@Override
	protected void _replaceCurrent(Object new_array) {
		arr = (byte[])new_array;
	}

	@Override
	protected Object _alloc(int size) {
		return new byte[size];
	}

	@Override
	protected void _setAlloc(Object alloc, int index, Byte val) {
		((byte[])alloc)[index] = val;
	}
}
