package com.jc.util.core;

public class BaseUtils {
	
	private static final String vals = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	
	public static int getHexValue(char c, int pow) {//INTEGER POWA
		assert(pow >= 0);
		c = Character.toUpperCase(c);
		int cnst = (int)Math.pow(16, pow);
		for(int i = 0; i < 16; i++) {
			if(c == vals.charAt(i)) {
				return cnst * i;
			}
		}
		throw new RuntimeException("Invalid Base Value!");
	}
	
	public static int getPowaValue(char c, int pow, int base) {
		assert(base >= 2);
		assert(pow >= 0);
		c = Character.toUpperCase(c);
		int cnst = (int)Math.pow(base, pow);
		for(int i = 0; i < base; i++) {
			if(c == vals.charAt(i)) {
				return cnst * i;
			}
		}
		throw new RuntimeException("Invalid Base Value!");
	}
	
	
}
