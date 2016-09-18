package com.jc.util.config;

import java.util.List;

public interface IConfigEntry {


	/**
	 * @exception RuntimeException if the entry doesn't exist
	 * @param name the id of the entry
	 * @return the integer value
	 */
	int getInt(String name);
	/**
	 * @exception RuntimeException if the entry doesn't exist
	 * @param name the id of the entry
	 * @return the float value
	 */
	float getFloat(String name);
	/**
	 * @exception RuntimeException if the entry doesn't exist
	 * @param name the id of the entry
	 * @return is the entry is a void
	 */
	boolean hasNull(String name);
	/**
	 * @exception RuntimeException if the entry doesn't exist
	 * @param name the id of the entry
	 * @return the boolean value
	 */
	boolean getBool(String name);
	/**
	 * @exception RuntimeException if the entry doesn't exist
	 * @param name the id of the entry
	 * @return the string value
	 */
	String getString(String name);

	/**
	 * @param name the id of the entry
	 * @param def the value to set it to if it doesn't exist
	 * @return the value(def if does'nt exist)
	 */
	int getIntWithDefault(String name, int def);
	/**
	 * @param name the id of the entry
	 * @param def the value to set it to if it doesn't exist
	 * @return the value(def if does'nt exist)
	 */
	float getFloatWithDefault(String name, float def);
	/**
	 * @param name the id of the entry
	 * @return the value(null if does'nt exist)
	 */
	boolean isNullSetNullDefault(String name);
	/**
	 * @param name the id of the entry
	 * @param def the value to set it to if it doesn't exist
	 * @return the value(def if does'nt exist)
	 */
	boolean getBoolWithDefault(String name, boolean def);
	/**
	 * @param name the id of the entry
	 * @param def the value to set it to if it doesn't exist
	 * @return the value(def if does'nt exist)
	 */
	String getStringWithDefault(String name,String def);

	/**
	 * @param name the entry id
	 * @param val the new value
	 */
	void setIntValue(String name, int val);
	/**
	 * @param name the entry id
	 * @param val the new value
	 */
	void setFloatValue(String name, float val);
	/**
	 * @param name the entry id
	 */
	void setNullValue(String name);
	/**
	 * @param name the entry id
	 * @param val the new value
	 */
	void setBoolValue(String name, boolean val);
	/**
	 * @param name the entry id
	 * @param val the new value
	 */
	void setStringValue(String name, String val);

	/**
	 * @exception if the entry isn't a list of strings
	 * @param name the id of the entry
	 * @return the array
	 */
	String[] getStringArray(String name);
	/**
	 * @exception if the entry isn't a list of floats
	 * @param name the id of the entry
	 * @return the array
	 */
	float[] getFloatArray(String name);
	/**
	 * @exception if the entry isn't a list of ints
	 * @param name the id of the entry
	 * @return the array
	 */
	int[] getIntArray(String name);
	/**
	 * @exception if the entry isn't a list of bools
	 * @param name the id of the entry
	 * @return the array
	 */
	boolean[] getBoolArray(String name);

	/**
	 * @exception if the entry isn't a list of strings
	 * @param name the id of the entry
	 * @return the list
	 */
	List<String> getStringList(String name);
	/**
	 * @exception if the entry isn't a list of floats
	 * @param name the id of the entry
	 * @return the list
	 */
	List<Float> getFloatList(String name);
	/**
	 * @exception if the entry isn't a list of ints
	 * @param name the id of the entry
	 * @return the list
	 */
	List<Integer> getIntList(String name);
	/**
	 * @exception if the entry isn't a list of bools
	 * @param name the id of the entry
	 * @return the list
	 */
	List<Boolean> getBoolList(String name);

	/**
	 * @exception if the entry isn't a list of strings
	 * @param name the id of the entry
	 * @return the list, that will automatically update to and from the config
	 */
	List<String> getAutoSaveStringList(String name);
	/**
	 * @exception if the entry isn't a list of floats
	 * @param name the id of the entry
	 * @return the list, that will automatically update to and from the config
	 */
	List<Float> getAutoSaveFloatList(String name);
	/**
	 * @exception if the entry isn't a list of ints
	 * @param name the id of the entry
	 * @return the list, that will automatically update to and from the config
	 */
	List<Integer> getAutoSaveIntList(String name);
	/**
	 * @exception if the entry isn't a list of bools
	 * @param name the id of the entry
	 * @return the list, that will automatically update to and from the config
	 */
	List<Boolean> getAutoSaveBoolList(String name);

	/**
	 * @param name the id of the entry
	 * @return is the entry exists
	 */
	boolean entryExists(String name);
	/**
	 * @param name the id of the entry
	 * @return is the entry exists and is a float
	 */
	boolean floatEntryExists(String name);
	/**
	 * @param name the id of the entry
	 * @return is the entry exists and is an int
	 */
	boolean intEntryExists(String name);
	/**
	 * @param name the id of the entry
	 * @return is the entry exists and is a string
	 */
	boolean stringEntryExists(String name);
	/**
	 * @param name the id of the entry
	 * @return is the entry exists and is a boolean
	 */
	boolean boolEntryExists(String name);
	/**
	 * @param name the id of the entry
	 * @return is the entry exists and is a list
	 */
	boolean listEntryExists(String name);
	/**
	 * @param name the id of the entry
	 * @return is the entry exists and is a map
	 */
	boolean mapEntryExists(String name);
	/**
	 * @param name the id of the entry
	 * @return is the entry exists and is a sub entry(list of map)
	 */
	boolean subEntryExists(String name);

	/**
	 * @param name the id of the entry
	 * @return the sub map, or creates one if it doesn't exist
	 */
	IConfigEntry getMap(String name);
	/**
	 * @param name the id of the entry
	 * @return the sub list, or creates one if it doesn't exist
	 */
	IConfigEntry getList(String name);

	/**
	 * @param name the id of the entry
	 * @return the sub entry
	 */
	IConfigEntry getSubEntry(String name);


	/**
	 * @return the size of the list of map
	 */
	int size();

	/**
	 * @return if this entry is a list
	 */
	boolean isList();

	/**
	 * @return if this entry is a map
	 */
	boolean isMap();

	/**
	 * Remove All Entries
	 */
	void clearEntries();

	/**
	 * @return the master config file
	 */
	IConfig getConfig();

	/**
	 * @exception RuntimeException if the index is out of bounds or not an int
	 * @param loc the index of the item
	 * @return the integer value
	 */
	int getIntAt(int loc);
	/**
	 * @exception RuntimeException if the index is out of bounds or not an float
	 * @param loc the index of the item
	 * @return the integer value
	 */
	float getFloatAt(int loc);
	/**
	 * @exception RuntimeException if the index is out of bounds or not an boolean
	 * @param loc the index of the item
	 * @return the integer value
	 */
	boolean getBoolAt(int loc);
	/**
	 * @exception RuntimeException if the index is out of bounds or not an string
	 * @param loc the index of the item
	 * @return the integer value
	 */
	String getStringAt(int loc);

	/**
	 * @param loc the index of the item
	 * @return if the entry is an int
	 */
	boolean entryIsInt(int loc);
	/**
	 * @param loc the index of the item
	 * @return if the entry is an float
	 */
	boolean entryIsFloat(int loc);
	/**
	 * @param loc the index of the item
	 * @return if the entry is an void
	 */
	boolean entryIsNull(int loc);
	/**
	 * @param loc the index of the item
	 * @return if the entry is an bool
	 */
	boolean entryIsBool(int loc);
	/**
	 * @param loc the index of the item
	 * @return if the entry is an string
	 */
	boolean entryIsString(int loc);

	/**
	 * @param loc the index of the item
	 * @param val the new value
	 */
	void setIntAt(int loc, int val);
	/**
	 * @param loc the index of the item
	 * @param val the new value
	 */
	void setFloatAt(int loc, float val);
	/**
	 * @param loc the index of the item
	 */
	void setNullAt(int loc);
	/**
	 * @param loc the index of the item
	 * @param val the new value
	 */
	void setBoolAt(int loc, boolean val);
	/**
	 * @param loc the index of the item
	 * @param val the new value
	 */
	void setStringAt(int loc, String val);

	/**
	 * @param val the value to append to the list
	 */
	void appendIntVal(int val);
	/**
	 * @param val the value to append to the list
	 */
	void appendFloatVal(float val);
	/**
	 * append a null to the list
	 */
	void appendNullVal();
	/**
	 * @param val the value to append to the list
	 */
	void appendBoolVal(boolean val);
	/**
	 * @param val the value to append to the list
	 */
	void appendStringVal(String val);

	/**
	 * @exception RuntimeException if the entry is not a map
	 * @param loc the index of the entry
	 * @return the map
	 */
	IConfigEntry getMap(int loc);
	/**
	 * @exception RuntimeException if the entry is not a list
	 * @param loc the index of the entry
	 * @return the list
	 */
	IConfigEntry getList(int loc);

	/**
	 * @exception RuntimeException if the entry is not a map or list
	 * @param loc the index of the entry
	 * @return the entry
	 */
	IConfigEntry getSubEntry(int loc);

}
