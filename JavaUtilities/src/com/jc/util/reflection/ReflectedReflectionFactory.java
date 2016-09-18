package com.jc.util.reflection;

import com.jc.util.reflection.Reflector.ReflectedConstructor;
import com.jc.util.reflection.Reflector.ReflectedField;
import com.jc.util.reflection.Reflector.ReflectedMethod;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

@SuppressWarnings({ "all" })
public class ReflectedReflectionFactory<E> {
	
	//STATIC SILENT REFLECTION FACTORY NINJANESS
	private static ReflectedField<Class> factField;
	private static Reflector factoryReflect;
	static {
		Reflector<Class> ref = new Reflector(Class.class);
		factField = ref.getField("reflectionFactory");
		factoryReflect = new Reflector("sun.reflect.ReflectionFactory");
	}
	
	private Class<E> clz;
	private Object factory;
	private Reflector<E> reflect;
	
	protected ReflectedReflectionFactory(Class<E> c,Reflector<E> reflector) {
		clz = c;
		reflect = reflector;
		factory = factField.get(clz);
	}
	
	///////////////////REFLECTION FACTORY EVILENESS//////////////////
	public Object invokeFactory(String name,Object... objs) {
		return factoryReflect.getMethod(name).invoke(factory,(Object[])objs);
	}
	
	/** Returns A ConstructorAccessor **/
//	public Object generateConstructorAccessor(Class... clazzes){
//		ReflectedConstructor con = reflect.getConstructor((Class[])clazzes);
//		return factoryReflect.getMethod("newConstructorAccessor").invoke(factory, con.getReference());
//	}
	
/////////////////////////////BASIC ACCESSOR CLASSES////////////////////////////////
	
	public ReflectedConstructorAccessor<E> generateConstructorAccessor(Constructor<? extends E> construct) {
		return new ReflectedConstructorAccessor<E>(clz, invokeFactory("newConstructorAccessor",construct));
	}
	
	public ReflectedFieldAccessor<E> generateFieldAccessor(Field field) {		//TODO FALSE OR TRUE?
		return new ReflectedFieldAccessor<E>(clz, invokeFactory("newFieldAccessor",field,false));
	}
	
	public ReflectedMethodAccessor<E> generateMethodAccessor(Method method) {
		return new ReflectedMethodAccessor<E>(clz,invokeFactory("newMethodAccessor",method));
	}
	
////////////////////////ADVANCED GENERATION SHIZZLE////////////////////////////////
	
	public ReflectedConstructorAccessor<E> generateConstructorAccessor(Class... clazzes) {
		ReflectedConstructor con = reflect.getConstructor((Class[])clazzes);
		return generateConstructorAccessor(con.getReference());
	}
	
	public ReflectedFieldAccessor<E> generateFieldAccessor(String name) {
		ReflectedField field = reflect.getField(name);
		return generateFieldAccessor(field.getReference());
	}
	
	public ReflectedMethodAccessor<E> generateMethodAccessor(String name,int paramcount) {
		ReflectedMethod meth = reflect.getMethod(name, paramcount);
		return generateMethodAccessor(meth.getReference());
	}
	
	public ReflectedFieldAccessor<E> generateFieldAccessor(Field field, boolean flag) {		//TODO FALSE OR TRUE?
		return new ReflectedFieldAccessor<E>(clz, invokeFactory("newFieldAccessor",field,flag));
	}
	
	/**Rewrite Shizzle**/
	////REMOVED - DUE TO REMOVAL OF STUFF//////
	//////public ReflectiveClassWriter<E> generateClassWriter() {
	//////	return new ReflectiveClassWriter<E>(clz, reflect, this);
	////////}
	
/////////////////////////ACCESSOR WRAPPING CLASSES////////////////////
	
	public static class ReflectedFieldAccessor<E> {
		private Object accessor;
		private Class<? extends E> clazz;
		private static final Reflector freflect = new Reflector<>("sun.reflect.FieldAccessor");
		private ReflectedFieldAccessor(Class<? extends E> c,Object a) {
			clazz = c;accessor=a;
		}
		private Object invokeAccessor(String name,Object... values) {
			return freflect.getMethod(name).invoke(accessor, (Object[])values);
		}
		public Object getAccessor() {
			return accessor;
		}
		//////////////ACTUAL ACCESSOR SHIZZLE//////////////////
		public Object get(Object obj) {
			return invokeAccessor("get",obj);
		}
		public boolean getBoolean(Object obj)	  {
			return (boolean) invokeAccessor("getBoolean",obj);
		}
		public byte getByte(Object obj) {
			return (byte) invokeAccessor("getByte",obj);
		}
		public char getChar(Object obj) {
			return (char) invokeAccessor("getChar",obj);
		}
		public short getShort(Object obj) {
			return (short) invokeAccessor("getShort",obj);
		}
		public int getInt(Object obj) {
			return (int) invokeAccessor("getInt",obj);
		}
		public long getLong(Object obj) {
			return (long) invokeAccessor("getLong",obj);
		}
		public float getFloat(Object obj) {
			return (float) invokeAccessor("getFloat", obj);
		}
		public double getDouble(Object obj) {
			return (double) invokeAccessor("getDouble",obj);
		}
		public void set(Object obj, Object val) {
			invokeAccessor("set",obj,val);
		}
		public void setBoolean(Object obj, boolean val) {
			invokeAccessor("setBoolean",obj,val);
		}
		public void setByte(Object obj, byte val) {
			invokeAccessor("setByte",obj,val);
		}
		public void setChar(Object obj, char val) {
			invokeAccessor("setChar",obj,val);
		}
		public void setShort(Object obj, short val) {
			invokeAccessor("setShort",obj,val);
		}
		public void setInt(Object obj, int val) {
			invokeAccessor("setInt",obj,val);
		}
		public void setLong(Object obj, long val) {
			invokeAccessor("setLong",obj,val);
		}
		public void setFloat(Object obj, float val) {
			invokeAccessor("setFloat", obj,val);
		}
		public void setDouble(Object obj, double val) {
			invokeAccessor("setDouble", obj,val);
		}
	}
	
	public static class ReflectedMethodAccessor<E> {
		private Object accessor;
		private Class<? extends E> clazz;
		private static final Reflector mreflect = new Reflector<>("sun.reflect.MethodAccessor");
		private ReflectedMethodAccessor(Class<? extends E> c,Object a) {
			clazz = c;accessor=a;
		}
		public Object getAccessor() {
			return accessor;
		}
		private Object invokeAccessor(String name,Object... values) {
			return mreflect.getMethod(name).invoke(accessor, (Object[])values);
		}
		public Object invoke(Object val,Object... args) {
			return invokeAccessor("invoke", val,(Object[])args);
		}
		public Object invokeStatic(Object... args) {
			return invoke(null,(Object[])args);
		}
	}

	public static class ReflectedConstructorAccessor<E> {
		private Object accessor;
		private Class<? extends E> clazz;
		private static final Reflector creflect = new Reflector<>("sun.reflect.ConstructorAccessor");
		private ReflectedConstructorAccessor(Class<? extends E> c,Object a) {
			clazz = c;accessor=a;
		}
		public Object getAccessor() {
			return accessor;
		}
		private Object invokeAccessor(String name,Object... values) {
			return creflect.getMethod(name).invoke(accessor, (Object[])values);
		}
		public E newInstance(Object... args) {
			return (E) invokeAccessor("newInstance",(Object[])args);
		}
	}
}
