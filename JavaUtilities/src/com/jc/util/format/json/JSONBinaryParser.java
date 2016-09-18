package com.jc.util.format.json;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by James on 05/08/2016.
 */
class JSONBinaryParser {

	JSONBinaryParser() {throw new RuntimeException("NOPE!");}

	static JSONObject _parse(byte[] data) {
		ByteArrayInputStream in = new ByteArrayInputStream(data);
		try {
			return _getObject(in);
		}catch(Exception e) {
			throw new RuntimeException("Invalid Binary JSON",e);
		}
	}

	static JSONObject _parse(InputStream inputStream) {
		try{
			return _getObject(inputStream);
		}catch(Exception e) {
			throw new RuntimeException("Invalid Binary JSON",e);
		}
	}

	private static int fromByteArray(byte[] bytes) {
		return bytes[0] << 24 | (bytes[1] & 0xFF) << 16 | (bytes[2] & 0xFF) << 8 | (bytes[3] & 0xFF);
	}
	private static long fromLongByteArray(byte[] bytes) {
		return bytes[0] << 56 | (bytes[1] & 0xFF) << 48 | (bytes[2] & 0xFF) << 40 | (bytes[3] & 0xFF) << 32 |
		(bytes[4] & 0xFF) << 24 | (bytes[5] & 0xFF) << 16 | (bytes[6] & 0xFF) << 8 | (bytes[7] & 0xFF);
	}

	private static int _readInt(InputStream inputStream) throws IOException{
		byte[] arr = new byte[4];
		inputStream.read(arr,0,4);
		return fromByteArray(arr);
	}
	private static long _readLong(InputStream inputStream) throws IOException{
		byte[] arr = new byte[8];
		inputStream.read(arr,0,8);
		return fromLongByteArray(arr);
	}
	private static float _readFloat(InputStream inputStream) throws IOException{
		return Float.intBitsToFloat(_readInt(inputStream));
	}
	private static double _readDouble(InputStream inputStream) throws IOException{
		return Double.longBitsToDouble(_readLong(inputStream));
	}

	private static boolean _readBool(InputStream inputStream) throws IOException{
		return inputStream.read() != 0;
	}

	private static JSONObject _getObject(InputStream in) throws Exception{
		JSONType type = JSONType.values()[in.read()];
		switch(type) {
			case BOOL:
				return new JSONBoolean(_readBool(in));
			case FLOAT:
				return new JSONFloat(_readFloat(in));
			case DOUBLE:
				return new JSONDouble(_readDouble(in));
			case INT:
				return new JSONInteger(_readInt(in));
			case LONG:
				return new JSONLong(_readLong(in));
			case LIST:
				JSONList<JSONObject> list = new JSONList<>();
				int size = _readInt(in);
				for(int i = 0; i < size; i++) {
					JSONObject obj = _getObject(in);
					list.add(obj);
				}
				return list;
			case MAP:
				JSONMap<JSONObject> map = new JSONMap<>();
				int count = _readInt(in);
				for(int i = 0; i < count; i++) {
					JSONObject obj1 = _getObject(in);
					JSONObject obj2 = _getObject(in);
					map.put((JSONString)obj1,obj2);
				}
				return map;
			case NULL:
				return new JSONNull();
			case STRING:
				int str_len = _readInt(in);
				byte[] dat = new byte[str_len];
				in.read(dat);
				return new JSONString(new String(dat));
			default:
				throw new RuntimeException("Invalid JSON TYPE!");
		}
	}
}
