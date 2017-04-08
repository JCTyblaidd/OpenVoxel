package com.jc.util.reflection;

import com.jc.util.reflection.ReflectedReflectionFactory.ReflectedConstructorAccessor;
import com.jc.util.reflection.ReflectedReflectionFactory.ReflectedFieldAccessor;
import com.jc.util.reflection.ReflectedReflectionFactory.ReflectedMethodAccessor;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/*** Advanced Reflection Library
 * 	 Supports Basic, Advanced And Memory Based Reflection
 *   As Well As A Great Method/Field/Constructor Resolving Framework
 *   Be Careful With Unsafe Calls -> They May Be Very Powerful But They Can Easily Crash The JVM
 * ***/
public class Reflector<E> {
	
	/**Whether to silently fail or not**/
	private static boolean debugmode = false;
	
	/**Call Only On Debug Builds**/
	public static void enableDebugMode() {
		debugmode = true;
	}
	public static void disableDebugMode() {
		debugmode = false;
	}
	public static boolean isDebugMode() {
		return debugmode;
	}
	
	private Class<E> clazz;
	
	public Reflector(Class<E> clz) {
		this.clazz = clz;
	}
	/**After Using This it is wise to check if there is an error**/
	@SuppressWarnings("unchecked")
	public Reflector(String classname) {
		try {
			this.clazz = (Class<E>) Class.forName(classname);
		} catch (ClassNotFoundException e) {
			this.clazz = null;
		}
	}
	
	public boolean isErrored() {
		return this.clazz == null;
	}
	
	public Class<E> Class() {
		return clazz;
	}
	
	public ClassLoader getLoader() {//TODO WRAP IN REFLECTIVITY
		return clazz.getClassLoader();
	}
	
	///////FIELD ACCESSING///////
	
	/***Get the number'th field**/
	public ReflectedField<E> getField(int number) {
		Field[] fields = clazz.getDeclaredFields();
		if(number < 0 || number >= fields.length) {
			if(debugmode) {
				System.err.println("[Reflector] Field Not Found!");
			}
			return null;
		}else {
			return new ReflectedField<E>(clazz, fields[number],this);
		}
	}
	
	/**Gets The Field With The Name**/
	public ReflectedField<E> getField(String name) {
		try{
			return new ReflectedField<E>(clazz,clazz.getDeclaredField(name),this);
		}catch(Exception e) {
			if(debugmode) {
				System.err.println("[Reflector] Field Not Found!");
			}
			return null;
		}
	}
	
	/**Gets The Nth Field Of Type [0=first,etc...]**/
	public ReflectedField<E> getField(Class<?> type,int number) {
		Field[] fields = clazz.getDeclaredFields();
		int count = -1;
		for(Field field : fields) {
			if(field.getType() == type) {
				count++;
				if(count == number) {
					return new ReflectedField<E>(clazz,field,this);
				}
			}
		}
		if(debugmode) {
			System.err.println("[Reflector] Field Not Found!");
		}
		return null;
	}
	
	/**Get Type Field With Leniency On The Class**/
	public ReflectedField<E> getFieldFuzzy(Class<?> type, int number) {
		Field[] fields = clazz.getDeclaredFields();
		int count = -1;
		for(Field field : fields) {
			if(field.getType().isAssignableFrom(type)) {//FUZZYNESS
				count++;
				if(count == number) {
					return new ReflectedField<E>(clazz,field,this);
				}
			}
		}
		if(debugmode) {
			System.err.println("[Reflector] Fuzzy Field Not Found!");
		}
		return null;
	}
	
	/**Scans Everywhere for the field*/
	@SuppressWarnings("unchecked")
	public ReflectedField<E> getFieldDeep(String name) {
		ReflectedField<E> field = this.getField(name);
		if(field != null) {
			return field;
		}
		//TRY SUPERCLASS
		if(hasSuperClass()) {
			field = (ReflectedField<E>)getSuperClass().getFieldDeep(name);
			if(field != null) {
				return field;
			}
		}
		//TRY THE INTERFACES
		if(hasInterfaces()) {
			for(int i = 0; i < getInterfaceCount(); i++) {
				field = (ReflectedField<E>) getInterface(i).getFieldDeep(name);
				if(field != null) {
					return field;
				}
			}
		}
		///OK WE FAILED MEN
		return null;
	}
	
	///////METHOD ACCESSING///////
	/***Get Method With Name**/
	public ReflectedMethod<E> getMethod(String name) {
		Method[] ms = clazz.getDeclaredMethods();
		for(Method m : ms) {
			if(m.getName().equals(name)) {
				return new ReflectedMethod<E>(clazz,m,this);
			}
		}
		if(debugmode) {
			System.err.println("[Reflector] Method Not Found!");
		}
		return null;
	}
	/**Get Method With Name And Parameter Count**/
	public ReflectedMethod<E> getMethod(String name,int paramcount) {
		Method[] ms = clazz.getDeclaredMethods();
		for(Method m : ms) {
			if(m.getName().equals(name) && m.getParameterCount() == paramcount) {
				return new ReflectedMethod<E>(clazz,m,this);
			}
		}
		if(debugmode) {
			System.err.println("[Reflector] Method Not Found!");
		}
		return null;
	}
	
	/**Get Method With Name, Parameter Count And Return Type**/
	public ReflectedMethod<E> getMethod(String name,int paramcount, Class<?> returntype) {
		Method[] ms = clazz.getDeclaredMethods();
		for(Method m : ms) {
			if(m.getName().equals(name) && m.getParameterCount() == paramcount && m.getReturnType() == returntype) {
				return new ReflectedMethod<E>(clazz,m,this);
			}
		}
		if(debugmode) {
			System.err.println("[Reflector] Method Not Found!");
		}
		return null;
	}
	/**Get Method With Name, And Class Parameter Array**/
	public ReflectedMethod<E> getMethod(String name,Class<?>... objs) {
		try{
			return new ReflectedMethod<E>(clazz,clazz.getDeclaredMethod(name, (Class[])objs),this);
		}catch(Exception e) {
			if(debugmode) {
				System.err.println("[Reflector] Method Not Found!");
			}
			return null;
		}
	}
	
	/**Scans Everywhere for the method*/
	@SuppressWarnings("unchecked")
	public ReflectedMethod<E> getMethodDeep(String name) {
		ReflectedMethod<E> field = this.getMethod(name);
		if(field != null) {
			return field;
		}
		//TRY SUPERCLASS
		if(hasSuperClass()) {
			field = (ReflectedMethod<E>)getSuperClass().getMethodDeep(name);
			if(field != null) {
				return field;
			}
		}
		//TRY THE INTERFACES
		if(hasInterfaces()) {
			for(int i = 0; i < getInterfaceCount(); i++) {
				field = (ReflectedMethod<E>) getInterface(i).getMethodDeep(name);
				if(field != null) {
					return field;
				}
			}
		}
		///OK WE FAILED MEN
		return null;
	}
	
	/**Scans Everywhere for the method*/
	@SuppressWarnings("unchecked")
	public ReflectedMethod<E> getMethodDeep(String name,int paramcount) {
		ReflectedMethod<E> field = this.getMethod(name,paramcount);
		if(field != null) {
			return field;
		}
		//TRY SUPERCLASS
		if(hasSuperClass()) {
			field = (ReflectedMethod<E>)getSuperClass().getMethodDeep(name,paramcount);
			if(field != null) {
				return field;
			}
		}
		//TRY THE INTERFACES
		if(hasInterfaces()) {
			for(int i = 0; i < getInterfaceCount(); i++) {
				field = (ReflectedMethod<E>) getInterface(i).getMethodDeep(name,paramcount);
				if(field != null) {
					return field;
				}
			}
		}
		///OK WE FAILED MEN
		return null;
	}
	
	/**Scans Everywhere for the method*/
	@SuppressWarnings("unchecked")
	public ReflectedMethod<E> getMethodDeep(String name,int paramcount,Class<?> returntype) {
		ReflectedMethod<E> field = this.getMethod(name,paramcount,returntype);
		if(field != null) {
			return field;
		}
		//TRY SUPERCLASS
		if(hasSuperClass()) {
			field = (ReflectedMethod<E>)getSuperClass().getMethodDeep(name,paramcount,returntype);
			if(field != null) {
				return field;
			}
		}
		//TRY THE INTERFACES
		if(hasInterfaces()) {
			for(int i = 0; i < getInterfaceCount(); i++) {
				field = (ReflectedMethod<E>) getInterface(i).getMethodDeep(name,paramcount,returntype);
				if(field != null) {
					return field;
				}
			}
		}
		///OK WE FAILED MEN
		return null;
	}
	
	/**Scans Everywhere For The Method**/
	@SuppressWarnings("unchecked")
	public ReflectedMethod<E> getMethodDeep(String name,Class<?>... clazzes) {
		ReflectedMethod<E> field = this.getMethod(name,(Class<?>[])clazzes);
		if(field != null) {
			return field;
		}
		//TRY SUPERCLASS
		if(hasSuperClass()) {
			field = (ReflectedMethod<E>)getSuperClass().getMethodDeep(name,(Class<?>[])clazzes);
			if(field != null) {
				return field;
			}
		}
		//TRY THE INTERFACES
		if(hasInterfaces()) {
			for(int i = 0; i < getInterfaceCount(); i++) {
				field = (ReflectedMethod<E>) getInterface(i).getMethodDeep(name,(Class<?>[])clazzes);
				if(field != null) {
					return field;
				}
			}
		}
		///OK WE FAILED MEN
		return null;
	}
	
	///////CONSTRUCTOR ACCESSING///////
	public ReflectedConstructor<E> getConstructor(Class<?>... data) {
		try {
			return new ReflectedConstructor<E>(clazz, clazz.getDeclaredConstructor((Class[])data),this);
		} catch (Exception e) {
			if(debugmode) {
				System.err.println("Constructor not found: @"+data.length+" params");
			}
			return null;//NOOP
		}
	}
	public ReflectedConstructor<E> getConstructor() {
		try {
			return new ReflectedConstructor<E>(clazz, clazz.getDeclaredConstructor(),this);
		} catch (Exception e) {
			if(debugmode) {
				System.err.println("Constructor not found: @0 params");
			}
			return null;//NOOP
		}
	}
	
	@SuppressWarnings("unchecked")
	public ReflectedConstructor<E> getConstructor(int paramnum) {
		Constructor<?>[] constructs = clazz.getDeclaredConstructors();
		for(Constructor<?> constr : constructs) {
			if(constr.getParameterCount() == paramnum) {
				return new ReflectedConstructor<E>(clazz, (Constructor<E>) constr,this);
			}
		}
		if(debugmode) {
			System.err.println("Constructor not found!");
		}
		return null;//NOOP
	}
	
////////////////////TYPE CONDITIONAL FUNCS/////////////////////////////////////////
	
	public boolean isArray() {
		return clazz.isArray();
	}
	
	public boolean isEnum() {
		return clazz.isEnum();
	}
	
	public boolean isInterface() {
		return clazz.isInterface();
	}
	
	public boolean isAssignableFrom(Class<?> clz) {
		return clazz.isAssignableFrom(clz);
	}
	public boolean isAssignableFrom(Reflector<?> ref) {
		return clazz.isAssignableFrom(ref.clazz);
	}
	public boolean isLocalClass() {
		return clazz.isLocalClass();
	}
	public boolean isMemberClass() {
		return clazz.isMemberClass();
	}
	
	public Reflector<?> getInterface(int num) {
		try{
			return new Reflector<>(clazz.getInterfaces()[num]);
		}catch(Exception e) {
			if(debugmode) {
				System.err.println("[Reflector] Error Accessing Debug Mode");
			}
			return null;
		}
	}
	
	public int getInterfaceCount() {
		return clazz.getInterfaces().length;
	}
	
	public boolean hasInterfaces() {
		return getInterfaceCount() != 0;
	}
	
	public  Reflector<? super E> getSuperClass() {
		try{
			Class<? super E> sup = clazz.getSuperclass();
			if(sup == null) {
				if(debugmode) {
					System.err.format("Class [%a] Lacks SuperClass", getName());
				}
				return null;//NO SUPERCLASS
			}
			return new Reflector<>(sup);
		}catch(Exception e) {
			if(debugmode) {
				System.err.println("[Reflector] Error Accessing Superclass");
			}
			return null;
		}
	}
	
	public boolean hasSuperClass() {
		if(clazz.isPrimitive()) {
			return false;
		}
		Class<?> scl = clazz.getSuperclass();//NOT REALLY USEFUL
		if(scl == Object.class || scl == Void.class) {
			return false;
		}
		//ALREADY AT THE TOP
		if(clazz == Enum.class || clazz.isArray())  {
			return false;
		}
		///OK HAS A SUPERCLASS
		return true;
	}
	
	/////////SUBCLASS ACCESSING///////
	public Reflector<?> getSubClass(int number) {
		Class<?>[] subclzs = clazz.getDeclaredClasses();
		if(number >= 0 && number < subclzs.length) {
			return new Reflector<>(subclzs[number]);
		}
		return null;
	}
	
	public Reflector<?> getSubClass(String name) {
		Class<?>[] subclzs = clazz.getDeclaredClasses();
		for(Class<?> clz : subclzs) {
			if(clz.getName().equals(name)) {
				return new Reflector<>(clz);
			}
		}
		return null;
	}
	
	public int getSubClassCount() {
		return clazz.getDeclaredClasses().length;
	}
	
//////ADVANCED CLASS ACCESSING FUNCTIONALITY/////
	
	public ClassModifier<E> getModifiers() {
		return new ClassModifier<E>(clazz);
	}
	
	/**Gets Annotations For The Class**/
	public ReflectedAnnotations<E> getAnnotations() {
		return new ReflectedAnnotations<>(clazz, clazz);
	}
	
	/**Gets A Enum Util for Enum Manipulation**/
	@SuppressWarnings("all")//TODO FIX TYPES I HATE GENERICS
	public  EnumBuster<? super E> getEnumBuster() {///SHHUUUUUT UPPP
		if(isEnum()) {///AAAAHHHHHH STUPID
			//return new EnumBuster(clazz,this);
			return new EnumBuster(clazz,this);
		}else {
			return null;
		}
	}
	/**Gets A Reflection Wrapped Instance Of The Reflection Factory [ADVANCED]**/
	public ReflectedReflectionFactory<E> getReflectionFactory() {
		return new ReflectedReflectionFactory<E>(clazz,this);
	}
	/**Cast This Reflector To One Of That Class*/
	@SuppressWarnings("unchecked")
	public <T> Reflector<T> getAsReflectorOf(Class<T> clz) {
		if(this.clazz.isAssignableFrom(clz) || this.clazz.equals(clz)) {
			return (Reflector<T>)this;
		}else{
			if(debugmode) {
				System.err.println("[Reflector] Invalid Reflector Cast");
			}
			return null;
		}
	}
	
	/***Get The Unsafe Instance -> Is Unsafe Of Course**/
	public static ReflectedUnsafe getUnsafe() {
		return ReflectedUnsafe.Unsafe();//REFLECT
	}
	
	/**Gets The Referenced Package**/
	public ReflectedPackage<E> getPackage() {
		return new ReflectedPackage<E>(clazz);
	}
	
	/**Gets An Experimental Framework for interfaces manipulation**/
	public ClassInterfaceReflector<E> getInterfaceModifier() {
		return new ClassInterfaceReflector<E>(clazz);
	}
	public ReflectedClassTypes<E, Class<E>> getGenerics() {
		return new ReflectedClassTypes<E,Class<E>>(clazz);
	}
	
	//////////////MONOLITHIC DEEPLY REFLECTIVE TOSTRING FUNCTION
	public String getName() {
		return clazz.getSimpleName();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(getModifiers().toString());
		builder.append("class ");
		builder.append(getName());
		//CLASS EXTENTION
		builder.append(" extends "+getSuperClass().getName()+" ");
		if(getInterfaceCount() != 0) {
			builder.append("implements ");
			for(int i = 0; i < getInterfaceCount(); i++) {
				builder.append(getInterface(i).getName());
				if(i != (getInterfaceCount() - 1)) {
					builder.append(", ");
				}
			}
		}
		builder.append(" { \n\n");
		//ALL FIELDS
		Field[] fields = clazz.getDeclaredFields();
		for(Field field : fields) {
			ReflectedField<E> rf = new ReflectedField<E>(clazz, field,this);
			builder.append(rf.toString()+"\n");
		}
		builder.append("\n\n");
		//ALL CONSTRUCTORS//
		Constructor<?>[] constructs = clazz.getDeclaredConstructors();
		for(Constructor<?> constr : constructs) {
			ReflectedConstructor<E> rc = new ReflectedConstructor<E>(clazz,(Constructor<E>) constr,this);
			builder.append(rc.toString()+"\n");
		}
		builder.append("\n\n");
		//ALL METHODS
		Method[] methods = clazz.getDeclaredMethods();
		for(Method method : methods) {
			ReflectedMethod<E> rm = new ReflectedMethod<E>(clazz, method,this);
			builder.append(rm.toString()+"\n");
		}
		builder.append("\n\n");
		///ALL SUBCLASSES
		if(getSubClassCount() != 0) {
			for(int i = 0; i < getSubClassCount(); i++) {
				String subclasstr = getSubClass(i).toString();
				subclasstr = subclasstr.replace("\n", "\n     ");
				subclasstr = subclasstr.substring(0,subclasstr.length() - 5);
				subclasstr = subclasstr +  "}";
				builder.append(subclasstr+"\n\n");
			}
		}
		
		builder.append("\n}");
		return builder.toString();
	}
	
	
	
	
////////////////////////////REFLECTOR CLASSES/////////////////////////////////////
	
	
	public static class ReflectedConstructor<E> {
		private Class<E> clazz;
		private Constructor<E> reference;
		private Reflector<E> reflect;
		private ReflectedConstructor(Class<E> clz, Constructor<E> m,Reflector<E> r) {
			clazz=clz;reference=m;reflect = r;
			try{
				reference.setAccessible(true);
			}catch(Exception e) {
				if(debugmode) {
					System.out.println("[Reflector] Constructor Accessibility Denied!");
				}
			}
		}
		public Class<E> Class() {
			return clazz;
		}
		public Constructor<E> getReference() {
			return reference;
		}
		/****/
		public E Create(Object... values) {
			try{
				if(values.length > 0) {
					E o = reference.newInstance((Object[])values);
					return o;
				}else {//SAVE WORK
					E o = reference.newInstance();
					return o;
				}
			}catch(Exception e) {
				if(debugmode) {
					System.err.println("[Reflector] Invokation Error");
				}
				return null;
			}
		}
		///////////////ADVANCED ACCESS FUNCS///////////////
		public ConstructorModifier<E> getModifiers() {
			return new ConstructorModifier<>(this, clazz);
		}
		public ReflectedAnnotations<E> getAnnotations() {
			return new ReflectedAnnotations<E>(clazz, reference);
		}
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public ReflectedConstructorAccessor<E> genAccessor() {
			return reflect.getReflectionFactory().generateConstructorAccessor((Constructor)reference);
		}
		public Reflector<E> getReflector() {
			return reflect;
		}
		public ReflectedClassTypes<E, Constructor<E>> getGenerics() {
			return new ReflectedClassTypes<E,Constructor<E>>(clazz,reference);
		}
		//////STRING UTIL FUNCTIONS////
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append(getModifiers().toString());
			builder.append(clazz.getSimpleName()+"( ");
			Class<?>[] params = reference.getParameterTypes();
			for(int i = 0; i < params.length; i++) {
				builder.append(params[i].getSimpleName());
				if(i != (params.length - 1)) {
					builder.append(", ");
				}
			}
			builder.append(" )");
			Class<?>[] errs = reference.getExceptionTypes();
			if(errs.length != 0) {
				builder.append(" throws ");
				for(int i = 0; i < errs.length; i++) {
					builder.append(errs[i].getSimpleName());
					if(i != (errs.length-1)) {
						builder.append(", ");
					}
				}
			}
			builder.append(" {\n   [code]\n};");
			return builder.toString();
		}
	}
	
	
	
	public static class ReflectedMethod<E> {
		private Class<E> clazz;
		private Method reference;
		private Reflector<E> reflect;
		private ReflectedMethod(Class<E> clz, Method m,Reflector<E> r) {
			clazz=clz;reference=m;reflect = r;
			reference.setAccessible(true);
		}
		public Class<E> Class() {
			return clazz;
		}
		public Method getReference() {
			return reference;
		}
		/////////////////////ACCESS FUNCS//////////////////
		public Object invokeStatic(Object... values) {
			return invoke(null,(Object[])values);
		}
		public Object invoke(E val,Object... values) {
			try{
				return reference.invoke(val, (Object[])values);
			}catch(Exception e) {
				if(debugmode) {
					System.err.println("[Reflector] Method Invokation Error");
					e.printStackTrace();
				}
//				try {	//OK ERRS => TODO FIX
//					return Void.TYPE.newInstance();
//				} catch (Exception e2) {
					return null;
//				}
			}
		}
		public Class<?> returnType() {
			return reference.getReturnType();
		}
		///////////////ADVANCED ACCESS FUNCS///////////////
		public MethodModifier<E> getModifiers() {
			return new MethodModifier<>(this, clazz);
		}
		public ReflectedAnnotations<E> getAnnotations() {
			return new ReflectedAnnotations<E>(clazz, reference);
		}
		public ReflectedMethodAccessor<E> genAccessor() {
			return reflect.getReflectionFactory().generateMethodAccessor(reference);
		}
		public Reflector<E> getReflector() {
			return reflect;
		}
		public ReflectedClassTypes<E, Method> getGenerics() {
			return new ReflectedClassTypes<E,Method>(clazz,reference);
		}
		///////STRING UTIL FUNCS/////
		public String getName() {
			return reference.getName();
		}
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append(getModifiers().toString());
			if(returnType() != Void.class) {
				builder.append(returnType().getSimpleName()+" ");
			}else {
				builder.append("void ");//LOWERCASE
			}
			builder.append(getName()+"( ");
			Class<?>[] params = reference.getParameterTypes();
			for(int i = 0; i < params.length; i++) {
				builder.append(params[i].getSimpleName());
				if(i != (params.length - 1)) {
					builder.append(", ");
				}
			}
			builder.append(" )");
			Class<?>[] errs = reference.getExceptionTypes();
			if(errs.length != 0) {
				builder.append(" throws ");
				for(int i = 0; i < errs.length; i++) {
					builder.append(errs[i].getSimpleName());
					if(i != (errs.length-1)) {
						builder.append(", ");
					}
				}
			}
			builder.append(" {\n   [code]\n};");
			return builder.toString();
		}
	}
	
	public static class ReflectedField<E> {
		private Class<E> clazz;
		private Field reference;
		private Reflector<E> reflect;
		private String oldname;//FOR PUSHING TO CLAZZ
		private ReflectedField(Class<E> clz, Field f,Reflector<E> r) {
			clazz=clz;reference=f;reflect = r;
			reference.setAccessible(true);
			oldname = clazz.getName();
		}
		//TYPE SHIZZLE
		public Class<E> Class() {
			return clazz;
		}//TODO SEE WHAT I CAN DO TO BAKE THE TYPE
		public Class<?> Type() {
			return reference.getType();
		}
		public Field getReference() {
			return reference;
		}
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public Reflector getTypeReflector() {
			return new Reflector(Type());
		}
		/////////////////////ACCESS FUNCS//////////////////
		public Object getStatic() {
			return get(null);
		}
		public Object get(E obj) {
			try{
				return reference.get(obj);
			}catch(Exception e) {
				return null;
			}
		}
		public void setStatic(Object val) {
			set(null,val);
		}
		public void set(E obj,Object val) {
			try{
				reference.set(obj, val);;
			}catch(Exception e) {
				if(debugmode) {
					System.err.println("[Reflector] Error Setting Field");
					e.printStackTrace();
				}
			}
		}
		///////////FINAL ACCESS UTIL FUNCS///////////////
		public void setStaticFinal(Object val) {
			setFinal(null,val);
		}
		public void setFinal(E obj, Object val) {
			FieldModifier<E> modifier = getModifiers();
			boolean flag = modifier.isFinal();
			modifier.setFinal(false);
			if(modifier.isFinal() && debugmode) {
				System.err.println("[Reflector] Final Bit Setting Error!");
			}
			set(obj,val);
			if(!(get(obj) == val)) {//FIXES SETTING STATIC FINALS
				if(debugmode) {
					System.err.println("[Reflector] Error Setting Final Bit!");
				}
				genAccessor().set(obj, val);
				if(!(get(obj) == val)) {	//AND IF ALL IS LOST MWAHAHAHAHA...
					//OK NOW TO BRING OUT THE BIG GUNS :p
					ReflectedUnsafe unsafe = getUnsafe();
					if(getModifiers().isStatic()) {//HANDLE STATIC CALLBACK
						unsafe.UnsafelySetStaticValue(clazz, val, reference);
					}else {
						unsafe.UnsafelySetValue(obj, val, reference);
					}
					if(!(get(obj) == val)) {
						if(debugmode) {
							System.err.println("[Reflector] Failed Completely To Set Value");
						}
					}else{
						if(debugmode) {
							System.err.println("[Reflector] Unsafely Set Value!");
						}
					}
				}else{
					if(debugmode) {
						System.err.println("[Reflector] Accessively Set Value!");
					}
				}
			}
			modifier.setFinal(flag);
		}
		/////////////MERELY EVIL//////////////////////////
		//IS VERY DANGEROUS USE WITH CARE
		/**Doesn't Care [Handles Final As Well]
		 * -> IS DANGEROUS!!! {CAREFUL WITH USE}*/
		public void setStaticFieldToWrongType(Object val) {
			setFieldToWrongType(null, val);
		}
		/**Doesn't Care [Handles Final As Well]
		 *  * -> IS DANGEROUS!!! {CAREFUL WITH USE}*/
		public void setFieldToWrongType(E obj,Object val) {
			FieldModifier<E> modifier = getModifiers();
			boolean flag1 = modifier.isFinal();
			Class<?> flag2 = modifier.getFieldType();
			modifier.setFinal(false);
			modifier.setFieldType(val.getClass());
			//THE ACTUAL SETTING
			if(modifier.getFieldType() != val.getClass() && debugmode) {
				System.out.println("[Reflector] Classless Setting Errored!");
			}
			set(obj,val);
			if(!(get(obj) == val)) {//FIXES SETTING STATIC FINALS
				if(debugmode) {
					System.err.println("[Reflector] Error Setting Final Bit!");
				}
				genAccessor().set(obj, val);
				if(!(get(obj) == val)) {	//AND IF ALL IS LOST MWAHAHAHAHA...
					//OK NOW TO BRING OUT THE BIG GUNS :p
					ReflectedUnsafe unsafe = getUnsafe();
					if(getModifiers().isStatic()) {//HANDLE STATIC CALLBACK
						unsafe.UnsafelySetStaticValue(clazz, val, reference);
					}else {
						unsafe.UnsafelySetValue(obj, val, reference);
					}
					if(!(get(obj) == val)) {
						if(debugmode) {
							System.err.println("[Reflector] Failed Completely To Set Value");
						}
					}else{
						if(debugmode) {
							System.err.println("[Reflector] Unsafely Set Value!");
						}
					}
				}else{
					if(debugmode) {
						System.err.println("[Reflector] Accessively Set Value!");
					}
				}
			}
			modifier.setFinal(flag1);
			modifier.setFieldType(flag2);
		}
		
		
		/////////////TERRIBLE HABBITS//////////////////////
		/**Direct Memory Manipulation**/
		public void setStaticUnsafely(Object val) {
			if(!getModifiers().isStatic()) {//DANGEROUS
				if(debugmode) {
					System.err.println("[Reflector] Unsafe Static Called On Non Static!");
				}
				return;
			}
			ReflectedUnsafe unsafe = getUnsafe();
			unsafe.UnsafelySetStaticValue(clazz, val, reference);
		}
		/**Direct Memory Manipulation**/
		public void setUnsafely(Object obj,Object val) {
			if(getModifiers().isStatic()) {//DANGEROUS
				if(debugmode) {
					System.err.println("[Reflector] Unsafe Normal Called On Static!");
				}
				return;
			}
			if(obj == null) {
				if(debugmode) {
					System.err.println("[Reflector] Unsafe Normal Called On Null Value {Use Static For Static}");
				}
				return;
			}
			ReflectedUnsafe unsafe = getUnsafe();
			unsafe.UnsafelySetValue(obj, val, reference);
		}
		public void pushFieldChangesToClass() {
			ReflectedUnsafe.pushEditedFieldToClass(clazz, reference, oldname);
		}
		///////////////ADVANCED ACCESS FUNCS///////////////
		public FieldModifier<E> getModifiers() {
			return new FieldModifier<>(this, clazz);
		}
		public ReflectedAnnotations<E> getAnnotations() {
			return new ReflectedAnnotations<E>(clazz, reference);
		}
		public ReflectedFieldAccessor<E> genAccessor() {
			return reflect.getReflectionFactory().generateFieldAccessor(reference);
		}
		public Reflector<E> getReflector() {
			return reflect;
		}
		///////STRING UTIL FUNCS/////
		public String getName() {
			return reference.getName();
		}
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append(getModifiers().toString());
			builder.append(Type().getSimpleName()+" ");
			builder.append(getName()+";");
			return builder.toString();
		}
	}
	
	
	//////////////////////////ACCESSED SHIZZLE MODIFIERS////////////////////////////
	
	private abstract static class AbstractModifier<E> {
		//ABSTRACTNESS [Protected So No One Makes Volatile Constructors :p]
		protected abstract int getModifiers();
		protected abstract void setModifiers(int v);
		protected boolean isModifier(int mod) {
			return (getModifiers() & mod) != 0;
		}
		protected void setModifier(int mod, boolean choice) {
			int m = getModifiers();
			if(choice) {//OR TO SET
				m |= mod;
			}else {//AND WITH INVERSE TO REMOVE
				m &= ~mod;
			}
			setModifiers(m);
		}
		////////UTILITY ACCESS FUNCTIONALITY/////////////
		public int getModifierFlags() {
			return getModifiers();
		}
		public String getModifierBits() {
			return Integer.toBinaryString(getModifiers());
		}
		
		////////////ALL OF THE TYPES VALID FOR METHOD+FIELD+CONSTRUCTOR////////
		//BASIC ACCESSING SHIZZLE
		public boolean isPublic() {
			return isModifier(Modifier.PUBLIC);
		}
		public boolean isProtected() {
			return isModifier(Modifier.PROTECTED);
		}
		public boolean isPrivate() {
			return isModifier(Modifier.PRIVATE);
		}
		/////////////////////////////////
		public void setPublic(boolean b) {
			setModifier(Modifier.PUBLIC, b);
		}
		public void setProtected(boolean b) {
			setModifier(Modifier.PROTECTED, b);
		}
		public void setPrivate(boolean b) {
			setModifier(Modifier.PRIVATE, b);
		}
		//TO STRING AS MODIFIER ARRAY
		@Override
		public String toString() {
			String str =  Modifier.toString(getModifiers());
			if(!str.equals("")) {
				str = str + " ";
			}
			return str;
		}
	}
	
	private static abstract class AbstractModifierPlus<E> extends AbstractModifier<E> {
		//ADDITIONAL MODIFIERS
		public boolean isFinal() {
			return isModifier(Modifier.FINAL);
		}
		public boolean isStatic() {
			return isModifier(Modifier.STATIC);
		}
		////////////////////////////////
		public void setStatic(boolean b) {
			setModifier(Modifier.STATIC, b);
		}
		public void setFinal(boolean b) {
			setModifier(Modifier.FINAL, b);
		}
	}
	
	public static class MethodModifier<E> extends AbstractModifierPlus<E>{
		//STATIC PRELOADED REFLECTION
		private static Field modifierField;
		private static Field nameField;
		private static Field returnTypeField;
		private static Field parameterTypeField;
		static {
			try{
				Field field = Method.class.getDeclaredField("modifiers");
				field.setAccessible(true);
				modifierField = field;
			}catch(Exception e) {
				modifierField = null;
				if(debugmode) {
					System.err.println("[Reflector] Error Loading Method Modifier");
					e.printStackTrace();
				}
			}
			try{
				Field field = Method.class.getDeclaredField("name");
				field.setAccessible(true);
				nameField = field;
			}catch(Exception e) {
				nameField = null;
				if(debugmode) {
					System.err.println("[Reflector] Error Loading Method Name Modifier");
				}
			}
			try{
				Field field = Method.class.getDeclaredField("returnType");
				field.setAccessible(true);
				returnTypeField = field;
			}catch(Exception e) {
				returnTypeField = null;
				if(debugmode) {
					System.err.println("[Reflector] Error Loading Method Return Type Modifier");
				}
			}
			try{
				Field field = Method.class.getDeclaredField("parameterTypes");
				field.setAccessible(true);
				parameterTypeField = field;
			}catch(Exception e) {
				parameterTypeField = null;
				if(debugmode) {
					System.err.println("[Reflector] Error Loading Method Parameter Type Modifier");
				}
			}
		}
		//////////
		private ReflectedMethod<E> method;
		private Class<E> clazz;
		private MethodModifier(ReflectedMethod<E> m,Class<E> c) {
			clazz = c; method = m;
		}
		public Class<E> Class() {
			return clazz;
		}
		@Override
		public int getModifiers() {
			return method.reference.getModifiers();
		}
		@Override
		public void setModifiers(int v) {
			try{
				modifierField.setInt(method.reference, v);
			}catch(Exception e) {
				if(debugmode) {
					System.err.println("[Reflector] Method Modifier Setting Error");
				}
			}
		}
		/////////////////////UTTER EVILENESS////////////////////////
		public void setMethodName(String newname) {
			try{
				nameField.set(method.reference, newname);
			}catch(Exception e) {
				if(debugmode) {
					System.err.println("[Reflector] Method Modifier Setting Name Error");
				}
			}
		}
		public String getMethodName() {
			return method.reference.getName();
		}
		public void setMethodReturnType(Class<?> returntype) {
			try{
				returnTypeField.set(method.reference, returntype);
			}catch(Exception e) {
				if(debugmode) {
					System.err.println("[Reflector] Method Modifier Setting Return Type Error");
				}
			}
		}
		public Class<?> getMethodReturnType() {
			return method.reference.getReturnType();
		}
		public void setMethodParameterTypes(Class<?>... parameters) {
			try{
				Object[] objs = new Object[1];objs[0] = (Class[])parameters;
				parameterTypeField.set(method.reference,(Object[])objs);
			}catch(Exception e) {
				if(debugmode) {
					System.err.println("[Reflector] Method Modifier Setting Parameter Types Error");
				}
			}
		}
		public Class<?>[] getMethodParameterTypes() {
			return method.reference.getParameterTypes();
		}
		
		//////////////ADDITIONAL MODIFIERS FOR METHODS//////////////
		//ABSTRACT,NATIVE,SYNC,STRICTFP
		public boolean isAbstract() {
			return isModifier(Modifier.ABSTRACT);
		}
		public boolean isNative() {
			return isModifier(Modifier.NATIVE);
		}
		public boolean isSynchronized() {
			return isModifier(Modifier.SYNCHRONIZED);
		}
		public  boolean isStrict() {
			return isModifier(Modifier.STRICT);
		}
		////////
		public void setAbstract(boolean b) {
			setModifier(Modifier.ABSTRACT, b);
		}
		public void setNative(boolean b) {
			setModifier(Modifier.NATIVE,b);
		}
		public void setSynchronized(boolean b) {
			setModifier(Modifier.SYNCHRONIZED, b);
		}
		public void setStrict(boolean b) {
			setModifier(Modifier.STRICT, b);
		}
	}
	
	public static class FieldModifier<E> extends AbstractModifierPlus<E>{
		//STATIC PRELOADED REFLECTION
		private static Field modifierField;
		private static Field typeField;
		private static Field nameField;
		static {
			try{
				Field field = Field.class.getDeclaredField("modifiers");
				field.setAccessible(true);
				modifierField = field;
			}catch(Exception e) {
				modifierField = null;
				if(debugmode) {
					System.err.println("[Reflector] Error Loading Field Modifier");
					e.printStackTrace();
				}
			}
			try{
				Field field = Field.class.getDeclaredField("type");
				field.setAccessible(true);
				typeField = field;
			}catch(Exception e) {
				typeField = null;
				if(debugmode) {
					System.err.println("[Reflector] Error Loading Field Type Modifier");
					e.printStackTrace();
				}
			}
			try{
				Field field = Field.class.getDeclaredField("name");
				field.setAccessible(true);
				nameField = field;
			}catch(Exception e) {
				nameField = null;
				if(debugmode) {
					System.err.println("[Reflector] Error Loading Field Name Modifier");
					e.printStackTrace();
				}
			}
		}
		/////////////
		private ReflectedField<E> field;
		private Class<E> clazz;
		private FieldModifier(ReflectedField<E> f,Class<E> c) {
			clazz = c; field = f;
		}
		public Class<E> Class() {
			return clazz;
		}
		@Override
		public int getModifiers() {
			return field.reference.getModifiers();
		}
		@Override
		public void setModifiers(int v) {
			try{
				modifierField.setInt(field.reference, v);
			}catch(Exception e) {
				if(debugmode) {
					System.err.println("[Reflector] FieldModifier Setting Error");
					e.printStackTrace();
				}
			}
		}
		/////UTTER EVILENESS///////////////////////
		public void setFieldType(Class<?> type) {
			try{
				typeField.set(field.reference, type);
			}catch(Exception e) {
				if(debugmode) {
					System.err.println("[Reflector] FieldModifier Failed Changing Field Type");
					e.printStackTrace();
				}
			}
		}
		public Class<?> getFieldType() {
			return field.reference.getType();
		}
		public void setFieldName(String newname) {
			try{
				nameField.set(field.reference, newname);
			}catch(Exception e) {
				if(debugmode) {
					System.err.println("[Reflector] FieldModifier Failed Changing Field Name");
				}
			}
		}
		public String getFieldName() {
			return field.reference.getName();
		}
		
		//////////////ADDITIONAL MODIFIERS FOR FIELDS//////////////
		//TRANSIENT,VOLATILE
		public boolean isTransient() {
			return isModifier(Modifier.TRANSIENT);
		}
		public boolean isVolatile() {
			return isModifier(Modifier.VOLATILE);
		}
		//////
		public void setTransient(boolean b) {
			setModifier(Modifier.TRANSIENT, b);
		}
		public void setVolatile(boolean b) {
			setModifier(Modifier.VOLATILE, b);
		}
	}
	public static class ConstructorModifier<E> extends AbstractModifier<E>{
		private static Field modifierField;
		static {
			try{
				Field field = Constructor.class.getDeclaredField("modifiers");
				field.setAccessible(true);
				modifierField = field;
			}catch(Exception e) {
				modifierField = null;
				if(debugmode) {
					System.err.println("[Reflector] Error Loading Constructor Modifier");
					e.printStackTrace();
				}
			}
		}
		/////////////
		private ReflectedConstructor<E> construct;
		private Class<E> clazz;
		private ConstructorModifier(ReflectedConstructor<E> cc,Class<E> clz) {
			clazz = clz;construct=cc;
		}
		public Class<E> Class() {
			return clazz;
		}
		@Override
		public int getModifiers() {
			return construct.reference.getModifiers();
		}

		@Override
		public void setModifiers(int v) {
			try{
				modifierField.setInt(construct.reference, v);
			}catch(Exception e) {
				System.err.println("[Reflector] ConstructModifier Setting Error");
			}
		}
		
	}
	
	public static class ClassModifier<E> extends AbstractModifierPlus<E> {
		private Class<E> clazz;
		private ClassModifier(Class<E> c) {
			clazz = c;
		}
		public Class<E> Class() {
			return clazz;
		}
		@Override
		public int getModifiers() {
			return clazz.getModifiers();
		}

		@Override
		public void setModifiers(int v) {
			//DUNNO
			throw new RuntimeException("Class Modifier Changing Not Supported!");
		}
		//Abstract
		public boolean isAbstract() {
			return isModifier(Modifier.ABSTRACT);
		}
		////
		public void setAbstract(boolean b) {
			setModifier(Modifier.ABSTRACT, b);
		}
	}
	
	public static class ReflectedAnnotations<E> implements Iterable<ReflectedAnnotation<E>>{
		private Class<E> clazz;
		private AnnotatedElement ref;
		private List<ReflectedAnnotation<E>> annotates;
		private ReflectedAnnotations(Class<E> clz,AnnotatedElement ref) {
			clazz=clz;this.ref=ref;
			//LOAD ANNOTATIONS
			annotates = new ArrayList<>();
			Annotation[] anns = this.ref.getDeclaredAnnotations();
			for(Annotation ann : anns) {
				annotates.add(new ReflectedAnnotation<E>(ann));
			}
		}
		public Class<E> Class() {
			return clazz;
		}
		@Override
		public Iterator<ReflectedAnnotation<E>> iterator() {
			return annotates.iterator();
		}
		public int count() {
			return annotates.size();
		}
		public ReflectedAnnotation<E> get(int loc) {
			return annotates.get(loc);
		}
	}
	
	public static class ReflectedAnnotation<E> {
		private Annotation ann;
		private ReflectedAnnotation(Annotation a) {
			ann = a;
		}
		public Class<? extends Annotation> Type() {
			return ann.annotationType();
		}
		public Annotation getAnnotation() {
			return ann;
		}
		public Reflector<? extends Annotation> Reflector() {
			return new Reflector<>(ann.annotationType());
		}
	}
	
	public static class ReflectedPackage<E> {
		private Package pack;
		private Class<E> clz;
		private ReflectedPackage(Class<E> clz) {
			this.clz=clz;this.pack=clz.getPackage();
		}
		public boolean isSealed() {
			return pack.isSealed();
		}
		public ReflectedAnnotations<E> getAnnotations() {
			return new ReflectedAnnotations<E>(clz, pack);
		}
		//TODO WORK OUT GETTING CLASSES IN THE PACKAGE
	}

///////////////////HORRENDOUSLY DEEP REFLECTION SHIZZLE////////////	
	
	public static class ClassInterfaceReflector<E> {
		private Class<E> clz;
		private ClassInterfaceReflector(Class<E> clz) {
			this.clz=clz;
		}
		/////////ALLOW FOR ADDING AND REMOVING INTERFACES -> QUE HORROR
		public Class<E> Class() {
			return clz;
		}
		//TODO IMPLEMENT
		
	}
	
	public static class ReflectedClassTypes<E,TYPE> {
		private Class<E> clz;
		private Method m;//IS NULL IN CASE FOR OTHERS
		private Constructor<E> c;//IS NULL IN CASE FOR OTHERS
		private ReflectedClassTypes(Class<E> clz) {
			this.clz=clz;
		}
		private ReflectedClassTypes(Class<E> clz,Method m) {
			this(clz);this.m = m;
		}
		private ReflectedClassTypes(Class<E> clz,Constructor<E> c) {
			this(clz);this.c=c;
		}
		public TypeVariable<?>[] getTypes() {
			if(m != null) {
				return m.getTypeParameters();
			}
			if(c != null) {
				return c.getTypeParameters();
			}
			return clz.getTypeParameters();
		}
		@SuppressWarnings("unchecked")
		public TYPE getReference() {
			if(m != null) {
				return (TYPE) m;
			}
			if(c != null) {
				return (TYPE) c;
			}
			return (TYPE) clz;
		}
		public int getTypeCount() {
			return getTypes().length;
		}
	}
	
}
