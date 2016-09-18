package com.jc.util.utils;

/**
 * Created by James on 07/08/2016.
 */
public class IntegerGrid extends AbstractSpecialisedGrid<Integer> {

	private int[] arr = new int[1];

	@Override
	protected void _setCurrent(int index, Integer value) {
		arr[index] = value;
	}

	@Override
	protected Integer _getCurrent(int index) {
		return arr[index];
	}

	@Override
	protected void _replaceCurrent(Object new_array) {
		arr = (int[])new_array;
	}

	@Override
	protected Object _alloc(int size) {
		return new int[size];
	}

	@Override
	protected void _setAlloc(Object alloc, int index, Integer val) {
		((int[])alloc)[index] = val;
	}
}
