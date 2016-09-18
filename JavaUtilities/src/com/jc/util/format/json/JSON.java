package com.jc.util.format.json;

import java.io.*;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;

public class JSON {

	private static final SimpleDateFormat FORMAT_OUTPUT = new SimpleDateFormat("yyyy.MM.dd@HH:mm:ss");
	
	JSON() {}


	/**
	 * @return the JSON tree from the binary format
	 */
	public static JSONObject fromBinary(byte[] data) {
		return JSONBinaryParser._parse(data);
	}

	public static JSONObject fromBinary(InputStream byteStream) {
		return JSONBinaryParser._parse(byteStream);
	}

	/**
	 * @return the JSON tree from the string format
	 */
	public static JSONObject fromString(String str) {
		return JSONParser._parse(str);
	}

	/**
	 * @param stringStream the inputstream to read from
	 * @param charset the charset to use
	 * @return the parsed JSON tree
	 */
	public static JSONObject fromStringStream(InputStream stringStream, Charset charset) {
		return JSONParser._parseStream(new InputStreamReader(stringStream,charset));
	}

	/**
	 * @param stringStream the inputStream to read from
	 * @return the parsed JSON tree
	 */
	public static JSONObject fromStringStream(InputStream stringStream) {
		return fromStringStream(stringStream,Charset.defaultCharset());
	}

	/**
	 * @param f File To Load From
	 * @return the parsed JSON tree
	 * @throws IOException if file error
	 */
	public static JSONObject fromFile(File f, Charset charset) throws IOException{
		return fromStringStream(new FileInputStream(f),charset);
	}

	/**
	 * @param f File To Load From
	 * @return the parsed JSON tree
	 * @throws IOException if file error
	 */
	public static JSONObject fromFile(File f) throws IOException{
		return fromFile(f,Charset.defaultCharset());
	}

	/**
	 * @return the JSON tree from the default object tree
	 */
	public static JSONObject fromObject(Object obj) {
		if(obj == null) {
			return new JSONNull();
		}
		if(obj instanceof JSONObject) {
			return (JSONObject) obj;
		}
		if(obj instanceof String) {
			return new JSONString((String)obj);
		}
		if(obj instanceof Float) {
			return new JSONFloat((Float)obj);
		}
		if(obj instanceof Double) {
			return new JSONDouble((Double)obj);
		}
		if(obj instanceof Integer) {
			return new JSONInteger((Integer)obj);
		}
		if(obj instanceof Long) {
			return new JSONLong((Long)obj);
		}
		if(obj instanceof Boolean) {
			return new JSONBoolean((Boolean)obj);
		}
		if (obj instanceof Date) {
			return new JSONString(FORMAT_OUTPUT.format(obj));
		}
		if(obj instanceof List) {
			List<?> l = (List<?>)obj;
			List<JSONObject> objs = new ArrayList<>();
			boolean valid = true;
			for(Object o : l) {
				try{
					objs.add(fromObject(o));
				}catch(Exception e) {
					valid = false;
					break;
				}
			}
			if(!valid) {
				throw new RuntimeException("Can't Convert To JSON!");
			}else{
				return new JSONList<>(objs);
			}
		}
		if(obj instanceof Map) {
			Map<?,?> m = (Map<?,?>)obj;
			Map<JSONString,JSONObject> objs = new HashMap<>();
			boolean valid = true;
			for(Object k : m.keySet()) {
				Object v = m.get(k);
				try{
					JSONObject k1 = fromObject(k);
					JSONObject v1 = fromObject(v);
					if(!(k1 instanceof JSONString)) throw new RuntimeException("Can't Convert Non String keyed map to json!");
					objs.put((JSONString)k1, v1);
				}catch(Exception e) {
					valid = false;
					break;
				}
			}
			if(!valid) {
				throw new RuntimeException("Can't Convert To JSON!");
			}else{
				return new JSONMap<>(objs);
			}
		}
		throw new RuntimeException("Can't Convert To JSON!");
	}

	/**
	 * @return the target(returns @param target) with the data from the source tree on it
	 */
	public static JSONObject translateData(JSONObject target, JSONObject source) {
		//TODO:


		return target;
	}
}
