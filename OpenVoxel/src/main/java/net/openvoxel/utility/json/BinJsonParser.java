package net.openvoxel.utility.json;

import net.openvoxel.api.PublicAPI;

import java.nio.ByteBuffer;
import java.util.List;


//
// TODO: REWRITE ENTIRE CLASS
//
public class BinJsonParser extends JsonParser {

	private ByteBuffer binary;
	private List<String> string_table;

	private static final int TYPE_NULL = 0;
	private static final int TYPE_FALSE = 1;
	private static final int TYPE_TRUE = 2;
	private static final int TYPE_BYTE = 3;
	private static final int TYPE_INT = 4;
	private static final int TYPE_LONG = 5;
	private static final int TYPE_FLOAT = 6;
	private static final int TYPE_DOUBLE = 7;
	private static final int TYPE_STRING = 8;
	private static final int TYPE_LIST = 9;
	private static final int TYPE_MAP = 10;

	private int current_list_type = -1;
	private int remaining_elements = 0;
	private int seek_offset = 0;

	@PublicAPI
	public BinJsonParser(byte[] data) {
		super(data);
	}

	@PublicAPI
	public BinJsonParser(ByteBuffer buffer) {
		super(buffer);
	}

	//////////////////////////
	/// Public API Methods ///
	//////////////////////////

	@PublicAPI
	public void reset() {
		pos = 0;
		current_list_type = -1;
		remaining_elements = 0;
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
		if(bytes.get(pos) == TYPE_MAP) {
			remaining_elements = bytes.getInt(pos + 1);
			current_list_type = -1;
			seek_offset = 5;
			return true;
		}
		return false;
	}

	@PublicAPI
	public boolean isMapEnd() {
		if(remaining_elements == 0) {
			current_list_type = -1;
			seek_offset = 1;
			return true;
		}
		return false;
	}

	@PublicAPI
	public boolean isArrayBegin() {
		if(bytes.get(pos) == TYPE_LIST) {
			remaining_elements = bytes.getInt(pos + 1);
			current_list_type = bytes.get(pos + 5);
			seek_offset = 6;
			return true;
		}
		return false;
	}

	@PublicAPI
	public boolean isArrayEnd() {
		if(remaining_elements == 0) {
			current_list_type = -1;
			seek_offset = 1;
			return true;
		}
		return false;
	}

	@PublicAPI
	public boolean isSeparator() {
		return remaining_elements > 0;
	}

	@PublicAPI
	public boolean isMapping() {
		return remaining_elements > 0 && current_list_type == -1;
	}

	@PublicAPI
	public boolean seekNext() {
		pos += seek_offset;
		seek_offset = 0;
		return pos < bytes.capacity();
	}


	@PublicAPI
	public boolean seekTrue() {
		if(bytes.get(pos) == TYPE_TRUE) {
			pos += 1;
			return true;
		}
		return false;
	}

	@PublicAPI
	public boolean seekFalse() {
		if(bytes.get(pos) == TYPE_FALSE) {
			pos += 1;
			return true;
		}
		return false;
	}

	@PublicAPI
	public boolean seekNull() {
		if(bytes.get(pos) == TYPE_NULL) {
			pos += 1;
			return true;
		}
		return false;
	}

	//TODO: isLong && isDouble && isString(returns null so not needed?)

	@PublicAPI
	public long seekLong() {
		int type = current_list_type;
		if(type != -1) {

		}
		if(bytes.get(pos) == TYPE_BYTE) {
			long val = bytes.get(pos+1);
			pos += 2;
		}else if(bytes.get(pos) == TYPE_INT) {

		}else if(bytes.get(pos) == TYPE_LONG) {

		}
		return 0;//TODO: REWRITE
	}

	@PublicAPI
	public double seekDouble() {
		return 0.0;//TODO:
	}


	@PublicAPI
	public String seekString() {
		return "";//TODO:
	}
}
