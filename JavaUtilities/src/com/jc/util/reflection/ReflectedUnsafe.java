package com.jc.util.reflection;

import com.jc.util.reflection.Reflector.ReflectedField;

import java.lang.ref.SoftReference;
import java.lang.reflect.Field;

@SuppressWarnings({"rawtypes","unchecked"})
public class ReflectedUnsafe {
	
	/**THE ONE AND ONLY UNSAFE**/
	private static Object theunsafe;
	private static Reflector unreflect;
	
	private static ReflectedUnsafe instance;
	
	static {
		unreflect = new Reflector<>("sun.misc.Unsafe");
		theunsafe = unreflect.getField("theUnsafe").getStatic();
		if(theunsafe == null) {
			System.err.println("[ReflectedUnsafe] Unsafe Access Blocked");
			/////////////////TRY THE CORRECT WAY OF ACCESSING IT////////////////////////
			theunsafe = unreflect.getMethod("getUnsafe").invokeStatic();
			if(theunsafe == null) {
				System.err.println("[ReflectedUnsafe] Unsafe Secondary Access Blocked");
				/////////////RESORTING TO DESPERATED MEASURES/////////////////////////////
				theunsafe = unreflect.getMethod("getUnsafe").genAccessor().invokeStatic();
				if(theunsafe == null) {
					System.err.println("[ReflectedUnsafe] Unsafe Tertiary Access Blocked");
					System.err.println("     => We Have Failed Men :(");
					System.err.println("     => The UNSAFE Has Escaped From Us!");
					//TODO THEN USE NATIVE HAX TO ACTUALLY GET IT :p
				}else{
					System.err.println("[ReflectedUnsafe] Unsafe Tertiary Acceess Succeeded");
				}
			}else {
				System.err.println("[ReflectedUnsafe] Unsafe Secondary Access Succeeded");
			}
		}
	}
	
	public boolean isErrored() {
		return theunsafe == null;
	}
	public Object getTheUnsafeObject() {
		return theunsafe;
	}
	
	protected ReflectedUnsafe() {
		instance = this;
	}
	
	/**Reflection Wrapped Control Of The JVM Memory And All [EVILE]**/
	public static ReflectedUnsafe Unsafe() {
		if(instance == null)instance = new ReflectedUnsafe();
		return instance;
	}
	
	//////////////////////UNSAFE ACCESS//////////////////////
	
	/**Unsafely Set A Value  {DANGEROUS}*/
	public void UnsafelySetValue(Object obj,Object val,Field set) {
		if(obj == null) {
			System.out.println("[Reflected Unsafe] Use Static Set not null");
			return;
		}
		invokeUnsafe("putObjectVolatile",obj,getFieldOffset(set),val);
	}
	
	/**Unsafely Set A Static Value {DANGEROUS} */
	public void UnsafelySetStaticValue(Class<?> clazz,Object val,Field set) {
		Object base = getStaticFieldBase(clazz);
		UnsafelySetValue(base,val,set);
	}
	
	
	/**Unsafely Get A Value  {DANGEROUS}*/
	public Object UnsafelyGetValue(Object obj,Field get) {
		return invokeUnsafe("getObjectVolatile",obj,getFieldOffset(get));
	}
	
	
	
	/////////////UTILITY UNSAFE/////////////////////////
	
	public long getFieldOffset(Field field) {
		return (long) (int)invokeUnsafe("fieldOffset",field);
	}
	
	public Object getStaticFieldBase(Class<?> clz) {
		return invokeUnsafe("staticFieldBase",new Class[]{Class.class},new Object[]{clz});
	}
	
	/**Only Verified By Name And Parameter Count*/
	public Object invokeUnsafe(String name,Object... objs) {
		return unreflect.getMethod(name,objs.length).invoke(theunsafe,(Object[])objs);
	}
	
	/**Best Form Of Unsafe Access Although Wrapped Ones Save Your Sole Better**/
	public Object invokeUnsafe(String name,Class[] clazzes,Object[] objs) {
		return unreflect.getMethod(name,(Class[])clazzes).invoke(theunsafe, (Object[])objs);
	}
	
	
/////////////////NON USING UNSAFE BUT STILL DANGEROUS UTILITIES///////////////
	
	/** Allows Actually Editing The Classes Reflection Data :p [replacename is what to kickout]
	 *  ReplaceName == The Old Name Of The Field**/
	protected static void pushEditedFieldToClass(Class<?> clazz,Field field,String replacename) {
		SoftReference softref = (SoftReference) Reflect.on.Class().getField("reflectionData").get(clazz);
		if(softref == null) {
			if(Reflector.isDebugMode()) {
				System.err.println("[Class Lacks Soft Reference]");
			}
			return;
		}
		Object reflectdata = softref.get();
		if(reflectdata == null) {
			if(Reflector.isDebugMode()) {
				System.err.println("[Class Lacks Soft ReflectData");
			}
			return;
		}
		/////////////////
		Reflector datareflect = Reflect.onClass(reflectdata);
		ReflectedField refdat = datareflect.getField("declaredFields");
		Field[] fields = (Field[]) refdat.get(reflectdata);
		for(int i = 0; i < fields.length; i++) {
			Field f = fields[i];
			if(f.getName() == replacename) {
				fields[i] = field;
				break;
			}
		}
		///PUSH THE CHANGES	[HOPEFULLY MIGHT WORK]

		refdat.set(reflectdata, fields);
		//SOOOO IT WORKED YET IT DIDNT WORK WHAT..
		if(Reflector.isDebugMode()) {
			System.out.println("CLASS DATA PUSH POSSIBLY SUCCESSFUL?");
		}
	}
	
	
	
	
	
}
