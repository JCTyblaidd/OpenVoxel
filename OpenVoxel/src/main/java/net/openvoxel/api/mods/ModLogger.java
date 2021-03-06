package net.openvoxel.api.mods;

import net.openvoxel.api.PublicAPI;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by James on 08/04/2017.
 *
 * Mark a field that will be populated with your mods logger
 */
@PublicAPI
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ModLogger {
}
