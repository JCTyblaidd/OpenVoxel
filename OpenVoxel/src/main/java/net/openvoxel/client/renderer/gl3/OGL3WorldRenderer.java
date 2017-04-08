package net.openvoxel.client.renderer.gl3;

import net.openvoxel.api.logger.Logger;
import net.openvoxel.client.renderer.generic.WorldRenderer;
import net.openvoxel.client.renderer.generic.config.CompressionLevel;
import net.openvoxel.client.renderer.generic.config.RenderConfig;
import net.openvoxel.client.renderer.gl3.worldutil.OGL3CacheManager;
import net.openvoxel.common.world.Chunk;
import net.openvoxel.common.world.World;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by James on 25/08/2016.
 *
 * World Renderer
 */
public final class OGL3WorldRenderer implements WorldRenderer{


	private RenderConfig currentSettings;
	private AtomicBoolean settingsDirty = new AtomicBoolean(true);
	private OGL3GBufferManager gBufferManager;
	private Map<World,List<Chunk>> availableChunks;
	private OGL3CacheManager cacheManager;

	OGL3WorldRenderer() {
		currentSettings = new RenderConfig();
		availableChunks = new HashMap<>();
		cacheManager = new OGL3CacheManager();
	}

	private List<Chunk> pollAndRequestUpdatesForNearbyChunks(World world) {
		return null;
	}

	private void checkForSettingsChange() {
		if(settingsDirty.get()) {
			settingsDirty.set(false);
			//Handle: settings change
			OGL3Renderer.instance.blockAtlas.update(128,false, CompressionLevel.NO_COMPRESSION);
		}
	}


	@Override
	public void renderWorld(World world) {
		List<Chunk> toRender = pollAndRequestUpdatesForNearbyChunks(world);
		checkForSettingsChange();
		if(currentSettings.useDeferredPipeline) {
			//Deferred//

		}else{
			//Standard Pipeline//
			
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
		settingsDirty.set(true);
	}
}
