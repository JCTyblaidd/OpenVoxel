package com.jc.util.format.json;

import java.util.*;

import static com.jc.util.format.json.JSONType.LIST;

public class JSONList<T extends JSONObject> extends JSONObject implements Iterable<JSONObject>{
	
	private List<T> backing;
	
	public JSONList(List<T> obj) {
		backing = obj;
	}
	
	public JSONList() {
		backing = new ArrayList<>();
	}

	@Override
	public JSONType getType() {
		return LIST;
	}
	
	@SuppressWarnings("unchecked")
	public void add(String str) {
		backing.add((T) new JSONString(str));
	}
	
	public void add(T obj) {
		backing.add(obj);
	}
	
	public T get(int i) {
		return backing.get(i);
	}
	
	public void remove(T obj) {
		backing.remove(obj);
	}

	public void set(int index, T obj) {
		backing.set(index, obj);
	}
	
	public void clear() {
		backing.clear();
	}
	
	public int size() {
		return backing.size();
	}
	
	@SuppressWarnings("all")
	public void sort() {
		Collections.sort((List) backing);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public JSONList<JSONObject> asList() {
		return (JSONList<JSONObject>) this;
	}
	
	@Override
	void _buildJSON(JSONBuilder builder) {
		builder.push();
		builder.writeLine("[");
		for(int i = 0; i < backing.size(); i++) {
			JSONObject obj = backing.get(i);
			obj._buildJSON(builder);
			if(i != (backing.size() - 1)) {
				builder.writeConditional(",", ", ");
				builder.nl();
			}
		}
		builder.pop();
		builder.nl();
		builder.write("]");
	}

	@Override
	void _buildBinaryJSON(JSONBinaryBuilder builder) {
		builder.writeType(LIST);
		builder.writeInteger(backing.size());
		for(T entry : backing) {
			entry._buildBinaryJSON(builder);
		}
	}

	@Override
	boolean _equal(Object obj) {
		JSONList<?> l = (JSONList<?>)obj;
		if(l.size() != size()) return false;
		return l.backing.containsAll(backing);//SHOULC WORK I THINK
	}
	
	public List<JSONObject> getJavaList() {
		List<JSONObject> list = new ArrayList<JSONObject>();
		for (JSONObject obj : this.backing) {
			list.add(obj);
		}
		return list;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Iterator<JSONObject> iterator() {
		return (Iterator<JSONObject>) backing.iterator();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Spliterator<JSONObject> spliterator() {
		return (Spliterator<JSONObject>) backing.spliterator();
	}
}
