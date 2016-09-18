package com.jc.util.config.util;

import com.jc.util.utils.AbstractListBase;

import java.util.List;

/**
 * Created by James on 13/08/2016.
 */
public class SubList<T> extends AbstractListBase<T>{

	private final List<T> val;
	private final int from;
	private final int to;


	public SubList(List<T> t, int from, int to) {
		val = t;
		this.from = from;
		this.to = to;
	}


	@Override
	public int size() {
		int s2 = val.size();
		if(from >= s2) return 0;
		int rMax = to > s2 ? s2 : to;
		return rMax - from;
	}

	@Override
	public boolean add(T t) {
		int s = size();
		if(s == 0) {
			return val.add(t);
		}else {
			val.add(from + s,t);
			return true;
		}
	}

	@Override
	public void clear() {

	}

	@Override
	public T get(int index) {
		if(size() != 0) {
			return get(index + from);
		}
		return null;
	}

	@Override
	public T set(int index, T element) {
		if(size() != 0) {
			return set(index + from,element);
		}
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
