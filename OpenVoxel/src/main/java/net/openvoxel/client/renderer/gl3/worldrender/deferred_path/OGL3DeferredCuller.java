package net.openvoxel.client.renderer.gl3.worldrender.deferred_path;

import net.openvoxel.client.renderer.gl3.worldrender.cache.OGL3RenderCache;
import net.openvoxel.client.utility.FrustumCuller;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by James on 11/04/2017.
 *
 * Deferred Renderer Culling Utility
 *
 * Culls the scene into sections for:
 *      - Standard Rendering
 *      - Voxel Rendering
 *      - Shadow Rendering
 */
public class OGL3DeferredCuller {

	private FrustumCuller culler;


	/**
	 * Initialize the asynchronous culling service
	 */
	public void startCulling() {

	}

	/**
	 * Request and await the standard cull render request
	 */
	public List<OGL3RenderCache> requestCullStandard() {
		return new ArrayList<>();
	}

	/**
	 * Request and await the voxel based cull render request
	 */
	public List<OGL3RenderCache> requestCullVoxel() {
		return new ArrayList<>();
	}

	/**
	 * Request and await the shadow based cull render request
	 */
	public List<OGL3RenderCache> requestCullShadow() {
		return new ArrayList<>();
	}
}
