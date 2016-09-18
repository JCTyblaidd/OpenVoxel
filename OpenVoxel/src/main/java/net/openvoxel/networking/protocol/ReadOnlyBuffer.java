package net.openvoxel.networking.protocol;

import com.jc.util.stream.utils.Producer;
import gnu.trove.map.TIntObjectMap;
import io.netty.buffer.ByteBuf;
import io.netty.util.CharsetUtil;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Created by James on 01/09/2016.
 */
public class ReadOnlyBuffer {

	private ByteBuf backing;

	public ReadOnlyBuffer() {
		backing = null;
	}

	public ReadOnlyBuffer setBuffer(ByteBuf buf) {
		backing = buf;
		return this;
	}

	public ByteBuf getBuffer() {
		return backing;
	}

	public boolean readBoolean() {
		return backing.readByte() != 0;
	}
	public byte readByte() {
		return backing.readByte();
	}
	public char readChar() {
		return backing.readChar();
	}
	public short readShort() {
		return backing.readShort();
	}
	public int readInt() {
		return backing.readInt();
	}
	public long readLong() {
		return backing.readLong();
	}
	public float readFloat() {
		return backing.readFloat();
	}
	public double readDouble() {
		return backing.readDouble();
	}

	public UUID readUUID() {
		return new UUID(backing.readLong(),backing.readLong());
	}

	public byte[] readBytes() {
		int len = backing.readInt();
		byte[] data = new byte[len];
		backing.readBytes(data);
		return data;
	}
	public byte[] readBytesFixedLen(int len) {
		byte[] data = new byte[len];
		backing.readBytes(len);
		return data;
	}
	public void readBytes(byte[] Bytes) {
		backing.readBytes(Bytes);
	}
	public void readBytes(byte[] data, int destIndex, int length) {
		backing.readBytes(data,destIndex,length);
	}
	public String readString(Charset charset) {
		byte[] data = readBytes();
		return new String(data,charset);
	}
	public String readString() {
		return readString(CharsetUtil.US_ASCII);
	}
	public String readUnicodeString() {
		return readString(CharsetUtil.UTF_16);
	}
	public short[] readShorts() {
		int len = backing.readInt();
		short[] array = new short[len];
		for(int i = 0; i < len; i++) {
			array[i] = backing.readShort();
		}
		return array;
	}
	public short[] readShortsFixedLen(int len) {
		short[] array = new short[len];
		for(int i = 0; i < len; i++) {
			array[i] = backing.readShort();
		}
		return array;
	}
	public void readShortsFixedLen(short[] array) {
		for(int i = 0; i < array.length; i++) {
			array[i] = backing.readShort();
		}
	}
	public void readShortsFixedLen(short[] array, int destIndex, int length) {
		for(int i = destIndex; i < destIndex+length; i++) {
			array[i] = backing.readShort();
		}
	}
	public int[] readInts() {
		int len = backing.readInt();
		int[] array = new int[len];
		for(int i = 0; i < len; i++) {
			array[i] = backing.readInt();
		}
		return array;
	}
	public int[] readIntsFixedLen(int len) {
		int[] array = new int[len];
		for(int i = 0; i < len; i++) {
			array[i] = backing.readInt();
		}
		return array;
	}
	public void readIntsFixedLen(int[] array) {
		for(int i = 0; i < array.length; i++) {
			array[i] = backing.readInt();
		}
	}
	public void readIntsFixedLen(int[] array, int destIndex, int length) {
		for(int i = destIndex; i < destIndex+length; i++) {
			array[i] = backing.readInt();
		}
	}
	public long[] readLongs() {
		int len = backing.readInt();
		long[] array = new long[len];
		for(int i = 0; i < len; i++) {
			array[i] = backing.readLong();
		}
		return array;
	}
	public long[] readLongsFixedLen(int len) {
		long[] array = new long[len];
		for(int i = 0; i < len; i++) {
			array[i] = backing.readLong();
		}
		return array;
	}
	public void readLongsFixedLen(long[] array) {
		for(int i = 0; i < array.length; i++) {
			array[i] = backing.readLong();
		}
	}
	public void readLongsFixedLen(long[] array, int destIndex, int length) {
		for(int i = destIndex; i < destIndex+length; i++) {
			array[i] = backing.readLong();
		}
	}
	public float[] readFloats() {
		int len = backing.readInt();
		float[] array = new float[len];
		for(int i = 0; i < len; i++) {
			array[i] = backing.readFloat();
		}
		return array;
	}
	public float[] readFloatsFixedLen(int len) {
		float[] array = new float[len];
		for(int i = 0; i < len; i++) {
			array[i] = backing.readFloat();
		}
		return array;
	}
	public void readFloatsFixedLen(float[] array) {
		for(int i = 0; i < array.length; i++) {
			array[i] = backing.readFloat();
		}
	}
	public void readFloatsFixedLen(float[] array, int destIndex, int length) {
		for(int i = destIndex; i < destIndex+length; i++) {
			array[i] = backing.readFloat();
		}
	}
	public double[] readDoubles() {
		int len = backing.readInt();
		double[] array = new double[len];
		for(int i = 0; i < len; i++) {
			array[i] = backing.readDouble();
		}
		return array;
	}
	public double[] readDoublesFixedLen(int len) {
		double[] array = new double[len];
		for(int i = 0; i < len; i++) {
			array[i] = backing.readDouble();
		}
		return array;
	}
	public void readDoublesFixedLen(double[] array) {
		for(int i = 0; i < array.length; i++) {
			array[i] = backing.readDouble();
		}
	}
	public void readDoublesFixedLen(double[] array, int destIndex, int length) {
		for(int i = destIndex; i < destIndex+length; i++) {
			array[i] = backing.readDouble();
		}
	}
	public boolean[] readBooleans() {
		int len = backing.readInt();
		boolean[] array = new boolean[len];
		for(int i = 0; i < len; i++) {
			array[i] = backing.readBoolean();
		}
		return array;
	}
	public boolean[] readBooleansFixedLen(int len) {
		boolean[] array = new boolean[len];
		for(int i = 0; i < len; i++) {
			array[i] = backing.readBoolean();
		}
		return array;
	}
	public void readBooleansFixedLen(boolean[] array) {
		for(int i = 0; i < array.length; i++) {
			array[i] = backing.readBoolean();
		}
	}
	public void readBooleansFixedLen(boolean[] array, int destIndex, int length) {
		for(int i = destIndex; i < destIndex+length; i++) {
			array[i] = backing.readBoolean();
		}
	}
	public boolean[] readBooleansCompressed() {
		int len = backing.readInt();
		int byteCount = (len+7)/8;//Ceil Div len / 8 Func
		boolean[] array = new boolean[len];
		for(int i = 0; i < byteCount; i++) {
			byte val = backing.readByte();
			for(int j = 0; j < 8; j++) {
				int Index = i * 8 + j;
				if(Index >= len) {
					break;
				}else{
					array[Index] = ((val >> j) & 1) == 1;
				}
			}
		}
		return array;
	}
	public boolean[] readBooleansCompressedFixedLen(int len) {
		int byteCount = (len+7)/8;//Ceil Div len / 8 Func
		boolean[] array = new boolean[len];
		for(int i = 0; i < byteCount; i++) {
			byte val = backing.readByte();
			for(int j = 0; j < 8; j++) {
				int Index = i * 8 + j;
				if(Index >= len) {
					break;
				}else{
					array[Index] = ((val >> j) & 1) == 1;
				}
			}
		}
		return array;
	}
	public String[] readStrings(Charset charset) {
		int len = backing.readInt();
		String[] arr = new String[len];
		for(int i = 0; i < len; i++) {
			arr[i] = readString(charset);
		}
		return arr;
	}
	public String[] readStrings() {
		return readStrings(CharsetUtil.US_ASCII);
	}
	public String[] readUnicodeStrings() {
		return readStrings(CharsetUtil.UTF_16);
	}

	public <E extends Enum<E>> E readEnum(Class<E> type) {
		return type.getEnumConstants()[backing.readInt()];
	}

	public <T> void readIterable(Producer<T> readFunc, Consumer<T> addFunc) {
		int len = backing.readInt();
		for(int i = 0; i < len; i++) {
			addFunc.accept(readFunc.create());
		}
	}
	public <T> void readIterable(Producer<T> readFunc, Collection<T> data) {
		int len = backing.readInt();
		for(int i = 0; i < len; i++) {
			data.add(readFunc.create());
		}
	}

	public <K,V> void readMap(Map<K,V> map, Producer<K> readKey, Producer<V> readVal) {
		int len = backing.readInt();
		for(int i = 0; i < len; i++) {
			K k = readKey.create();
			V v = readVal.create();
			map.put(k,v);
		}
	}

	public <V> void readIntMap(TIntObjectMap<V> map, Producer<V> read) {
		int len = backing.readInt();
		for(int i = 0; i < len; i++) {
			int k = backing.readInt();
			V v = read.create();
			map.put(k,v);
		}
	}

}
