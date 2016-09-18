package com.jc.util.format.json;

import java.io.InputStreamReader;

/**Utility Class -> Ignore For Most Purposes*/
class JSONParser {
	
	JSONParser() {
		throw new RuntimeException("DENIED!");
	}

	static JSONObject _parseStream(InputStreamReader reader) {
		//TODO: convert to streamed reader : this is a temp solution//
		StringBuilder builder = new StringBuilder();
		try {
			while (reader.ready()) {
				builder.append((char) reader.read());
			}
		}catch(Exception e) {
			throw new RuntimeException("Error Parsing JSON");
		}
		return _parse(builder.toString());
	}


	static JSONObject _parse(String s) {
		int maplevel = 0;
		int listlevel = 0;
		boolean escapeflag = false;
		boolean instr = false;
		s = s.replace("\n", "");
		s = s.trim();
		//TODO HANDLE
		if(s.startsWith("[")) {///THIS BE A LIST
			if(!s.endsWith("]")) throw new RuntimeException("Invalid List Format!");
			JSONList<JSONObject> l = new JSONList<>();
			///GO THROUGH AND REPEAT
			char[] chars = s.toCharArray();
			int prevcomma = 0;
			for(int i = 1; i < chars.length-1; i++) {
				char c = chars[i];
				////HANDLE CHAR ESCAPING (Handles against all important shizzle)
				if(escapeflag) {
					escapeflag = false;
				}else
				if(c == '\\') {
					escapeflag = true;
					continue;
				}
				if(c == '\"') {
					instr = !instr;
				}
				if(!instr) {
					if(c == '{') {
						maplevel++;
					}else
					if(c == '}') {
						maplevel--;
					}else
					if(c == '[') {
						listlevel++;
					}else
					if(c == ']') {
						listlevel--;
					}else if(maplevel == 0 && listlevel == 0){
						//HANDLE OPERATIONS
						if(c == ',') {///COMMA HANDLE SHIZZLE
							String str = s.substring(prevcomma+1, i);
							str = str.trim();
							prevcomma = i;
							JSONObject obj = _parse(str);
							l.add(obj);
						}
					}
				}
			}
			///APPEND THE LAST
			try{
				String str = s.substring(prevcomma+1, s.length()-1);
				str = str.trim();
				JSONObject obj = _parse(str);
				l.add(obj);
			}catch(RuntimeException e) {
				if(!e.getMessage().startsWith("Unsupported JSON String!!!")) {
					throw new RuntimeException("Relayed Handled Exception",e);
				}
			}
			return l;
		}
		if(s.startsWith("{")) {///THIS BE A MAP
			if(!s.endsWith("}")) throw new RuntimeException("Invalid Map Format! -> "+s.substring(s.length()-8));
			///GO THROUGH AND REPEAT
			JSONMap<JSONObject> map = new JSONMap<>();
			char[] chars = s.toCharArray();
			int prevcomma = 0;
			int prevcolon = -1;
			for(int i = 1; i < chars.length-1; i++) {
				char c = chars[i];
				if(escapeflag) {
					escapeflag = false;
				}else
				if(c == '\\') {
					escapeflag = true;
					continue;
				}
				if(c == '\"') {
					instr = !instr;
				}
				if(!instr) {
					if(c == '{') {
						maplevel++;
					}else
					if(c == '}') {
						maplevel--;
					}else
					if(c == '[') {
						listlevel++;
					}else
					if(c == ']') {
						listlevel--;
					}else if(maplevel == 0 && listlevel == 0){
						//HANDLE OPERATIONS
						if(c == ':') {
							prevcolon = i;
						}else
						if(c == ',') {///COMMA HANDLE SHIZZLE
							String str1 = s.substring(prevcomma+1, prevcolon);
							str1 = str1.trim();
							String str2 = s.substring(prevcolon+1, i);
							prevcomma = i;
							JSONObject obj1 = _parse(str1);
							JSONObject obj2 = _parse(str2);
							if(!(obj1 instanceof JSONString)) throw new RuntimeException("Map keys must be strings!");
							map.put((JSONString)obj1, obj2);
						}
					}
				}
			}
			///APPEND THE LAST
			if(prevcolon != -1) {
				String str1 = s.substring(prevcomma+1, prevcolon);
				str1 = str1.trim();
				String str2 = s.substring(prevcolon+1, s.length()-1);
				str2 = str2.trim();
				JSONObject obj1 = _parse(str1);
				JSONObject obj2 = _parse(str2);
				if(!(obj1 instanceof JSONString)) throw new RuntimeException("Map keys must be strings!");
				map.put((JSONString)obj1, obj2);
			}
			return map;
		}
		if(s.startsWith("\"")) {///THIS BE A STRING
			///PARSE DAT STRING
			if(s.endsWith("\"")) {
				String str = s.substring(1, s.length() - 1);
				JSONString js = new JSONString(str);
				js._wasParsed();
				return js;
			}
			throw new RuntimeException("Error Parsing String ["+s+"]");
		}
		if(s.equals("null")) {///THIS BE A NULL
			return new JSONNull();
		}
		if(s.equals("true")) {//THIS BE A TRUE
			return new JSONBoolean(true);
		}
		if(s.equals("false")) {//THIS BE A FALSE
			return new JSONBoolean(false);
		}
		if(s.contains(".")) {//THIS BE A FLOAT
			try{
				float f = Float.parseFloat(s);
				return new JSONFloat(f);
			}catch(Exception e) {}
			try{
				double d = Double.parseDouble(s);
				return new JSONDouble(d);
			}catch(Exception e) {}
		}
		try{
			int i = Integer.parseInt(s);
			return new JSONInteger(i);
		}catch(Exception e) {}
		try{
			long l = Long.parseLong(s);
			return new JSONLong(l);
		}catch (Exception e) {}
		
		throw new RuntimeException("Unsupported JSON String!!! Res:["+s+"]");
	}
	

	
}
