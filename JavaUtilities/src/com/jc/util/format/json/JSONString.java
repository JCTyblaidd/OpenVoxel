package com.jc.util.format.json;

import com.jc.util.core.EscapeUtils;

import static com.jc.util.format.json.JSONType.STRING;

public class JSONString extends JSONObject{
	
	private String backing;

	public JSONString() {
		backing = null;
	}
	
	void _wasParsed() {
		backing = unEscape(backing);
	}
	
	/**Call When Parsing**/
	static String unEscape(String str) {//TODO
		return EscapeUtils.UnEscape_JSON(str);
	}
	
	/**Call When ToString-ing**/
	static String Escape(String str) {////ERMMM///
		return EscapeUtils.Escape_JSON(str);
	}
	
	public JSONString(String str) {
		backing = str;
	}
	
	public void setStr(String str) {
		backing = str;
	}
	
	public String getStr() {
		return backing;
	}
	
	@Override
	public String asString() {
		return getStr();
	}
	
	@Override
	public String toString() {
		return getStr();
	}
	
	@Override
	public int hashCode() {
		return getStr().hashCode();
	}

	@Override
	public JSONType getType() {
		return STRING;
	}

	@Override
	void _buildJSON(JSONBuilder builder) {
		builder.write("\"",Escape(backing),"\"");
	}

	@Override
	void _buildBinaryJSON(JSONBinaryBuilder builder) {
		builder.writeType(STRING);
		builder.writeString(backing);
	}

	@Override
	boolean _equal(Object obj) {
		JSONString str = (JSONString)obj;
		return str.backing.equals(backing);
	}
}
