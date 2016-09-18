package com.jc.util.format.json;

import static com.jc.util.format.json.JSONType.INT;

public class JSONInteger extends JSONObject{
	
	private int backing;
	
	public JSONInteger() {
		backing = 0;
	}
	
	public JSONInteger(Number n) {
		backing = n.intValue();
	}
	public JSONInteger(int val) {
		backing = val;
	}
	
	public void set(int v) {
		backing = v;
	}
	public void set(Number n) {
		backing = n.intValue();
	}
	public int get() {
		return backing;
	}

	@Override
	public JSONType getType() {
		return INT;
	}

	@Override
	void _buildJSON(JSONBuilder builder) {
		builder.write(backing);
	}

	@Override
	void _buildBinaryJSON(JSONBinaryBuilder builder) {
		builder.writeType(INT);
		builder.writeInteger(backing);
	}

	@Override
	public int asInteger() {
		return backing;
	}
	
	/***
	 * Allow the possibility of turning
	 * Integer values into floats
	 * Incase of JSON formatting errors
	 * **/
	public float asFloat() {
		return (float)asInteger();
	}

	@Override
	public long asLong() {
		return backing;
	}

	@Override
	boolean _equal(Object obj) {
		JSONInteger i = (JSONInteger) obj;
		return i.backing == backing;
	}
	
	@Override
	public int hashCode() {
		return asInteger();
	}
	
}
