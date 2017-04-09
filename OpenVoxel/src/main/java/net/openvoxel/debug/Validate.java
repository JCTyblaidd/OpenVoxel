package net.openvoxel.debug;

import net.openvoxel.loader.optimizer.tags.Validation;

/**
 * Created by James on 09/04/2017.
 *
 * Standard Correctness of use validations
 */
public class Validate {

	/**
	 * Ensure that this method was called when a server exists
	 */
	@Validation
	public static void ServerExists() {

	}
}
