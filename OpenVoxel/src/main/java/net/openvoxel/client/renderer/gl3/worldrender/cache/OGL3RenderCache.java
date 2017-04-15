package net.openvoxel.client.renderer.gl3.worldrender.cache;

import net.openvoxel.client.utility.IRenderDataCache;
import net.openvoxel.world.client.ClientChunk;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glDisableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;


/**
 * Created by James on 09/04/2017.
 *
 * Cache Associated with a chunk section
 *
 * TODO: kill and regenerate buffers to allow for updating them
 */
public class OGL3RenderCache implements IRenderDataCache {

	///////////////////////
	// Bonus Information //
	///////////////////////

	public ClientChunk chunk;
	public int yPos;


	///////////////////////////////
	// OpenGL Draw Buffer Caches //
	///////////////////////////////

	private int bufferPos;
	private int bufferUV;
	private int bufferNormal;
	private int bufferTangent;
	private int bufferColourMask;
	private int bufferLighting;

	//TODO: is using lots of vertex arrays worth it
	private int VAO;

	////////////////////////////////////////
	// OpenGL Draw Buffer Data Generation //
	////////////////////////////////////////

	ByteBuffer dataPos;
	ByteBuffer dataUV;
	ByteBuffer dataNormal;
	ByteBuffer dataTangent;
	ByteBuffer dataColourMask;
	ByteBuffer dataLighting;

	private int valueCount = 0;
	int newValueCount = 0;

	void initGL() {
		int[] bufferArray = new int[6];
		glGenBuffers(bufferArray);
		bufferPos = bufferArray[0];
		bufferUV = bufferArray[1];
		bufferNormal = bufferArray[2];
		bufferTangent = bufferArray[3];
		bufferColourMask = bufferArray[4];
		bufferLighting = bufferArray[5];
		//
		VAO = glGenVertexArrays();
		glBindVertexArray(VAO);
		glBindBuffer(GL_ARRAY_BUFFER,bufferPos);
		glVertexAttribPointer(0,3,GL_FLOAT,false,0,0);
		glBindBuffer(GL_ARRAY_BUFFER,bufferUV);
		glVertexAttribPointer(1,2,GL_UNSIGNED_SHORT,true,0,0);
		glBindBuffer(GL_ARRAY_BUFFER,bufferNormal);
		glVertexAttribPointer(2,3,GL_UNSIGNED_BYTE,true,0,0);
		glBindBuffer(GL_ARRAY_BUFFER,bufferTangent);
		glVertexAttribPointer(3,3,GL_UNSIGNED_BYTE,true,0,0);
		glBindBuffer(GL_ARRAY_BUFFER,bufferColourMask);
		glVertexAttribPointer(4,4,GL_UNSIGNED_BYTE,true,0,0);
		glBindBuffer(GL_ARRAY_BUFFER,bufferLighting);
		glVertexAttribPointer(5,4,GL_UNSIGNED_BYTE,true,0,0);
		glEnableVertexAttribArray(0);
		glEnableVertexAttribArray(1);
		glEnableVertexAttribArray(2);
		glEnableVertexAttribArray(3);
		glEnableVertexAttribArray(4);
		glEnableVertexAttribArray(5);
		glEnableVertexAttribArray(6);
		glBindVertexArray(0);
		glDisableVertexAttribArray(0);
		glDisableVertexAttribArray(1);
		glDisableVertexAttribArray(2);
		glDisableVertexAttribArray(3);
		glDisableVertexAttribArray(4);
		glDisableVertexAttribArray(5);
		glDisableVertexAttribArray(6);
	}

	private void set_data(int buffer,ByteBuffer data) {
		glBindBuffer(GL_ARRAY_BUFFER,buffer);
		glBufferData(GL_ARRAY_BUFFER,data,GL_DYNAMIC_DRAW);
	}

	/**
	 * Called in the render loop to allow for updating the buffers before any draws occur
	 */
	public void updateGLAndRelease() {
		if(dataPos != null) {
			set_data(bufferPos, dataPos);
			set_data(bufferUV, dataUV);
			set_data(bufferNormal, dataNormal);
			set_data(bufferTangent,dataTangent);
			set_data(bufferColourMask, dataColourMask);
			set_data(bufferLighting, dataLighting);
			//Release//
			MemoryUtil.memFree(dataPos);
			MemoryUtil.memFree(dataUV);
			MemoryUtil.memFree(dataNormal);
			MemoryUtil.memFree(dataTangent);
			MemoryUtil.memFree(dataColourMask);
			MemoryUtil.memFree(dataLighting);
			//Cleanup//
			dataPos = null;
			dataUV = null;
			dataNormal = null;
			dataColourMask = null;
			dataLighting = null;
			valueCount = newValueCount;
			newValueCount = 0;
		}
	}

	/**
	 * Cleanup All OpenGL Information
	 */
	void removeGL() {
		int[] bufferArray = new int[]{
				bufferPos,bufferUV,bufferNormal,bufferTangent,bufferColourMask,bufferLighting
		};
		glDeleteBuffers(bufferArray);
		glDeleteVertexArrays(VAO);
	}

	public boolean cacheExists() {
		return valueCount > 0;
	}

	public void draw() {
		glBindVertexArray(VAO);
		glDrawArrays(GL_TRIANGLES, 0, valueCount);
	}

}
