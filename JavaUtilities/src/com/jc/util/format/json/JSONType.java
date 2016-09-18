package com.jc.util.format.json;

/**All different types of JSON**/
public enum JSONType {
	MAP,//0
	LIST,//1
	STRING,//2
	INT,//3
	LONG,//4
	FLOAT,//5
	DOUBLE,//6
	BOOL,//5
	NULL;//6
	
	public static class WrongJSONTypeException extends RuntimeException {
		private static final long serialVersionUID = 6252182933310291175L;
		public WrongJSONTypeException(String str) {
			super(str);
		}
	}
	
}
