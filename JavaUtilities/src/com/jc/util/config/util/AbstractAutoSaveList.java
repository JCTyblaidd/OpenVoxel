package com.jc.util.config.util;

import com.jc.util.config.IConfigEntry;
import com.jc.util.utils.AbstractListBase;

/**
 * Created by James on 13/08/2016.
 */
public class AbstractAutoSaveList<T> extends AbstractListBase<T> {

	private IConfigEntry entry;

	@Override
	public int size() {
		return entry.size();
	}

	@Override
	public boolean add(T t) {
		return false;
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public T get(int index) {
		return null;
	}

	@Override
	public T set(int index, T element) {
		return null;
	}

	@Override
	public void add(int index, T element) {

	}

	@Override
	public T remove(int index) {
		return null;
	}
}
