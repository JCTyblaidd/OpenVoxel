package net.openvoxel.api.mods;

import net.openvoxel.api.PublicAPI;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by James on 25/08/2016.
 *
 * Marks a class as an ASM Code Tweaker
 */
@PublicAPI
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ASMHandler {

	String version() default "0.0.1-Alpha";

	String id();

	String name() default "";

	boolean isSeparateMod() default true;

	String getConnectedMod() default "";
}
