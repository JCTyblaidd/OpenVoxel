package net.openvoxel.client.renderer.base;

import net.openvoxel.client.renderer.common.IBlockRenderer;
import net.openvoxel.client.textureatlas.BaseIcon;
import net.openvoxel.client.textureatlas.Icon;
import net.openvoxel.world.client.ClientChunkSection;
import net.openvoxel.world.client.ClientWorld;

import java.nio.ByteBuffer;

/*
 * Renderer that can be called from many threads {}
 */
public abstract class BaseWorldRenderer implements IBlockRenderer {


	///////////////////////////
	/// World Draw Task API ///
	///////////////////////////

	public void StartAsyncGenerate(int asyncID) {

	}

	/*
	 * The chunk has been updated - generate and update it
	 *  NB: This function may be called asynchronously
	 *
	 */
	public void AsyncGenerate(ClientWorld world, ClientChunkSection chunkSection,int asyncID,boolean transfer) {

	}

	public void AsyncDraw(ClientWorld world, ClientChunkSection chunkSection,int asyncID) {

	}

	public void StopAsyncGenerate(int asyncID) {

	}

	/*
	 * The chunk has been unloaded - forget about it
	 *  NB: This function may be called asynchronously
	 */
	public void InvalidateChunkSection(ClientChunkSection section) {

	}


	///////////////////////////////
	/// Sub Chunk Renderer Code ///
	///////////////////////////////

	private BaseIcon nullIcon = new BaseIcon();
	protected Icon currentIcon = nullIcon;

	private ByteBuffer memoryMap;
	private int capacity;
	private int offset;


	protected void DrawSubChunkOnMemory() {

	}



	///////////////////////////
	/// Block Renderer Code ///
	///////////////////////////

	@Override
	public void addVertex(float X, float Y, float Z, float U, float V, float xNorm, float yNorm, float zNorm, float xTangent, float yTangent, float zTangent) {
		addVertexWithCol(X,Y,Z,U,V,xNorm,yNorm,zNorm,xTangent,yTangent,zTangent,0xFFFFFFFF);
	}

	@Override
	public void addVertexWithCol(float X, float Y, float Z, float U, float V, float xNorm, float yNorm, float zNorm, float xTangent, float yTangent, float zTangent, int Colour) {
		memoryMap.putFloat(offset,X);
		memoryMap.putFloat(offset+4,Y);
		memoryMap.putFloat(offset+8,Z);


		memoryMap.putShort(offset+12,(short)(U / 65535));
		memoryMap.putShort(offset+14,(short)(V / 65535));

		memoryMap.put(offset+16,(byte)(xNorm / 255));
		memoryMap.put(offset+17,(byte)(yNorm / 255));
		memoryMap.put(offset+18,(byte)(zNorm / 255));

		//memoryMap.put(offset)
	}


	@Override
	public void setCurrentIcon(Icon icon) {
		currentIcon = icon;
	}

}
