package net.openvoxel.client.renderer.gl3.atlas;

import net.openvoxel.api.logger.Logger;
import net.openvoxel.client.STBITexture;
import net.openvoxel.client.textureatlas.Icon;
import net.openvoxel.common.resources.ResourceHandle;

/**
 * Created by James on 16/09/2016.
 *
 * References a texture atlas w/ reference for stuff
 */
public class OGL3Icon implements Icon{
	public float u_min, u_max, v_min, v_max;

	public float getU(float u) {
		//float val =  u_min + (u_max - u_min)*u;
		//System.out.println(u+"->"+val);
		//return val;
		return u;
	}

	public float getV(float v) {
		//return v_min + (v_max - v_min)*v;
		return v;
	}

	private int currHeight, currWidth;

	int getIconSize() {
		return currHeight / currWidth;
	}

	void reload() {
		Logger log = Logger.getLogger("OGL3 Block Icon");
		if(tex_diff != null) {
			tex_diff.Free();
		}
		if(tex_norm != null) {
			tex_norm.Free();
		}
		if(tex_pbr != null) {
			tex_pbr.Free();
		}
		tex_diff = new STBITexture(diffuse_dat.getByteData());
		tex_norm = new STBITexture(normal_dat.getByteData());
		tex_pbr = new STBITexture(pbr_dat.getByteData());
		currHeight = tex_diff.height;
		currWidth = tex_diff.width;
		int smallWidth = currWidth,smallHeight = currHeight;
		boolean erred = false;
		if(tex_norm.height != currHeight) {
			log.Info("Normal Height != Diffuse Height");
			smallHeight = Math.min(currHeight,tex_norm.height);
			erred = true;
		}
		if(tex_norm.width != currWidth) {
			log.Info("Normal Width != Diffuse Width");
			smallWidth = Math.min(currWidth,tex_norm.width);
			erred = true;
		}
		if(tex_pbr.width != currWidth) {
			log.Info("PBR Width != Diffuse Width");
			smallWidth = Math.min(smallWidth,tex_pbr.width);
			erred = true;
		}
		if(tex_pbr.height != currHeight) {
			log.Info("PBR Height != Diffuse Height");
			smallHeight = Math.min(smallHeight,tex_pbr.height);
			erred = true;
		}
		if(erred) {
			currHeight = smallHeight;
			currWidth = smallWidth;
			log.Info("Error Loading: " + diffuse_dat.getResourceID() + ", minimize fix attempted");
		}
	}



	void cleanup() {
		tex_diff.Free();
		tex_norm.Free();
		tex_pbr.Free();
		tex_diff = null;
		tex_norm = null;
		tex_pbr = null;
	}

	STBITexture tex_diff,tex_norm,tex_pbr;

	ResourceHandle diffuse_dat, normal_dat,pbr_dat;
}
