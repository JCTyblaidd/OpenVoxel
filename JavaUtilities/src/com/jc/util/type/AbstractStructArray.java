package com.jc.util.type;

import java.util.Iterator;

/**
 * Created by James on 30/08/2016.
 *
 * Not Thread Safe
 */
public abstract class AbstractStructArray<T extends AbstractStructArray.StructItem> implements Iterable<T>{


//Smart Initialization Code//
	public static Object INT_STRUCT;
	public static Object FLOAT_STRUCT;

	public static <E extends StructItem> AbstractStructArray<E> newStructArray(int Size, Class<E> type) {
		try {
			return new AbstractStructArray<E>(Size,type.newInstance()) {
			};
		} catch(Throwable e) {
			return null;
		}
	}

//IMPLEMENTATION//
	protected final byte[] Data;
	protected final int size;
	protected final int structSize;
	protected final T tempT;

	public static abstract class StructItem {
		protected AbstractStructArray<? extends StructItem> array;
		protected int currentOffset;
		void initHandle(AbstractStructArray<? extends StructItem> array) {
			this.array = array;
		}
		void setOffset(int offset) {
			currentOffset = offset;
		}
		protected abstract int byteSize();
	}

	protected AbstractStructArray(int count, T t) {
		int byteSize = t.byteSize();
		Data = new byte[count * byteSize];
		size = count;
		structSize = byteSize;
		tempT = t;
		tempT.initHandle(this);
	}

	@Override
	public Iterator<T> iterator() {
		return new Iterator<T>() {
			int index = -1;
			@Override
			public boolean hasNext() {
				return index!=size-1;
			}

			@Override
			public T next() {
				int v = structSize*(index++);
				tempT.setOffset(v);
				return tempT;
			}
		};
	}
	public final boolean readBoolAtLocation(int byteOffset) {
		return Data[byteOffset] != 0;
	}
	public final byte readByteAtLocation(int byteOffset) {
		return Data[byteOffset];
	}
	public final short readShortAtLocation(int byteOffset) {
		return (short)(Data[byteOffset] << 8| Data[byteOffset+1]);
	}
	public final int readIntAtLocation(int byteOffset) {
		return Data[byteOffset] << 24 | Data[byteOffset+1] << 16 | Data[byteOffset+2] << 8 | Data[byteOffset + 3];
	}
	public final long readLongAtLocation(int byteOffset) {
		return Data[byteOffset] << 56 | Data[byteOffset+1] << 48 | Data[byteOffset+2] << 40 | Data[byteOffset + 3] << 32
				| Data[byteOffset+4] << 24 | Data[byteOffset+5]  << 16 | Data[byteOffset+6] << 8 | Data[byteOffset+7];
	}
	public final float readFloatAtLocation(int byteOffset) {
		return Float.intBitsToFloat(readIntAtLocation(byteOffset));
	}
	public final double readDoubleAtLocation(int byteOffset) {
		return Double.longBitsToDouble(readLongAtLocation(byteOffset));
	}

	public final void writeBoolAtLocation(int byteOffset,boolean val) {
		Data[byteOffset] = val ? (byte)1 : (byte)0;
	}
	public final void writeByteAtLocation(int byteOffset,byte val) {
		Data[byteOffset] = val;
	}
	public final void writeShortAtLocation(int byteOffset,short val) {
		Data[byteOffset] = (byte)(val >> 8);
		Data[byteOffset+1] = (byte)(val);
	}
	public final void writeIntAtLocation(int byteOffset,int val) {
		Data[byteOffset]=(byte)(val >> 24);
		Data[byteOffset+1]=(byte)(val >> 16);
		Data[byteOffset+2]=(byte)(val >> 8);
		Data[byteOffset+3]=(byte)(val);
	}
	public final void writeLongAtLocation(int byteOffset, long val) {
		Data[byteOffset]=(byte)(val >> 56);
		Data[byteOffset+1]=(byte)(val >> 48);
		Data[byteOffset+2]=(byte)(val >> 40);
		Data[byteOffset+3]=(byte)(val >> 32);
		Data[byteOffset+4]=(byte)(val >> 24);
		Data[byteOffset+5]=(byte)(val >> 16);
		Data[byteOffset+6]=(byte)(val >> 8);
		Data[byteOffset+7]=(byte)(val);
	}
	public final void writeFloatAtLocation(int byteOffset, float val) {
		writeIntAtLocation(byteOffset,Float.floatToRawIntBits(val));
	}
	public final void writeDoubleAtLocation(int byteOffset, double val) {
		writeLongAtLocation(byteOffset,Double.doubleToRawLongBits(val));
	}
}
