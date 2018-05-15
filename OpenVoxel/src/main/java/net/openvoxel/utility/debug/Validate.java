package net.openvoxel.utility.debug;

import net.openvoxel.api.PublicAPI;
import net.openvoxel.loader.classloader.Validation;

/**
 * Created by James on 09/04/2017.
 *
 * Standard Correctness of use validations
 */
public class Validate {

	/*
	 * Stores the main Thread for validation statements...
	 */
	private static Thread _mainThread;

	@PublicAPI
	@Validation
	public static void SetAsMainThread() {
		if(_mainThread != null) Unreachable();
		_mainThread = Thread.currentThread();
	}

	private static void _throw(String err) {
		throw new RuntimeException("Validate - " + err);
	}

	@PublicAPI
	@Validation
	private static void _validate(boolean param,String err) {
		if(!param) _throw(err);
	}

	@PublicAPI
	@Validation
	public static void Condition(boolean success,String error) {
		_validate(success,error);
	}


	@PublicAPI
	@Validation
	public static void IsMainThread() {
		_validate(Thread.currentThread() == _mainThread,"Must be called from main thread");
	}

	@PublicAPI
	@Validation
	public static void True(boolean param) {
		_validate(param,"Assertion is not true");
	}

	@PublicAPI
	@Validation
	public static void False(boolean param) {
		_validate(!param,"Assertion is not false");
	}

	@PublicAPI
	@Validation
	public static void Null(Object obj) {
		_validate(obj == null,"Assertion is not null");
	}

	@PublicAPI
	@Validation
	public static void NotNull(Object obj) {
		_validate(obj != null,"Assertion is null");
	}

	@PublicAPI
	@Validation
	public static void Unreachable() {
		_throw("Unreachable statement reached");
	}
}
