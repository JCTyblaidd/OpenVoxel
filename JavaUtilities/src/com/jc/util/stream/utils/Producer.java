package com.jc.util.stream.utils;

/**
 * Created by James on 01/09/2016.
 */
@FunctionalInterface
public interface Producer<T> {

	T create();

}
