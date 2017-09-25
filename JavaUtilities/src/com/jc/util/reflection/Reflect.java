package com.jc.util.reflection;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Monolithic Reflection Handling And Accessor
 *  Use To Access Spin Of Classes In The Package
 * */
public class Reflect {
///////////////////////// QUICK INITIATION//////////////////////////
	public static <E extends Object> WrappedReflection<E> on(E obj) {
		return new WrappedReflection<>(obj);
	}
	
	public static <E> Reflector<E> on(Class<E> clazz) {
		return new Reflector<>(clazz);
	}
	
	public static <E extends Enum<E>> EnumBuster<E> onEnum(Class<E> clazz) {
		return new EnumBuster<>(clazz, Reflect.on(clazz));
	}
	
	public static <E> ReflectedReflectionFactory<E> byFactory(Class<E> clazz) {
		return new ReflectedReflectionFactory<>(clazz, Reflect.on(clazz));
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <E> Reflector<E> onClass(E obj) {
		return new Reflector(obj.getClass());
	}
	
//////////////NON CLASS RELATED?//////////////////////////////////
	/**Get Wrapper To Access The Memory Of The JVM
	 * {Allows Silly Things Like Setting An Integer to a String Value
	 *   -  So Be Careful}
	 * **/
	public static ReflectedUnsafe withUnsafe() {
		return ReflectedUnsafe.Unsafe();
	}
	/**Enable Debug Spew.. [Off By Default]**/
	public static void withDebug() {
		Reflector.enableDebugMode();
	}
	/**Only Needs To Be Called After Enabling [Off By Default]**/
	public static void withoutDebug() {
		Reflector.disableDebugMode();
	}
	
	//TODO SEE IF I CAN TRICK ECLIPSE INTO RESOLVING THE NAME
	@SuppressWarnings("unchecked")
	public static <E> Reflector<E> byName(String name) {
		try{
			return Reflect.on((Class<E>)Class.forName(name));
		}catch(Exception e) {
			return null;//SHIZZLE/SHIZZLE.exe
		}
	}
	
///////////////////////DEFAULT REFLECTIONS/////////////////////////////
public static class on {
	public static Reflector<Byte> Byte() {
		return Reflect.on(Byte.class);
	}
	public static Reflector<Short> Short() {
		return Reflect.on(Short.class);
	}
	public static Reflector<Integer> Integer() {
		return Reflect.on(Integer.class);
	}
	public static Reflector<Long> Long() {
		return Reflect.on(Long.class);
	}
	public static Reflector<Float> Float() {
		return Reflect.on(Float.class);
	}
	public static Reflector<Double> Double() {
		return Reflect.on(Double.class);
	}
	public static Reflector<Character> Char() {
		return Reflect.on(Character.class);
	}
	public static Reflector<String> String() {
		return Reflect.on(String.class);
	}
	@SuppressWarnings("rawtypes")
	public static Reflector<Class> Class() {
		return Reflect.on(Class.class);
	}
	public static Reflector<Field> Field() {
		return Reflect.on(Field.class);
	}
	public static Reflector<Method> Method() {
		return Reflect.on(Method.class);
	}
	@SuppressWarnings("rawtypes")
	public static Reflector<Constructor> Constructor() {
		return Reflect.on(Constructor.class);
	}
	//TODO COME UP WITH OTHERS
}
	
	
////////////////////UTILITY CLASSES/////////////////////////////////
	public static class byType {
		
		public static  Class<?> toprimitive(Object obj) {
			return toprimitive(obj.getClass());
		}
		
		public static  Class<?> toprimitive(Class<?> clazz) {
			if(clazz == Integer.class) {
				return int.class;
			}
			if(clazz == Byte.class) {
				return byte.class;
			}
			if(clazz == Short.class) {
				return short.class;
			}
			if(clazz == Character.class) {
				return char.class;
			}
			if(clazz == Long.class) {
				return long.class;
			}
			if(clazz == Float.class) {
				return float.class;
			}
			if(clazz == Double.class) {
				return double.class;
			}
			if(clazz.isArray()) {//HANDLE All Array Depths
				int arraydepth = 0;
				Class<?> cl = clazz;
				while(cl.isArray()) {
					cl = cl.getComponentType();
					arraydepth++;
				}
				//GENERATE ORIGINAL
				cl = toprimitive(cl);
				int[] genarr = new int[arraydepth];
				for(int i = 0; i < arraydepth; i++) genarr[i] = 2;
				return Array.newInstance(cl,(int[])genarr).getClass();
				
			}
			
			return clazz;
		}
	}
////////////////////////////MORE SHIZZLE/////////////////////////////////
	
	public static class getInfo {
		
		private static final Reflector<?> inforeflect = new Reflector<>("sun.reflect.Reflection");
		
		public static Class<?> onCallerClass() {
			return (Class<?>) inforeflect.getMethod("getCallerClass").invokeStatic();
		}
		
		public static boolean onIfCallerIsSensitive(Method method) {
			return (boolean) inforeflect.getMethod("isCallerSensitive").invokeStatic(method);
		}
	}
	
//////////////////////EVILEISH YET USEFUL SHIZZLE//////////////////////////
	public static class quickly {
		public static String onString(String toset,CharSequence newval) {
			char[] chars = new char[newval.length()];
			for(int i = 0; i < newval.length(); i++)chars[i]=newval.charAt(i);
			on.String().getField("value").setFinal(toset, chars);
			return toset;
		}
		
	}
}
