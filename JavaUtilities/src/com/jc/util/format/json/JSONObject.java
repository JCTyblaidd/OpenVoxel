package com.jc.util.format.json;

import com.jc.util.format.json.JSONBuilder.NormalJSONBuilder;
import com.jc.util.format.json.JSONBuilder.PrettyJSONBuilder;
import com.jc.util.format.json.JSONType.WrongJSONTypeException;

public abstract class JSONObject {
	
	public abstract JSONType getType();
	abstract void _buildJSON(JSONBuilder builder);
	abstract void _buildBinaryJSON(JSONBinaryBuilder builder);
	
	public String asString() {
		_err("Not A String Type");
		return null;
	}
	
	public int asInteger() {
		_err("Not A Integer Type");
		return 0;
	}
	public long asLong() {
		_err("Not A Long Type");
		return 0;
	}
	
	public float asFloat() {
		_err("Not A Float Type");
		return 0;
	}
	public double asDouble() {
		_err("Not A Double Type");
		return 0;
	}
	
	public boolean asBool() {
		_err("Not A Bool Type");
		return false;
	}
	
	public boolean isNull() {
		return false;
	}
	
	public JSONMap<JSONObject> asMap() {
		_err("Not A Map Type");
		return null;
	}
	
	public JSONList<JSONObject> asList() {
		_err("Not A List Type");
		return null;
	}
	
	
	void _err(String reason) throws WrongJSONTypeException{
		throw new WrongJSONTypeException(reason);
	}
	
	public final String toJSONString() {
		JSONBuilder build = new NormalJSONBuilder();
		_buildJSON(build);
		return build.BUILD();
	}
	
	public final String toPrettyJSONString() {
		JSONBuilder build = new PrettyJSONBuilder();
		_buildJSON(build);
		return build.BUILD();
	}

	public final byte[] toBinaryJSON() {
		JSONBinaryBuilder build = new JSONBinaryBuilder();
		_buildBinaryJSON(build);
		return build.getBytes();
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj == null) return false;
		if(obj.getClass() != getClass()) return false;
		return _equal(obj);
	}
	
	boolean _equal(Object obj) {
		return obj == this;
	}
	
	@Override
	public String toString() {///ME LIKE PRETTY
		return toPrettyJSONString();
	}
	
}
