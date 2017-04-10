package net.openvoxel.client.renderer.gl3;

import net.openvoxel.api.logger.Logger;
import net.openvoxel.client.ClientInput;
import net.openvoxel.client.renderer.generic.WorldRenderer;
import net.openvoxel.client.renderer.generic.config.CompressionLevel;
import net.openvoxel.client.renderer.generic.config.RenderConfig;
import net.openvoxel.client.renderer.gl3.atlas.OGL3TextureAtlas;
import net.openvoxel.client.renderer.gl3.util.OGL3CubeMapTexture;
import net.openvoxel.client.renderer.gl3.worldrender.OGL3ForwardWorldRenderer;
import net.openvoxel.client.renderer.gl3.worldrender.cache.OGL3RenderCache;
import net.openvoxel.client.renderer.gl3.worldrender.cache.OGL3RenderCacheManager;
import net.openvoxel.client.renderer.gl3.worldrender.deferred_path.OGL3DeferredWorldRenderer;
import net.openvoxel.client.renderer.gl3.worldrender.shader.OGL3World_ShaderCache;
import net.openvoxel.client.renderer.gl3.worldrender.shader.OGL3World_UniformCache;
import net.openvoxel.common.entity.living.player.EntityPlayerSP;
import net.openvoxel.utility.MatrixUtils;
import net.openvoxel.world.World;
import net.openvoxel.world.chunk.Chunk;
import net.openvoxel.world.client.ClientChunk;
import net.openvoxel.world.client.ClientChunkSection;
import net.openvoxel.world.client.ClientWorld;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.lwjgl.opengl.GL11.*;

/**
 * Created by James on 25/08/2016.
 *
 * World Renderer
 */
public final class OGL3WorldRenderer implements WorldRenderer{


	private RenderConfig currentSettings;
	private AtomicBoolean settingsDirty = new AtomicBoolean(true);
	private OGL3DeferredWorldRenderer deferredWorldRenderer;
	private OGL3ForwardWorldRenderer forwardWorldRenderer;
	private OGL3RenderCacheManager cacheManager;

	OGL3WorldRenderer() {
		currentSettings = new RenderConfig();
		cacheManager = new OGL3RenderCacheManager();
		OGL3World_UniformCache.Load();
		OGL3World_ShaderCache.Load();
		deferredWorldRenderer = new OGL3DeferredWorldRenderer();
		forwardWorldRenderer = new OGL3ForwardWorldRenderer();
	}

	private List<ClientChunk> pollAndRequestUpdatesForNearbyChunks(EntityPlayerSP player,ClientWorld world) {
		int playerChunkX = (int)(player.xPos / 16);
		int playerChunkZ = (int)(player.zPos / 16);
		int xMin = playerChunkX - 8;
		int xMax = playerChunkX + 8;
		int zMin = playerChunkZ - 8;
		int zMax = playerChunkZ + 8;
		List<ClientChunk> chunks = new ArrayList<>();
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
			//TODO: enable settings tweaking
			Logger.getLogger("World Renderer").Info("Settings Update");
			OGL3Renderer.instance.blockAtlas.update(128,false, CompressionLevel.NO_COMPRESSION);
		}
	}


	private void setupUniforms(EntityPlayerSP player,ClientWorld world) {
		//Update Per Frame Uniform//
		int animCounter = 0;
		float aspectRatio = (float)ClientInput.currentWindowWidth.get() / ClientInput.currentWindowHeight.get();
		float fov = 80.0F;
		Vector3f cameraPos = new Vector3f((float)player.xPos,(float)player.yPos,(float)player.zPos);
		float yaw = player.getYaw();
		float pitch = player.getPitch();
		float dayProgress = 0;
		float skylightPower = 0;
		Vector3f skylightColour = new Vector3f(1,1,1);
		boolean skyEnabled = true;
		Vector3f fogColour = new Vector3f(0,0,0);
		boolean isRaining = false;
		boolean isThunder = false;
		Vector2f tileSize = new Vector2f(1,1);//TODO:
		OGL3World_UniformCache.calcAndUpdateFrameInformation(animCounter,(float)Math.toRadians(fov),
				new Vector2f(0.1F,1000.0F),aspectRatio,cameraPos,yaw,pitch,dayProgress,skylightPower,skylightColour,
				skyEnabled,fogColour,isRaining,isThunder,tileSize);
	}


	private void setupCacheUniform(ClientChunk chunk,int yHeight) {
		float X = chunk.chunkX * 16.0F;
		float Y = yHeight * 16.0F;
		float Z = chunk.chunkZ * 16.0F;
		Matrix4f matrix = MatrixUtils.genChunkPositionMatrix(X,Y,Z);
		OGL3World_UniformCache.setChunkUniform(matrix);
	}


	private void updateChunks(List<ClientChunk> chunkData) {
		for(ClientChunk chunk : chunkData) {
			if(chunk != null) {
				for(int y = 0; y < 16; y++) {
					ClientChunkSection section = chunk.getSectionAt(y);
					if(section.renderCache == null) {
						cacheManager.requestRenderCacheGeneration(section);
					}else{
						OGL3RenderCache cache = cacheManager.loadRenderCache(section);
						cache.updateGLAndRelease();
					}
				}
			}
		}
	}

	private void generateWorldBackground(EntityPlayerSP player, ClientWorld world) {
		int worldSkyScatter = 0xCFFFFF;
		int worldLightColour = 0xDFE5A2;
		//TODO: sky cube map
		glDisable(GL_DEPTH_TEST);
		glClearColor(187.F/255.F,1.F,1.F,1.F);
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
	}

	/**
	 * TODO: rework entirely [async section clipping from player & from sun]
	 *  TODO: add support for deferred render path
	 * @param player the player to draw from the viewpoint of
	 * @param world the world to draw
	 */
	@Override
	public void renderWorld(EntityPlayerSP player, ClientWorld world) {
		List<ClientChunk> toRender = pollAndRequestUpdatesForNearbyChunks(player,world);
		checkForSettingsChange();
		generateWorldBackground(player, world);
		glEnable(GL_DEPTH_TEST);
		glEnable(GL_CULL_FACE);
		updateChunks(toRender);
		setupUniforms(player,world);
		OGL3World_ShaderCache.BLOCK_SIMPLE.use();
		for(ClientChunk chunk : toRender) {
			if(chunk != null) {
				for(int y = 0; y < 16; y++) {
					ClientChunkSection section = chunk.getSectionAt(y);
					if(section.renderCache != null) {
						OGL3RenderCache cache = cacheManager.loadRenderCache(section);
						if (cache.cacheExists()) {
							//Set Uniform Vertex//
							setupCacheUniform(chunk,y);
							cache.draw();
						}
					}
				}
			}
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

	void onSettingsChanged(RenderConfig settingChangeRequested) {
		currentSettings = settingChangeRequested;
		settingsDirty.set(true);
	}
}
