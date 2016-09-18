package com.jc.util.type;

import java.io.Serializable;
import java.util.*;
import java.util.function.Consumer;

/**
 * Created by James on 29/08/2016.
 *
 * Primitive Wrapped Version of ArrayList:
 * More Memory Efficient than ArrayList<E>
 *
 * However is Only Faster Than ArrayList<E> with large memory samples(where both are behind the system array by a while)
 * And Can Be Significantly Slower Than ArrayList<E> with smaller sections of memory
 */
public class PrimitiveList<E> extends AbstractList<E> implements List<E>, RandomAccess, Cloneable, Serializable {

	private final Primitive<E,Object> primitive;
	private Object array;
	private int allocSize;
	private int objSize;

	private static final int DEFAULT_CAPACITY = 10;

	private static final long serialVersionUID = 8683252581022892089L;//Random//

	public <ARR> PrimitiveList(Primitive<E,ARR> type) {
		primitive = (Primitive<E,Object>)type;//for type-sake
		array = primitive.getEmptyArray();
		allocSize = 0;
		objSize = 0;
	}

	public <ARR> PrimitiveList(Primitive<E,ARR> type, int alloc) {
		primitive = (Primitive<E,Object>)type;//for type-sake
		if (alloc > 0) {
			array = primitive.newArray(alloc);
		} else if(alloc == 0) {
			array = primitive.getEmptyArray();
		} else {
			throw new IllegalArgumentException("Illegal Capacity: "+
					                                   alloc);
		}
		allocSize = alloc;
		objSize = 0;
	}

	public void trimToSize() {
		modCount++;
		if (objSize < allocSize) {
			if(objSize == 0) {
				array = primitive.getEmptyArray();
				objSize = 0;
				allocSize = 0;
			}else{
				array = (objSize == 0) ? primitive.getEmptyArray() : primitive.copyOf(array,objSize);
				allocSize = objSize;
			}
		}
	}

	public void ensureCapacity(int minCapacity) {
		int minExpand = (array != null) ? 0 : DEFAULT_CAPACITY;
		if (minCapacity > minExpand) {
			ensureExplicitCapacity(minCapacity);
		}
	}

	private void ensureCapacityInternal(int minCapacity) {
		if (array == null) {
			minCapacity = Math.max(DEFAULT_CAPACITY, minCapacity);
		}

		ensureExplicitCapacity(minCapacity);
	}

	private void ensureExplicitCapacity(int minCapacity) {
		modCount++;

		// overflow-conscious code
		if (minCapacity - allocSize > 0)
			grow(minCapacity);
	}

	private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

	private void grow(int minCapacity) {
		// overflow-conscious code
		int newCapacity = allocSize + (allocSize >> 1);
		if (newCapacity - minCapacity < 0)
			newCapacity = minCapacity;
		if (newCapacity - MAX_ARRAY_SIZE > 0)
			newCapacity = hugeCapacity(minCapacity);
		// minCapacity is usually close to size, so this is a win:
		array = primitive.copyOf(array, newCapacity);
	}

	private static int hugeCapacity(int minCapacity) {
		if (minCapacity < 0) // overflow
			throw new OutOfMemoryError();
		return (minCapacity > MAX_ARRAY_SIZE) ?
				       Integer.MAX_VALUE :
				       MAX_ARRAY_SIZE;
	}

	public int size() {
		return objSize;
	}

	public boolean isEmpty() {
		return objSize == 0;
	}

	public boolean contains(Object o) {
		return indexOf(o) >= 0;
	}

	public int indexOf(Object o) {
		if (o != null) {
			for (int i = 0; i < objSize; i++) {
				if (o.equals(primitive.get(array, i))) {
					return i;
				}
			}
		}
		return -1;
	}

	public int lastIndexOf(Object o) {
		if (o != null) {
			for (int i = objSize-1; i >= 0; i--) {
				if (o.equals(primitive.get(array,i))) {
					return i;
				}
			}
		}
		return -1;
	}

	public Object clone() {
		try {
			PrimitiveList<?> v = (PrimitiveList<?>) super.clone();
			v.array = primitive.copyOf(array,objSize);
			v.modCount = 0;
			return v;
		} catch (CloneNotSupportedException e) {
			// this shouldn't happen, since we are Cloneable
			throw new InternalError(e);
		}
	}

	public Object[] toArray() {
		Object[] obj = new Object[objSize];
		for(int i = 0; i < objSize; i++) {//Must Be Converted//
			obj[i] = primitive.get(array,i);
		}
		return obj;
	}

	public Object toPrimitiveArray() {
		return primitive.copyOf(array,objSize);
	}

	public <T> T[] toArray(T[] a) {
		throw new UnsupportedOperationException("Primitive Array Doesn't Support toArray(T2[] a)");
	}

	E elementData(int index) {
		return primitive.get(array,index);
	}

	@Override
	public E get(int index) {
		rangeCheck(index);
		return elementData(index);
	}

	public E set(int index, E element) {
		rangeCheck(index);

		E oldValue = elementData(index);
		primitive.set(array,index,element);
		return oldValue;
	}

	public boolean add(E e) {
		ensureCapacityInternal(objSize + 1);  // Increments modCount!!
		primitive.set(array,objSize++,e);
		return true;
	}

	public void add(int index, E element) {
		rangeCheckForAdd(index);

		ensureCapacityInternal(objSize + 1);  // Increments modCount!!
		System.arraycopy(array, index, array, index + 1, objSize - index);
		primitive.set(array,index,element);
		objSize++;
	}

	public E remove(int index) {
		rangeCheck(index);

		modCount++;
		E oldValue = elementData(index);

		int numMoved = objSize - index - 1;
		if (numMoved > 0) {
			System.arraycopy(array, index + 1, array, index, numMoved);
		}
		primitive.set(array,--objSize,primitive.getZero()); // Zero
		return oldValue;
	}

	public boolean remove(Object o) {
		if (o != null) {
			for (int index = 0; index < objSize; index++) {
				if (o.equals(primitive.get(array,index))) {
					fastRemove(index);
					return true;
				}
			}
		}
		return false;
	}

	private void fastRemove(int index) {
		modCount++;
		int numMoved = objSize - index - 1;
		if (numMoved > 0) {
			System.arraycopy(array, index + 1, array, index, numMoved);
		}
		primitive.set(array,--objSize,primitive.getZero());// Zero
	}

	public void clear() {
		modCount++;

		//Zero
		E zero = primitive.getZero();
		for (int i = 0; i < objSize; i++) {
			primitive.set(array,i,zero);
		}
		objSize = 0;
	}

	public boolean addAll(Collection<? extends E> c) {
		Object[] a = c.toArray();
		int numNew = a.length;
		ensureCapacityInternal(objSize + numNew);  // Increments modCount
		//System.arraycopy(a, 0, array, objSize, numNew);Due To Primitive: This is Invalid//
		for(int offset = 0; offset < numNew; offset++) {
			primitive.set(array,objSize+offset,(E)a[offset]);
		}
		objSize += numNew;
		return numNew != 0;
	}

	public boolean addAll(int index, Collection<? extends E> c) {
		rangeCheckForAdd(index);

		Object[] a = c.toArray();
		int numNew = a.length;
		ensureCapacityInternal(objSize + numNew);  // Increments modCount

		int numMoved = objSize - index;
		if (numMoved > 0) {
			System.arraycopy(array, index, array, index + numNew,
					numMoved);
		}

		//System.arraycopy(a, 0, elementData, index, numNew);Due to Primitive: This is Invalid//
		for(int offset = 0; offset < numNew; offset++) {
			primitive.set(array,index+offset,(E)a[offset]);
		}
		objSize += numNew;
		return numNew != 0;
	}

	protected void removeRange(int fromIndex, int toIndex) {
		modCount++;
		int numMoved = objSize - toIndex;
		System.arraycopy(array, toIndex, array, fromIndex,
				numMoved);
		//Zero
		E zero = primitive.getZero();
		int newSize = objSize - (toIndex-fromIndex);
		for (int i = newSize; i < objSize; i++) {
			//elementData[i] = null;
			primitive.set(array,i,zero);
		}
		objSize = newSize;
	}
	private void rangeCheck(int index) {
		if (index >= objSize)
			throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
	}

	private void rangeCheckForAdd(int index) {
		if (index > objSize || index < 0)
			throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
	}

	private String outOfBoundsMsg(int index) {
		return "Index: "+index+", Size: "+objSize;
	}

	public boolean removeAll(Collection<?> c) {
		Objects.requireNonNull(c);
		return batchRemove(c, false);
	}

	public boolean retainAll(Collection<?> c) {
		Objects.requireNonNull(c);
		return batchRemove(c, true);
	}

	private boolean batchRemove(Collection<?> c, boolean complement) {
		//final Object[] elementData = this.elementData;
		final Object array = this.array;
		int r = 0, w = 0;
		boolean modified = false;
		try {
			for (; r < objSize; r++)
				if (c.contains(primitive.get(array,r)) == complement)
					primitive.set(array,w++,primitive.get(array,r));
		} finally {
			// Preserve behavioral compatibility with AbstractCollection,
			// even if c.contains() throws.
			if (r != objSize) {
				System.arraycopy(array, r, array, w, objSize - r);
				w += objSize - r;
			}
			if (w != objSize) {
				//Zero
				E zero = primitive.getZero();
				for (int i = w; i < objSize; i++) {
					primitive.set(array,i,zero);
				}
				modCount += objSize - w;
				objSize = w;
				modified = true;
			}
		}
		return modified;
	}

	// TODO: 29/08/2016 Copy And Handle writeObject, readObject

	public ListIterator<E> listIterator(int index) {
		if (index < 0 || index > objSize)
			throw new IndexOutOfBoundsException("Index: "+index);
		return new ListItr(index);
	}

	public ListIterator<E> listIterator() {
		return new ListItr(0);
	}

	public Iterator<E> iterator() {
		return new Itr();
	}

	private class Itr implements Iterator<E> {
		int cursor;       // index of next element to return
		int lastRet = -1; // index of last element returned; -1 if no such
		int expectedModCount = modCount;

		public boolean hasNext() {
			return cursor != objSize;
		}

		@SuppressWarnings("unchecked")
		public E next() {
			checkForComodification();
			int i = cursor;
			if (i >= objSize) {
				throw new NoSuchElementException();
			}
			Object array = PrimitiveList.this.array;
			if (i >= primitive.length(array))
				throw new ConcurrentModificationException();
			cursor = i + 1;
			return primitive.get(array,lastRet = i);
		}

		public void remove() {
			if (lastRet < 0)
				throw new IllegalStateException();
			checkForComodification();

			try {
				PrimitiveList.this.remove(lastRet);
				cursor = lastRet;
				lastRet = -1;
				expectedModCount = modCount;
			} catch (IndexOutOfBoundsException ex) {
				throw new ConcurrentModificationException();
			}
		}

		@Override
		@SuppressWarnings("unchecked")
		public void forEachRemaining(Consumer<? super E> consumer) {
			Objects.requireNonNull(consumer);
			final int size = PrimitiveList.this.objSize;
			int i = cursor;
			if (i >= size) {
				return;
			}
			final Object array = PrimitiveList.this.array;
			if (i >= primitive.length(array)) {
				throw new ConcurrentModificationException();
			}
			while (i != size && modCount == expectedModCount) {
				consumer.accept(primitive.get(array,i++));
			}
			// update once at end of iteration to reduce heap write traffic
			cursor = i;
			lastRet = i - 1;
			checkForComodification();
		}

		final void checkForComodification() {
			if (modCount != expectedModCount)
				throw new ConcurrentModificationException();
		}
	}

	private class ListItr extends Itr implements ListIterator<E> {
		ListItr(int index) {
			super();
			cursor = index;
		}

		public boolean hasPrevious() {
			return cursor != 0;
		}

		public int nextIndex() {
			return cursor;
		}

		public int previousIndex() {
			return cursor - 1;
		}

		@SuppressWarnings("unchecked")
		public E previous() {
			checkForComodification();
			int i = cursor - 1;
			if (i < 0) {
				throw new NoSuchElementException();
			}
			Object array = PrimitiveList.this.array;
			if (i >= primitive.length(array)) {
				throw new ConcurrentModificationException();
			}
			cursor = i;
			return primitive.get(array,lastRet = i);
		}

		public void set(E e) {
			if (lastRet < 0)
				throw new IllegalStateException();
			checkForComodification();

			try {
				PrimitiveList.this.set(lastRet, e);
			} catch (IndexOutOfBoundsException ex) {
				throw new ConcurrentModificationException();
			}
		}

		public void add(E e) {
			checkForComodification();

			try {
				int i = cursor;
				PrimitiveList.this.add(i, e);
				cursor = i + 1;
				lastRet = -1;
				expectedModCount = modCount;
			} catch (IndexOutOfBoundsException ex) {
				throw new ConcurrentModificationException();
			}
		}
	}

	public List<E> subList(int fromIndex, int toIndex) {
		subListRangeCheck(fromIndex, toIndex, objSize);
		return new SubList(this, 0, fromIndex, toIndex);
	}

	static void subListRangeCheck(int fromIndex, int toIndex, int size) {
		if (fromIndex < 0)
			throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);
		if (toIndex > size)
			throw new IndexOutOfBoundsException("toIndex = " + toIndex);
		if (fromIndex > toIndex)
			throw new IllegalArgumentException("fromIndex(" + fromIndex +
					                                   ") > toIndex(" + toIndex + ")");
	}

	private class SubList extends AbstractList<E> implements RandomAccess {
		private final PrimitiveList<E> parent;
		private final int parentOffset;
		private final int offset;
		int size;


		SubList(PrimitiveList<E> parent,
		        int offset, int fromIndex, int toIndex) {
			this.parent = parent;
			this.parentOffset = fromIndex;
			this.offset = offset + fromIndex;
			this.size = toIndex - fromIndex;
			this.modCount = PrimitiveList.this.modCount;
		}

		public E set(int index, E e) {
			rangeCheck(index);
			checkForComodification();
			E oldValue = PrimitiveList.this.elementData(offset + index);
			primitive.set(array,offset+index,e);
			return oldValue;
		}

		public E get(int index) {
			rangeCheck(index);
			checkForComodification();
			return PrimitiveList.this.elementData(offset + index);
		}

		public int size() {
			checkForComodification();
			return this.size;
		}

		public void add(int index, E e) {
			rangeCheckForAdd(index);
			checkForComodification();
			parent.add(parentOffset + index, e);
			this.modCount = parent.modCount;
			this.size++;
		}

		public E remove(int index) {
			rangeCheck(index);
			checkForComodification();
			E result = parent.remove(parentOffset + index);
			this.modCount = parent.modCount;
			this.size--;
			return result;
		}

		protected void removeRange(int fromIndex, int toIndex) {
			checkForComodification();
			parent.removeRange(parentOffset + fromIndex,
					parentOffset + toIndex);
			this.modCount = parent.modCount;
			this.size -= toIndex - fromIndex;
		}

		public boolean addAll(Collection<? extends E> c) {
			return addAll(this.size, c);
		}

		public boolean addAll(int index, Collection<? extends E> c) {
			rangeCheckForAdd(index);
			int cSize = c.size();
			if (cSize==0)
				return false;

			checkForComodification();
			parent.addAll(parentOffset + index, c);
			this.modCount = parent.modCount;
			this.size += cSize;
			return true;
		}

		public Iterator<E> iterator() {
			return listIterator();
		}

		public ListIterator<E> listIterator(final int index) {
			checkForComodification();
			rangeCheckForAdd(index);
			final int offset = this.offset;

			return new ListIterator<E>() {
				int cursor = index;
				int lastRet = -1;
				int expectedModCount = PrimitiveList.this.modCount;

				public boolean hasNext() {
					return cursor != PrimitiveList.SubList.this.size;
				}

				@SuppressWarnings("unchecked")
				public E next() {
					checkForComodification();
					int i = cursor;
					if (i >= PrimitiveList.SubList.this.size)
						throw new NoSuchElementException();
					Object array = PrimitiveList.this.array;
					if (offset + i >= primitive.length(array)) {
						throw new ConcurrentModificationException();
					}
					cursor = i + 1;
					return primitive.get(array,offset+(lastRet = i));
				}

				public boolean hasPrevious() {
					return cursor != 0;
				}

				@SuppressWarnings("unchecked")
				public E previous() {
					checkForComodification();
					int i = cursor - 1;
					if (i < 0)
						throw new NoSuchElementException();
					Object array = PrimitiveList.this.array;
					if (offset + i >= primitive.length(array)) {
						throw new ConcurrentModificationException();
					}
					cursor = i;
					return primitive.get(array,offset+(lastRet = i));
				}

				@SuppressWarnings("unchecked")
				public void forEachRemaining(Consumer<? super E> consumer) {
					Objects.requireNonNull(consumer);
					final int size = PrimitiveList.SubList.this.size;
					int i = cursor;
					if (i >= size) {
						return;
					}
					final Object array = PrimitiveList.this.array;
					if (offset + i >= primitive.length(array)) {
						throw new ConcurrentModificationException();
					}
					while (i != size && modCount == expectedModCount) {
						consumer.accept(primitive.get(array,offset+(i++)));
					}
					// update once at end of iteration to reduce heap write traffic
					lastRet = cursor = i;
					checkForComodification();
				}

				public int nextIndex() {
					return cursor;
				}

				public int previousIndex() {
					return cursor - 1;
				}

				public void remove() {
					if (lastRet < 0)
						throw new IllegalStateException();
					checkForComodification();

					try {
						PrimitiveList.SubList.this.remove(lastRet);
						cursor = lastRet;
						lastRet = -1;
						expectedModCount = PrimitiveList.this.modCount;
					} catch (IndexOutOfBoundsException ex) {
						throw new ConcurrentModificationException();
					}
				}

				public void set(E e) {
					if (lastRet < 0)
						throw new IllegalStateException();
					checkForComodification();

					try {
						PrimitiveList.this.set(offset + lastRet, e);
					} catch (IndexOutOfBoundsException ex) {
						throw new ConcurrentModificationException();
					}
				}

				public void add(E e) {
					checkForComodification();

					try {
						int i = cursor;
						PrimitiveList.SubList.this.add(i, e);
						cursor = i + 1;
						lastRet = -1;
						expectedModCount = PrimitiveList.this.modCount;
					} catch (IndexOutOfBoundsException ex) {
						throw new ConcurrentModificationException();
					}
				}

				final void checkForComodification() {
					if (expectedModCount != PrimitiveList.this.modCount)
						throw new ConcurrentModificationException();
				}
			};
		}

		public List<E> subList(int fromIndex, int toIndex) {
			subListRangeCheck(fromIndex, toIndex, size);
			return new PrimitiveList.SubList(PrimitiveList.this, offset, fromIndex, toIndex);
		}

		private void rangeCheck(int index) {
			if (index < 0 || index >= this.size)
				throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
		}

		private void rangeCheckForAdd(int index) {
			if (index < 0 || index > this.size)
				throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
		}

		private String outOfBoundsMsg(int index) {
			return "Index: "+index+", Size: "+this.size;
		}

		private void checkForComodification() {
			if (PrimitiveList.this.modCount != this.modCount)
				throw new ConcurrentModificationException();
		}

		public Spliterator<E> spliterator() {
			checkForComodification();
			//return new ArrayList.ArrayListSpliterator<E>(PrimitiveList.this, offset,
			//		                                            offset + this.size, this.modCount);
			return super.spliterator();//TODO: spliterator
		}
	}


	@Override
	public void forEach(Consumer<? super E> action) {
		Objects.requireNonNull(action);
		final int expectedModCount = modCount;
		@SuppressWarnings("unchecked")
		//final E[] elementData = (E[]) this.elementData;
		final Object array = this.array;
		final int size = this.objSize;
		for (int i=0; modCount == expectedModCount && i < size; i++) {
			action.accept(primitive.get(array,i));
		}
		if (modCount != expectedModCount) {
			throw new ConcurrentModificationException();
		}
	}

	// TODO: 29/08/2016 ArrayListSpliterator
}
