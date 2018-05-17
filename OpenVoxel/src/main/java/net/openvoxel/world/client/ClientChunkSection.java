package net.openvoxel.world.client;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import net.openvoxel.OpenVoxel;
import net.openvoxel.api.side.Side;
import net.openvoxel.api.side.SideOnly;
import net.openvoxel.client.utility.IRenderDataCache;
import net.openvoxel.common.block.Block;
import net.openvoxel.common.registry.RegistryBlocks;
import net.openvoxel.common.util.BlockFace;
import net.openvoxel.world.chunk.Chunk;
import net.openvoxel.world.chunk.ChunkSection;

import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by James on 09/04/2017.
 *
 * Client Side Chunk Section Implementation
 */
@SideOnly(side = Side.CLIENT)
public class ClientChunkSection extends ChunkSection {

	///
	/// Renderer Information
	///
	private boolean isVisibilityDirty = true;
	private boolean isDrawDirty = true;
	public int Renderer_Info_Opaque = 0;
	public int Renderer_Size_Opaque = -1;
	public int Renderer_Info_Transparent = 0;
	public int Renderer_Size_Transparent = -1;

	///
	/// Culling Information
	///
	private long visibilityMetadata = 0;

	public ClientChunkSection(ClientChunk refChunk,int idx) {
		super(refChunk,idx);
	}

	public ClientChunk getChunk() {
		return (ClientChunk)refChunk;
	}

	public int RawDataAt(int x, int y, int z) {
		int reqIndex = x * 256 + y * 16 + z;
		return blockInformation.get(reqIndex);
	}

	public Block blockAt(int x, int y, int z) {
		int reqIndex = x * 256 + y * 16 + z;
		int val = blockInformation.get(reqIndex);
		return OpenVoxel.getInstance().blockRegistry.getBlockFromID(val >> 8);
	}

	public final IntBuffer getBlocks() {
		return blockInformation;
	}

	public final ShortBuffer getLights() {
		return this.blockLightInfo;
	}

	public boolean isDrawDirty() {
		return isDrawDirty;
	}

	public void markDrawUpdated() {
		isDrawDirty = false;
	}


	///
	/// Client Visibility Culling
	///

	public boolean visibilityNeedsRegen() {
		return isVisibilityDirty;
	}

	public void generateVisibilityMap() {
		//Reset
		visibilityMetadata = 0L;

		//isChunkEmpty shortcut
		if(isChunkEmpty()) {
			visibilityMetadata = 0xFFFFFFFFFFFFFFFFL;
			isVisibilityDirty = false;
			return;
		}

		TIntSet connectSet = new TIntHashSet();
		final int block_count = 16 * 16 * 16;
		int[] floodQueue = new int[block_count];
		boolean[] hasVisited = new boolean[block_count];
		for(int i = 0; i < block_count; i++) {
			if(!hasVisited[i]) {
				connectSet.clear();
				floodFill(hasVisited,connectSet,i,floodQueue);
				TIntIterator iterator_a = connectSet.iterator();
				while(iterator_a.hasNext()) {
					int A = iterator_a.next();
					TIntIterator iterator_b = connectSet.iterator();
					while(iterator_b.hasNext()) {
						int B = iterator_b.next();
						setVisible(A,B);
					}
				}
			}
		}

		//Complete
		isVisibilityDirty = false;
	}

	//TODO: ADD LIGHTING CALCULATION AS WELL???
	private void floodFill(boolean[] visited,TIntSet intSet,int start,int[] floodQueue) {
		int startPos = 0;
		int endPos = 1;
		floodQueue[startPos] = start;

		//Constant References
		RegistryBlocks registry = OpenVoxel.getInstance().blockRegistry;

		//While values exist
		while(startPos < endPos) {
			//Pop From Queue
			int position = floodQueue[startPos];
			startPos += 1;

			//Mark as Visited
			visited[position] = true;
			int pos_z = position % 16;
			int pos_y = (position / 16) % 16;
			int pos_x = (position / 256);

			//Get Block Info
			int blockInfo = blockInformation.get(position);
			Block block = registry.getBlockFromID(blockInfo >> 8);
			//byte meta = (byte)(blockInfo & 0xFF);

			//TODO: USE METADATA!!!
			for(BlockFace face : BlockFace.values()) {
				if(block.isOpaque(face)) {
					int new_z = face.zOffset + pos_z;
					int new_y = face.yOffset + pos_y;
					int new_x = face.xOffset + pos_x;

					//Out of bounds check
					int max_val = Math.max(new_x,Math.max(new_y,new_z));
					int min_val = Math.min(new_x,Math.min(new_y,new_z));
					if(min_val < 0 || max_val >= 16) {
						intSet.add(face.faceID);
					}else{
						int new_position = new_x * 256 + new_y * 16 + new_z;
						floodQueue[endPos] = new_position;
						endPos += 1;
					}
				}
			}
		}
	}


	public boolean isVisible(int face1, int face2) {
		int mask = 1 << (face1 * 6 + face2);
		return (visibilityMetadata & mask) != 0;
	}

	private void setVisible(int face1, int face2) {
		int mask_a = 1 << (face1 * 6 + face2);
		int mask_b = 1 << (face2 * 6 + face1);
		visibilityMetadata |= (mask_a | mask_b);
	}
}
