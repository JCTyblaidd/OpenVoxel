package net.openvoxel.networking.protocol;

import gnu.trove.map.TIntObjectMap;
import io.netty.buffer.ByteBuf;
import io.netty.util.CharsetUtil;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Created by James on 01/09/2016.
 *
 * Compatible with ReadOnlyBuffer
 *
 * Wraps A ByteBuffer
 *
 */
public class WriteOnlyBuffer {

	private ByteBuf backing;

	public WriteOnlyBuffer() {
		backing = null;
	}
	public WriteOnlyBuffer setBuffer(ByteBuf buf) {
		backing = buf;
		return this;
	}

	public ByteBuf getBuffer() {
		return backing;
	}

	public void writeBoolean(boolean val) {
		backing.writeByte(val ? 1 : 0);
	}
	public void writeByte(byte val) {
		backing.writeByte(val);
	}
	public void writeChar(char val) {
		backing.writeChar(val);
	}
	public void writeShort(short val) {
		backing.writeShort(val);
	}
	public void writeInt(int val) {
		backing.writeInt(val);
	}
	public void writeLong(long val) {
		backing.writeLong(val);
	}
	public void writeFloat(float val) {
		backing.writeFloat(val);
	}
	public void writeDouble(double val) {
		backing.writeDouble(val);
	}

	public void writeUUID(UUID uuid) {
		backing.writeLong(uuid.getMostSignificantBits());
		backing.writeLong(uuid.getLeastSignificantBits());
	}

	//ARRAY//
	public void writeBytes(byte[] val) {
		backing.writeInt(val.length);
		backing.writeBytes(val);
	}
	public void writeBytesFixedLen(byte[] val) {
		backing.writeBytes(val);
	}
	public void writeBytesFixedLen(byte[] val, int offset, int len) {
		backing.writeBytes(val,offset,len);
	}
	public void writeString(String val,Charset charset) {
		writeBytes(val.getBytes(charset));
	}
	public void writeString(String val) {
		writeString(val,CharsetUtil.US_ASCII);
	}
	public void writeUnicodeString(String val) {
		writeString(val,CharsetUtil.UTF_16);
	}
	public void writeShorts(short[] val) {
		backing.writeInt(val.length);
		for(int i = 0; i < val.length; i++) {
			backing.writeShort(val[i]);
		}
	}
	public void writeShortsFixedLen(short[] val) {
		for(int i = 0; i < val.length; i++) {
			backing.writeShort(val[i]);
		}
	}
	public void writeInts(int[] val) {
		backing.writeInt(val.length);
		for(int i = 0; i < val.length; i++) {
			backing.writeInt(val[i]);
		}
	}
	public void writeIntsFixedLen(int[] val) {
		for(int i = 0; i < val.length; i++) {
			backing.writeInt(val[i]);
		}
	}
	public void writeLongs(long[] val) {
		backing.writeInt(val.length);
		for(int i = 0; i < val.length; i++) {
			backing.writeLong(val[i]);
		}
	}
	public void writeLongsFixedLen(long[] val) {
		for(int i = 0; i < val.length; i++) {
			backing.writeLong(val[i]);
		}
	}
	public void writeFloats(float[] val) {
		backing.writeInt(val.length);
		for(int i = 0; i < val.length; i++) {
			backing.writeFloat(val[i]);
		}
	}
	public void writeFloatsFixedLen(float[] val) {
		for(int i = 0; i < val.length; i++) {
			backing.writeFloat(val[i]);
		}
	}
	public void writeDoubles(double[] val) {
		backing.writeInt(val.length);
		for(int i = 0; i < val.length; i++) {
			backing.writeDouble(val[i]);
		}
	}
	public void writeDoublesFixedLen(double[] val) {
		for(int i = 0; i < val.length; i++) {
			backing.writeDouble(val[i]);
		}
	}
	public void writeBooleans(boolean[] val) {
		backing.writeInt(val.length);
		for(int i = 0; i < val.length; i++) {
			backing.writeByte(val[i] ? 1 : 0);
		}
	}
	public void writeBooleansFixedLen(boolean[] val) {
		for(int i = 0; i < val.length; i++) {
			backing.writeByte(val[i] ? 1 : 0);
		}
	}
	public void writeBooleansCompressed(boolean[] val) {
		backing.writeInt(val.length);
		int byteCount = (val.length+7)/8;//Ceil Div len / 8 Func
		for(int i = 0; i < byteCount; i++) {
			byte value = 0;
			for(int j = 0; j < 8; j++) {
				int Index = i * 8 + j;
				if(Index >= val.length) {
					break;
				}else{
					if(val[Index]) {
						value |= 1 << j;
					}
				}
			}
			backing.writeByte(value);
		}
	}
	public void writeBooleansCompressedFixedLen(boolean[] val) {
		int byteCount = (val.length+7)/8;//Ceil Div len / 8 Func
		for(int i = 0; i < byteCount; i++) {
			byte value = 0;
			for(int j = 0; j < 8; j++) {
				int Index = i * 8 + j;
				if(Index >= val.length) {
					break;
				}else{
					if(val[Index]) {
						value |= 1 << j;
					}
				}
			}
			backing.writeByte(value);
		}
	}
	public void writeStrings(String[] val,Charset charset) {
		backing.writeInt(val.length);
		for(int i = 0; i < val.length; i++) {
			writeString(val[i],charset);
		}
	}
	public void writeStrings(String[] val) {
		writeStrings(val,CharsetUtil.US_ASCII);
	}
	public void writeUnicodeStrings(String[] val) {
		writeStrings(val,CharsetUtil.UTF_16);
	}

	public <E extends Enum<E>> void writeEnum(E val) {
		backing.writeInt(val.ordinal());
	}

	public <T> void writeIterable(Iterable<T> iterable, Consumer<T> storeFunc) {
		final int I = backing.writerIndex();
		backing.skipBytes(4);//Skip Int Length//
		int count = 0;
		for(T t : iterable) {
			storeFunc.accept(t);
			count++;
		}
		backing.setInt(I,count);
	}

	public <K,V> void writeMap(Map<K,V> map, Consumer<K> keyFunc, Consumer<V> valFunc) {
		backing.writeInt(map.size());
		map.forEach((k,v) -> {
			keyFunc.accept(k);
			valFunc.accept(v);
		});
	}

	public <V> void writeIntMap(TIntObjectMap<V> map,Consumer<V> storeFunc) {
		backing.writeInt(map.size());
		map.forEachEntry((k,v) -> {
			backing.writeInt(k);
			storeFunc.accept(v);
			return true;
		});
	}
}
