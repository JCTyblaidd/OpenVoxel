package net.openvoxel.client.renderer.common;

import net.openvoxel.client.textureatlas.Icon;

public interface IBlockRenderer {
	/**
	 * Normal add Vertex Draw Request
	 */
	void addVertex(float X, float Y, float Z, float U, float V,float xNorm,float yNorm,float zNorm,float xTangent,float yTangent, float zTangent);

	/**
	 * Add Vertex Draw Request For Block Sections With Variable Colours
	 */
	void addVertexWithCol(float X, float Y, float Z, float U, float V,float xNorm,float yNorm,float zNorm,float xTangent,float yTangent, float zTangent, int Colour);

	/**
	 * notifyEvent that UV: values are to be considered in relation to this icon
	 * @param icon an icon reference
	 */
	void setCurrentIcon(Icon icon);
}
