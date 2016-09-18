package com.jc.util.event;

@FunctionalInterface
public interface EventWrappedFunction<OBJ extends EventListener,E> {
	void push(OBJ obj, E e);
}
