package net.openvoxel.client.renderer.gl3.worldrender.cache;

import net.openvoxel.client.control.Renderer;
import net.openvoxel.client.renderer.generic.WorldRenderer;
import net.openvoxel.client.renderer.gl3.atlas.OGL3Icon;
import net.openvoxel.client.textureatlas.Icon;
import net.openvoxel.common.block.Block;
import net.openvoxel.common.block.BlockAir;
import net.openvoxel.common.block.IBlockAccess;
import net.openvoxel.common.util.BlockFace;
import net.openvoxel.world.client.ClientChunk;
import net.openvoxel.world.client.ClientChunkSection;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

/**
 * Created by James on 09/04/2017.
 *
 * OpenGL Render Cache Manager
 */
public class OGL3RenderCacheManager {



	public OGL3RenderCache loadRenderCache(ClientChunkSection section) {
		return (OGL3RenderCache)section.renderCache.get();
	}

	public void handleChunkLoad(ClientChunk chunk) {
		for(int y = 0; y < 16; y++) {
			ClientChunkSection section = chunk.getSectionAt(y);
			OGL3RenderCache cache = new OGL3RenderCache();
			cache.initGL();
			section.renderCache.set(cache);
			Renderer.renderCacheManager.addWork(() -> GenerateData(section,cache));
		}
	}

	public void handleChunkUnload(ClientChunk chunk) {
		for(int y = 0; y < 16; y++) {
			ClientChunkSection section = chunk.getSectionAt(y);
			OGL3RenderCache cache = loadRenderCache(section);
			if(cache != null) {
				cache.removeGL();
			}else{
				//TODO: handle this situation: add to queue to clean after the data has been generated
				System.out.println("WARN : Unload No Cache: Leak?");
			}
		}
	}

	public void handleDirtySection(ClientChunkSection section) {
		//TODO: implement
	}


	private static void GenerateData(ClientChunkSection section,OGL3RenderCache cache) {
		ogl3BlockRenderer blockRenderer = new ogl3BlockRenderer(0,0,0);
		oglDrawStateAccess drawStateAccess = new oglDrawStateAccess();
		drawStateAccess.setSubAccess(new oglDrawStateAccess());
		drawStateAccess.preUpdate(section);
		for(int x = 0; x < 16; x++) {
			for(int y = 0; y < 16; y++) {
				for(int z = 0; z <16; z++) {
					blockRenderer.setOffset(x,y,z);
					Block block = section.blockAt(x,y,z);
					byte meta = section.getPrevMeta();//TODO: add state access
					drawStateAccess.update(block,0,meta,x,y,z);
					block.getRenderHandler().storeBlockData(blockRenderer,drawStateAccess,true);
				}
			}
		}
		//Update//
		cache.dataPos = blockRenderer.pos;
		cache.dataUV = blockRenderer.uv;
		cache.dataNormal = blockRenderer.norm;
		cache.dataTangent = blockRenderer.tangent;
		cache.dataLighting = blockRenderer.lighting;
		cache.dataColourMask = blockRenderer.colMask;
		cache.newValueCount = blockRenderer.count;
		cache.dataPos.position(0);
		cache.dataUV.position(0);
		cache.dataNormal.position(0);
		cache.dataTangent.position(0);
		cache.dataLighting.position(0);
		cache.dataColourMask.position(0);
	}

	/**
	 * TODO: implement properly
	 */
	private static class oglDrawStateAccess implements IBlockAccess {

		private oglDrawStateAccess subAccess;
		private ClientChunkSection section;
		private int x;
		private int y;
		private int z;
		private Block block;
		private int it;
		private byte meta;
		void preUpdate(ClientChunkSection section) {
			this.section = section;
		}
		void setSubAccess(oglDrawStateAccess access) {
			subAccess = access;
			if(subAccess != this) {
				subAccess.setSubAccess(subAccess);
			}
		}

		public void update(Block b,int id,byte meta,int x, int y, int z) {
			block = b;
			it = id;
			this.meta = meta;
			this.x = x;
			this.y = y;
			this.z = z;
		}

		@Override
		public int getBlockID() {
			return it;
		}

		@Override
		public byte getBlockMetaData() {
			return meta;
		}

		@Override
		public Block getBlock() {
			return block;
		}

		@Override
		public boolean blockLoaded() {
			return false;
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

		@Override
		public IBlockAccess getOffsetBlockData(BlockFace face) {
			int targetX = x + face.xOffset;
			int targetY = y + face.yOffset;
			int targetZ = z + face.zOffset;
			subAccess.x = targetX;
			subAccess.y = targetY;
			subAccess.z = targetZ;
			if(targetX < 0 || targetX >= 16 || targetY < 0 || targetY >= 16 || targetZ < 0 || targetZ >= 16) {
				subAccess.block = BlockAir.BLOCK_AIR;
				subAccess.it = 0;
				subAccess.meta = 0;
			}else{//TODO: properly implement
				subAccess.block = section.blockAt(targetX,targetY,targetZ);
				subAccess.it = 1;
				subAccess.meta = 0;
			}
			return subAccess;
		}
	}

	private static class ogl3BlockRenderer implements WorldRenderer.WorldBlockRenderer {
		private OGL3Icon currentIcon = null;
		private float xOffset;
		private float yOffset;
		private float zOffset;

		private ByteBuffer pos;
		private ByteBuffer uv;
		private ByteBuffer norm;
		private ByteBuffer tangent;
		private ByteBuffer colMask;
		private ByteBuffer lighting;
		private int count = 0;
		private int maxCount = 1024;


		private ogl3BlockRenderer(float xOffset, float yOffset, float zOffset) {
			this.xOffset = xOffset;
			this.yOffset = yOffset;
			this.zOffset = zOffset;
			pos = MemoryUtil.memAlloc(maxCount * 12);//3*float
			uv = MemoryUtil.memAlloc(maxCount * 4);//2*unsigned short
			norm = MemoryUtil.memAlloc(maxCount * 3);//3*byte
			tangent = MemoryUtil.memAlloc(maxCount * 3);//3*byte
			colMask = MemoryUtil.memAlloc(maxCount * 4);//4*byte
			lighting = MemoryUtil.memAlloc(maxCount * 4);//4*byte
		}

		private void ensureCapacity() {
			if(count >= maxCount) {
				int newCount = maxCount + 2048;
				pos = MemoryUtil.memRealloc(pos,newCount * 12);
				uv = MemoryUtil.memRealloc(uv, newCount * 4);
				norm = MemoryUtil.memRealloc(norm,newCount * 3);
				tangent = MemoryUtil.memRealloc(tangent,newCount * 3);
				colMask = MemoryUtil.memRealloc(colMask,newCount * 4);
				lighting = MemoryUtil.memRealloc(lighting, newCount * 4);
				maxCount = newCount;
			}
		}

		void setOffset(float x, float y, float z) {
			xOffset = x;
			yOffset = y;
			zOffset = z;
		}

		@Override
		public void addVertex(float X, float Y, float Z, float U, float V, float xNorm, float yNorm, float zNorm, float xTangent, float yTangent, float zTangent) {
			addVertexWithCol(X,Y,Z,U,V,xNorm,yNorm,zNorm,xTangent,yTangent,zTangent,0xFFFFFFFF);
		}

		private static final float USHORT_MAX = 65535.0F;
		private short floatToShort(float f) {
			return (short)(f * USHORT_MAX);
		}

		private static final float UBYTE_MAX = 255.0F;
		private byte floatToByte(float f) {
			return (byte)(f * UBYTE_MAX);
		}

		@Override
		public void addVertexWithCol(float X, float Y, float Z, float U, float V, float xNorm, float yNorm, float zNorm, float xTangent, float yTangent, float zTangent,int Colour) {
			ensureCapacity();
			pos.putFloat(X + xOffset);
			pos.putFloat(Y + yOffset);
			pos.putFloat(Z + zOffset);
			uv.putShort(floatToShort(currentIcon.getU(U)));
			uv.putShort(floatToShort(currentIcon.getV(V)));
			norm.put(floatToByte(xNorm));
			norm.put(floatToByte(yNorm));
			norm.put(floatToByte(zNorm));
			tangent.put(floatToByte(xTangent));
			tangent.put(floatToByte(yTangent));
			tangent.put(floatToByte(zTangent));
			colMask.putInt(Colour);
			lighting.putInt(0xFFFFFFFF);//TODO: implement
			count++;
		}

		@Override
		public void addVertexWithColFlags(float X, float Y, float Z, float U, float V, float xNorm, float yNorm, float zNorm, float xTangent, float yTangent, float zTangent, int Colour, int flags) {
			//NO OP// :: TODO IMPLEMENT
		}

		@Override
		public void setCurrentIcon(Icon icon) {
			currentIcon = (OGL3Icon)icon;
		}
	}

}
