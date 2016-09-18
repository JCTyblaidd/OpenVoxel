package com.jc.util.stream.utils;

@FunctionalInterface
public interface QuadFunction<T,A,B,C,R> {
	R apply(T t, A a, B b, C c);
}
