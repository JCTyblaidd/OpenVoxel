package net.openvoxel.statistics;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by James on 15/04/2017.
 *
 * Memory Allocation Statistics
 */
public class MemoryStatistics {

	private static final AtomicLong chunkMemory = new AtomicLong(0L);

	public static void trackChunk(long diff) {
		chunkMemory.getAndAdd(diff);
	}

	public static long getChunkMemory() {
		return chunkMemory.get();
	}

}
