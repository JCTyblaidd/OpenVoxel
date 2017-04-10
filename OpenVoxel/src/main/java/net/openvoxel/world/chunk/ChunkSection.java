package net.openvoxel.world.chunk;

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
	protected IntBuffer blockInformation = IntBuffer.allocate(16*16*16);
	/**
	 * Short based 4xNibble [red,green,blue,sunlight]
	 */
	protected ShortBuffer blockLightInfo = ShortBuffer.allocate(16*16*16);
	/**
	 * 6x side mapping information
	 * each side [z+,z-,x+,x-,y+,y-] used for culling
	 */
	protected ByteBuffer chunkSideInfo = ByteBuffer.allocate(6);

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


}
