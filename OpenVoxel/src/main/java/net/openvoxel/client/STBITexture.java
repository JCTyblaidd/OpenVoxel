package net.openvoxel.client;

import net.openvoxel.OpenVoxel;
import net.openvoxel.utility.CrashReport;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.stb.STBImage.stbi_image_free;
import static org.lwjgl.stb.STBImage.stbi_load_from_memory;

/**
 * Created by James on 28/08/2016.
 *
 * STB Image Texture (Loading Utility Function)
 */
public class STBITexture {
	public int width;
	public int height;
	public int componentCount;
	public ByteBuffer pixels;


	public STBITexture(byte[] array) {
		try(MemoryStack stack = MemoryStack.stackPush()) {
			ByteBuffer buffer = MemoryUtil.memAlloc(array.length);
			buffer.put(array);
			buffer.position(0);
			IntBuffer xBuffer = stack.mallocInt(1);
			IntBuffer yBuffer = stack.mallocInt(1);
			IntBuffer compBuffer = stack.mallocInt(1);
			pixels = stbi_load_from_memory(buffer, xBuffer, yBuffer, compBuffer, STBImage.STBI_rgb_alpha);
			xBuffer.position(0);
			yBuffer.position(0);
			compBuffer.position(0);
			width = xBuffer.get();
			height = yBuffer.get();
			componentCount = compBuffer.get();
			pixels.position(0);
			if (pixels.capacity() != 4 * width * height) {
				CrashReport crashReport = new CrashReport("Image Load Error")
						                          .invalidState("componentCount != 4")
						                          .invalidState("Capacity = " + pixels.capacity())
						                          .invalidState("Expected Capacity = " + (4 * width * height));
				OpenVoxel.reportCrash(crashReport);
			}
			MemoryUtil.memFree(buffer);
		}
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
		if(pixels != null) {
			stbi_image_free(pixels);
			pixels = null;//Cleanup//
		}
	}

	public boolean hasData() {
		return pixels != null;
	}
}
