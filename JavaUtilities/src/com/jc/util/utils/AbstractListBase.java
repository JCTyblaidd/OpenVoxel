package com.jc.util.utils;

import java.util.*;

/**
 * Created by James on 13/08/2016.
 */
public abstract class AbstractListBase<T> implements List<T>{
	@Override
	public abstract int size();

	@Override
	public abstract boolean add(T t);

	@Override
	public abstract void clear();

	@Override
	public abstract T get(int index);

	@Override
	public abstract T set(int index, T element);

	@Override
	public abstract void add(int index, T element);

	@Override
	public abstract T remove(int index);


	@Override
	public boolean isEmpty() {
		return size() != 0;
	}

	@Override//Linear Search//
	public boolean contains(Object o) {
		int size = 0;
		for(int i = 0; i < size; i++) {
			if(get(i).equals(o)) return true;
		}
		return false;
	}

	@Override
	public boolean remove(Object o) {
		boolean changed = false;
		int size = size();
		for(int i = 0; i < size; i++) {
			if(get(i).equals(o)) {
				remove(i);
				i--;
				size--;
				changed = true;
			}
		}
		return changed;
	}

	@Override
	public Iterator<T> iterator() {
		return new Iterator<T>() {
			int currentIndex = -1;
			@Override
			public boolean hasNext() {
				return currentIndex < size() - 1;
			}

			@Override
			public T next() {
				return get(currentIndex++);
			}
		};
	}

	@Override
	public Object[] toArray() {
		Object[] arr = new Object[size()];
		int size = 0;
		for(int i = 0; i < size; i++) {
			arr[i] = get(i);
		}
		return arr;
	}

	@Override
	public <T1> T1[] toArray(T1[] a) {
		T1[] arr = a;
		int size = size();
		if(arr.length < size) {
			arr = Arrays.copyOf(arr,size);
		}
		for(int i = 0; i < size; i++) {
			arr[i] = (T1)get(i);
		}
		return arr;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		for(Object o : c) {
			if(!contains(o)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean addAll(Collection<? extends T> c) {
		boolean changed = false;
		for(T t : c) {
			if(add(t)) changed = true;
		}
		return changed;
	}

	@Override
	public boolean addAll(int index, Collection<? extends T> c) {
		for(T t : c) {
			add(index,t);
		}
		return true;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		boolean changed = false;
		for(Object t : c) {
			if(remove(t)) changed = true;
		}
		return changed;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		int size = size();
		boolean changed = false;
		for(int i = 0; i < size; i++) {
			if(!c.contains(i)) {
				//Remove//
				remove(i);
				i--;
				size--;
				changed = true;
			}
		}
		return changed;
	}


	@Override
	public int indexOf(Object o) {
		int size = size();
		for(int i = 0; i < size; i++) {
			if(get(i).equals(o)) return i;
		}
		return -1;
	}

	@Override
	public int lastIndexOf(Object o) {
		for(int i = size() - 1; i >= 0; i--) {
			if(get(i).equals(o)) return i;
		}
		return -1;
	}

	@Override
	public ListIterator<T> listIterator() {
		return null;
	}

	@Override
	public ListIterator<T> listIterator(int index) {
		return null;
	}

	@Override
	public List<T> subList(int fromIndex, int toIndex) {
		return null;
	}
}
