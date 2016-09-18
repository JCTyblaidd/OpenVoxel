package net.openvoxel.client.task;

import net.openvoxel.common.block.Block;
import net.openvoxel.common.block.IBlockAccess;
import net.openvoxel.common.util.BlockFace;
import net.openvoxel.common.world.Chunk;
import net.openvoxel.common.world.ChunkCoordinate;
import net.openvoxel.common.world.World;

/**
 * Created by James on 02/09/2016.
 */
public abstract class BaseChunkRenderTask implements Runnable{

	public World world;
	public Chunk mainChunk;
	public Chunk chunk_ZPlus;
	public Chunk chunk_ZMinus;
	public Chunk chunk_XPlus;
	public Chunk chunk_XMinus;
	public Chunk chunk_XPlus_ZPlus;
	public Chunk chunk_XMinus_ZPlus;
	public Chunk chunk_XPlus_ZMinus;
	public Chunk chunk_XMinus_ZMinus;
	public ISubChunkDataGenerator generator;

	public BaseChunkRenderTask(World world, ChunkCoordinate coordinate, ISubChunkDataGenerator generator) {
		this.world = world;
		this.generator = generator;
		//TODO: loadChunks
	}

	public RenderBlockAccess createAccess(int X, int Y, int Z) {
		return new RenderBlockAccess(this,X,Y,Z);
	}

	public static class RenderBlockAccess implements IBlockAccess {
		private BaseChunkRenderTask task;
		private int xpos,ypos,zpos;
		private boolean hasCache = false;
		private int Cache;
		private boolean loadFailed;
		public RenderBlockAccess(BaseChunkRenderTask task) {
			this(task,0,0,0);
		}
		public RenderBlockAccess(BaseChunkRenderTask task, int X, int Y, int Z) {
			this.xpos = X;
			this.ypos = Y;
			this.zpos = Z;
			this.task = task;
		}
		public void setPosition(int X, int Y, int Z) {
			xpos=X;ypos=Y;zpos=Z;
		}
		private void loadCache() {
			loadFailed = false;
			int vX = xpos / 16;
			int vZ = zpos / 16;
			try {
				if (vX == 0) {
					if (vZ == 0) {//Main Chunk//
						Cache = task.mainChunk.getBlockAt(xpos, ypos, zpos);
					} else if (vZ == 1) {
						Cache = task.chunk_ZPlus.getOptimisticLockedBlockData(xpos, ypos, zpos - 16);
					} else if (vZ == -1) {
						Cache = task.chunk_ZMinus.getOptimisticLockedBlockData(xpos, ypos, zpos + 16);
					} else {
						loadFailed = true;
					}
				} else if (vX == 1) {
					if (vZ == 0) {
						Cache = task.chunk_XPlus.getOptimisticLockedBlockData(xpos - 16, ypos, zpos);
					} else if (vZ == 1) {
						Cache = task.chunk_XPlus_ZPlus.getOptimisticLockedBlockData(xpos - 16, ypos, zpos - 16);
					} else if (vZ == -1) {
						Cache = task.chunk_XPlus_ZMinus.getOptimisticLockedBlockData(xpos - 16, ypos, zpos + 16);
					} else {
						loadFailed = true;
					}
				} else if (vX == -1) {
					if (vZ == 0) {
						Cache = task.chunk_XMinus.getOptimisticLockedBlockData(xpos + 16, ypos, zpos);
					} else if (vZ == 1) {
						Cache = task.chunk_XMinus_ZPlus.getOptimisticLockedBlockData(xpos + 16, ypos, zpos - 16);
					} else if (vZ == -1) {
						Cache = task.chunk_XMinus_ZMinus.getOptimisticLockedBlockData(xpos + 16, ypos, zpos + 16);
					} else {
						loadFailed = true;
					}
				} else {
					loadFailed = true;
				}
			}catch(Exception e) {
				loadFailed = true;
			}
			hasCache = false;
		}

		@Override
		public int getBlockID() {
			if(!hasCache) {
				loadCache();
			}
			return Cache >> 8;
		}

		@Override
		public byte getBlockMetaData() {
			if(!hasCache) {
				loadCache();
			}
			return (byte)(Cache & 0xFF);
		}

		@Override
		public Block getBlock() {
			return null;//TODO: implement
		}

		@Override
		public boolean blockLoaded() {
			return !loadFailed;
		}

		@Override
		public int getX() {
			return xpos;
		}

		@Override
		public int getY() {
			return ypos;
		}

		@Override
		public int getZ() {
			return zpos;
		}

		@Override
		public IBlockAccess getOffsetBlockData(BlockFace face) {
			return new RenderBlockAccess(task,xpos+face.xOffset,ypos+face.yOffset,zpos+face.zOffset);
		}
	}

}
