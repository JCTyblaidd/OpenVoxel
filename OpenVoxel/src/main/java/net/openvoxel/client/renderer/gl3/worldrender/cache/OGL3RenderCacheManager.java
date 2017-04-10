package net.openvoxel.client.renderer.gl3.worldrender.cache;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.openvoxel.client.renderer.generic.WorldRenderer;
import net.openvoxel.client.renderer.gl3.atlas.OGL3Icon;
import net.openvoxel.client.textureatlas.Icon;
import net.openvoxel.common.block.Block;
import net.openvoxel.common.block.BlockAir;
import net.openvoxel.common.block.IBlockAccess;
import net.openvoxel.common.util.BlockFace;
import net.openvoxel.vanilla.VanillaBlocks;
import net.openvoxel.vanilla.block.BlockBricks;
import net.openvoxel.world.client.ClientChunkSection;
import org.lwjgl.BufferUtils;

import java.nio.Buffer;
import java.nio.ByteBuffer;

/**
 * Created by James on 09/04/2017.
 *
 * OpenGL Render Cache Manager
 */
public class OGL3RenderCacheManager {



	public OGL3RenderCache loadRenderCache(ClientChunkSection section) {
		return (OGL3RenderCache)section.renderCache;
	}

	public void requestRenderCacheGeneration(ClientChunkSection section) {
		OGL3RenderCache cache = new OGL3RenderCache();
		cache.initGL();
		//Generate//
		/* todo: make async
		Renderer.renderCacheManager.addWork(() -> {

		});
		 */
		GenerateData(section, cache);
		section.renderCache = cache;
	}

	private static void GenerateData(ClientChunkSection section,OGL3RenderCache cache) {
		ogl3BlockRenderer blockRenderer = new ogl3BlockRenderer(0,0,0);
		oglDrawStateAccess drawStateAccess = new oglDrawStateAccess();
		for(int x = 0; x < 16; x++) {
			for(int y = 0; y < 16; y++) {
				for(int z = 0; z <16; z++) {
					blockRenderer.setOffset(x,y,z);
					Block block = section.blockAt(x,y,z);
					byte meta = section.getPrevMeta();//TODO: add state access
					drawStateAccess.update(block,0,meta);
					block.getRenderHandler().storeBlockData(blockRenderer,drawStateAccess);
				}
			}
		}
		//Update//
		cache.dataPos = blockRenderer.pos;
		cache.dataUV = blockRenderer.uv;
		cache.dataNormal = blockRenderer.norm;
		cache.dataLighting = blockRenderer.lighting;
		cache.dataColourMask = blockRenderer.colMask;
		cache.newValueCount = blockRenderer.count;
		cache.dataPos.position(0);
		cache.dataUV.position(0);
		cache.dataNormal.position(0);
		cache.dataLighting.position(0);
		cache.dataColourMask.position(0);
		//Cleanup//
	}

	/**
	 * TODO: implement properly
	 */
	private static class oglDrawStateAccess implements IBlockAccess {

		private Block block;
		private int it;
		private byte meta;

		public void update(Block b,int id,byte meta) {
			block = b;
			it = id;
			this.meta = meta;
		}

		@Override
		public int getBlockID() {
			return 0;
		}

		@Override
		public byte getBlockMetaData() {
			return 0;
		}

		@Override
		public Block getBlock() {
			return VanillaBlocks.BLOCK_BRICKS;
		}

		@Override
		public boolean blockLoaded() {
			return false;
		}

		@Override
		public int getX() {
			return 0;
		}

		@Override
		public int getY() {
			return 0;
		}

		@Override
		public int getZ() {
			return 0;
		}

		@Override
		public IBlockAccess getOffsetBlockData(BlockFace face) {
			return this;//TODO actually implement
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
		private ByteBuffer colMask;
		private ByteBuffer lighting;
		private int count = 0;


		private ogl3BlockRenderer(float xOffset, float yOffset, float zOffset) {
			this.xOffset = xOffset;
			this.yOffset = yOffset;
			this.zOffset = zOffset;
			pos = BufferUtils.createByteBuffer(49159*4*3);
			uv = BufferUtils.createByteBuffer(49159*4*2);
			norm = BufferUtils.createByteBuffer(49159*4*3);
			colMask = BufferUtils.createByteBuffer(49159*4);
			lighting = BufferUtils.createByteBuffer(49159*4);
		}

		public void setOffset(float x, float y, float z) {
			xOffset = x;
			yOffset = y;
			zOffset = z;
		}

		@Override
		public void addVertex(float X, float Y, float Z, float U, float V, float xNorm, float yNorm, float zNorm) {
			addVertexWithCol(X,Y,Z,U,V,xNorm,yNorm,zNorm,0xFFFFFFFF);
		}

		@Override
		public void addVertexWithCol(float X, float Y, float Z, float U, float V, float xNorm, float yNorm, float zNorm, int Color) {
			pos.putFloat(X + xOffset);
			pos.putFloat(Y + yOffset);
			pos.putFloat(Z + zOffset);
			uv.putFloat(currentIcon.getU(U));
			uv.putFloat(currentIcon.getV(V));
			norm.putFloat(xNorm);
			norm.putFloat(yNorm);
			norm.putFloat(zNorm);
			colMask.putInt(Color);
			lighting.putInt(0xFFFFFFFF);//TODO: implement
			count++;
		}

		@Override
		public void setCurrentIcon(Icon icon) {
			currentIcon = (OGL3Icon)icon;
		}
	}

}
