package net.openvoxel.client;

import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBImage;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * Created by James on 28/08/2016.
 */
public class STBITexture {
	public int width;
	public int height;
	public int componentCount;
	public ByteBuffer pixels;

	public STBITexture(byte[] array) {
		//STBImage.stbi_load_from_memory()
		ByteBuffer buffer = BufferUtils.createByteBuffer(array.length);
		buffer.put(array);
		buffer.position(0);
		IntBuffer xBuffer = BufferUtils.createIntBuffer(1);
		IntBuffer yBuffer = BufferUtils.createIntBuffer(1);
		IntBuffer compBuffer = BufferUtils.createIntBuffer(1);
		pixels = STBImage.stbi_load_from_memory(buffer,xBuffer,yBuffer,compBuffer,4);
		xBuffer.position(0);
		yBuffer.position(0);
		compBuffer.position(0);
		width = xBuffer.get();
		height = yBuffer.get();
		componentCount = compBuffer.get();
	}


	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public ByteBuffer getPixels() {
		return pixels;
	}

	public void Free() {
		STBImage.stbi_image_free(pixels);
		pixels = null;//Cleanup//
	}

	public boolean hasData() {
		return pixels != null;
	}
}
