package net.openvoxel.client.task;

import net.openvoxel.common.world.ChunkCoordinate;
import net.openvoxel.common.world.World;

/**
 * Created by James on 02/09/2016.
 */
public class CalculateChunkTask extends BaseChunkRenderTask{


	public CalculateChunkTask(World world, ChunkCoordinate coordinate, ISubChunkDataGenerator generator) {
		super(world, coordinate, generator);
	}

	@Override
	public void run() {
		long lock = mainChunk.stampedLock.readLock();
		for(int yV = 0; yV < 16; yV++) {
			generator.generate(this,yV);
		}
		mainChunk.stampedLock.unlockRead(lock);
	}
}
