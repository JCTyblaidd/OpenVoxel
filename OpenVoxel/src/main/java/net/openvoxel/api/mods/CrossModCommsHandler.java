package net.openvoxel.api.mods;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by James on 25/08/2016.
 *
 * Marks a method in the @Mod class to handle cross mod communication
 *
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CrossModCommsHandler {
	String value();
}
