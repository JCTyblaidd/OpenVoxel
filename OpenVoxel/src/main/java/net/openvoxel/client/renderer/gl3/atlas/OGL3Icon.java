package net.openvoxel.client.renderer.gl3.atlas;

import net.openvoxel.client.textureatlas.Icon;
import net.openvoxel.common.resources.ResourceHandle;

/**
 * Created by James on 16/09/2016.
 *
 * References a texture atlas w/ reference for stuff
 */
public class OGL3Icon implements Icon{
	public float u_min;
	public float u_max;
	public float v_min;
	public float v_max;

	public ResourceHandle diffuse_dat;
	public ResourceHandle normal_dat;
	public ResourceHandle pbr_dat;
}
