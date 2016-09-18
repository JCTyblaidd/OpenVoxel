package com.jc.util.type;

import com.jc.util.stream.utils.TriConsumer;

import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Created by James on 29/08/2016.
 */
public final class Primitive<T,ARR> {

	public static Primitive<Boolean,boolean[]>  BOOL = new Primitive<>(boolean[]::new,  (arr,i)->arr[i], (arr,i,v)->arr[i]=v, a->a.length, Arrays::copyOf, null);//TODO: enable sorting
	public static Primitive<Byte,byte[]>        BYTE = new Primitive<>(byte[]::new,     (arr,i)->arr[i], (arr,i,v)->arr[i]=v, a->a.length, Arrays::copyOf, Arrays::sort);
	public static Primitive<Character,char[]>   CHAR = new Primitive<>(char[]::new,     (arr,i)->arr[i], (arr,i,v)->arr[i]=v, a->a.length, Arrays::copyOf, Arrays::sort);
	public static Primitive<Short,short[]>      SHORT = new Primitive<>(short[]::new,   (arr,i)->arr[i], (arr,i,v)->arr[i]=v, a->a.length, Arrays::copyOf, Arrays::sort);
	public static Primitive<Integer,int[]>      INT = new Primitive<>(int[]::new,       (arr,i)->arr[i], (arr,i,v)->arr[i]=v, a->a.length, Arrays::copyOf, Arrays::sort);
	public static Primitive<Long,long[]>        LONG = new Primitive<>(long[]::new,     (arr,i)->arr[i], (arr,i,v)->arr[i]=v, a->a.length, Arrays::copyOf, Arrays::sort);
	public static Primitive<Float,float[]>      FLOAT = new Primitive<>(float[]::new,   (arr,i)->arr[i], (arr,i,v)->arr[i]=v, a->a.length, Arrays::copyOf, Arrays::sort);
	public static Primitive<Double,double[]>    DOUBLE = new Primitive<>(double[]::new, (arr,i)->arr[i], (arr,i,v)->arr[i]=v, a->a.length, Arrays::copyOf, Arrays::sort);

	private Function<Integer,ARR> newArrayMethod;
	private BiFunction<ARR,Integer,T> getterMethod;
	private TriConsumer<ARR,Integer,T> setterMethod;
	private Function<ARR,Integer> getLengthMethod;
	private BiFunction<ARR,Integer,ARR> arrayCopyOfMethod;
	private TriConsumer<ARR,Integer,Integer> sortMethod;

	private ARR empty_array;
	private T zero;

	private Primitive(Function<Integer,ARR> newArr,BiFunction<ARR,Integer,T> get,TriConsumer<ARR,Integer,T> set,Function<ARR,Integer> getLen,BiFunction<ARR,Integer,ARR> copy,TriConsumer<ARR,Integer,Integer> sort) {
		newArrayMethod = newArr;
		getterMethod = get;
		setterMethod = set;
		getLengthMethod = getLen;
		arrayCopyOfMethod = copy;
		sortMethod = sort;
		empty_array = newArray(0);
		zero = get(newArray(1),0);//Get Instantiation Value//
	}

	public ARR newArray(int length) {
		return newArrayMethod.apply(length);
	}

	public T get(ARR array, int index) {
		return getterMethod.apply(array,index);
	}

	public void set(ARR array, int index, T value) {
		setterMethod.apply(array,index,value);
	}

	public int length(ARR array) {
		return getLengthMethod.apply(array);
	}

	public ARR copyOf(ARR array, int newLength) {
		return arrayCopyOfMethod.apply(array,newLength);
	}

	public void sort(ARR array, int fromIndex, int toIndex) {
		sortMethod.apply(array,fromIndex,toIndex);
	}

	public ARR getEmptyArray() {
		return empty_array;
	}

	public T getZero() {
		return zero;
	}
}
