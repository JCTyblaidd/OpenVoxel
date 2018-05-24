package net.openvoxel.client.renderer.base;

import gnu.trove.impl.sync.TSynchronizedIntObjectMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import net.openvoxel.client.ClientInput;
import net.openvoxel.client.renderer.common.IBlockRenderer;
import net.openvoxel.client.textureatlas.ArrayAtlas;
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

	protected AsyncWorldHandler getWorldHandlerFor(int asyncID) {
		AsyncWorldHandler handler = objectMap.get(asyncID);
		if(handler == null) {
			handler = CreateAsyncHandler(asyncID);
			objectMap.put(asyncID,handler);
		}
		return handler;
	}

	protected AsyncWorldHandler CreateAsyncHandler(int asyncID) {
		return new AsyncWorldHandler(asyncID);
	}

	////////////////////////
	/// Public Async API ///
	////////////////////////

	public int getNearbyCullSize() {
		return 0;//TODO: IMPLEMENT
	}

	public int getShadowFrustumCount() {
		return 0;//TODO: IMPLEMENT
	}

	public void SetupAsync(int asyncID) {
		getWorldHandlerFor(asyncID).Start();
	}

	public void FinishAsync(int asyncID) {
		getWorldHandlerFor(asyncID).Finish();
	}

	public void GenerateChunkSection(ClientChunkSection section,int asyncID) {
		getWorldHandlerFor(asyncID).AsyncGenerate(section);
	}

	public void DrawWorldChunkSection(ClientChunkSection section,int asyncID) {
		AsyncDraw(getWorldHandlerFor(asyncID),section,asyncID);
	}

	public void DrawShadowChunkSection(ClientChunkSection section,int asyncID) {
		//TODO: IMPLEMENT
	}

	public void DrawNearbyChunkSection(ClientChunkSection section,int asyncID) {
		//TODO: IMPLEMENT
	}

	public abstract void InvalidateChunkSection(ClientChunkSection section);

	//////////////////////////////////
	/// Implemented By Super Class ///
	//////////////////////////////////

	protected abstract void AsyncDraw(AsyncWorldHandler handle, ClientChunkSection chunkSection,int asyncID);

	protected abstract void AllocateChunkMemory(AsyncWorldHandler handle, boolean isOpaque);

	protected abstract void ExpandChunkMemory(AsyncWorldHandler handle, boolean isOpaque);

	protected abstract void FinalizeChunkMemory(AsyncWorldHandler handle,int asyncID, ClientChunkSection chunkSection, boolean isOpaque);

	protected abstract void StartAsyncGenerate(AsyncWorldHandler handle,int asyncID);

	protected abstract void StopAsyncGenerate(AsyncWorldHandler handle, int asyncID);


	////////////////////////////////////
	/// Asynchronous Generation Code ///
	////////////////////////////////////

	public class AsyncWorldHandler implements IBlockRenderer {

		private BaseBlockAccess blockAccess = new BaseBlockAccess();
		public final int asyncID;
		private boolean isOpaqueDraw;

		protected AsyncWorldHandler(int asyncID) {
			this.asyncID = asyncID;
		}

		public void Start() {
			blockAccess.bindWorld(theWorld);
			StartAsyncGenerate(this,asyncID);
		}

		public void Finish() {
			StopAsyncGenerate(this,asyncID);
		}

		/*
		 * The chunk has been updated - generate and update it
		 *  NB: This function may be called asynchronously
		 *
		 */
		void AsyncGenerate(ClientChunkSection chunkSection) {
			blockAccess.bindChunkSection(chunkSection);
			AllocateChunkMemory(this,true);
			write_offset = start_offset;
			isOpaqueDraw = true;
			for (int xOff = 0; xOff < 16; xOff++) {
				for (int yOff = 0; yOff < 16; yOff++) {
					for (int zOff = 0; zOff < 16; zOff++) {
						Block block = chunkSection.blockAt(xOff, yOff, zOff);
						blockAccess.bindSectionOffset(xOff, yOff, zOff);
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
		public int write_offset;


		///
		/// Renderer State
		///

		private ArrayAtlas.ArrayIcon nullIcon = new ArrayAtlas.ArrayIcon();
		private ArrayAtlas.ArrayIcon currentIcon = nullIcon;

		public static final int SIZE_OF_ENTRY = 32;
		public static final int OFFSET_POSITION = 0;
		public static final int OFFSET_TANGENT = 12;
		public static final int OFFSET_COLOUR = 16;
		public static final int OFFSET_LIGHTING = 20;
		public static final int OFFSET_UV_COORD = 24;
		public static final int OFFSET_TEX_COORD = 26;
		public static final int OFFSET_ANIM_VAL = 30;

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

			//Store Position Information...
			memoryMap.putFloat(write_offset + OFFSET_POSITION, (X + blockAccess.getOffsetX()));
			memoryMap.putFloat(write_offset + OFFSET_POSITION + 4, (Y + blockAccess.getOffsetY()));
			memoryMap.putFloat(write_offset + OFFSET_POSITION + 8, (Z + blockAccess.getOffsetZ()));

			//Store Quaternion Information TODO: ACTUALLY IMPLEMENT
			memoryMap.put(write_offset + OFFSET_TANGENT    , (byte)(0));
			memoryMap.put(write_offset + OFFSET_TANGENT + 1, (byte)(0));
			memoryMap.put(write_offset + OFFSET_TANGENT + 2, (byte)(0));
			memoryMap.put(write_offset + OFFSET_TANGENT + 3, (byte)(0));

			//Store Colour Information
			memoryMap.putInt(write_offset + OFFSET_COLOUR, Colour);

			//Store Lighting Information TODO: ACTUALLY IMPLEMENT
			memoryMap.putInt(write_offset + OFFSET_LIGHTING,0xFFFFFFFF);

			//Store UV Information
			memoryMap.put(write_offset + OFFSET_UV_COORD    ,(byte)(U * 255.F));
			memoryMap.put(write_offset + OFFSET_UV_COORD + 1,(byte)(V * 255.F));

			//Store Texture Information
			memoryMap.putShort(write_offset + OFFSET_TEX_COORD    ,(short)currentIcon.arrayIdx);
			memoryMap.putShort(write_offset + OFFSET_TEX_COORD + 2,(short)currentIcon.textureIdx);
			memoryMap.putShort(write_offset + OFFSET_ANIM_VAL     ,(short)currentIcon.animationCount);

			write_offset += SIZE_OF_ENTRY;
		}


		@Override
		public void setCurrentIcon(Icon icon) {
			currentIcon = icon == null ? nullIcon : (ArrayAtlas.ArrayIcon)icon;
		}
	}
}
