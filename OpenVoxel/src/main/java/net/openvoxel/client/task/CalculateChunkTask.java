package net.openvoxel.client.task;

import net.openvoxel.common.world.World;

/**
 * Created by James on 02/09/2016.
 *
 * Generate Chunk Information Task
 */
public class CalculateChunkTask extends BaseChunkRenderTask{
	@Override
	public void run() {

	}

/**
	public CalculateChunkTask(World world, C= coordinate, ISubChunkDataGenerator generator) {
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
	**/
}
