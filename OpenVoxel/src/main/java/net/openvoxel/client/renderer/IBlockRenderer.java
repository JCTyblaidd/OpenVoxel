package net.openvoxel.client.renderer;

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
	 * Add Vertex Draw Request For Block Sections With Variable Colours & Flags
	 * Valid Flags:
	 *  TODO: IMPLEMENT
	 */
	void addVertexWithColFlags(float X, float Y, float Z, float U, float V, float xNorm, float yNorm, float zNorm, float xTangent, float yTangent, float zTangent, int Colour,int flags);

	/**
	 * notifyEvent that UV: values are to be considered in relation to this icon
	 * @param icon
	 */
	void setCurrentIcon(Icon icon);
}
