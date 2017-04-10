package net.openvoxel.client.renderer.gl3.worldrender.cache;

import net.openvoxel.client.async_caches.IRenderDataCache;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glDrawBuffers;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;


/**
 * Created by James on 09/04/2017.
 *
 * Cache Associated with a chunk section
 */
public class OGL3RenderCache implements IRenderDataCache{

	///////////////////////////////
	// OpenGL Draw Buffer Caches //
	///////////////////////////////

	int bufferPos;
	int bufferUV;
	int bufferNormal;
	int bufferColourMask;
	int bufferLighting;

	//BAD???///
	int VAO;

	////////////////////////////////////////
	// OpenGL Draw Buffer Data Generation //
	////////////////////////////////////////

	ByteBuffer dataPos;
	ByteBuffer dataUV;
	ByteBuffer dataNormal;
	ByteBuffer dataColourMask;
	ByteBuffer dataLighting;

	boolean hasData = true;//TODO: init to false
	int valueCount = 0;
	int newValueCount = 0;

	public void initGL() {
		int[] bufferArray = new int[5];
		glGenBuffers(bufferArray);
		bufferPos = bufferArray[0];
		bufferUV = bufferArray[1];
		bufferNormal = bufferArray[2];
		bufferColourMask = bufferArray[3];
		bufferLighting = bufferArray[4];
		//Set as no data//
		ByteBuffer emptyData = ByteBuffer.allocate(0);
		//TODO: remove(waste of opengl calls debug only)
		set_data(bufferPos,emptyData);
		set_data(bufferUV,emptyData);
		set_data(bufferNormal,emptyData);
		set_data(bufferColourMask,emptyData);
		set_data(bufferLighting,emptyData);
		//
		VAO = glGenVertexArrays();
		glBindVertexArray(VAO);
		glBindBuffer(GL_ARRAY_BUFFER,bufferPos);
		glVertexAttribPointer(0,3,GL_FLOAT,false,0,0);
		glBindBuffer(GL_ARRAY_BUFFER,bufferUV);
		glVertexAttribPointer(1,2,GL_FLOAT,false,0,0);
		glBindBuffer(GL_ARRAY_BUFFER,bufferNormal);
		glVertexAttribPointer(2,3,GL_FLOAT,false,0,0);
		glBindBuffer(GL_ARRAY_BUFFER,bufferColourMask);
		glVertexAttribPointer(3,3,GL_UNSIGNED_BYTE,true,0,0);
		glBindBuffer(GL_ARRAY_BUFFER,bufferLighting);
		glVertexAttribPointer(4,3,GL_UNSIGNED_BYTE,true,0,0);
		glEnableVertexAttribArray(0);
		glEnableVertexAttribArray(1);
		glEnableVertexAttribArray(2);
		glEnableVertexAttribArray(3);
		glEnableVertexAttribArray(4);
		glBindVertexArray(0);
	}

	private void set_data(int buffer,ByteBuffer data) {
		glBindBuffer(GL_ARRAY_BUFFER,buffer);
		glBufferData(GL_ARRAY_BUFFER,data,GL_DYNAMIC_DRAW);
	}

	public void updateGLAndRelease() {
		if(dataPos != null) {
			set_data(bufferPos, dataPos);
			set_data(bufferUV, dataUV);
			set_data(bufferNormal, dataNormal);
			set_data(bufferColourMask, dataColourMask);
			set_data(bufferLighting, dataLighting);
			dataPos = null;
			dataUV = null;
			dataNormal = null;
			dataColourMask = null;
			dataLighting = null;
			valueCount = newValueCount;
			newValueCount = 0;
		}
	}

	public void removeGL() {
		int[] bufferArray = new int[]{
				bufferPos,bufferUV,bufferNormal,bufferColourMask,bufferLighting
		};
		glDeleteBuffers(bufferArray);
	}

	public boolean cacheExists() {
		return true;//IGNORE//
	}

	public void draw() {
		if(valueCount != 0) {
			glBindVertexArray(VAO);
			glDrawArrays(GL_TRIANGLES, 0, valueCount);
		}
	}

}
