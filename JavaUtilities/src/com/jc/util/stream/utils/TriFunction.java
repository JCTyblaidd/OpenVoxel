package com.jc.util.stream.utils;

@FunctionalInterface
public interface TriFunction<T,A,B,R> {
	R apply(T t, A a, B b);
}
