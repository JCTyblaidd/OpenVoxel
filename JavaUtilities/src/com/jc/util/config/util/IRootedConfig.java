package com.jc.util.config.util;

import com.jc.util.config.IConfig;
import com.jc.util.config.IConfigEntry;

import java.util.List;

/**
 * Created by James on 13/08/2016.
 */
public interface IRootedConfig extends IConfig {

	IConfigEntry getRoot();

	@Override
	default int getInt(String name) {
		return getRoot().getInt(name);
	}

	@Override
	default float getFloat(String name) {
		return getRoot().getFloat(name);
	}

	@Override
	default boolean hasNull(String name) {
		return getRoot().hasNull(name);
	}

	@Override
	default boolean getBool(String name) {
		return getRoot().getBool(name);
	}

	@Override
	default String getString(String name) {
		return getRoot().getString(name);
	}

	@Override
	default int getIntWithDefault(String name, int def) {
		return getRoot().getIntWithDefault(name,def);
	}

	@Override
	default float getFloatWithDefault(String name, float def) {
		return getRoot().getFloatWithDefault(name,def);
	}

	@Override
	default boolean isNullSetNullDefault(String name) {
		return getRoot().isNullSetNullDefault(name);
	}

	@Override
	default boolean getBoolWithDefault(String name, boolean def) {
		return getRoot().getBoolWithDefault(name,def);
	}

	@Override
	default String getStringWithDefault(String name, String def) {
		return getRoot().getStringWithDefault(name,def);
	}

	@Override
	default void setIntValue(String name, int val) {
		getRoot().setIntValue(name,val);
	}

	@Override
	default void setFloatValue(String name, float val) {
		getRoot().setFloatValue(name,val);
	}

	@Override
	default void setNullValue(String name) {
		getRoot().setNullValue(name);
	}

	@Override
	default void setBoolValue(String name, boolean val) {
		getRoot().setBoolValue(name,val);
	}

	@Override
	default void setStringValue(String name, String val) {
		getRoot().setStringValue(name,val);
	}

	@Override
	default String[] getStringArray(String name) {
		return getRoot().getStringArray(name);
	}

	@Override
	default float[] getFloatArray(String name) {
		return getRoot().getFloatArray(name);
	}

	@Override
	default int[] getIntArray(String name) {
		return getRoot().getIntArray(name);
	}

	@Override
	default boolean[] getBoolArray(String name) {
		return getRoot().getBoolArray(name);
	}

	@Override
	default List<String> getStringList(String name) {
		return getRoot().getStringList(name);
	}

	@Override
	default List<Float> getFloatList(String name) {
		return getRoot().getFloatList(name);
	}

	@Override
	default List<Integer> getIntList(String name) {
		return getRoot().getIntList(name);
	}

	@Override
	default List<Boolean> getBoolList(String name) {
		return getRoot().getBoolList(name);
	}

	@Override
	default List<String> getAutoSaveStringList(String name) {
		return getRoot().getAutoSaveStringList(name);
	}

	@Override
	default List<Float> getAutoSaveFloatList(String name) {
		return getRoot().getAutoSaveFloatList(name);
	}

	@Override
	default List<Integer> getAutoSaveIntList(String name) {
		return getRoot().getAutoSaveIntList(name);
	}

	@Override
	default List<Boolean> getAutoSaveBoolList(String name) {
		return getRoot().getAutoSaveBoolList(name);
	}

	@Override
	default boolean entryExists(String name) {
		return getRoot().entryExists(name);
	}

	@Override
	default boolean floatEntryExists(String name) {
		return getRoot().floatEntryExists(name);
	}

	@Override
	default boolean intEntryExists(String name) {
		return getRoot().intEntryExists(name);
	}

	@Override
	default boolean stringEntryExists(String name) {
		return getRoot().stringEntryExists(name);
	}

	@Override
	default boolean boolEntryExists(String name) {
		return getRoot().boolEntryExists(name);
	}

	@Override
	default boolean listEntryExists(String name) {
		return getRoot().listEntryExists(name);
	}

	@Override
	default boolean mapEntryExists(String name) {
		return getRoot().mapEntryExists(name);
	}

	@Override
	default boolean subEntryExists(String name) {
		return getRoot().subEntryExists(name);
	}

	@Override
	default IConfigEntry getMap(String name) {
		return getRoot().getMap(name);
	}

	@Override
	default IConfigEntry getList(String name) {
		return getRoot().getList(name);
	}

	@Override
	default IConfigEntry getSubEntry(String name) {
		return getRoot().getSubEntry(name);
	}

	@Override
	default int size() {
		return getRoot().size();
	}

	@Override
	default boolean isList() {
		return getRoot().isList();
	}

	@Override
	default boolean isMap() {
		return getRoot().isMap();
	}

	@Override
	default void clearEntries() {
		getRoot().clearEntries();
	}

	@Override
	default int getIntAt(int loc) {
		return getRoot().getIntAt(loc);
	}

	@Override
	default float getFloatAt(int loc) {
		return getRoot().getFloatAt(loc);
	}

	@Override
	default boolean getBoolAt(int loc) {
		return getRoot().getBoolAt(loc);
	}

	@Override
	default String getStringAt(int loc) {
		return getRoot().getStringAt(loc);
	}

	@Override
	default boolean entryIsInt(int loc) {
		return getRoot().entryIsInt(loc);
	}

	@Override
	default boolean entryIsFloat(int loc) {
		return getRoot().entryIsFloat(loc);
	}

	@Override
	default boolean entryIsNull(int loc) {
		return getRoot().entryIsNull(loc);
	}

	@Override
	default boolean entryIsBool(int loc) {
		return getRoot().entryIsBool(loc);
	}

	@Override
	default boolean entryIsString(int loc) {
		return getRoot().entryIsString(loc);
	}

	@Override
	default void setBoolAt(int loc, boolean val) {
		getRoot().setBoolAt(loc,val);
	}

	@Override
	default void setFloatAt(int loc, float val) {
		getRoot().setFloatAt(loc,val);
	}

	@Override
	default void setIntAt(int loc, int val) {
		getRoot().setIntAt(loc,val);
	}

	@Override
	default void setNullAt(int loc) {
		getRoot().setNullAt(loc);
	}

	@Override
	default void setStringAt(int loc, String val) {
		getRoot().setStringAt(loc,val);
	}

	@Override
	default void appendBoolVal(boolean val) {
		getRoot().appendBoolVal(val);
	}

	@Override
	default void appendFloatVal(float val) {
		getRoot().appendFloatVal(val);
	}

	@Override
	default void appendIntVal(int val) {
		getRoot().appendIntVal(val);
	}

	@Override
	default void appendNullVal() {
		getRoot().appendNullVal();
	}

	@Override
	default void appendStringVal(String val) {
		getRoot().appendStringVal(val);
	}

	@Override
	default IConfigEntry getMap(int loc) {
		return getRoot().getMap(loc);
	}

	@Override
	default IConfigEntry getList(int loc) {
		return getRoot().getList(loc);
	}

	@Override
	default IConfigEntry getSubEntry(int loc) {
		return getRoot().getSubEntry(loc);
	}

}
