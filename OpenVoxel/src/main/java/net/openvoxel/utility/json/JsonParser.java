package net.openvoxel.utility.json;

import net.openvoxel.api.PublicAPI;

import java.nio.ByteBuffer;

/*
 * Accepts ASCII ONLY!!!
 */
public class JsonParser {

	ByteBuffer bytes;
	int pos;

	@PublicAPI
	public JsonParser(byte[] data) {
		this(ByteBuffer.wrap(data));
	}

	@PublicAPI
	public JsonParser(ByteBuffer data) {
		bytes = data;
		reset();
	}

	private boolean isWhitespace() {
		byte _byte = bytes.get(pos);
		if(_byte == '\n') return true;
		if(_byte == '\t') return true;
		if(_byte == '\r') return true;
		return _byte == ' ';
	}

	private void skipWhitespace() {
		while(pos < bytes.capacity() && isWhitespace()) {
			pos += 1;
		}
	}

	//////////////////////////
	/// Public API Methods ///
	//////////////////////////

	@PublicAPI
	public void reset() {
		pos = 0;
		skipWhitespace();
	}

	@PublicAPI
	public int getPointer() {
		return pos;
	}

	@PublicAPI
	public void setPointer(int pos) {
		this.pos = pos;
	}

	@PublicAPI
	public boolean isMapBegin() {
		return bytes.get(pos) == '{';
	}

	@PublicAPI
	public boolean isMapEnd() {
		return bytes.get(pos) == '}';
	}

	@PublicAPI
	public boolean isArrayBegin() {
		return bytes.get(pos) == '[';
	}

	@PublicAPI
	public boolean isArrayEnd() {
		return bytes.get(pos) == ']';
	}

	@PublicAPI
	public boolean isSeparator() {
		return bytes.get(pos) == ',';
	}

	@PublicAPI
	public boolean isMapping() {
		return bytes.get(pos) == ':';
	}

	@PublicAPI
	public boolean seekNext() {
		pos += 1;
		skipWhitespace();
		return pos < bytes.capacity();
	}


	@PublicAPI
	public boolean seekTrue() {
		if(bytes.get(pos)   != 't') return false;
		if(bytes.get(pos+1) != 'r') return false;
		if(bytes.get(pos+2) != 'u') return false;
		if(bytes.get(pos+3) != 'e') return false;
		pos = pos + 3;
		return true;
	}

	@PublicAPI
	public boolean seekFalse() {
		if(bytes.get(pos)   != 'f') return false;
		if(bytes.get(pos+1) != 'a') return false;
		if(bytes.get(pos+2) != 'l') return false;
		if(bytes.get(pos+3) != 's') return false;
		if(bytes.get(pos+4) != 'e') return false;
		pos = pos + 4;
		return true;
	}

	@PublicAPI
	public boolean seekNull() {
		if(bytes.get(pos)   != 'n') return false;
		if(bytes.get(pos+1) != 'u') return false;
		if(bytes.get(pos+2) != 'l') return false;
		if(bytes.get(pos+3) != 'l') return false;
		pos = pos + 3;
		return true;
	}

	@PublicAPI
	public long seekLong() {
		int old_pos = pos;
		byte val = bytes.get(pos);
		boolean negate = false;
		if(val == '-') {
			negate = true;
			pos += 1;
			old_pos = pos;
			val = bytes.get(pos);
		}
		while(val >= '0' && val <= '9') {
			pos += 1;
			val = bytes.get(pos);
		}
		pos -= 1;

		long _value = 0;
		for(int idx = old_pos; idx <= pos; idx++) {
			long _val = bytes.get(idx) - '0';
			_value = (_value * 10) - _val;
		}
		return negate ? _value : -_value;
	}

	@PublicAPI
	public double seekDouble() {
		int old_pos = pos;
		byte val = bytes.get(pos);
		while((val >= '0' && val <= '9')
				      || val == '.' || val == '-'
				      || val == 'e' || val == 'E'
				) {
			pos += 1;
			val = bytes.get(pos);
		}
		pos -= 1;
		byte[] _data = new byte[pos - old_pos + 1];
		bytes.position(old_pos);
		bytes.get(_data);
		bytes.position(0);
		return Double.parseDouble(new String(_data));
	}


	@PublicAPI
	public String seekString() {
		if(bytes.get(pos) != '"') return null;
		pos += 1;
		int old_pos = pos;
		boolean escape = false;
		while(true) {
			pos += 1;
			byte val = bytes.get(pos);
			if(escape) {
				escape = false;
			}else if(val == '\\') {
				escape = true;
			}else if(val == '"') {
				break;
			}
		}
		StringBuilder builder = new StringBuilder();
		escape = false;
		for(int i = old_pos; i < pos; i++) {
			byte _val = bytes.get(i);
			if(escape) {
				if(_val == 'n') builder.append('\n');
				else if(_val == 't') builder.append('\t');
				else if(_val == 'r') builder.append('\r');
				else builder.append((char)_val);
			}else if(_val == '\\') {
				escape = true;
			}else {
				builder.append((char)_val);
			}
		}
		return builder.toString();
	}

}
