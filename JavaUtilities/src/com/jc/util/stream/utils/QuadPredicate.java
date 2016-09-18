package com.jc.util.stream.utils;


@FunctionalInterface
public interface QuadPredicate<T,A,B,C> {
	boolean test(T t, A a, B b, C c);
}
