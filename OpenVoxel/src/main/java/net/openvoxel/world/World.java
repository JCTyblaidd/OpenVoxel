package net.openvoxel.world;

import net.openvoxel.common.entity.Entity;
import net.openvoxel.utility.collection.ChunkMap;
import net.openvoxel.world.chunk.Chunk;
import net.openvoxel.world.generation.IWorldGenerator;

/**
 * Created by James on 25/08/2016.
 *
 * Represents the world of a dimension on the server
 */
public class World {

	public final ChunkMap<Chunk> chunkMap;
	public final IWorldGenerator generator;

	public World(IWorldGenerator generator) {
		chunkMap = new ChunkMap<>();
		this.generator = generator;
	}

	public void unloadChunk(int x, int z) {
		Chunk res = chunkMap.get(x,z);
		if(res != null) {
			res.releaseData();
			chunkMap.remove(x,z);
		}
	}
	public Chunk requestChunk(int x, int z) {
		Chunk res = chunkMap.get(x,z);
		if(res == null) {
			res = generator.generateChunk(x,z);
			chunkMap.set(x,z,res);
		}//TODO: improve
		return res;
	}


	/**
	 * Unload The Entire World : Release All Chunk Data
	 */
	public void releaseAllChunkData() {
		chunkMap.forEachChunk(e -> {
			e.releaseData();
			return true;
		});
		chunkMap.emptyAll();
	}

	/**
	 * Run Required Updates on the location
	 */
	public void gameLogicTick() {
		//TODO: implement
	}

	/**
	 * Run required simulated updates on the location [minimal]
	 */
	public void clientSimLogicTick() {
		//TODO: implement
	}

	/**
	 * @return the acceleration due to gravity (in m/tick^2)
	 */
	public float getGravity() {
		return 9.81F / 400.0F;
	}


	public void addEntityToWorld(Entity entity) {
		//TODO: implement
	}
}
