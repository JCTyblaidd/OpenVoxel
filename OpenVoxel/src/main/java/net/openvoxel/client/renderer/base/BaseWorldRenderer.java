package net.openvoxel.client.renderer.base;

import net.openvoxel.client.renderer.common.IBlockRenderer;
import net.openvoxel.client.textureatlas.Icon;

import java.nio.ByteBuffer;

/*
 * Renderer that can be called from many threads {}
 */
public abstract class BaseWorldRenderer implements IBlockRenderer {







	///////////////////////////////
	/// Sub Chunk Renderer Code ///
	///////////////////////////////

	protected Icon currentIcon = null;

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
