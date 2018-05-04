package net.openvoxel.utility.collection;

import java.util.HashMap;
import java.util.Map;

public class SubChunkMap<T> {

	private static class Coord {
		long x;
		int y;
		long z;
		private Coord(long x, int y, long z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}
	}

	private Map<Coord,T> DataMap = new HashMap<>();

	public void setSubChunk(long chunkX,long chunkZ,int subChunkY, T t) {
		DataMap.put(new Coord(chunkX,subChunkY,chunkZ),t);
	}

	public T getSubChunk(long chunkX,long chunkZ,int subChunkY) {
		return DataMap.get(new Coord(chunkX,subChunkY,chunkZ));
	}

	public T removeSubChunk(long chunkX,long chunkZ, int subChunkY) {
		return DataMap.remove(new Coord(chunkX,subChunkY,chunkZ));
	}

}
