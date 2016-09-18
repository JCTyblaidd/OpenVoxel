package com.jc.util.event;

@FunctionalInterface
public interface EventFunctionHandle<T extends BaseEvent> {
	void acceptEvent(T event);
}
