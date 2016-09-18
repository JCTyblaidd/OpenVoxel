package com.jc.util.format.json;

import java.io.ByteArrayOutputStream;

/**
 * Created by James on 05/08/2016.
 */
public class JSONBinaryBuilder {

	private ByteArrayOutputStream out;

	public JSONBinaryBuilder() {
		out = new ByteArrayOutputStream();
	}

	public void writeType(JSONType type) {
		out.write(type.ordinal());/**Limit to byte*/
	}
	public void writeInteger(int i) {
		out.write(intToByteArray(i),0,4);
	}
	public void writeLong(long l) { out.write(longToByteArray(l),0,8);}
	public void writeFloat(float f) {
		writeInteger(Float.floatToRawIntBits(f));
	}
	public void writeDouble(double d) {writeLong(Double.doubleToRawLongBits(d));}
	public void writeFlag(boolean flag) {
		out.write(flag ? 1 : 0);
	}
	public void writeString(String str) {
		byte[] bytes = str.getBytes();
		writeInteger(bytes.length);
		out.write(bytes,0,bytes.length);
	}

	private static final byte[] intToByteArray(int value) {
		return new byte[] {
				(byte)(value >>> 24),
				(byte)(value >>> 16),
				(byte)(value >>> 8),
				(byte)value};
	}
	public byte[] longToByteArray(long value) {
		return new byte[] {
				(byte) (value >> 56),
				(byte) (value >> 48),
				(byte) (value >> 40),
				(byte) (value >> 32),
				(byte) (value >> 24),
				(byte) (value >> 16),
				(byte) (value >> 8),
				(byte) value
		};
	}

	public byte[] getBytes() {
		return out.toByteArray();
	}
}
