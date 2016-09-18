package com.jc.util.format.json;

import static com.jc.util.format.json.JSONType.NULL;

public class JSONNull extends JSONObject{

	@Override
	public JSONType getType() {
		return NULL;
	}

	@Override
	void _buildJSON(JSONBuilder builder) {
		builder.write("null");
	}

	@Override
	void _buildBinaryJSON(JSONBinaryBuilder builder) {
		builder.writeType(NULL);
	}

	@Override
	boolean _equal(Object obj) {
		return true;//NULL NULL
	}
	
	@Override
	public boolean isNull() {
		return true;
	}
}
