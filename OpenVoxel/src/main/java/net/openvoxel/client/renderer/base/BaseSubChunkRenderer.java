package net.openvoxel.client.renderer.base;

import net.openvoxel.client.renderer.common.IBlockRenderer;
import net.openvoxel.client.textureatlas.Icon;

import java.nio.ByteBuffer;

public abstract class BaseSubChunkRenderer implements IBlockRenderer {

	protected Icon currentIcon = null;

	private ByteBuffer memoryMap;
	private int capacity;
	private int offset;






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

		// float{x,y,z} {u,v}
	}


	@Override
	public void setCurrentIcon(Icon icon) {
		currentIcon = icon;
	}

}
