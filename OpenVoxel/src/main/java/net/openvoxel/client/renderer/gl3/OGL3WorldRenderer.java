package net.openvoxel.client.renderer.gl3;

import net.openvoxel.api.logger.Logger;
import net.openvoxel.client.ClientInput;
import net.openvoxel.client.renderer.generic.WorldRenderer;
import net.openvoxel.client.renderer.generic.config.CompressionLevel;
import net.openvoxel.client.renderer.generic.config.RenderConfig;
import net.openvoxel.client.renderer.gl3.worldrender.OGL3ForwardWorldRenderer;
import net.openvoxel.client.renderer.gl3.worldrender.cache.OGL3RenderCacheManager;
import net.openvoxel.client.renderer.gl3.worldrender.deferred_path.OGL3DeferredWorldRenderer;
import net.openvoxel.client.renderer.gl3.worldrender.shader.OGL3World_ShaderCache;
import net.openvoxel.client.renderer.gl3.worldrender.shader.OGL3World_UniformCache;
import net.openvoxel.common.entity.living.player.EntityPlayerSP;
import net.openvoxel.utility.MatrixUtils;
import net.openvoxel.world.client.ClientChunk;
import net.openvoxel.world.client.ClientChunkSection;
import net.openvoxel.world.client.ClientWorld;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import static org.lwjgl.opengl.GL11.*;

/**
 * Created by James on 25/08/2016.
 *
 * World Renderer
 */
public final class OGL3WorldRenderer implements WorldRenderer{

	public static final float Z_NEAR = 0.1F;
	public static final float Z_FAR = 1000.0F;

	private static final int UPDATE_ITERATION_COUNT = 7;

	private RenderConfig currentSettings;
	private AtomicBoolean settingsDirty = new AtomicBoolean(true);
	private OGL3DeferredWorldRenderer deferredWorldRenderer;
	private OGL3ForwardWorldRenderer forwardWorldRenderer;
	public OGL3RenderCacheManager cacheManager;

	private ReentrantLock sectionLock = new ReentrantLock();
	private List<ClientChunk> toGenerateSections = new ArrayList<>();
	private List<ClientChunk> toRemoveSections = new ArrayList<>();

	private Set<ClientChunk> enabledChunks = new HashSet<>();

	OGL3WorldRenderer() {
		currentSettings = new RenderConfig();
		cacheManager = new OGL3RenderCacheManager();
		OGL3World_UniformCache.Load();
		OGL3World_ShaderCache.Load();
		deferredWorldRenderer = new OGL3DeferredWorldRenderer(this);
		forwardWorldRenderer = new OGL3ForwardWorldRenderer(this);
	}

	private void checkForSettingsChange() {
		if(settingsDirty.get()) {
			settingsDirty.set(false);
			//TODO: enable settings tweaking
			Logger.getLogger("World Renderer").Info("Settings Update");
			OGL3Renderer.instance.blockAtlas.update(128,false, CompressionLevel.NO_COMPRESSION);
		}
	}

	private static final Vector2f nearFarVector = new Vector2f();
	private static final Vector3f cameraPosVector = new Vector3f();
	private void setupUniforms(EntityPlayerSP player,ClientWorld world) {
		//Update Per Frame Uniform//
		int animCounter = 0;
		float aspectRatio = (float)ClientInput.currentWindowWidth.get() / ClientInput.currentWindowHeight.get();
		float fov = 50.0F;
		cameraPosVector.set((float)player.xPos,(float)player.yPos,(float)player.zPos);
		float yaw = player.getYaw();
		float pitch = player.getPitch();
		float dayProgress = 0;
		float skylightPower = 0;
		Vector3f skylightColour = new Vector3f(1,1,1);
		boolean skyEnabled = true;
		Vector3f fogColour = new Vector3f(0,0,0);
		boolean isRaining = false;
		boolean isThunder = false;
		Vector2f tileSize = new Vector2f(1,1);
		nearFarVector.set(Z_NEAR, Z_FAR);
		OGL3World_UniformCache.calcAndUpdateFrameInformation(animCounter,(float)Math.toRadians(fov),
				nearFarVector,aspectRatio,cameraPosVector,yaw,pitch,dayProgress,skylightPower,skylightColour,
				skyEnabled,fogColour,isRaining,isThunder,tileSize);
		deferredWorldRenderer.updateUniforms();
	}


	public void setupCacheUniform(ClientChunk chunk,int yHeight) {
		float X = chunk.chunkX * 16.0F;
		float Y = yHeight * 16.0F;
		float Z = chunk.chunkZ * 16.0F;
		Matrix4f matrix = MatrixUtils.genChunkPositionMatrix(X,Y,Z);
		OGL3World_UniformCache.setChunkUniform(matrix);
	}

	/**
	 * Execute the target a limited number of executions
	 */
	private void handleLimitedGenSections() {
		int listSize;
		for(int i = 0; i < UPDATE_ITERATION_COUNT && (listSize = toGenerateSections.size()) != 0; i++) {
			ClientChunk chunk = toGenerateSections.get(listSize-1);
			cacheManager.handleChunkLoad(chunk);
			enabledChunks.add(chunk);
			toGenerateSections.remove(listSize-1);
		}
	}

	/**
	 * Execute the target a limited number of executions
	 */
	private void handleLimitedRemoveSections() {
		int listSize;
		for(int i = 0; i < UPDATE_ITERATION_COUNT && (listSize = toRemoveSections.size()) != 0; i++) {
			ClientChunk chunk = toRemoveSections.get(listSize-1);
			cacheManager.handleChunkUnload(chunk);
			enabledChunks.remove(chunk);
			toRemoveSections.remove(listSize-1);
		}
	}


	/**
	 * Handle the updating and removing of new chunk information
	 *
	 * TODO: limit the processing rate to stop large stutters
	 */
	private void updateChunks() {
		sectionLock.lock();
		handleLimitedGenSections();
		handleLimitedRemoveSections();
		sectionLock.unlock();
		for(ClientChunk chunk : enabledChunks) {
			for(int y = 0; y < 16; y++) {
				ClientChunkSection section = chunk.getSectionAt(y);
				if(section.renderCache.get() != null) {
					cacheManager.loadRenderCache(section).updateGLAndRelease();
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
		setupUniforms(player,world);
		if(!currentSettings.useDeferredPipeline) {
			deferredWorldRenderer.preRenderWorld(player,enabledChunks);
		}
		glEnable(GL_DEPTH_TEST);
		glEnable(GL_CULL_FACE);
		updateChunks();
		checkForSettingsChange();
		generateWorldBackground(player, world);
		if(!currentSettings.useDeferredPipeline) {//TODO: change back
			deferredWorldRenderer.renderWorld(player,world);
		}else{
			//forwardWorldRenderer.renderWorld(player,world,enabledChunks);
		}
	}

	/**
	 * Called When a Chunk is Loaded : Client Server Thread
	 */
	@Override
	public void onChunkLoaded(ClientChunk chunk) {
		sectionLock.lock();
		toGenerateSections.add(chunk);
		sectionLock.unlock();
	}

	/**
	 * Called When Chunk Information is Dirty : Client Server Thread
	 */
	@Override
	public void onChunkDirty(ClientChunk chunk) {
		for(int y = 0; y < 16; y++) {
			ClientChunkSection section = chunk.getSectionAt(y);
			if(section.isDirty()) {
				cacheManager.handleDirtySection(section);
			}
		}
	}

	/**
	 * Called When A Chunk is Unloaded : Client Server Thread
	 */
	@Override
	public void onChunkUnloaded(ClientChunk chunk) {
		sectionLock.lock();
		toRemoveSections.add(chunk);
		sectionLock.unlock();
	}

	/**
	 * Called When The Settings Are Changed : Renderer Thread
	 */
	void onSettingsChanged(RenderConfig settingChangeRequested) {
		currentSettings = settingChangeRequested;
		settingsDirty.set(true);
	}

	/**
	 * Called When The Window is Resized : Renderer Thread
	 */
	void onWindowResized(int width, int height) {
		deferredWorldRenderer.onFrameResize(width, height);
	}
}
