package com.jc.util.config.util;

import com.jc.util.config.IConfigEntry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by James on 13/08/2016.
 */
public abstract class AbstractConfigEntry implements IConfigEntry{

	@Override
	public String getStringWithDefault(String name, String def) {
		if(stringEntryExists(name)) {
			return getString(name);
		}
		setStringValue(name,def);
		return def;
	}

	@Override
	public boolean getBoolWithDefault(String name, boolean def) {
		if(boolEntryExists(name)) {
			return getBool(name);
		}
		setBoolValue(name,def);
		return def;
	}

	@Override
	public int getIntWithDefault(String name, int def) {
		if(intEntryExists(name)) {
			return getInt(name);
		}
		setIntValue(name,def);
		return def;
	}

	@Override
	public float getFloatWithDefault(String name, float def) {
		if(floatEntryExists(name)) {
			return getFloat(name);
		}
		setFloatValue(name,def);
		return def;
	}

	@Override
	public boolean isNullSetNullDefault(String name) {
		if(!entryExists(name)) {
			setNullValue(name);
		}
		return hasNull(name);
	}

	//Build Array//
	@Override
	public String[] getStringArray(String name) {
		IConfigEntry entry = getList(name);
		String[] arr = new String[entry.size()];
		for(int i = 0; i < arr.length; i++) {
			arr[i] = entry.getStringAt(i);
		}
		return arr;
	}

	@Override
	public boolean[] getBoolArray(String name) {
		IConfigEntry entry = getList(name);
		boolean[] arr = new boolean[entry.size()];
		for(int i = 0; i < arr.length; i++) {
			arr[i] = entry.getBoolAt(i);
		}
		return arr;
	}

	@Override
	public float[] getFloatArray(String name) {
		IConfigEntry entry = getList(name);
		float[] arr = new float[entry.size()];
		for(int i = 0; i < arr.length; i++) {
			arr[i] = entry.getFloatAt(i);
		}
		return arr;
	}

	@Override
	public int[] getIntArray(String name) {
		IConfigEntry entry = getList(name);
		int[] arr = new int[entry.size()];
		for(int i = 0; i < arr.length; i++) {
			arr[i] = entry.getIntAt(i);
		}
		return arr;
	}



	@Override
	public List<String> getStringList(String name) {
		return Arrays.asList(getStringArray(name));
	}

	@Override
	public List<Boolean> getBoolList(String name) {
		boolean[] arr = getBoolArray(name);
		List<Boolean> list = new ArrayList<>(arr.length);
		for (boolean anArr : arr) {
			list.add(anArr);
		}
		return list;
	}

	@Override
	public List<Float> getFloatList(String name) {
		float[] arr = getFloatArray(name);
		List<Float> list = new ArrayList<>(arr.length);
		for (float anArr : arr) {
			list.add(anArr);
		}
		return list;
	}

	@Override
	public List<Integer> getIntList(String name) {
		int[] arr = getIntArray(name);
		List<Integer> list = new ArrayList<>(arr.length);
		for (int anArr : arr) {
			list.add(anArr);
		}
		return list;
	}


	@Override
	public List<String> getAutoSaveStringList(String name) {
		return null;
	}

	@Override
	public List<Boolean> getAutoSaveBoolList(String name) {
		return null;
	}

	@Override
	public List<Float> getAutoSaveFloatList(String name) {
		return null;
	}

	@Override
	public List<Integer> getAutoSaveIntList(String name) {
		return null;
	}
}
