package net.openvoxel.client;

import net.openvoxel.OpenVoxel;
import net.openvoxel.utility.CrashReport;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.stb.STBImage.nstbi_load_from_memory;
import static org.lwjgl.system.Checks.CHECKS;
import static org.lwjgl.system.Checks.checkBuffer;
import static org.lwjgl.system.MemoryUtil.memAddress;
import static org.lwjgl.system.MemoryUtil.memByteBuffer;

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

	/**
	 * Fix for stbi_load_from_memory since the buffer size reported is incorrect
	 */
	private ByteBuffer _correctSTBIMem(ByteBuffer buffer,IntBuffer x,IntBuffer y, IntBuffer comp,int req_comp) {
		if ( CHECKS ) {
			checkBuffer(x, 1);
			checkBuffer(y, 1);
			checkBuffer(comp, 1);
		}
		long __result = nstbi_load_from_memory(memAddress(buffer), buffer.remaining(), memAddress(x), memAddress(y), memAddress(comp), req_comp);
		return memByteBuffer(__result, x.get(x.position()) * y.get(y.position()) * req_comp);//FIX:
	}

	public STBITexture(byte[] array) {
		ByteBuffer buffer = MemoryUtil.memAlloc(array.length);
		buffer.put(array);
		buffer.position(0);
		IntBuffer xBuffer = MemoryUtil.memAllocInt(1);
		IntBuffer yBuffer = MemoryUtil.memAllocInt(1);
		IntBuffer compBuffer = MemoryUtil.memAllocInt(1);
		pixels = _correctSTBIMem(buffer,xBuffer,yBuffer,compBuffer,STBImage.STBI_rgb_alpha);
		xBuffer.position(0);
		yBuffer.position(0);
		compBuffer.position(0);
		width = xBuffer.get();
		height = yBuffer.get();
		componentCount = compBuffer.get();
		//FIX//
		pixels.position(0);
		if(pixels.capacity() != 4*width*height) {
			CrashReport crashReport = new CrashReport("Image Load Error").invalidState("componentCount != 4").invalidState("Capacity = "+pixels.capacity()).invalidState("Expected Capacity = " + (4 * width * height));
			OpenVoxel.reportCrash(crashReport);
		}
		MemoryUtil.memFree(buffer);
		MemoryUtil.memFree(xBuffer);
		MemoryUtil.memFree(yBuffer);
		MemoryUtil.memFree(compBuffer);
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
