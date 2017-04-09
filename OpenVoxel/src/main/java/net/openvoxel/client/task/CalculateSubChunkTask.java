package net.openvoxel.client.task;

/**
 * Created by James on 02/09/2016.
 */
public class CalculateSubChunkTask extends BaseChunkRenderTask {

	private int chunkID;

	@Override
	public void run() {

	}
	/**
	public CalculateSubChunkTask(World world, ChunkCoordinate coordinate, int subChunkID, ISubChunkDataGenerator generator) {
		super(world,coordinate,generator);
		chunkID = subChunkID;
	}

	@Override
	public void run() {
		long lock = mainChunk.stampedLock.readLock();
		generator.generate(this,chunkID);
		mainChunk.stampedLock.unlockRead(lock);
	}
	**/
}
