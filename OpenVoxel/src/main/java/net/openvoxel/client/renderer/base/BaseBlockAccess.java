package net.openvoxel.client.renderer.base;

import net.openvoxel.OpenVoxel;
import net.openvoxel.common.block.Block;
import net.openvoxel.common.block.IBlockAccess;
import net.openvoxel.common.util.BlockFace;
import net.openvoxel.world.client.ClientChunk;
import net.openvoxel.world.client.ClientChunkSection;
import net.openvoxel.world.client.ClientWorld;

public class BaseBlockAccess implements IBlockAccess {

	private ClientWorld theWorld;
	private long chunkX;
	private long chunkY;
	private long chunkZ;
	private DeltaAccess originAccess = new DeltaAccess();
	private DeltaAccess nearbyAccess = new DeltaAccess();

	private ClientChunkSection[] nearbySections = new ClientChunkSection[27];

	public void bindWorld(ClientWorld world) {
		theWorld = world;
	}

	public void bindChunkSection(ClientChunkSection section) {
		chunkX = section.getChunkX() * 16L;
		chunkY = section.getChunkY();
		chunkZ = section.getChunkZ() * 16L;
		originAccess.setOffset(0,0,0);
		//Load nearby sections
		for(int dx = -1; dx <= 1; dx++) {
			for(int dz = -1; dz <= 1; dz++) {
				ClientChunk _chunk = theWorld.requestChunk(chunkX+dx,chunkZ+dz,false);
				for(int dy = -1; dy <= -1; dy++) {
					int _index = (dx+1) * 9 + (dy+1) * 3 + (dz+1);
					int _ycoord = (int)(chunkY+dy);
					boolean _valid = _chunk != null && _ycoord >= 0 && _ycoord < 16;
					nearbySections[_index] = _valid ? _chunk.getSectionAt(_ycoord) : null;
				}
			}
		}
	}

	public void bindSectionOffset(int x, int y, int z) {
		originAccess.setOffset(x,y,z);
	}


	///////////////////
	/// API Methods ///
	///////////////////


	@Override
	public int getBlockID() {
		return originAccess.getBlockID();
	}

	@Override
	public byte getBlockMetaData() {
		return originAccess.getBlockMetaData();
	}

	@Override
	public Block getBlock() {
		return originAccess.getBlock();
	}

	@Override
	public boolean blockLoaded() {
		return originAccess.blockLoaded();
	}

	@Override
	public long getX() {
		return originAccess.getX();
	}

	@Override
	public long getY() {
		return originAccess.getY();
	}

	@Override
	public long getZ() {
		return originAccess.getZ();
	}

	@Override
	public IBlockAccess getOffsetBlockData(BlockFace face) {
		return originAccess.getOffsetBlockData(face);
	}

	private class DeltaAccess implements IBlockAccess {
		private int offsetX;
		private int offsetY;
		private int offsetZ;

		private void setOffset(int x, int y, int z) {
			offsetX = x;
			offsetY = y;
			offsetZ = z;
		}

		private int getBlockValue() {
			int deltaChunkX = ((offsetX + 16) / 16);// - 1;
			int deltaChunkY = ((offsetY + 16) / 16);// - 1;
			int deltaChunkZ = ((offsetZ + 16) / 16);// - 1;
			int chunkIndex = deltaChunkX * 9 + deltaChunkY * 3 + deltaChunkZ;
			ClientChunkSection section = nearbySections[chunkIndex];
			int subOffsetX = offsetX + (deltaChunkX * -16);
			int subOffsetY = offsetY + (deltaChunkY * -16);
			int subOffsetZ = offsetZ + (deltaChunkZ * -16);
			return section == null ? 0 : section.RawDataAt(subOffsetX,subOffsetY,subOffsetZ);
		}

		@Override
		public int getBlockID() {
			return getBlockValue() >> 8;
		}

		@Override
		public byte getBlockMetaData() {
			return (byte)(getBlockValue() & 0xFF);
		}

		@Override
		public Block getBlock() {
			return OpenVoxel.getInstance().blockRegistry.getBlockFromID(getBlockID());
		}

		@Override
		public boolean blockLoaded() {
			return true;//TODO: IMPLEMENT {IS NEEDED?}
		}

		@Override
		public long getX() {
			return chunkX + offsetX;
		}

		@Override
		public long getY() {
			return chunkY + offsetY;
		}

		@Override
		public long getZ() {
			return chunkZ + offsetZ;
		}

		@Override
		public IBlockAccess getOffsetBlockData(BlockFace face) {
			nearbyAccess.setOffset(
					offsetX + face.xOffset,
					offsetY+face.yOffset,
					offsetZ + face.zOffset
			);
			return nearbyAccess;
		}
	}

}
