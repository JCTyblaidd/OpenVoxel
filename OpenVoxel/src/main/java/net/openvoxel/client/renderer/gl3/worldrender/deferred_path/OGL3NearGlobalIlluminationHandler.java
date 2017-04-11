package net.openvoxel.client.renderer.gl3.worldrender.deferred_path;

/**
 * Created by James on 11/04/2017.
 *
 * Handles Realistic Global Illumination for areas close to the player
 */
class OGL3NearGlobalIlluminationHandler {

	private OGL3DeferredWorldRenderer deferredWorldRenderer;

	private int voxelRenderTarget;


	OGL3NearGlobalIlluminationHandler(OGL3DeferredWorldRenderer deferredWorldRenderer) {
		this.deferredWorldRenderer = deferredWorldRenderer;
	}
}
