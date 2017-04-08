package net.openvoxel.common.world;

import java.util.concurrent.locks.StampedLock;

/**
 * Created by James on 25/08/2016.
 *
 * Wrapper around stored chunk information
 */
public class Chunk {

	public int[] BlockData;
	public boolean[] UpdateData;//TODO: split update info into sub_chunks
	public int UpdateCount;

	public ChunkCoordinate coordinate;

	//Used Client-Side For Synchronisation Between the Render and Game Thread
	public StampedLock stampedLock;

	public Chunk(ChunkCoordinate coordinate) {
		BlockData = new int[16 * 16 * 256];
		UpdateData = new boolean[16 * 16 * 256];
		UpdateCount = 0;
		this.coordinate = coordinate;
		stampedLock = new StampedLock();
	}

	private static int getArrayOffsetFromRelative(int x, int y, int z) {
		return (x * 256 * 16) + (z * 256) + y;
	}

	public int getBlockAt(int x, int y, int z) {
		return BlockData[getArrayOffsetFromRelative(x,y,z)];
	}

	public int getOptimisticLockedBlockData(int x, int y, int z) {
		int arrayOffset = getArrayOffsetFromRelative(x,y,z);
		long stamp = stampedLock.tryOptimisticRead();
		int V = BlockData[arrayOffset];
		if(!stampedLock.validate(stamp)) {
			stamp = stampedLock.readLock();
			V = BlockData[arrayOffset];
			stampedLock.unlockRead(stamp);
		}
		return V;
	}

	public void setBlockAt(int x, int y, int z, int BlockID, byte MetaData) {
		int offset = getArrayOffsetFromRelative(x,y,z);
		BlockData[offset] = (BlockID << 8) | MetaData;
		if(!UpdateData[offset]) {
			UpdateData[offset] = true;
			UpdateCount++;
		}
	}
	public void setBlockAtNoUpdate(int x, int y, int z, int BlockID, byte MetaData) {
		BlockData[getArrayOffsetFromRelative(x,y,z)] = (BlockID << 8) | MetaData;
	}
}
