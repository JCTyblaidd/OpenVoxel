package net.openvoxel.client.renderer.vk.world;

import net.openvoxel.OpenVoxel;
import net.openvoxel.api.logger.Logger;
import net.openvoxel.client.renderer.generic.WorldRenderer;
import net.openvoxel.client.renderer.vk.VkTexAtlas;
import net.openvoxel.client.textureatlas.Icon;
import net.openvoxel.common.block.Block;
import net.openvoxel.common.block.IBlockAccess;
import net.openvoxel.common.registry.RegistryBlocks;
import net.openvoxel.common.util.BlockFace;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

class VkChunkRenderer implements WorldRenderer.WorldBlockRenderer, IBlockAccess {

	private IntBuffer currentBlockData;
	private ShortBuffer currentLightData;
	private RegistryBlocks blockRegistry;
	private int x_pos;
	private int y_pos;
	private int z_pos;
	private SubBlockAccess subBlockAccess = new SubBlockAccess();

	private class SubBlockAccess implements IBlockAccess{
		private int x;
		private int y;
		private int z;
		@Override
		public int getBlockID() {
			if(!blockLoaded()) return 0;
			int offset = x * 256 + y * 16 + z;
			return currentBlockData.get(offset) >> 8;
		}

		@Override
		public byte getBlockMetaData() {
			if(!blockLoaded()) return 0;
			int offset = x * 256 + y * 16 + z;
			return (byte)(currentBlockData.get(offset) & 0xFF);
		}

		@Override
		public Block getBlock() {
			return blockRegistry.getBlockFromID(this.getBlockID());
		}

		@Override
		public boolean blockLoaded() {
			return x >= 0 && x < 16 && y >= 0 && y < 16 && z >= 0 && z < 16;
		}

		@Override
		public int getX() {
			return x;
		}

		@Override
		public int getY() {
			return y;
		}

		@Override
		public int getZ() {
			return z;
		}

		/**
		 * Invalid Operation//
		 */
		@Override
		public IBlockAccess getOffsetBlockData(BlockFace face) {
			Logger.getLogger("VK Block Gen").Warning("Block Access Attempted to access an adjacent of an adjacent block");
			return null;
		}
	}

	/**
	 * Generate Draw Information,
	 * Returns null if 0 draws
	 * Returns buffer with position set to vertex count otherwise
	 * Remember to reset after receiving
	 * The data is split section by section
	 *
	 * TODO: add method of getting info from neighbouring sections
	 */
	ByteBuffer GenerateDrawInfo(IntBuffer blockInfo, ShortBuffer lightInfo, boolean genOpaque) {
		create_buffers();
		blockRegistry = OpenVoxel.getInstance().blockRegistry;
		currentBlockData = blockInfo;
		currentLightData = lightInfo;
		for(int x = 0; x < 16; x++) {
			for(int y = 0; y < 16; y++) {
				for(int z = 0; z < 16; z++) {
					int offset = x * 256 + y * 16 + z;
					int blockData = blockInfo.get(offset);
					Block oldBlock = blockRegistry.getBlockFromID(blockData >> 8);
					x_pos = x;
					y_pos = y;
					z_pos = z;
					oldBlock.getRenderHandler().storeBlockData(this,this,genOpaque);
				}
			}
		}
		return merge_buffers();
	}

	@Override
	public int getBlockID() {
		int offset = x_pos * 256 + y_pos * 16 + z_pos;
		return currentBlockData.get(offset) >> 8;
	}

	@Override
	public byte getBlockMetaData() {
		int offset = x_pos * 256 + y_pos * 16 + z_pos;
		return (byte)(currentBlockData.get(offset) & 0xFF);
	}

	@Override
	public Block getBlock() {
		return blockRegistry.getBlockFromID(getBlockID());
	}

	@Override
	public boolean blockLoaded() {
		return x_pos >= 0 && x_pos < 16 && y_pos >= 0 && y_pos < 16 && z_pos >= 0 && z_pos < 16;
	}

	@Override
	public int getX() {
		return x_pos;
	}

	@Override
	public int getY() {
		return y_pos;
	}

	@Override
	public int getZ() {
		return z_pos;
	}

	@Override
	public IBlockAccess getOffsetBlockData(BlockFace face) {
		subBlockAccess.x = x_pos + face.xOffset;
		subBlockAccess.y = y_pos + face.yOffset;
		subBlockAccess.z = z_pos + face.zOffset;
		return subBlockAccess;
	}

	//////////////////////////////
	/// Result Generation Code ///
	//////////////////////////////

	/**
	 * Default to useless entire map ICON
	 */
	private static VkTexAtlas.VkTexIcon nullIcon = new VkTexAtlas.VkTexIcon();
	private VkTexAtlas.VkTexIcon currentIcon;
	private ByteBuffer bufferPos, bufferUV, bufferNorm, bufferTangent, bufferCol, bufferLight, bufferFlags;
	private int vertexCount;
	private int prevCount;

	private void create_buffers() {
		prevCount = 1024;
		vertexCount = 0;
		currentIcon = nullIcon;
		bufferPos = MemoryUtil.memAlloc(prevCount*12);
		bufferUV = MemoryUtil.memAlloc(prevCount*4);
		bufferNorm = MemoryUtil.memAlloc(prevCount*3);
		bufferTangent = MemoryUtil.memAlloc(prevCount*3);
		bufferCol = MemoryUtil.memAlloc(prevCount*4);
		bufferLight = MemoryUtil.memAlloc(prevCount*4);
		bufferFlags = MemoryUtil.memAlloc(prevCount);
	}

	private ByteBuffer merge_buffers() {
		ByteBuffer toRet = null;
		if(vertexCount != 0) {
			toRet = MemoryUtil.memAlloc(31*vertexCount);
			bufferPos.limit(12*vertexCount);
			bufferUV.limit(4*vertexCount);
			bufferNorm.limit(3*vertexCount);
			bufferTangent.limit(3*vertexCount);
			bufferCol.limit(4*vertexCount);
			bufferLight.limit(4*vertexCount);
			bufferFlags.limit(vertexCount);
			toRet.put(bufferPos);
			toRet.put(bufferUV);
			toRet.put(bufferNorm);
			toRet.put(bufferTangent);
			toRet.put(bufferCol);
			toRet.put(bufferLight);
			toRet.put(bufferFlags);
		}
		MemoryUtil.memFree(bufferPos);
		MemoryUtil.memFree(bufferUV);
		MemoryUtil.memFree(bufferNorm);
		MemoryUtil.memFree(bufferTangent);
		MemoryUtil.memFree(bufferCol);
		MemoryUtil.memFree(bufferLight);
		MemoryUtil.memFree(bufferFlags);
		return toRet;
	}

	private void ensure_capacity() {
		if(vertexCount >= prevCount) {
			prevCount += 2048;
			bufferPos = MemoryUtil.memRealloc(bufferPos,prevCount*12);
			bufferUV = MemoryUtil.memRealloc(bufferUV,prevCount*4);
			bufferNorm = MemoryUtil.memRealloc(bufferNorm,prevCount*3);
			bufferTangent = MemoryUtil.memRealloc(bufferTangent,prevCount*3);
			bufferCol = MemoryUtil.memRealloc(bufferCol,prevCount*4);
			bufferLight = MemoryUtil.memRealloc(bufferLight,prevCount*4);
			bufferFlags = MemoryUtil.memRealloc(bufferFlags,prevCount);
		}
	}

	private static final float USHORT_MAX = 65535.0F;
	private short floatToShort(float f) {
		return (short)(f * USHORT_MAX);
	}

	private static final float SBYTE_MAX = 127.0F;
	private byte floatToByte(float f) {
		return (byte)(f * SBYTE_MAX);
	}

	@Override
	public void addVertex(float X, float Y, float Z, float U, float V, float xNorm, float yNorm, float zNorm, float xTangent, float yTangent, float zTangent) {
		addVertexWithColFlags(X,Y,Z,U,V,xNorm,yNorm,zNorm,xTangent,yTangent,zTangent,0xFFFFFFFF,0);
	}

	@Override
	public void addVertexWithCol(float X, float Y, float Z, float U, float V, float xNorm, float yNorm, float zNorm, float xTangent, float yTangent, float zTangent, int Colour) {
		addVertexWithColFlags(X,Y,Z,U,V,xNorm,yNorm,zNorm,xTangent,yTangent,zTangent,Colour,0);
	}

	@Override
	public void addVertexWithColFlags(float X, float Y, float Z, float U_ico, float V_ico, float xNorm, float yNorm, float zNorm, float xTangent, float yTangent, float zTangent, int Colour,int flags) {
		vertexCount += 1;
		ensure_capacity();
		final float U = currentIcon.u_min + (currentIcon.u_max - currentIcon.u_min) * U_ico;
		final float V = currentIcon.v_min + (currentIcon.v_max - currentIcon.v_min) * V_ico;
		bufferPos.putFloat(vertexCount*12,X);
		bufferPos.putFloat(vertexCount*12+4,Y);
		bufferPos.putFloat(vertexCount*12+8,Z);
		bufferUV.putShort(vertexCount*4,floatToShort(U));
		bufferUV.putShort(vertexCount*4+2,floatToShort(V));
		bufferNorm.put(vertexCount*3,floatToByte(xNorm));
		bufferNorm.put(vertexCount*3+1,floatToByte(yNorm));
		bufferNorm.put(vertexCount*3+2,floatToByte(zNorm));
		bufferTangent.put(vertexCount*3,floatToByte(xTangent));
		bufferTangent.put(vertexCount*3+1,floatToByte(yTangent));
		bufferTangent.put(vertexCount*3+2,floatToByte(zTangent));
		bufferCol.putInt(vertexCount*4,Colour);
		bufferLight.putInt(vertexCount*4,0xFFFFFFFF);//TODO: IMPLEMENT//
		bufferFlags.put(vertexCount,(byte)flags);
	}

	@Override
	public void setCurrentIcon(Icon icon) {
		currentIcon = (VkTexAtlas.VkTexIcon)icon;
	}
}
