package net.openvoxel.world.chunk;

import net.openvoxel.OpenVoxel;
import net.openvoxel.common.block.Block;
import net.openvoxel.common.entity.Entity;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by James on 08/04/2017.
 *
 * 16x16x256 Section of Block Data
 */
public class Chunk {

	/**
	 * Chunk Information
	 */
	protected ChunkSection[] chunkSections = new ChunkSection[16];

	/**
	 * Chunk Height Map [unsigned byte]
	 */
	private ByteBuffer heightMap = MemoryUtil.memAlloc(16*16);

	/**
	 * Biome Information Map
	 */
	private ShortBuffer biomeMap = MemoryUtil.memAllocShort(16*16);

	/**
	 * Chunk needs update flag
	 */
	private boolean needsUpdate = true;

	private List<Entity> entities = new ArrayList<>();

	public final int chunkX;

	public final int chunkZ;

	public Chunk(int x, int z) {
		this.chunkX = x;
		this.chunkZ = z;
		for(int i = 0; i < 16; i++) {
			chunkSections[i] = new ChunkSection(i);
		}
	}

	protected Chunk(int x, int z,boolean noGenFlag) {
		this.chunkX = x;
		this.chunkZ = z;
	}

	/**
	 * Cleanup All Allocated Memory
	 *
	 * Must be called before references are lost
	 */
	public void releaseData() {
		MemoryUtil.memFree(heightMap);
		MemoryUtil.memFree(biomeMap);
		for(ChunkSection section : chunkSections) {
			section.freeMemory();
		}
		//TODO: how are we going to handle entities
		//TODO: add onFinalize Validation as an option
		entities.clear();
	}


	public int getHeightAt(int x, int z) {
		return Byte.toUnsignedInt(heightMap.get(x * 16 + z));
	}

	public short getBiomeAt(int x, int z) {
		return biomeMap.get(x * 16 + z);
	}

	public void setBiomeAt(int x, int z, short biomeID) {
		biomeMap.put(x * 16 + z,biomeID);
	}

	public int getBlockAt(int x, int y, int z) {
		ChunkSection chunkSection = chunkSections[y / 16];
		int ySub = y % 16;
		return chunkSection.blockInformation.get(x * 256 + ySub * 16 + z);
	}

	public void setBlock(int x, int y, int z, Block block, byte meta) {
		ChunkSection chunkSection = chunkSections[y / 16];
		int ySub = y % 16;
		int reqIndex = x * 256 + ySub * 16 + z;
		int oldID =  chunkSection.blockInformation.get(reqIndex);
		int newID = OpenVoxel.getInstance().blockRegistry.getIDFromBlock(block) << 8 | meta;
		Block oldBlock = OpenVoxel.getInstance().blockRegistry.getBlockFromID(oldID >> 8);
		chunkSection.blockInformation.put(reqIndex,newID);
		//Mark Update Flags//
		chunkSection.lightCalculationDirty |= !(oldBlock.isCompleteOpaque() & block.isCompleteOpaque());
		chunkSection.chunkDirtyInfo |= 0b00001;
		chunkSection.chunkDirtyInfo |=    z == 15 ? 0b1000000 : 0;
		chunkSection.chunkDirtyInfo |=    z == 0  ? 0b0100000 : 0;
		chunkSection.chunkDirtyInfo |=    x == 15 ? 0b0010000 : 0;
		chunkSection.chunkDirtyInfo |=    x == 0  ? 0b0001000 : 0;
		chunkSection.chunkDirtyInfo |= ySub == 15 ? 0b0000100 : 0;
		chunkSection.chunkDirtyInfo |= ySub == 0  ? 0b0000010 : 0;
		needsUpdate = true;
		//Update Height Map//
		int val = getHeightAt(x,z);
		if(val < y && newID != 0) {
			heightMap.put(x * 16 + z,(byte)val);
		}else if(val == y && newID == 0) {
			//search down//
			int yi;
			for(yi = y; yi >= 0; yi --) {
				if(getBlockAt(x,yi,z) != 0) {
					break;
				}
			}
			heightMap.put(x * 16 + z,(byte)yi);
		}
		//Update Ref Count//
		chunkSection.nonAirBlockRefCount -= oldID == 0 ? 0 : 1;
		chunkSection.nonAirBlockRefCount += newID == 0 ? 0 : 1;
		chunkSection.randomUpdateRefCount -= oldBlock.hasRandomBlockUpdates() ? 1 : 0;
		chunkSection.randomUpdateRefCount += block.hasRandomBlockUpdates() ? 1 : 0;
	}

	public short getBlockLightInfo(int x, int y, int z) {
		ChunkSection chunkSection = chunkSections[y / 16];
		int ySub = y % 16;
		return chunkSection.blockLightInfo.get(x * 256 + ySub * 16 + z);
	}

	public boolean requiresUpdate() {
		return needsUpdate;
	}

	public void markUpdated() {
		needsUpdate = false;
	}

	public List<Entity> getEntityList() {
		return entities;
	}
}
