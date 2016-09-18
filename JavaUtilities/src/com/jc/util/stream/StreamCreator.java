package com.jc.util.stream;

import java.util.Iterator;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class StreamCreator {

	/** Iterates util one of the streams finishes
	 * 
	 * @param a iterable 1
	 * @param b iterable 2
	 * @param func
	 */
	public static<SUB_A,SUB_B,OBJ_A extends Iterable<SUB_A>, OBJ_B extends Iterable<SUB_B>> void forEachDual
							(OBJ_A a,OBJ_B b,BiConsumer<SUB_A, SUB_B> func) {
		Iterator<SUB_A> aIter = a.iterator();
		Iterator<SUB_B> bIter = b.iterator();
		while(aIter.hasNext() && bIter.hasNext()) {
			SUB_A suba = aIter.next();
			SUB_B subb = bIter.next();
			func.accept(suba, subb);
		}
	}


	public static <T> void forEachArray(T[] arr, Consumer<T> consumer) {
		for(T t : arr) {
			consumer.accept(t);
		}
	}
	
}
