package net.openvoxel.client.renderer.gl3.util;

import net.openvoxel.client.STBITexture;
import net.openvoxel.common.block.IBlockAccess;
import net.openvoxel.common.resources.ResourceHandle;
import net.openvoxel.common.resources.ResourceManager;
import net.openvoxel.common.util.BlockFace;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_WRAP_R;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;

/**
 * Created by James on 10/04/2017.
 *
 * Cube Map Texture Implementation
 */
public class OGL3CubeMapTexture {


	private ResourceHandle HandleUp, HandleDown, HandleNorth, HandleSouth, HandleWest, HandleEast;

	private int texID;

	public OGL3CubeMapTexture(String prefix) {
		this(ResourceManager.getImage(prefix+"Up"),
				ResourceManager.getImage(prefix+"Down"),
				ResourceManager.getImage(prefix+"North"),
				ResourceManager.getImage(prefix+"South"),
				ResourceManager.getImage(prefix+"West"),
				ResourceManager.getImage(prefix+"East")
				);
	}

	private OGL3CubeMapTexture(ResourceHandle up,ResourceHandle down,ResourceHandle north, ResourceHandle south, ResourceHandle west, ResourceHandle east) {
		HandleUp = up;
		HandleDown = down;
		HandleNorth = north;
		HandleSouth = south;
		HandleWest = west;
		HandleEast = east;
		texID = glGenTextures();
		glActiveTexture(GL_TEXTURE0);
		glBindTexture(GL_TEXTURE_CUBE_MAP,texID);
		glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		glGenerateMipmap(GL_TEXTURE_CUBE_MAP);
		glBindTexture(GL_TEXTURE_CUBE_MAP,0);
		ensureData(true);
	}


	public void bind(int samplerID) {
		ensureData(false);
		glActiveTexture(GL_TEXTURE0+samplerID);
		glBindTexture(GL_TEXTURE_2D,texID);
		glActiveTexture(GL_TEXTURE0);
	}

	private void _data(int key,ResourceHandle handle) {
		STBITexture tex = new STBITexture(handle.getByteData());
		glTexImage2D(key,0,GL_RGBA,tex.width,tex.height,0,GL_RGBA,GL_UNSIGNED_BYTE,tex.pixels);
		tex.Free();
	}

	private void ensureData(boolean forceLoad) {
		if(HandleUp != null) {
			if(HandleUp.checkIfChanged() || HandleDown.checkIfChanged()
					   || HandleWest.checkIfChanged() || HandleEast.checkIfChanged()
					   || HandleNorth.checkIfChanged() || HandleSouth.checkIfChanged()) {
				HandleUp.reloadData();
				HandleDown.reloadData();
				HandleWest.reloadData();
				HandleEast.reloadData();
				HandleSouth.reloadData();
				HandleNorth.reloadData();
			}else if(!forceLoad){
				return;
			}
			//Set the Data//
			glBindTexture(GL_TEXTURE_CUBE_MAP,texID);
			_data(GL_TEXTURE_CUBE_MAP_POSITIVE_X,HandleNorth);
			_data(GL_TEXTURE_CUBE_MAP_NEGATIVE_X,HandleSouth);
			_data(GL_TEXTURE_CUBE_MAP_POSITIVE_Y,HandleUp);
			_data(GL_TEXTURE_CUBE_MAP_NEGATIVE_Y,HandleDown);
			_data(GL_TEXTURE_CUBE_MAP_POSITIVE_Z,HandleWest);
			_data(GL_TEXTURE_CUBE_MAP_NEGATIVE_Z,HandleEast);
			glBindTexture(GL_TEXTURE_CUBE_MAP,0);
		}
	}
}
