package com.jc.util.config.json;

import com.jc.util.config.IConfig;
import com.jc.util.config.IConfigEntry;
import com.jc.util.config.util.AbstractConfigEntry;
import com.jc.util.format.json.*;

/**
 * Created by James on 13/08/2016.
 *
 */
public class JSONConfigEntry extends AbstractConfigEntry{

	JSONObject ref_object;
	JSONConfig ref_config;

	JSONConfigEntry(JSONObject obj,JSONConfig config) {
		ref_object = obj;
		ref_config = config;
	}


	private JSONMap<JSONObject> _map() {
		return ref_object.asMap();
	}
	private JSONList<JSONObject> _list() {
		return ref_object.asList();
	}


	private boolean _isMap() {
		return ref_object.getType() == JSONType.MAP;
	}

	private boolean _isList() {
		return ref_object.getType() == JSONType.LIST;
	}

	@Override
	public int getInt(String name) {
		return _map().get(name).asInteger();
	}

	@Override
	public float getFloat(String name) {
		return _map().get(name).asFloat();
	}

	@Override
	public boolean hasNull(String name) {
		return _map().get(name).isNull();
	}

	@Override
	public boolean getBool(String name) {
		return _map().get(name).asBool();
	}

	@Override
	public String getString(String name) {
		return _map().get(name).asString();
	}

	@Override
	public void setIntValue(String name, int val) {
		_map().put(name,val);

	}

	@Override
	public void setFloatValue(String name, float val) {
		_map().put(name,val);
	}

	@Override
	public void setNullValue(String name) {
		_map().put(name,null);
	}

	@Override
	public void setBoolValue(String name, boolean val) {
		_map().put(name,val);
	}

	@Override
	public void setStringValue(String name, String val) {
		_map().put(name,val);
	}

	@Override
	public boolean entryExists(String name) {
		return _map().get(name) != null;
	}

	@Override
	public boolean floatEntryExists(String name) {
		JSONObject obj = _map().get(name);
		return obj != null && obj.getType() == JSONType.FLOAT;
	}

	@Override
	public boolean intEntryExists(String name) {
		JSONObject obj = _map().get(name);
		return obj != null && obj.getType() == JSONType.INT;
	}

	@Override
	public boolean stringEntryExists(String name) {
		JSONObject obj = _map().get(name);
		return obj != null && obj.getType() == JSONType.STRING;
	}

	@Override
	public boolean boolEntryExists(String name) {
		JSONObject obj = _map().get(name);
		return obj != null && obj.getType() == JSONType.BOOL;
	}

	@Override
	public boolean listEntryExists(String name) {
		JSONObject obj = _map().get(name);
		return obj != null && obj.getType() == JSONType.LIST;
	}

	@Override
	public boolean mapEntryExists(String name) {
		JSONObject obj = _map().get(name);
		return obj != null && obj.getType() == JSONType.MAP;
	}

	@Override
	public boolean subEntryExists(String name) {
		JSONObject obj = _map().get(name);
		return obj != null && ( obj.getType() == JSONType.FLOAT || obj.getType() == JSONType.MAP);
	}

	@Override
	public IConfigEntry getMap(String name) {
		return new JSONConfigEntry(_map().get(name).asMap(),ref_config);
	}

	@Override
	public IConfigEntry getList(String name) {
		return new JSONConfigEntry(_map().get(name).asList(),ref_config);
	}

	@Override
	public IConfigEntry getSubEntry(String name) {
		return new JSONConfigEntry(_map().get(name),ref_config);
	}

	@Override
	public int size() {
		if(_isMap()) {
			return _map().size();
		}
		if(_isList()) {
			return _list().size();
		}
		throw new RuntimeException("Error: Can't get size!!");
	}

	@Override
	public boolean isList() {
		return _isList();
	}

	@Override
	public boolean isMap() {
		return _isMap();
	}

	@Override
	public void clearEntries() {
		if(_isList()) {
			_list().clear();
			return;
		}
		if(_isMap()) {
			_map().clear();
			return;
		}
		throw new RuntimeException("Error: Can't Clear");
	}

	@Override
	public IConfig getConfig() {
		return null;//TODO:
	}

	@Override
	public int getIntAt(int loc) {
		return _list().get(loc).asInteger();
	}

	@Override
	public float getFloatAt(int loc) {
		return _list().get(loc).asFloat();
	}

	@Override
	public boolean getBoolAt(int loc) {
		return _list().get(loc).asBool();
	}

	@Override
	public String getStringAt(int loc) {
		return _list().get(loc).asString();
	}

	@Override
	public boolean entryIsInt(int loc) {
		return _list().get(loc).getType() == JSONType.INT;
	}

	@Override
	public boolean entryIsFloat(int loc) {
		return _list().get(loc).getType() == JSONType.FLOAT;
	}

	@Override
	public boolean entryIsNull(int loc) {
		return _list().get(loc).getType() == JSONType.NULL;
	}

	@Override
	public boolean entryIsBool(int loc) {
		return _list().get(loc).getType() == JSONType.BOOL;
	}

	@Override
	public boolean entryIsString(int loc) {
		return _list().get(loc).getType() == JSONType.STRING;
	}

	@Override
	public void setIntAt(int loc, int val) {
		_list().set(loc,new JSONInteger(val));
	}

	@Override
	public void setFloatAt(int loc, float val) {
		_list().set(loc,new JSONFloat(val));
	}

	@Override
	public void setNullAt(int loc) {
		_list().set(loc,new JSONNull());
	}

	@Override
	public void setBoolAt(int loc, boolean val) {
		_list().set(loc,new JSONBoolean(val));
	}

	@Override
	public void setStringAt(int loc, String val) {
		_list().set(loc,new JSONString(val));
	}

	@Override
	public void appendIntVal(int val) {
		_list().add(new JSONInteger(val));
	}

	@Override
	public void appendFloatVal(float val) {
		_list().add(new JSONFloat(val));
	}

	@Override
	public void appendNullVal() {
		_list().add(new JSONNull());
	}

	@Override
	public void appendBoolVal(boolean val) {
		_list().add(new JSONBoolean(val));
	}

	@Override
	public void appendStringVal(String val) {
		_list().add(new JSONString(val));
	}

	@Override
	public IConfigEntry getMap(int loc) {
		return new JSONConfigEntry(_list().get(loc).asMap(),ref_config);
	}

	@Override
	public IConfigEntry getList(int loc) {
		return new JSONConfigEntry(_list().get(loc).asList(),ref_config);
	}

	@Override
	public IConfigEntry getSubEntry(int loc) {
		return new JSONConfigEntry(_list().get(loc),ref_config);
	}

}
