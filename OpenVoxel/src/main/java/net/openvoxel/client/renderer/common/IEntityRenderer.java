package net.openvoxel.client.renderer.common;

import org.joml.Matrix4f;

public interface IEntityRenderer {

	void beginDraw();
	void endDraw();

	void drawBuffer(float[] posarray, float[] uvarray);

	void setMatrix(Matrix4f mat);

	//Add Point//
	void addVertexWithUV(float x, float y, float z, float u, float v);
}
