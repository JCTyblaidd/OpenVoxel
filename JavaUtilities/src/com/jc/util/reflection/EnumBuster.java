package com.jc.util.reflection;

import com.jc.util.reflection.Reflector.ReflectedField;

import java.util.Arrays;

@SuppressWarnings("unused")
/**Sneaky Reflection Ninjaness To Edit Enums During Runtime**/
public class EnumBuster<E extends Enum<E>> {
	
	private Class<E> clazz;
	private Reflector<E> reflect;
	
	private static final String VALUES_FIELD = "ENUM$VALUES";
	private static final String ORDINAL_FIELD = "ordinal";
	private static final String NAMES_FIELD = "name";
	
	/**Only Create-able through the Reflector Class**/
	protected EnumBuster(Class<E> c,Reflector<E> r) {
		clazz = c;
		if(!clazz.isEnum()) {
			throw new RuntimeException("Enum Busters need ENUMS!!!");
		}//LOAD REFLECTOR
		reflect = r;
	}
	
	/**Q O L function**/
	@SuppressWarnings("unchecked")
	protected static <Q extends Enum<Q>> EnumBuster<Q> getCastedEnum(EnumBuster<?> e,Class<Q> c) {
		return (EnumBuster<Q>) e;
	}
	
	/**Get Referenced Reflector*/
	public Reflector<E> getReflector() {
		return reflect;
	}
	
	
///////////////////////ENUM BUSTING/////////////////////////////
	
	/**Create A New Enum Instance And Add To Enum Array**/
	public E append(String enumname,Object...otherargs) {
		ReflectedField<?> values = reflect.getField(VALUES_FIELD);
		if(values == null) {
			System.err.println("[EnumBuster] Enum Field Access Error");
			return null;
		}
		Object[] enumarr = (Object[])values.getStatic();
		int num = enumarr.length;
		E newenum = genEnum(enumname,num,(Object[])otherargs);
		/////////////////PROCEED TO INSERT/////////////
		Object[] arr = Arrays.copyOf(enumarr,enumarr.length+1);
		System.arraycopy(enumarr, 0, arr, 0, enumarr.length);
		arr[num] = newenum;//SHOULD WORK ON INSERTION
		values.setStaticFinal(arr);
		return newenum;
	}
	
	/**Simply Create A New Enum {All DATA}**/
	public E Create(String enumname,int enumid,Object... otherargs) {
		return genEnum(enumname, enumid, (Object[])otherargs);
	}
	
	/**Simply Create A New Enum With An Id That Suits Appending**/
	public E Create(String enumname,Object... otherargs) {
		ReflectedField<?> values = reflect.getField(VALUES_FIELD);
		if(values == null) {
			System.err.println("[EnumBuster] Enum Field Access Error");
			return null;
		}
		Object[] enumarr = (Object[])values.getStatic();
		int num = enumarr.length;
		return genEnum(enumname,num,(Object[])otherargs);
	}
	
	/**Append A Pre-created Enum As The Last Value[*/
	@SuppressWarnings("rawtypes")
	public E appendPreCreatedEnum(E created) {
		ReflectedField<?> values = reflect.getField(VALUES_FIELD);
		if(values == null) {
			System.err.println("[EnumBuster] Enum Field Access Error");
			return null;
		}
		Object[] enumarr = (Object[])values.getStatic();
		int num = enumarr.length;
		Object[] arr = Arrays.copyOf(enumarr, enumarr.length+1);
		System.arraycopy(enumarr, 0, arr, 0, enumarr.length);
		arr[num] = created;
		//NOW ENSURE ORIDINALS LINE UP
		Reflector<Enum> enumreflect = enumReflector();
		if(getOrdinal(created) != num) {
			//FIX
			setOrdinal(created, num);
		}
		///PUSH TO THE END
		values.setStaticFinal(arr);
		return created;
	}
	
	/**Remove A Enum From Service*/
	@SuppressWarnings("unchecked")
	public E removeEnum(int index) {
		ReflectedField<?> values = reflect.getField(VALUES_FIELD);
		if(values == null) {
			System.err.println("[EnumBuster] Enum Field Access Error");
			return null;
		}
		Object[] enumarr = (Object[])values.getStatic();
		int num = enumarr.length;
		Object[] arr = Arrays.copyOf(enumarr, num-1);
		//HANDLE REMOVAL
		E removed = null;
		for(int i = 0; i < num; i++) {
			if(i != index) {
				if(removed == null) {
					arr[i] = enumarr[i];
				}else{
					arr[i-1] = enumarr[i];
				}
			}else{
				removed = (E) enumarr[i];
			}
		}
		//NOW PUSH NEW VALUES
		values.setStaticFinal(arr);
		fixOrdinals();//FIX SHIZZLE
		return removed;
	}
	
	/**Insert A New Enum At The Position*/
	public E insertEnum(E insert, int index) {
		ReflectedField<?> values = reflect.getField(VALUES_FIELD);
		if(values == null) {
			System.err.println("[EnumBuster] Enum Field Access Error");
			return null;
		}
		Object[] enumarr = (Object[])values.getStatic();
		int num = enumarr.length;
		Object[] arr = Arrays.copyOf(enumarr, num+1);
		boolean added = false;
		for(int i = 0; i < arr.length; i++) {
			if(i == index) {
				added = true;
				arr[i] = insert;
			}else {
				if(added) {
					arr[i] = enumarr[i-1];
				}else {
					arr[i] = enumarr[i];
				}
			}
		}
		//SET
		values.setStaticFinal(arr);
		fixOrdinals();
		return insert;
	}
	
	
	/**Returns The Enum Value Shizzle**/
	public Object[] values() {
		return (Object[]) reflect.getField(VALUES_FIELD).getStatic();
	}
	
/////////////////////PATCH SWITCH FIELDS////////////////////////
	
	/**Patch The Switch Fields Used For This Enum In Other Classes**/
	public void patchSwitchFields(Class<?>... clazzes) {
		for(Class<?> clazz : clazzes) {
			patchSwitchField(clazz);
		}
	}
	
	/**Patch The Switch Field Used For This Enum In This Class**/
	public void patchSwitchField(Class<?> clazz) {
		//TODO
		Reflector<?> ref = new Reflector<>(clazz);
		//NOT YET IMPLEMENTED :(
		
	}
	
////////////////////UTILITY SHIZZLE///////////////////////////////
	/**Get The Ordinal Enum Value**/
	@SuppressWarnings("rawtypes")
	public int getOrdinal(E val) {
		return (int)enumReflector().getField(ORDINAL_FIELD).get((Enum)val);
	}
	/**Set The Ordinal Enum Value**/
	@SuppressWarnings("rawtypes")
	public void setOrdinal(E val, int newordinal) {
		enumReflector().getField(ORDINAL_FIELD).setFinal((Enum)val, newordinal);
	}
	/**Get The Enum Name Value**/
	@SuppressWarnings("rawtypes")
	public String getName(E val) {
		return (String)enumReflector().getField(NAMES_FIELD).get((Enum)val);
	}
	/**Set The Enum Name Value**/
	@SuppressWarnings("rawtypes")
	public void setName(E val, String newname) {
		enumReflector().getField(NAMES_FIELD).setFinal((Enum)val, newname);
	}
	
//////////////////////PRIVATE SHIZZLE////////////////////////////
	
	@SuppressWarnings("rawtypes")
	private E genEnum(String name, int num,Object... args) {
		Object[] arguments = new Object[args.length + 2];
		arguments[0] = name;
		arguments[1] = num;
		System.arraycopy(
		        args, 0, arguments, 2, args.length);
		//NOW FIND AND CREATE THE CONSTRUCTOR ACCESSOR
		Class[] clazzes = new Class[args.length + 2];
		clazzes[0] = String.class;
		clazzes[1] = int.class;
		for(int i = 0; i < args.length; i++) {
			clazzes[i+2] = args[i].getClass();
		}							//TODO REFERENCE
		//Object cac = reflect.getReflectionFactory().generateConstructorAccessor(clazzes).getAccessor();
		//return createEnum(cac,(Object[])arguments);
		E toret =  reflect.getReflectionFactory().generateConstructorAccessor(clazzes).newInstance((Object[])new Object[]{arguments});
		assert(toret != null);//ASSERTION
		return toret;
	}
	@SuppressWarnings("rawtypes")
	private static Reflector<Enum> instance;
	
	@SuppressWarnings("rawtypes")
	private Reflector<Enum> enumReflector() {
		if(instance != null) {
			return instance;
		}
		instance = new Reflector<Enum>(Enum.class);
		return instance;
	}
	
	@SuppressWarnings("unchecked")
	private void fixOrdinals() {
		ReflectedField<?> values = reflect.getField(VALUES_FIELD);
		Object[] enumarr = (Object[])values.getStatic();
		for(int i = 0; i < enumarr.length; i++) {
			if(getOrdinal((E)enumarr[i]) != i) {
				setOrdinal((E)enumarr[i], i);
			}
		}
	}
}
