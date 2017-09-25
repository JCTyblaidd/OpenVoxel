package net.openvoxel.api.side;

import net.openvoxel.api.PublicAPI;

/**
 * Created by James on 31/07/2016.
 *
 * Utility : For Side Only Code
 */
@PublicAPI
public enum Side {
	CLIENT,
	DEDICATED_SERVER;

	public static boolean isClient;
}
