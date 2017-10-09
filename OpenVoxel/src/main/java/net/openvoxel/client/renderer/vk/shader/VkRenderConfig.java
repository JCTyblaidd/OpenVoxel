package net.openvoxel.client.renderer.vk.shader;

import org.joml.Vector2f;

public class VkRenderConfig {

	public enum CullMethod {
		CULL_NONE,
		CULL_FRUSTRUM,
		CULL_SUBCHUNK,
	}

	public CullMethod cullMethod = CullMethod.CULL_SUBCHUNK;

	public boolean enableClouds = true;

	public float chosenGamma = 1.0F / 2.2F;

	public boolean enableShadowMapping = true;

	public boolean enableCascadeShadowMaps = true;

	public Vector2f chosenResolution = null;
}
