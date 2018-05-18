package net.openvoxel.client.renderer.base;

import gnu.trove.impl.sync.TSynchronizedIntObjectMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import net.openvoxel.client.ClientInput;
import net.openvoxel.client.renderer.common.IBlockRenderer;
import net.openvoxel.client.textureatlas.BaseIcon;
import net.openvoxel.client.textureatlas.Icon;
import net.openvoxel.common.block.Block;
import net.openvoxel.world.client.ClientChunkSection;
import net.openvoxel.world.client.ClientWorld;

import java.nio.ByteBuffer;

/*
 * Renderer that can be called from many threads {}
 */
public abstract class BaseWorldRenderer {


	protected ClientWorld theWorld;
	protected long originX;
	protected long originZ;
	protected int screenWidth;
	protected int screenHeight;

	///////////////////////////
	/// World Draw Task API ///
	///////////////////////////


	public void Setup(long _originX, long _originZ, ClientWorld _world) {
		theWorld = _world;
		originX = _originX;
		originZ = _originZ;
		screenWidth = ClientInput.currentWindowFrameSize.x;
		screenHeight = ClientInput.currentWindowFrameSize.y;
	}

	private TIntObjectMap<AsyncWorldHandler> objectMap = new TSynchronizedIntObjectMap<>(new TIntObjectHashMap<>());

	public AsyncWorldHandler getWorldHandlerFor(int asyncID) {
		AsyncWorldHandler handler = objectMap.get(asyncID);
		if(handler == null) {
			handler = new AsyncWorldHandler(asyncID);
			objectMap.put(asyncID,handler);
		}
		return handler;
	}

	///////////////////////////
	/// Async Generator API ///
	///////////////////////////

	public abstract void InvalidateChunkSection(ClientChunkSection section);

	protected abstract void AsyncDraw(AsyncWorldHandler handle, ClientChunkSection chunkSection,int asyncID);

	protected abstract void AllocateChunkMemory(AsyncWorldHandler handle, boolean isOpaque);

	protected abstract void ExpandChunkMemory(AsyncWorldHandler handle, boolean isOpaque);

	protected abstract void FinalizeChunkMemory(AsyncWorldHandler handle,int asyncID, ClientChunkSection chunkSection, boolean isOpaque);

	protected abstract void StartAsyncGenerate(AsyncWorldHandler handle,int asyncID);

	protected abstract void StopAsyncGenerate(AsyncWorldHandler handle, int asyncID);


	////////////////////////////////////
	/// Asynchronous Generation Code ///
	////////////////////////////////////

	public final class AsyncWorldHandler implements IBlockRenderer {

		private BaseBlockAccess blockAccess = new BaseBlockAccess();
		private int asyncID;
		private boolean isOpaqueDraw;

		private AsyncWorldHandler(int asyncID) {
			this.asyncID = asyncID;
		}

		public void Start() {
			blockAccess.bindWorld(theWorld);
			StartAsyncGenerate(this,asyncID);
		}

		public void AsyncDraw(ClientChunkSection section) {
			BaseWorldRenderer.this.AsyncDraw(this,section,asyncID);
		}

		public void Finish() {
			StopAsyncGenerate(this,asyncID);
		}

		/*
		 * The chunk has been updated - generate and update it
		 *  NB: This function may be called asynchronously
		 *
		 */
		public void AsyncGenerate(ClientChunkSection chunkSection) {
			blockAccess.bindChunkSection(chunkSection);
			AllocateChunkMemory(this,true);
			write_offset = start_offset;
			isOpaqueDraw = true;
			for (int xOff = 0; xOff < 16; xOff++) {
				for (int yOff = 0; yOff < 16; yOff++) {
					for (int zOff = 0; zOff < 16; zOff++) {
						blockAccess.bindSectionOffset(xOff, yOff, zOff);
						Block block = blockAccess.getBlock();
						//Block block = chunkSection.blockAt(xOff, yOff, zOff);
						block.getRenderHandler().storeBlockData(this, blockAccess, true);
					}
				}
			}
			FinalizeChunkMemory(this,asyncID,chunkSection,true);
			AllocateChunkMemory(this,false);
			write_offset = start_offset;
			isOpaqueDraw = false;
			for (int xOff = 0; xOff < 16; xOff++) {
				for (int yOff = 0; yOff < 16; yOff++) {
					for (int zOff = 0; zOff < 16; zOff++) {
						Block block = chunkSection.blockAt(xOff, yOff, zOff);
						blockAccess.bindSectionOffset(xOff, yOff, zOff);
						block.getRenderHandler().storeBlockData(this, blockAccess, false);
					}
				}
			}
			FinalizeChunkMemory(this,asyncID,chunkSection,false);
			chunkSection.markDrawUpdated();
		}

		///////////////////////////////
		/// Sub Chunk Renderer Code ///
		///////////////////////////////

		///
		/// State set by API
		///

		public ByteBuffer memoryMap = null;
		public int start_offset;
		public int end_offset;
		public int memory_id;
		public int write_offset;


		///
		/// Renderer State
		///

		private BaseIcon nullIcon = new BaseIcon();
		private BaseIcon currentIcon = nullIcon;

		private static final int SIZE_OF_ENTRY = 32;


		///////////////////////////
		/// Block Renderer Code ///
		///////////////////////////

		@Override
		public void addVertex(float X, float Y, float Z, float U, float V, float xNorm, float yNorm, float zNorm, float xTangent, float yTangent, float zTangent) {
			addVertexWithCol(X, Y, Z, U, V, xNorm, yNorm, zNorm, xTangent, yTangent, zTangent, 0xFFFFFFFF);
		}

		@Override
		public void addVertexWithCol(float X, float Y, float Z, float U, float V, float xNorm, float yNorm, float zNorm, float xTangent, float yTangent, float zTangent, int Colour) {
			if(write_offset + SIZE_OF_ENTRY >= end_offset) {
				ExpandChunkMemory(this,isOpaqueDraw);
			}

			memoryMap.putFloat(write_offset, (X + blockAccess.getOffsetX()));
			memoryMap.putFloat(write_offset + 4, (Y + blockAccess.getOffsetY()));
			memoryMap.putFloat(write_offset + 8, (Z + blockAccess.getOffsetZ()));

			float u_value = (U * (currentIcon.U1 - currentIcon.U0)) + currentIcon.U0;
			float delta_v = currentIcon.V1 - currentIcon.V0;
			float v_value = (V * delta_v) + currentIcon.V0;

			memoryMap.putShort(write_offset + 12, (short) (u_value * 65535.F));
			memoryMap.putShort(write_offset + 14, (short) (v_value * 65535.F));

			memoryMap.put(write_offset + 16, (byte) (xNorm * 255.F));
			memoryMap.put(write_offset + 17, (byte) (yNorm * 255.F));
			memoryMap.put(write_offset + 18, (byte) (zNorm * 255.F));

			memoryMap.put(write_offset + 19, (byte) (xTangent * 255.F));
			memoryMap.put(write_offset + 20, (byte) (yTangent * 255.F));
			memoryMap.put(write_offset + 21, (byte) (zTangent * 255.F));

			memoryMap.putInt(write_offset + 22, Colour);

			memoryMap.putInt(write_offset + 26, 0xFFFFFFFF);//Lighting TODO: IMPLEMENT

			memoryMap.put(write_offset + 30,(byte)(currentIcon.animationCount));
			memoryMap.put(write_offset + 31,(byte)(delta_v / currentIcon.animationCount));

			write_offset += SIZE_OF_ENTRY;
		}


		@Override
		public void setCurrentIcon(Icon icon) {
			currentIcon = icon == null ? nullIcon : (BaseIcon)icon;
		}
	}
}
