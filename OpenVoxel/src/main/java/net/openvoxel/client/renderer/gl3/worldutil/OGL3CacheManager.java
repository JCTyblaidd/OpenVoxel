package net.openvoxel.client.renderer.gl3.worldutil;

import net.openvoxel.collection.ChunkMap;

/**
 * Created by James on 04/09/2016.
 *
 * Manage OpenGL Chunk Data Caches {Render Information}
 */
public class OGL3CacheManager {

	private static final ChunkMap<OGL3SubChunkCache[]> CACHE = new ChunkMap<>();
	public static int currentWorld = -1;

	public static OGL3SubChunkCache Load(int chunkX, int chunkZ, int Y) {
		try{
			return CACHE.get(chunkX,chunkZ)[Y];
		}catch(NullPointerException e) {
			return null;
		}
	}

	public static OGL3SubChunkCache LoadCreate(int chunkX, int chunkZ, int Y) {
		OGL3SubChunkCache[] cacheArr = CACHE.get(chunkX,chunkZ);
		if(cacheArr == null) {
			cacheArr = new OGL3SubChunkCache[16];
			CACHE.set(chunkX,chunkZ,cacheArr);
			OGL3SubChunkCache newCache = new OGL3SubChunkCache();
			cacheArr[Y] = newCache;
			return newCache;
		}else{
			if(cacheArr[Y] == null){
				OGL3SubChunkCache newCache = new OGL3SubChunkCache();
				cacheArr[Y] = newCache;
				return newCache;
			}else{
				return cacheArr[Y];
			}
		}
	}

	/**
	 * Clear Chunk Rendering Data
	 */
	public static void Forget(int chunkX, int chunkZ) {
		OGL3SubChunkCache[] arr = CACHE.get(chunkX,chunkZ);
		if(arr != null) {
			for (int i = 0; i < arr.length; i++) {
				arr[i].kill();
				arr[i] = null;
			}
		}
		CACHE.remove(chunkX,chunkZ);
	}

	/**
	 * Clear All Loaded Data
	 */
	public static void ForgetAll() {
		//TODO: implement
	}
}
