package net.openvoxel.api.side;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by James on 31/07/2016.
 *
 * Enable to prevent the loading a particular function for the client or the server
 */
@SuppressWarnings("unused")
@Target({ElementType.FIELD,ElementType.METHOD,ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface SideOnly {
	Side side();
	SideOperation operation() default SideOperation.REMOVE_STRUCTURE;

	/**What to do to a structure when the siding is incorrect**/
	enum SideOperation {
		/**Makes a method do absolutely nothing, invalid for field*/
		REMOVE_CODE,
		/**Removes the field or method from the generated class**/
		REMOVE_STRUCTURE,
	}
}
