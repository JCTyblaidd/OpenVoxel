package net.openvoxel.loader.optimizer.tags;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by James on 09/04/2017.
 *
 * Mark as additional validation code - will be conditionally removed from the optimizer
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Validation {

}
