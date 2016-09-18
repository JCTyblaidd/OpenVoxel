package net.openvoxel.common.world;

import net.openvoxel.common.entity.Entity;
import net.openvoxel.common.world.generation.IWorldGenerator;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by James on 25/08/2016.
 *
 * Represents the world of a dimension on the server
 */
public class World {

	public final Map<ChunkCoordinate,Chunk> ChunkMap;
	public final IWorldGenerator generator;

	public World(IWorldGenerator generator) {
		ChunkMap = new HashMap<>();
		this.generator = generator;
	}

	public void unloadChunk(ChunkCoordinate chunkCoordinate) {}
	public void loadChunk(ChunkCoordinate chunkCoordinate) {}

	public void gameLogicTick() {
		//Run Game Logic: for real if server world, or partially simulated if client world;
	}

	/**
	 * @return the acceleration due to gravity (in m/tick^2)
	 */
	public float getGravity() {
		return 9.81F / 400.0F;
	}


	public void addEntityToWorld(Entity entity) {

	}
}
