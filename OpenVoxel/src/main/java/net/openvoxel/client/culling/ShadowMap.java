package net.openvoxel.client.culling;

/**
 * Created by James on 28/09/2016.
 *
 * Shadow Map Setup
 */
public class ShadowMap {

	//Cascades
	public DirectionalSlice SLICE_NEAR;
	public DirectionalSlice SLICE_MID;
	public DirectionalSlice SLICE_FAR;

	//For Lazy Shadow Map
	public DirectionalSlice SLICE_ALL;

}
