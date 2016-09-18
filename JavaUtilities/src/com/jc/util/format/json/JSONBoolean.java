package com.jc.util.format.json;

import static com.jc.util.format.json.JSONType.BOOL;

public class JSONBoolean extends JSONObject{
	
	private boolean backing;
	
	public JSONBoolean() {
		backing = false;
	}
	
	public JSONBoolean(boolean b) {
		backing = b;
	}
	public void set(boolean b) {
		backing = b;
	}
	public boolean get() {
		return backing;
	}
	@Override
	public boolean asBool() {
		return backing;
	}
	
	@Override
	public JSONType getType() {
		return BOOL;
	}

	@Override
	void _buildJSON(JSONBuilder builder) {
		builder.write(backing ? "true" : "false");
	}

	@Override
	void _buildBinaryJSON(JSONBinaryBuilder builder) {
		builder.writeType(BOOL);
		builder.writeFlag(backing);
	}

	@Override
	boolean _equal(Object obj) {
		JSONBoolean b = (JSONBoolean)obj;
		return b.backing == backing;
	}
	
	@Override
	public int hashCode() {
		return Boolean.hashCode(asBool());
	}
	
}
