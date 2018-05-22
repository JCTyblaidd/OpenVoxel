package net.openvoxel.utility.json.wrap;

import net.openvoxel.utility.json.JsonParser;

public class SimpleJsonParser {

	private JsonParser parser;
	private int pointer;

	public SimpleJsonParser(JsonParser parser) {
		this.parser = parser;
		this.pointer = parser.getPointer();
	}


	//
	// Map Access API
	//

	private void findMapKey(CharSequence key) {
		parser.setPointer(pointer);
	}

	public SimpleJsonParser get(CharSequence key) {
		findMapKey(key);
		return new SimpleJsonParser(parser);
	}

	public long getLong(CharSequence key) {
		findMapKey(key);
		return parser.seekLong();
	}

	public double getDouble(CharSequence key) {
		findMapKey(key);
		return parser.seekDouble();
	}

	public String getString(CharSequence key) {
		findMapKey(key);
		return parser.seekString();
	}

	public boolean getBoolean(CharSequence key) {
		findMapKey(key);
		return parser.seekTrue();
	}

	//
	// List Access API
	//
}
