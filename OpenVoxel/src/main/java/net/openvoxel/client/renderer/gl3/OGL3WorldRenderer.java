package net.openvoxel.client.renderer.gl3;

import net.openvoxel.api.logger.Logger;
import net.openvoxel.client.ClientInput;
import net.openvoxel.client.renderer.generic.WorldRenderer;
import net.openvoxel.client.renderer.generic.config.CompressionLevel;
import net.openvoxel.client.renderer.generic.config.RenderConfig;
import net.openvoxel.client.renderer.gl3.worldrender.OGL3GBufferManager;
import net.openvoxel.client.renderer.gl3.worldrender.cache.OGL3RenderCache;
import net.openvoxel.client.renderer.gl3.worldrender.cache.OGL3RenderCacheManager;
import net.openvoxel.client.renderer.gl3.worldrender.shader.OGL3World_ShaderCache;
import net.openvoxel.client.renderer.gl3.worldrender.shader.OGL3World_UniformCache;
import net.openvoxel.common.entity.living.player.EntityPlayerSP;
import net.openvoxel.world.World;
import net.openvoxel.world.chunk.Chunk;
import net.openvoxel.world.client.ClientChunk;
import net.openvoxel.world.client.ClientChunkSection;
import net.openvoxel.world.client.ClientWorld;
import org.lwjgl.opengl.GL11;

import javax.vecmath.Matrix4f;
import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;
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
	private OGL3GBufferManager gBufferManager;
	private OGL3RenderCacheManager cacheManager;

	OGL3WorldRenderer() {
		currentSettings = new RenderConfig();
		cacheManager = new OGL3RenderCacheManager();
		OGL3World_UniformCache.Load();
		OGL3World_ShaderCache.Load();
	}

	private List<ClientChunk> pollAndRequestUpdatesForNearbyChunks(EntityPlayerSP player,ClientWorld world) {
		int playerChunkX = (int)(player.xPos / 16);
		int playerChunkZ = (int)(player.zPos / 16);
		int xMin = playerChunkX - 5;
		int xMax = playerChunkX + 5;
		int zMin = playerChunkZ - 5;
		int zMax = playerChunkZ + 5;
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
			//Handle: settings change
			Logger.getLogger("World Renderer").Info("Settings Update");
			OGL3Renderer.instance.blockAtlas.update(128,false, CompressionLevel.NO_COMPRESSION);
		}
	}


	private void setupUniforms(EntityPlayerSP player,ClientWorld world) {
		//Update Per Frame Uniform//
		int animCounter = 0;
		float aspectRatio = (float)ClientInput.currentWindowWidth.get() / ClientInput.currentWindowHeight.get();
		float fov = 90.0F;
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
				new Vector2f(0.1F,100.0F),aspectRatio,cameraPos,yaw,pitch,dayProgress,skylightPower,skylightColour,
				skyEnabled,fogColour,isRaining,isThunder,tileSize);
		OGL3World_UniformCache.bindAndUpdateTextureAtlas(OGL3Renderer.instance.blockAtlas);
	}

	private void setupCacheUniform(ClientChunk chunk,int yHeight) {
		float X = chunk.chunkX * 16.0F;
		float Y = yHeight * 16.0F;
		float Z = chunk.chunkZ * 16.0F;
		Matrix4f matrix4f = new Matrix4f();
		matrix4f.setIdentity();
		matrix4f.transform(new Vector3f(X,Y,Z));
		OGL3World_UniformCache.setChunkUniform(matrix4f);
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
		//TODO: remove custom background reset
		glClearColor(0,0,0.3F,1);
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		glEnable(GL_DEPTH_TEST);
		//Setup Uniforms//
		setupUniforms(player,world);
		//Allow all caches updates//
		for(ClientChunk chunk : toRender) {
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
		OGL3World_ShaderCache.BLOCK_SIMPLE.use();
		//Draw All Caches : todo update//
		for(ClientChunk chunk : toRender) {
			if(chunk != null) {
				for(int y = 0; y < 16; y++) {
					ClientChunkSection section = chunk.getSectionAt(y);
					if(section.renderCache != null) {
						OGL3RenderCache cache = cacheManager.loadRenderCache(section);
						if (cache.cacheExists()) {
							//Set Uniform Vertex//
							setupCacheUniform(chunk,y);
							//Draw// //TODO: enable//
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

	public void onSettingsChanged(RenderConfig settingChangeRequested) {
		currentSettings = settingChangeRequested;
		settingsDirty.set(true);
	}
}
