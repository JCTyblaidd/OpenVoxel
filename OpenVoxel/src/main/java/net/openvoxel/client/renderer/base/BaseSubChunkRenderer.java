package net.openvoxel.client.renderer.base;

import net.openvoxel.client.renderer.common.IBlockRenderer;
import net.openvoxel.client.textureatlas.Icon;

public class BaseSubChunkRenderer implements IBlockRenderer {

	@Override
	public void addVertex(float X, float Y, float Z, float U, float V, float xNorm, float yNorm, float zNorm, float xTangent, float yTangent, float zTangent) {

	}

	@Override
	public void addVertexWithCol(float X, float Y, float Z, float U, float V, float xNorm, float yNorm, float zNorm, float xTangent, float yTangent, float zTangent, int Colour) {

	}

	@Override
	public void addVertexWithColFlags(float X, float Y, float Z, float U, float V, float xNorm, float yNorm, float zNorm, float xTangent, float yTangent, float zTangent, int Colour, int flags) {

	}

	@Override
	public void setCurrentIcon(Icon icon) {

	}

}
