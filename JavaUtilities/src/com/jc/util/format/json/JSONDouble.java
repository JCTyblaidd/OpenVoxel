package com.jc.util.format.json;

import static com.jc.util.format.json.JSONType.DOUBLE;

/**
 * Created by James on 25/08/2016.
 */
public class JSONDouble extends JSONObject{

	private double backing;

	public JSONDouble() {backing = 0;}

	public JSONDouble(double d) {
		backing = d;
	}
	public JSONDouble(Number n) {
		backing = n.doubleValue();
	}

	public void set(double v) {
		backing = v;
	}
	public void set(Number n) {
		backing = n.doubleValue();
	}
	public double get() {
		return backing;
	}


	@Override
	public JSONType getType() {
		return DOUBLE;
	}

	@Override
	void _buildJSON(JSONBuilder builder) {
		builder.write(backing);
	}

	@Override
	void _buildBinaryJSON(JSONBinaryBuilder builder) {
		builder.writeType(DOUBLE);
		builder.writeDouble(backing);
	}

	@Override
	public float asFloat() {
		return (float) backing;
	}

	@Override
	public int hashCode() {
		return Double.hashCode(backing);
	}

	@Override
	boolean _equal(Object obj) {
		return backing == ((JSONDouble)obj).backing;
	}
}
