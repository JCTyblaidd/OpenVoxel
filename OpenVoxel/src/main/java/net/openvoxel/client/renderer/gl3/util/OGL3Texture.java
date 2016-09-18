package net.openvoxel.client.renderer.gl3.util;

import net.openvoxel.client.STBITexture;
import net.openvoxel.common.resources.ResourceHandle;

import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;

/**
 * Created by James on 28/08/2016.
 *
 * References and OpenGL Texture Object
 */
public class OGL3Texture {

	private static Map<ResourceHandle,OGL3Texture> cache;
	static {
		cache = new HashMap<>();
	}

	public static OGL3Texture getTexture(ResourceHandle Handle) {
		if(cache.containsKey(Handle)) {
			return cache.get(Handle);
		}else{
			OGL3Texture tex = new OGL3Texture(Handle);
			cache.put(Handle,tex);
			return tex;
		}
	}

	private ResourceHandle Handle;
	public int height;
	public int width;

	private int texID;

	public void makeLinear() {
		glBindTexture(GL_TEXTURE_2D,texID);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);//TODO:? enable GL_LINEAR in some cases?
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		glBindTexture(GL_TEXTURE_2D,0);
	}

	private OGL3Texture(ResourceHandle handle) {
		this.Handle = handle;
		texID = glGenTextures();
		glBindTexture(GL_TEXTURE_2D,texID);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);//TODO:? enable GL_LINEAR in some cases?
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		glGenerateMipmap(GL_TEXTURE_2D);//TODO: limit mipmap generation//
		glBindTexture(GL_TEXTURE_2D,0);
		ensureData(true);
	}

	public void bind(int samplerID) {
		ensureData(false);
		glActiveTexture(GL_TEXTURE0+samplerID);
		glBindTexture(GL_TEXTURE_2D,texID);
	}

	private void ensureData(boolean forceLoad) {
		if(Handle != null) {
			if(Handle.checkIfChanged()) {
				Handle.reloadData();
			}else if(!forceLoad){
				return;
			}
			byte[] data = Handle.getByteData();
			STBITexture tex = new STBITexture(data);
			height = tex.height;
			width = tex.width;
			//Data//
			glBindTexture(GL_TEXTURE_2D,texID);
			glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA,width,height,0,GL_RGBA,GL_UNSIGNED_BYTE,tex.pixels);
			glBindTexture(GL_TEXTURE_2D,0);
			tex.Free();
		}
	}

	private OGL3Texture() {
		this(null);
	}


	public void setNewRGBAData(int[] Array, int width, int height) {
		glBindTexture(GL_TEXTURE_2D,texID);
		glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA,width,height,0,GL_RGBA,GL_UNSIGNED_BYTE,Array);
		glBindTexture(GL_TEXTURE_2D,0);
	}
}
