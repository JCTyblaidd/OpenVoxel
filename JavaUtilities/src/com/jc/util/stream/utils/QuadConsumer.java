package com.jc.util.stream.utils;

@FunctionalInterface
public interface QuadConsumer<T,A,B,C> {
	void apply(T t, A a, B b, C c);
}
