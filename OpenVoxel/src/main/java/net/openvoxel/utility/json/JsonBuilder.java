package net.openvoxel.utility.json;

public class JsonBuilder {

	private StringBuilder builder;
	private boolean pretty;
	private int offset;

	public JsonBuilder(boolean isPrettyPrint) {
		builder = new StringBuilder();
		pretty = isPrettyPrint;
		offset = 0;
	}

	@Override
	public String toString() {
		return builder.toString();
	}

	private void _pretty() {
		if(!pretty) return;
		for(int i = 0; i < offset; i++) builder.append('\t');
	}

	private void _space() {
		if(pretty) builder.append(' ');
	}

	private void _newline() {
		if(pretty) builder.append('\n');
	}

	public void enterMap() {
		_pretty();
		builder.append('{');
		_newline();
		offset += 1;
	}

	public void exitMap() {
		_newline();
		offset -= 1;
		_pretty();
		builder.append('}');
		_newline();
	}

	public void enterList() {
		_pretty();
		builder.append('[');
		_newline();
		offset += 1;
	}

	public void exitList() {
		_newline();
		offset -= 1;
		_pretty();
		builder.append(']');
		_newline();
	}

	public void key(CharSequence key) {
		_pretty();
		builder.append(key);
		_space();
		builder.append(':');
		_space();
	}

	public void next() {
		builder.append(',');
		_newline();
	}

	public void nullValue() {
		_pretty();
		builder.append("null");
	}

	public void value(long val) {
		_pretty();
		builder.append(val);
	}

	public void value(int val) {
		_pretty();
		builder.append(val);
	}

	public void value(double val) {
		_pretty();
		builder.append(val);
	}

	public void value(boolean val) {
		_pretty();
		builder.append(val);
	}

	public void value(CharSequence val) {
		_pretty();
		builder.append(val);
	}
}
