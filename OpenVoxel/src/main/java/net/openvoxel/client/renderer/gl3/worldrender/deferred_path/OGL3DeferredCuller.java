package net.openvoxel.client.renderer.gl3.worldrender.deferred_path;

import net.openvoxel.api.logger.Logger;
import net.openvoxel.client.control.Renderer;
import net.openvoxel.client.renderer.gl3.worldrender.cache.OGL3RenderCache;
import net.openvoxel.client.utility.FrustumCuller;
import net.openvoxel.common.entity.living.player.EntityPlayerSP;
import net.openvoxel.utility.AsyncRunnablePool;
import net.openvoxel.utility.MatrixUtils;
import net.openvoxel.utility.collection.ChunkMap;
import net.openvoxel.world.client.ClientChunk;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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

	private FrustumCuller culler = new FrustumCuller();
	private final Lock lock = new ReentrantLock();
	private Condition conditionStandard = lock.newCondition();
	private Condition conditionVoxel = lock.newCondition();
	private Condition conditionShadow = lock.newCondition();
	private AtomicBoolean flagStandard = new AtomicBoolean(false);

	private ChunkMap<ClientChunk> inputs = new ChunkMap<>();

	private List<OGL3RenderCache> cullStandardResult = new ArrayList<>();
	private List<OGL3RenderCache> cullVoxelResult = new ArrayList<>();
	private List<OGL3RenderCache> cullShadowResult = new ArrayList<>();

	private interface BiIntConsumer {
		void consumer(int x, int y);
	}

	private void callValid8x8Chunks(int xMin, int xMax, int zMin, int zMax,BiIntConsumer consumer) {
		for(int x8 = xMin; x8 < xMax; x8 += 8) {
			for(int z8 = zMin; z8 < zMax; z8 += 8) {
				if(culler.chunk8x8Collides(x8,z8)) {
					consumer.consumer(x8,z8);
				}
			}
		}
	}

	private void callValid4x4Chunks(int x8, int z8, BiIntConsumer consumer) {
		if(culler.chunk4x4Collides(x8,z8)) {
			consumer.consumer(x8,z8);
		}
		if(culler.chunk4x4Collides(x8+4,z8)) {
			consumer.consumer(x8+4,z8);
		}
		if(culler.chunk4x4Collides(x8,z8+4)) {
			consumer.consumer(x8,z8+4);
		}
		if(culler.chunk4x4Collides(x8+4,z8+4)) {
			consumer.consumer(x8+4,z8+4);
		}
	}

	private void callValid2x2Chunks(int x4, int z4, BiIntConsumer consumer) {
		if(culler.chunk2x2Collides(x4,z4)) {
			consumer.consumer(x4,z4);
		}
		if(culler.chunk2x2Collides(x4+2,z4)) {
			consumer.consumer(x4+2,z4);
		}
		if(culler.chunk2x2Collides(x4,z4+2)) {
			consumer.consumer(x4,z4+2);
		}
		if(culler.chunk2x2Collides(x4+2,z4+2)) {
			consumer.consumer(x4+2,z4+2);
		}
	}

	private void callValidChunks(int x2, int z2, BiIntConsumer consumer) {
		if(culler.chunkCollides(x2,z2)) {
			consumer.consumer(x2,z2);
		}
		if(culler.chunkCollides(x2+1,z2)) {
			consumer.consumer(x2+1,z2);
		}
		if(culler.chunkCollides(x2,z2+1)) {
			consumer.consumer(x2,z2+1);
		}
		if(culler.chunkCollides(x2+1,z2+1)) {
			consumer.consumer(x2+1,z2+1);
		}
	}


	/**
	 * Initialize the asynchronous culling service
	 *
	 * TODO: improve setup
	 */
	void startCulling(EntityPlayerSP player, Set<ClientChunk> clientChunks) {
		// Pre Cull Setup//
		int playerChunkX = (int)(player.xPos / 16);
		int playerChunkZ = (int)(player.zPos / 16);
		int xMaxC = playerChunkX;
		int xMinC = playerChunkX;
		int zMaxC = playerChunkZ;
		int zMinC = playerChunkZ;
		culler.updateFrustum(MatrixUtils.getLastProjectionViewMatrix());
		inputs.emptyAll();
		for(ClientChunk chunk : clientChunks) {
			inputs.set(chunk.chunkX,chunk.chunkZ,chunk);
			xMaxC = Math.max(xMaxC,chunk.chunkX);
			xMinC = Math.min(xMinC,chunk.chunkX);
			zMaxC = Math.max(zMaxC,chunk.chunkZ);
			zMinC = Math.min(zMinC,chunk.chunkZ);
		}
		cullStandardResult.clear();
		cullVoxelResult.clear();
		cullShadowResult.clear();
		flagStandard.set(false);
		/// Perform The Culling ///
		final int xMax = xMaxC;
		final int xMin = xMinC;
		final int zMax = zMaxC;
		final int zMin = zMinC;
		Renderer.renderCacheManager.addWork(() -> {
			//Perform Frustum Culling
			callValid8x8Chunks(xMin-3,xMax+4,zMin-3,zMax+4,(x8,z8) -> {
				callValid4x4Chunks(x8,z8, (x4,z4) -> {
					callValid2x2Chunks(x4,z4, (x2,z2) -> {
						callValidChunks(x2,z2, (x,z) -> {
							ClientChunk chunk = inputs.get(x,z);
							if(chunk != null) {
								if (culler.chunkHalfSectionCollides(x, 0, z)) {
									for(int y = 0; y < 8; y++) {
										if(culler.chunkSectionCollides(x,y,z)) {
											OGL3RenderCache cache = (OGL3RenderCache)chunk.getSectionAt(y).renderCache.get();
											cache.chunk = chunk;
											cache.yPos = y;
											cullStandardResult.add(cache);
										}
									}
								}
								if (culler.chunkHalfSectionCollides(x, 128, z)) {
									for(int y = 8; y < 16; y++) {
										if(culler.chunkSectionCollides(x,y,z)) {
											OGL3RenderCache cache = (OGL3RenderCache)chunk.getSectionAt(y).renderCache.get();
											cache.chunk = chunk;
											cache.yPos = y;
											cullStandardResult.add(cache);
										}
									}
								}
							}
						});
					});
				});
			});
			flagStandard.set(true);
		});
		Renderer.renderCacheManager.addWork(() -> {

			lock.lock();
			conditionShadow.signalAll();
			lock.unlock();
		});
		Renderer.renderCacheManager.addWork(() -> {

			lock.lock();
			conditionVoxel.signalAll();
			lock.unlock();
		});
	}

	private static final List<OGL3RenderCache> EMPTY_RESULT = new ArrayList<>();

	/**
	 * Request and await the standard cull render request
	 */
	List<OGL3RenderCache> requestCullStandard() {
		if(flagStandard.get()) {
			return cullStandardResult;
		}
		for(int i = 0; i < 20; i++) {
			try{
				Thread.sleep(1);
			}catch (InterruptedException ignored) {}
			if(flagStandard.get()) {
				return cullStandardResult;
			}
		}
		return EMPTY_RESULT;
	}

	/**
	 * Request and await the voxel based cull render request
	 */
	public List<OGL3RenderCache> requestCullVoxel() {
		conditionVoxel.awaitUninterruptibly();
		return cullVoxelResult;
	}

	/**
	 * Request and await the shadow based cull render request
	 */
	public List<OGL3RenderCache> requestCullShadow() {
		conditionShadow.awaitUninterruptibly();
		return cullShadowResult;
	}
}
