package net.openvoxel.client.renderer.gl3;

import net.openvoxel.client.renderer.generic.WorldRenderer;
import net.openvoxel.client.renderer.generic.config.CompressionLevel;
import net.openvoxel.client.renderer.generic.config.RenderConfig;
import net.openvoxel.client.renderer.gl3.worldutil.OGL3CacheManager;
import net.openvoxel.common.entity.living.player.EntityPlayerSP;
import net.openvoxel.common.world.World;
import net.openvoxel.common.world.chunk.Chunk;

import java.util.ArrayList;
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

	private List<Chunk> pollAndRequestUpdatesForNearbyChunks(EntityPlayerSP player,World world) {
		int playerChunkX = (int)(player.xPos / 16);
		int playerChunkZ = (int)(player.zPos / 16);
		int xMin = playerChunkX - 20;
		int xMax = playerChunkX + 20;
		int zMin = playerChunkZ - 20;
		int zMax = playerChunkZ + 20;
		List<Chunk> chunks = new ArrayList<>();
		for(int z = zMin; z <= zMax; z++) {
			for(int x = xMin; x <= xMax; x++) {
				chunks.add(world.requestChunk(x,z));
			}
		}
		return chunks;
	}

	private void checkForSettingsChange() {
		if(settingsDirty.get()) {
			settingsDirty.set(false);
			//Handle: settings change
			OGL3Renderer.instance.blockAtlas.update(128,false, CompressionLevel.NO_COMPRESSION);
		}
	}


	@Override
	public void renderWorld(EntityPlayerSP player, World world) {
		List<Chunk> toRender = pollAndRequestUpdatesForNearbyChunks(player,world);
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
