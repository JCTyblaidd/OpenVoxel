package com.jc.util.stream.utils;

@FunctionalInterface
public interface TriConsumer<T,A,B> {
	
	void apply(T t, A a, B b);
}
