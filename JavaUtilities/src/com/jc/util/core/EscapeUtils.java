package com.jc.util.core;


public class EscapeUtils {
	
	
	public static String UnEscape_JSON(String str) {
		StringBuilder build = new StringBuilder();
		char[] chars = str.toCharArray();
		boolean escaping = false;
		boolean deepescape = false;
		int deepescapecount = 0;
		String escapeBuildQueue = "";
		for(char c : chars) {
			if(deepescape) {  ///// \uFFFF <FORMAT
				if(deepescapecount == 3) {
					escapeBuildQueue = escapeBuildQueue + c;
					////BUILD THE ESCAPED CHAR///
					char h1 = escapeBuildQueue.charAt(2);
					char h2 = escapeBuildQueue.charAt(3);
					char h3 = escapeBuildQueue.charAt(4);
					char h4 = escapeBuildQueue.charAt(5);
					///FROM HEX
					int count = 0;
					count += BaseUtils.getHexValue(h4, 0);
					count += BaseUtils.getHexValue(h3, 1);
					count += BaseUtils.getHexValue(h2, 2);
					count += BaseUtils.getHexValue(h1, 3);
					char nc = (char)count;
					build.append(nc);
					escaping = false;
					deepescape = false;
					escapeBuildQueue = "";
				}else{
					escapeBuildQueue = escapeBuildQueue + c;
					deepescapecount++;
				}
			}else
			if(escaping) {
				if(c == '\\') {
					build.append('\\');
					escaping = false;
				}else
				if(c == '"') {
					build.append('\'');
					escaping = false;
				}else
				if(c == 'n') {
					build.append('\n');
					escaping = false;
				}else
				if(c == 't') {
					build.append('\t');
					escaping = false;
				}else
				if(c == 'r') {
					build.append('\r');
					escaping = false;
				}else
				if(c == 'b') {
					build.append('\b');
					escaping = false;
				}else
				if(c == 'f') {
					build.append('\f');
					escaping = false;
				}else
				if(c == 'u') {
					escapeBuildQueue = "\\" + c;
					deepescape = true;
					deepescapecount = 0;
				}
				else {
					throw new RuntimeException("Invalid escape \\"+c + ", from: " + str);
				}
			}else{
				if(c == '\\') {
					escaping = true;
				}else{
					build.append(c);
				}
			}
		}
		return build.toString();
	}
	
	public static String Escape_JSON(String str) {
		str = str.replace("\\","\\\\");
		str = str.replace("\n","\\n");
		str = str.replace("\"","\\\"");
		str = str.replace("\t","\\t");
		str = str.replace("\r","\\r");
		str = str.replace("\b","\\b");
		str = str.replace("\f","\\f");
		str = str.replace("=", "\u003d");///NEEDED?
		return str;
	}
	
}
