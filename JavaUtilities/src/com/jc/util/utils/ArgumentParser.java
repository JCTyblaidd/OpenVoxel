package com.jc.util.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by James on 02/08/2016.
 *
 * Parse Arguments:
 * Valid Format
 *  -flag
 *  key=value
 *  --key value
 *  -flag-
 *  --key= value
 *  flag
 */
public class ArgumentParser {

	private String[] args;
	private List<String> flags;
	private Map<String,String> vars;

	public ArgumentParser(String[] arguments) {
		args = arguments;
		flags = new ArrayList<>();
		vars = new HashMap<>();
		_parse();
	}
	private void _parse() {
		boolean nextArgMap = false;
		String info = null;
		for(String s : args) {
			if(nextArgMap) {
				//Is Value
				nextArgMap = false;
				vars.put(info,s);
			}else
			if(s.startsWith("-") && !s.startsWith("--")) {
				//Is a Flag//
				String flagV = s.replace("-","");
				flags.add(flagV);
			}else
			if(s.startsWith("--")) {
				//Is aMultiArg//
				info = s.replace("--","-");
				if(info.endsWith("=")) {
					info = info.replace("=","");
				}
				nextArgMap = true;
			}else
			if(s.contains("=")) {
				String[] pair = s.split("=");
				vars.put(pair[0],pair[1]);
			}else {
				flags.add(s);
			}
		}
	}

	public void storeRuntimeFlag(String flagID) {
		flags.remove(flagID);
		flags.add(flagID);
	}
	public void storeRuntimeMapping(String key,Object value) {
		vars.put(key,value.toString());
	}

	public boolean hasFlag(String flagID) {
		return flags.contains(flagID);
	}

	public boolean hasKey(String keyID) {
		return vars.containsKey(keyID);
	}

	public String getStringMap(String key) {
		return vars.get(key);
	}

	public int getIntegerMap(String key) {
		return Integer.parseInt(getStringMap(key));
	}

	public float getFloatMap(String key) {
		return Float.parseFloat(getStringMap(key));
	}

	public <E extends Enum<E>> E getEnum(String key, Class<E> e) {
		return Enum.valueOf(e,getStringMap(key));
	}
}
