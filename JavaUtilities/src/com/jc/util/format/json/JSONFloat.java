package com.jc.util.format.json;

import static com.jc.util.format.json.JSONType.FLOAT;

public class JSONFloat extends JSONObject{
	
private float backing;
	
	public JSONFloat() {
		backing = 0;
	}
	
	public JSONFloat(Number n) {
		backing = n.floatValue();
	}
	public JSONFloat(float val) {
		backing = val;
	}
	
	public void set(float v) {
		backing = v;
	}
	public void set(Number n) {
		backing = n.floatValue();
	}
	public float get() {
		return backing;
	}

	@Override
	public JSONType getType() {
		return FLOAT;
	}

	@Override
	void _buildJSON(JSONBuilder builder) {
		builder.write(backing);
	}

	@Override
	void _buildBinaryJSON(JSONBinaryBuilder builder) {
		builder.writeType(FLOAT);
		builder.writeFloat(backing);
	}

	@Override
	public float asFloat() {
		return backing;
	}
	
	@Override//AUTO CAST//
	public int asInteger() {
		return (int)backing;
	}

	@Override
	public double asDouble() {
		return backing;
	}

	@Override
	public long asLong() {
		return (long)backing;
	}

	@Override
	boolean _equal(Object obj) {
		JSONFloat f = (JSONFloat)obj;
		return f.backing == backing;
	}
	
	@Override
	public int hashCode() {
		return Float.hashCode(asFloat());
	}
	
}
