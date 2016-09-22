package com.jc.util.stream;

/**
 * Created by James on 22/09/2016.
 *
 * Range Loop Utilities
 */
public class Range {

	public interface IIntFunctor<T> {
		void apply(int value);
	}
	private int start;
	private int end;
	private int diff;

	Range(int m,int e,int d) {
		start = m;
		end = e;
		diff = d;
	}

	public <T> void call(IIntFunctor<T> functor) {
		for(int i =  start; i < end; i+=diff) {
			functor.apply(i);
		}
	}

	public static Range in(int min,int max,int diff) {
		return new Range(min,max,diff);
	}

	public static Range in(int min,int max) {
		return in(min,max,1);
	}

	public static Range in(int max) {
		return in(0,max,1);
	}
}
