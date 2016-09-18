package net.openvoxel.client.renderer.gl3.worldutil;

import static org.lwjgl.opengl.GL15.*;

/**
 * Created by James on 04/09/2016.
 *
 * Cache of a 16x16x16 Drawable Area
 *
 * TODO: Interleave Arrays? [will have to edit generation algorithm]
 */
public class OGL3SubChunkCache {

	private int bufferPos;
	private int bufferUV;
	private int bufferNormal;
	private int bufferColorMask;
	private int bufferLighting;

	//Flags: to enable culling for non-empty sub-chunks
	public boolean isSolid_Up = false;
	public boolean isSolid_Down = false;
	public boolean isSolid_North = false;
	public boolean isSolid_South = false;
	public boolean isSolid_West = false;
	public boolean isSolid_East = false;

	//Skip this chunk?
	public boolean isEmpty = false;

	public OGL3SubChunkCache() {
		int[] arr = new int[5];
		glGenBuffers(arr);
		bufferPos = arr[0];
		bufferUV = arr[1];
		bufferNormal = arr[2];
		bufferColorMask = arr[3];
		bufferLighting = arr[4];
	}

	public void setData(float[] PosArray, float[] UVArray,float[] NormArray,int[] ColArray,int[] LightArray) {
		glBindBuffer(GL_ARRAY_BUFFER,bufferPos);
		glBufferData(GL_ARRAY_BUFFER,PosArray,GL_DYNAMIC_DRAW);
		glBindBuffer(GL_ARRAY_BUFFER,bufferPos);
		glBufferData(GL_ARRAY_BUFFER,UVArray,GL_DYNAMIC_DRAW);
		glBindBuffer(GL_ARRAY_BUFFER,bufferNormal);
		glBufferData(GL_ARRAY_BUFFER,NormArray,GL_DYNAMIC_DRAW);
		glBindBuffer(GL_ARRAY_BUFFER,bufferColorMask);
		glBufferData(GL_ARRAY_BUFFER,ColArray,GL_DYNAMIC_DRAW);
		glBindBuffer(GL_ARRAY_BUFFER,bufferLighting);
		glBufferData(GL_ARRAY_BUFFER,LightArray,GL_DYNAMIC_DRAW);
		glBindBuffer(GL_ARRAY_BUFFER,0);
	}

	/**
	 * Cleanup OpenGL Buffer Memory
	 */
	public void kill() {
		glDeleteBuffers(new int[]{bufferPos,bufferUV,bufferNormal,bufferColorMask,bufferLighting});
	}
}
