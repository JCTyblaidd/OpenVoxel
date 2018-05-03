package net.openvoxel.utility.json;

import net.openvoxel.api.PublicAPI;

import java.nio.ByteBuffer;

public class JsonParser {

	private ByteBuffer bytes;
	private int pos;

	@PublicAPI
	public JsonParser(byte[] data) {
		bytes = ByteBuffer.wrap(data);
		pos = 0;
	}

	private boolean isWhitespace() {
		byte _byte = bytes.get(pos);
		if(_byte == '\n') return true;
		if(_byte == '\t') return true;
		if(_byte == '\r') return true;
		return _byte == ' ';
	}

	private void skipWhitespace() {
		while(isWhitespace()) {
			pos += 1;
		}
	}

	@PublicAPI
	public void reset() {
		pos = 0;
		skipWhitespace();
	}

	public long seekLong() {
		skipWhitespace();
		//TODO: IMPLEMENT
		return 0;
	}

	public int seekInt() {
		skipWhitespace();
		//TODO: IMPLEMENT
		return 0;
	}

	public String seekString() {
		skipWhitespace();
		//TODO: IMPLEMENT
		return "";
	}

	public boolean seekBoolean() {
		skipWhitespace();
		//TODO: IMPLEMENT
		return false;
	}

}
