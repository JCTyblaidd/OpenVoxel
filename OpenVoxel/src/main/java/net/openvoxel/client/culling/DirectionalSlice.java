package net.openvoxel.client.culling;

import javax.vecmath.Vector3f;

/**
 * Created by James on 28/09/2016.
 *
 * Infinite Length Slice, used to check if an object should be culled for the shadow maps
 */
public class DirectionalSlice {

	public Vector3f Direction;
	public Vector3f Location;
	public Vector3f Size;


	public DirectionalSlice() {
		Direction = new Vector3f();
		Location = new Vector3f();
		Size = new Vector3f();
	}

}
