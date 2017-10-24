package net.openvoxel.world.chunk;

import net.openvoxel.statistics.MemoryStatistics;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/**
 * Created by James on 08/04/2017.
 *
 * 16x16x16 section of chunk information
 */
public class ChunkSection {
	/**
	 * Block Data
	 */
	protected IntBuffer blockInformation = MemoryUtil.memCallocInt(16*16*16);
	/**
	 * Short based 4xNibble [red,green,blue,sunlight]
	 */
	protected ShortBuffer blockLightInfo = MemoryUtil.memCallocShort(16*16*16);
	/**
	 * 6x side mapping information
	 * each side [z+,z-,x+,x-,y+,y-] used for culling
	 */
	protected ByteBuffer chunkSideInfo = MemoryUtil.memCalloc(6);

	private static final long DIFF = (4*16*16*16) + (2*16*16*16)+6;

	public final int yIndex;

	public ChunkSection(int idx) {
		MemoryStatistics.trackChunk(DIFF);
		yIndex = idx;
	}

	/**
	 * 6x side dirty information
	 * each side [z+,z-,x+,x-,y+,y-,any] used to determine if other
	 * sections need updating due to possible next block changes
	 * and rendering
	 */
	protected byte chunkDirtyInfo = (byte)0xFF;

	/**
	 * Flag : Recalculate Light Information
	 */
	protected boolean lightCalculationDirty = true;

	/**
	 * Random Update Requirements
	 */
	protected int randomUpdateRefCount = 0;

	/**
	 * Does Block Contain Any Information At All
	 */
	protected int nonAirBlockRefCount = 0;

	/**
	 * Cleanup Allocated Data
	 */
	protected void freeMemory() {
		MemoryUtil.memFree(blockInformation);
		MemoryUtil.memFree(blockLightInfo);
		MemoryUtil.memFree(chunkSideInfo);
		MemoryStatistics.trackChunk(-DIFF);
	}
}
