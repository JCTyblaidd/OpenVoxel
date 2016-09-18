package com.jc.util.utils;

/**
 * Created by James on 06/08/2016.
 */
public interface Grid<T> {

	int getWidth();
	int getHeight();
	default int getSize()  {
		return getWidth() * getHeight();
	}
	int getMinX();
	int getMaxX();
	int getMinY();
	int getMaxY();

	T get(int x, int y);

	void set(int x, int y, T t);

	void ensureCovers(int minX, int maxX, int minY, int maxY);

}
