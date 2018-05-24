package net.openvoxel.client.renderer.common;

import net.openvoxel.client.textureatlas.Icon;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;

public interface IBlockRenderer {

	Quaternionfc X_POSITIVE = new Quaternionf().identity().rotateY((float)(Math.PI/2.0));
	Quaternionfc X_NEGATIVE = new Quaternionf().identity().rotateY((float)(3.0*Math.PI/2.0));
	Quaternionfc Y_POSITIVE = new Quaternionf().identity().rotateX((float)(3.0*Math.PI/2.0));
	Quaternionfc Y_NEGATIVE = new Quaternionf().identity().rotateX((float)(Math.PI/2.0));
	Quaternionfc Z_POSITIVE = new Quaternionf().identity();
	Quaternionfc Z_NEGATIVE = new Quaternionf().identity().rotateX((float)Math.PI);

	/**
	 * Normal add Vertex Draw Request
	 */
	void addVertex(float X, float Y, float Z,
	               float U, float V,
	               Quaternionfc quaternion);

	/**
	 * Normal add Vertex Draw Request
	 */
	void addVertex(float X, float Y, float Z,
                   float U, float V,
                   float xQuaternion,float yQuaternion,float zQuaternion,float wQuaternion);

	/**
	 * Add Vertex Draw Request For Block Sections With Variable Colours
	 */
	void addVertexWithCol(float X, float Y, float Z,
	                      float U, float V,
	                      Quaternionfc quaternion,
	                      int Colour);

	/**
	 * Add Vertex Draw Request For Block Sections With Variable Colours
	 */
	void addVertexWithCol(float X, float Y, float Z,
	                      float U, float V,
	                      float xQuaternion,float yQuaternion,float zQuaternion,float wQuaternion,
	                      int Colour);

	/**
	 * notifyEvent that UV: values are to be considered in relation to this icon
	 * @param icon an icon reference
	 */
	void setCurrentIcon(Icon icon);
}
