package com.jc.util.format.json;

import static com.jc.util.format.json.JSONType.LONG;

/**
 * Created by James on 25/08/2016.
 */
public class JSONLong extends JSONObject{

	private long backing;
	public JSONLong() {
		backing = 0;
	}
	public JSONLong(long l) {
		backing = l;
	}
	public JSONLong(Number n) {
		backing = n.longValue();
	}

	public void set(long v) {
		backing = v;
	}
	public void set(Number n) {
		backing = n.longValue();
	}
	public long get() {
		return backing;
	}

	@Override
	public JSONType getType() {
		return LONG;
	}

	@Override
	void _buildJSON(JSONBuilder builder) {
		builder.write(backing);
	}

	@Override
	void _buildBinaryJSON(JSONBinaryBuilder builder) {
		builder.writeType(LONG);
		builder.writeLong(backing);
	}

	@Override
	public int asInteger() {
		return (int) backing;
	}

	@Override
	public double asDouble() {
		return backing;
	}

	@Override
	public float asFloat() {
		return backing;
	}

	@Override
	boolean _equal(Object obj) {
		return backing == ((JSONLong)obj).backing;
	}

	@Override
	public int hashCode() {
		return Long.hashCode(backing);
	}
}
