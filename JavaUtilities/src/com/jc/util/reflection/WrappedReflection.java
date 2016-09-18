package com.jc.util.reflection;

/**Quick Lazy Class For Wrapping An Object And Twiddling With IT**/
@SuppressWarnings({"rawtypes","unchecked"})
public class WrappedReflection<E> {
	
	private E objectcache;
	private Class clazz;
	private Reflector reflect;
	
	protected WrappedReflection(E cache,Class<? extends E> clz,Reflector<? extends E> ref) {
		objectcache=cache;clazz=clz;reflect=ref;
	}
	protected WrappedReflection(E cache,Class<? extends E> clz) {
		objectcache=cache;clazz=clz;reflect = new Reflector<>(clazz);
	}
	protected WrappedReflection(E cache) {
		objectcache=cache;clazz=(Class<? extends E>)cache.getClass();reflect = new Reflector<>(clazz);
	}
	////////MORE USFULE//////////////////////////////
	public Reflector<E> deeply() {
		return reflect;
	}
	
	///////////////////////FIELDS///////////////////////////////
	public WrappedReflection<E> set(String name,Object newval) {
		reflect.getFieldDeep(name).set(objectcache, newval);
		return this;
	}
	public WrappedReflection<E> setFinal(String name,Object newval) {
		reflect.getFieldDeep(name).setFinal(objectcache, newval);
		return this;
	}
	public Object get(String name) {
		return reflect.getFieldDeep(name).get(objectcache);
	}
	////////////////METHODS///////////////////////////////////
	public Object invoke(String name,Object... params) {
		Class<?>[] clazzes = new Class<?>[params.length];
		for(int i = 0; i < params.length; i++) {
			clazzes[i] = Reflect.byType.toprimitive(params[i]);
		}
		return reflect.getMethodDeep(name,(Class[])clazzes).invoke(objectcache, params);
	}
	
}
