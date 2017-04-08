package net.openvoxel.client.renderer.gl3.atlas;

import net.openvoxel.api.logger.Logger;
import net.openvoxel.client.renderer.generic.config.CompressionLevel;
import net.openvoxel.client.textureatlas.Icon;
import net.openvoxel.client.textureatlas.IconAtlas;
import net.openvoxel.common.resources.ResourceHandle;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.KHRTextureCompressionASTCLDR;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;

/**
 * Created by James on 16/09/2016.
 *
 * OpenGL Texture Atlas
 *
 * Auto-Bind[DO NOT OVERRIDE]:
 *      diffuse = 10,
 *      normal = 11,
 *      pbr = 12,
 *      1D_anim_index = 13,
 */
public class OGL3TextureAtlas implements IconAtlas{

	private List<OGL3Icon> icons = new ArrayList<>();
	private int tex_diffuse;
	private int tex_normal;
	private int tex_pbr;

	private boolean astc_compression = false;

	public OGL3TextureAtlas() {
		tex_diffuse = glGenTextures();
		tex_normal = glGenTextures();
		tex_pbr = glGenTextures();
		astc_compression = GL.getCapabilities().GL_KHR_texture_compression_astc_ldr;
	}

	public void bind() {
		glActiveTexture(GL_TEXTURE10);
		glBindTexture(GL_TEXTURE_2D,tex_diffuse);
		glActiveTexture(GL_TEXTURE11);
		glBindTexture(GL_TEXTURE_2D,tex_normal);
		glActiveTexture(GL_TEXTURE12);
		glBindTexture(GL_TEXTURE_2D,tex_pbr);
		glActiveTexture(GL_TEXTURE0);
	}

	private int _getFormat(CompressionLevel level) {
		switch (level) {
			case NO_COMPRESSION:
				return GL_RGBA;
			case SMALL_COMPRESSION:
				if(astc_compression) {
					return KHRTextureCompressionASTCLDR.GL_COMPRESSED_RGBA_ASTC_4x4_KHR;
				}else{

				}
			case MEDIUM_COMPRESSION:
				if(astc_compression) {
					return KHRTextureCompressionASTCLDR.GL_COMPRESSED_RGBA_ASTC_6x6_KHR;
				}else{

				}
			case LARGE_COMPRESSION:
				if(astc_compression) {
					return KHRTextureCompressionASTCLDR.GL_COMPRESSED_RGBA_ASTC_10x10_KHR;
				}else{

				}
			case EXTREME_COMPRESSION:
				if(astc_compression) {
					return KHRTextureCompressionASTCLDR.GL_COMPRESSED_RGBA_ASTC_12x12_KHR;
				}else{

				}
		}
		return GL_RGBA;
	}

	private ByteBuffer refTo(int[] data) {
		ByteBuffer b = ByteBuffer.allocateDirect(data.length * 4);
		b.asIntBuffer().put(data);
		b.position(0);
		return b;
	}

	/**
	 *
	 * @param img_size the power of 2 texture resolution to use
	 * @param enableAnimation if animations are enabled
	 * @param level texture compression : currently not supported
	 */
	public void update(int img_size, boolean enableAnimation, CompressionLevel level) {
		retVal data = generate(img_size,enableAnimation);
		int type = _getFormat(level);
		//if(type == GL_RGBA) {
			glBindTexture(GL_TEXTURE_2D, tex_pbr);
			glTexImage2D(GL_TEXTURE_2D, 0,GL_RGBA, data.width, data.height, 0, GL_RGBA, GL_UNSIGNED_BYTE, data.imgDiff);
			glBindTexture(GL_TEXTURE_2D,tex_normal);
			glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA, data.width, data.height, 0, GL_RGBA, GL_UNSIGNED_BYTE, data.imgNorm);
			glBindTexture(GL_TEXTURE_2D,tex_pbr);
			glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA, data.width, data.height, 0, GL_RGBA, GL_UNSIGNED_BYTE, data.imgPBR);
			glBindTexture(GL_TEXTURE_2D,0);
		/**
		}else{
			glBindTexture(GL_TEXTURE_2D, tex_pbr);
			//glTexImage2D(GL_TEXTURE_2D, 0,GL_RGBA, data.width, data.height, 0, GL_RGBA, GL_UNSIGNED_BYTE, data.imgDiff);
			glCompressedTexImage2D(GL_TEXTURE_2D,0,type,data.width,data.height,0, refTo(data.imgDiff));
			glCompressedTexImage2D();
			glBindTexture(GL_TEXTURE_2D,tex_normal);
			glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA, data.width, data.height, 0, GL_RGBA, GL_UNSIGNED_BYTE, data.imgNorm);
			glBindTexture(GL_TEXTURE_2D,tex_pbr);
			glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA, data.width, data.height, 0, GL_RGBA, GL_UNSIGNED_BYTE, data.imgPBR);
			glBindTexture(GL_TEXTURE_2D,0);
		}
		 **/
	}


	private retVal generate(int img_size,boolean enableAnim) {
		icons.forEach(OGL3Icon::reload);
		int lim = icons.stream().mapToInt(OGL3Icon::getIconSize).sum();
		if(!enableAnim) {
			lim = icons.size();
		}
		int maxH = icons.stream().mapToInt(OGL3Icon::getIconSize).max().orElse(0);
		if(!enableAnim) maxH = 0;
		int tileWidth;
		int tileHeight;
		int maxGet = (int)Math.ceil(Math.sqrt(lim));
		int bestW = 0,bestH = 0,bestLeft = 1000000;
		for(int i = maxH; i <= maxGet; i++) {
			int width = (int)Math.ceil((float)lim / (float)i);
			int size = lim - (i * width);
			if(size < bestLeft) {
				bestLeft = size;
				bestH = i;
				bestW = width;
			}
		}
		Logger log = Logger.getLogger("Texture Atlas");
		log.Info("Block Atlas Information: "+lim + ", minH="+maxH);
		log.Info("Block Atlas Size: " + bestW + "x" + bestH);
		tileHeight = bestH;
		tileWidth = bestW;
		if(tileHeight == 0 || tileWidth == 0) {
			retVal v = new retVal();
			v.height = 0;
			v.width = 0;
			v.imgNorm = new int[0];
			v.imgDiff = v.imgNorm;
			v.imgPBR = v.imgNorm;
			return v;
		}
		//Generate//
		int[] DataDiff = new int[tileWidth * tileHeight * img_size * img_size];
		int[] DataNorm = new int[tileWidth * tileHeight * img_size * img_size];
		int[] DataPBR = new int[tileWidth * tileHeight * img_size * img_size];
		//TODO: Improve Allocation Algorithm//
		for(int i = 0; i < lim; i++) {
			OGL3Icon toPlace = icons.get(i);
			int xOff = img_size * (i / tileWidth);
			int yOff = img_size * (i - ((i / tileWidth) * tileWidth));
			for(int x = 0; x < img_size; x++) {
				for(int y = 0; y < img_size; y++) {
					int Index = (xOff + x) + ((img_size * tileWidth) * (y + yOff));
					int Index2 = 4 * (x + (y * img_size));
					DataDiff[Index] = toPlace.tex_diff.pixels.getInt(Index2);
					DataNorm[Index] = toPlace.tex_norm.pixels.getInt(Index2);
					DataPBR[Index] = toPlace.tex_pbr.pixels.getInt(Index2);
				}
			}
		}
		retVal val = new retVal();
		val.width = tileWidth * img_size;
		val.height = tileHeight * img_size;
		val.imgNorm = DataNorm;
		val.imgDiff = DataDiff;
		val.imgPBR = DataPBR;
		icons.forEach(OGL3Icon::cleanup);
		return val;
	}

	private static class retVal {
		int[] imgDiff;
		int[] imgNorm;
		int[] imgPBR;
		int width;
		int height;
	}
	@Override
	public Icon register(ResourceHandle handle_diffuse, ResourceHandle handle_normal, ResourceHandle handle_pbr) {
		OGL3Icon icon = new OGL3Icon();
		icon.diffuse_dat = handle_diffuse;
		icon.normal_dat = handle_normal;
		icon.pbr_dat = handle_pbr;
		icons.add(icon);
		return icon;
	}

	@Override
	public void performStitch() {
		update(100,true,CompressionLevel.NO_COMPRESSION);
	}

}
