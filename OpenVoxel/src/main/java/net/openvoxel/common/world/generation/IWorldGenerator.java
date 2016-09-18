package net.openvoxel.common.world.generation;

import net.openvoxel.common.resources.ResourceHandle;
import net.openvoxel.common.world.Chunk;
import net.openvoxel.common.world.ChunkCoordinate;
import net.openvoxel.common.world.World;

/**
 * Created by James on 25/08/2016.
 *
 * World Type Reference:
 */
public interface IWorldGenerator {

	Chunk generateChunk(ChunkCoordinate coordinate);

	default float getGravity(World world) {
		return 9.81F / 400.0F;
	}
	default boolean skyEnabled(World world) {
		return true;
	}
	default int fogColour(World world) {
		return 0x00000000;
	}
	default int skylightColour(World world) {
		return 0xFFFFFFFF;
	}

	//THIS HAS TO BE CONSTANT
	ResourceHandle getSkymapResource();
}
