package net.openvoxel.client.renderer.gl3;

import net.openvoxel.api.logger.Logger;
import net.openvoxel.client.renderer.generic.WorldRenderer;
import net.openvoxel.client.renderer.generic.config.CompressionLevel;
import net.openvoxel.client.renderer.generic.config.RenderConfig;
import net.openvoxel.client.renderer.gl3.atlas.OGL3TextureAtlas;
import net.openvoxel.common.world.Chunk;
import net.openvoxel.common.world.World;

/**
 * Created by James on 25/08/2016.
 *
 * World Renderer
 */
public final class OGL3WorldRenderer implements WorldRenderer{


	public RenderConfig currentSettings;
	public boolean settingsDirty;

	public OGL3WorldRenderer() {
		currentSettings = new RenderConfig();
	}

	private void pollAndRequestUpdatesForNearbyChunks() {
		//Request Chunk Updates//
	}

	private void checkForSettingsChange() {
		if(settingsDirty) {
			settingsDirty = false;
			//Handle:
			OGL3Renderer.instance.blockAtlas.update(128,false, CompressionLevel.NO_COMPRESSION);
		}
	}


	@Override
	public void renderWorld(World world) {
		pollAndRequestUpdatesForNearbyChunks();
		checkForSettingsChange();
		if(currentSettings.useDeferredPipeline) {
			//Deferred//
		}else{

		}
	}

	@Override
	public void onChunkLoaded(World world, Chunk chunk) {
		//Prepare Chunk For Rendering//
	}

	@Override
	public void onChunkUnloaded(World world, Chunk chunk) {
		//Cleanup Chunk Rendering Data//
	}

	public void onSettingsChanged(RenderConfig settingChangeRequested) {
		currentSettings = settingChangeRequested;
		settingsDirty = true;
	}
}
