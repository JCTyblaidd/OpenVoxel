package net.openvoxel.api.mods;

import net.openvoxel.api.PublicAPI;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by James on 25/08/2016.
 *
 * Marks an @Mod class method to handle an initialisation event
 *
 */
@PublicAPI
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ModInitEventHandler {
}
