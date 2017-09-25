package net.openvoxel.utility.debug;

import net.openvoxel.loader.classloader.Validation;

/**
 * Created by James on 09/04/2017.
 *
 * Standard Correctness of use validations
 */
public class Validate {

	@Validation
	private static void _validate(boolean param,String err) {
		if(!param) {
			throw new RuntimeException("Validate - " + err);
		}
	}


	@Validation
	public static void Condition(boolean success,String error) {
		_validate(success,error);
	}

	@Validation
	public static void ServerExists() {

	}

	@Validation
	public static void IsMainThread() {

	}

	@Validation
	public static void IsRenderThread() {

	}

	@Validation
	public static void True(boolean param) {
		_validate(param,"Assertion is not true");
	}

	@Validation
	public static void False(boolean param) {
		_validate(!param,"Assertion is not false");
	}

	@Validation
	public static void Null(Object obj) {
		_validate(obj == null,"Assertion is not null");
	}

	@Validation
	public static void NotNull(Object obj) {
		_validate(obj != null,"Assertion is null");
	}
}
