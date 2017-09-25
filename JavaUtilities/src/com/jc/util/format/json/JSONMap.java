package com.jc.util.format.json;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import static com.jc.util.format.json.JSONType.MAP;

public class JSONMap<V extends JSONObject> extends JSONObject {
	
	private Map<JSONString,V> backing;
	
	public JSONMap() {
		backing = new HashMap<>();
	}
	
	public JSONMap(Map<JSONString,V> m) {
		backing = m;
	}
	
	public void put(JSONString key, V val) {
		backing.put(key, val);
	}
	
	public boolean contains(JSONString str){
		return backing.containsKey(str);
	}
	
	public boolean contains(String str) {
		return contains(new JSONString(str));
	}
	
	@SuppressWarnings("unchecked")
	public void put(JSONString k, Object v) {
		JSONObject v1 = JSON.fromObject(v);
		backing.put(k,(V)v1);
	}
	
	@SuppressWarnings("unchecked")
	public void put(String k, Object v) {
		JSONString str = new JSONString(k);
		JSONObject v1 = JSON.fromObject(v);
		backing.put(str, (V) v1);
	}
	
	public V get(JSONString key) {
		return backing.get(key);
	}
	
	public V get(String key) {
		return backing.get(new JSONString(key));
	}
	
	public int size() {
		return backing.size();
	}

	public void clear() {
		backing.clear();
	}

	public JSONString getKeyAt(int i) {
		return (JSONString) backing.keySet().toArray()[i];
	}
	
	@SuppressWarnings("unchecked")
	public V getWithDefault(String key,Object dflt) {
		if(contains(key)) {
			return get(key);
		}else{
			put(key,dflt);
			return (V) dflt;
		}
	}
	
	@SuppressWarnings("unchecked")
	public V getWithDefault(JSONString key,Object dflt) {
		if(contains(key)) {
			return get(key);
		}else{
			put(key,dflt);
			return (V) dflt;
		}
	}

	@Override
	public JSONType getType() {
		return MAP;
	}

	@Override
	void _buildJSON(JSONBuilder builder) {
		builder.write("{");
		builder.push();
		builder.nl();
		///SHIZ
		int i = 0;
		for(JSONString key : backing.keySet()) {
			V val = backing.get(key);
			key._buildJSON(builder);
			builder.writeConditional(":", " : ");
			val._buildJSON(builder);
			if(i != (backing.size() - 1)) {
				builder.writeConditional(",", ", ");
				builder.nl();
			}
			i++;
		}
		builder.pop();
		builder.nl();
		builder.write("}");
	}

	@Override
	void _buildBinaryJSON(JSONBinaryBuilder builder) {
		builder.writeType(MAP);
		builder.writeInteger(backing.size());
		for(Map.Entry<JSONString,V> entry: backing.entrySet()) {
			entry.getKey()._buildBinaryJSON(builder);
			entry.getValue()._buildBinaryJSON(builder);
		}
 	}

	@SuppressWarnings("unchecked")
	@Override
	public JSONMap< JSONObject> asMap() {
		return (JSONMap<JSONObject>) this;
	}
	
	@Override
	boolean _equal(Object obj) {
		JSONMap<?> map = (JSONMap<?>) obj;
		if(map.backing.size() != backing.size()) return false;
		if(!map.backing.keySet().containsAll(backing.keySet())) return false;
		if(!map.backing.values().containsAll(backing.values())) return false;
		return true;//GOOD ENOUGH
	}
	
	public HashMap<String, V> getHashMap() {
		HashMap<String, V> m = new HashMap<>();
		for (JSONString s : backing.keySet()) {
			m.put(s.asString(), backing.get(s));
		}
		return m;
	}

	public void forEach(BiConsumer<JSONString,V> consumer) {
		backing.forEach(consumer::accept);
	}
	
}
