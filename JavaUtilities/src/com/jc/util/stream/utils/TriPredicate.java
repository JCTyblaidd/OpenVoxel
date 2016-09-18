package com.jc.util.stream.utils;

@FunctionalInterface
public interface TriPredicate<T,A,B> {
	boolean test(T t, A a, B b);
}
