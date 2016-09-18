package net.openvoxel.client.renderer.gl3.worldutil;

import net.openvoxel.common.world.ChunkCoordinate;

import java.util.HashMap;

/**
 * Created by James on 04/09/2016.
 *
 * Manage OpenGL Chunk Data Caches {Render Information}
 */
public class OGL3CacheManager {

	private static final HashMap<ChunkCoordinate,OGL3SubChunkCache[]> CACHE = new HashMap<>();
	public static int currentWorld = -1;

	public static OGL3SubChunkCache Load(ChunkCoordinate coordinate, int Y) {
		try{
			return CACHE.get(coordinate)[Y];
		}catch(NullPointerException e) {
			return null;
		}
	}

	public static OGL3SubChunkCache LoadCreate(ChunkCoordinate coordinate, int Y) {
		OGL3SubChunkCache[] cacheArr = CACHE.get(coordinate);
		if(cacheArr == null) {
			cacheArr = new OGL3SubChunkCache[16];
			CACHE.put(coordinate,cacheArr);
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
	 * @param coordinate
	 */
	public static void Forget(ChunkCoordinate coordinate) {
		OGL3SubChunkCache[] arr = CACHE.get(coordinate);
		if(arr != null) {
			for (int i = 0; i < arr.length; i++) {
				arr[i].kill();
				arr[i] = null;
			}
		}
		CACHE.remove(coordinate);
	}

	/**
	 * Clear All Loaded Data
	 */
	public static void ForgetAll() {
		CACHE.forEach((k,v) -> Forget(k));
	}
}
