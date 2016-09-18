package com.jc.util.format.json;

public abstract class JSONBuilder {
	
	public abstract void push();
	public abstract void pop();
	public abstract void write(Object... objs);
	public abstract void nl();
	
	public abstract boolean isPretty();
	public void writeConditional(Object norm,Object pretty) {
		if(isPretty()) {
			write(pretty);
		}else{
			write(norm);
		}
	}
	
	public abstract String BUILD();
	
	@Override
	public String toString() {
		return BUILD();
	}
	
	public void writeLine(Object... objs) {
		write(objs);
		nl();
	}
	
	public static class NormalJSONBuilder extends JSONBuilder {
		
		private StringBuilder build = new StringBuilder();
		
		@Override
		public void push() {}
		@Override
		public void pop() {}
		@Override
		public void write(Object... objs) {
			for(Object obj : objs) {
				build.append(obj.toString());
			}
		}

		@Override
		public void nl() {}
		
		@Override
		public String BUILD() {
			return build.toString();
		}
		
		@Override
		public boolean isPretty() {
			return false;
		}
	}
	
	public static class PrettyJSONBuilder extends JSONBuilder {
		
		private int num = 0;
		private StringBuilder build = new StringBuilder();
		
		@Override
		public void push() {
			num++;
		}

		@Override
		public void pop() {
			num--;
			if(num <0) throw new RuntimeException("Popping Error!");
		}

		@Override
		public void write(Object... objs) {
			for(Object obj : objs) {
				build.append(obj.toString());
			}
		}

		@Override
		public void nl() {
			build.append('\n');
			for(int i = 0; i < num; i++) {
				build.append('\t');
			}
		}

		@Override
		public String BUILD() {
			return build.toString();
		}
		
		
		@Override
		public boolean isPretty() {
			return true;
		}
	}
	
}
