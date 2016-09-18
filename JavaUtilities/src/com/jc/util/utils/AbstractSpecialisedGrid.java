package com.jc.util.utils;

/**
 * Created by James on 07/08/2016.
 */
public abstract class AbstractSpecialisedGrid<T> implements Grid<T>{


	private int minX = 0;
	private int maxX = 0;
	private int minY = 0;
	private int maxY = 0;

	protected abstract void _setCurrent(int index, T value);
	protected abstract T _getCurrent(int index);
	protected abstract void _replaceCurrent(Object new_array);
	protected abstract Object _alloc(int size);
	protected abstract void _setAlloc(Object alloc, int index, T val);

	private void _ensureCapacity(int x, int y) {
		if(x >= minX && x <= maxX && y >= minY && y <= maxY) return;//Already OK//
		//OK We Need to Fix//
		int new_minX = x < minX ? x : minX;
		int new_maxX = x > maxX ? x : maxX;
		int new_minY = y < minY ? y : minY;
		int new_maxY = y > maxY ? y : maxY;
		Object new_data = _alloc( (new_maxX - new_minX + 1) * (new_maxY - new_minY + 1) );
		//Copy Data//
		for(int xV = minX; xV <= maxX; xV++) {
			int n_x_offset = (xV - new_minX) * (new_maxX - new_minX + 1);
			for(int yV = minY; yV <= maxY; yV ++) {
				int n_index = n_x_offset + (yV - new_minY);
				_setAlloc(new_data,n_index,  _getCurrent(_getIndexOf(x,y)));
			}
		}
		//Set Data//
		minX = new_minX;
		maxX = new_maxX;
		minY = new_minY;
		maxY = new_maxY;
		_replaceCurrent(new_data);
	}

	private int _getIndexOf(int x, int y) {
		int xDiff = x - minX;
		int yDiff = y - minY;
		return (xDiff * getWidth()) + yDiff;
	}

	@Override
	public T get(int x, int y) {
		_ensureCapacity(x,y);
		return _getCurrent(_getIndexOf(x,y));
	}

	@Override
	public void set(int x, int y, T t) {
		_ensureCapacity(x,y);
		_setCurrent(_getIndexOf(x,y),t);
	}

	@Override
	public void ensureCovers(int minX, int maxX, int minY, int maxY) {
		_ensureCapacity(minX,minY);
		_ensureCapacity(maxX,maxY);
	}

	@Override
	public int getWidth() {
		return maxX - minX + 1;
	}

	@Override
	public int getHeight() {
		return maxY - minY + 1;
	}

	@Override
	public int getMinX() {
		return minX;
	}

	@Override
	public int getMaxX() {
		return maxX;
	}

	@Override
	public int getMinY() {
		return minY;
	}

	@Override
	public int getMaxY() {
		return maxY;
	}
}
