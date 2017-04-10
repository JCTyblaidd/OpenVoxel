package net.openvoxel.client.renderer.generic;


import org.joml.Matrix4f;

/**
 * Created by James on 25/08/2016.
 */
public interface EntityRenderer {

	void beginDraw();
	void endDraw();

	void drawBuffer(float[] posarray, float[] uvarray);

	void setMatrix(Matrix4f mat);

	//Add Point//
	void addVertexWithUV(float x, float y, float z, float u, float v);
}