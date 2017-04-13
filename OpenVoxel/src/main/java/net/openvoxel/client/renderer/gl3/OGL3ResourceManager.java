package net.openvoxel.client.renderer.gl3;

import net.openvoxel.client.renderer.gl3.util.OGL3Texture;

/**
 * Created by James on 25/08/2016.
 *
 * TODO: DEPRECATE AFTER MOVING THE REQUIRED INFORMATION TO ANOTHER LOCATION
 */
public class OGL3ResourceManager {

	public static OGL3ResourceManager instance;

	public static OGL3ResourceManager getInstance() {
		return instance;
	}

	public OGL3ResourceManager() {
		//Load//

	}


	/**
	 * R = red_diffuse
	 * G = green_diffuse
	 * B = blue_diffuse
	 * A = alpha_diffuse
	 * @return
	 */
	public OGL3Texture getTextureAtlas_Diffuse() {
		return null;
	}

	/**
	 * R = x_normal
	 * G = y_normal {normal assumes 100%Y = outwards}
	 * B = z_normal
	 * A = Flags    { (8 bits): //TODO: Finalize the Bit Flags
	 *                     1= Water Wave Animation
	 *                     2= Water Rain Animation
	 *                     3= Wind Animation
	 *                     4=
	 *                     5=
	 *                     6=
	 *                     7=
	 *                     8= Enable_Glow Animation
	 *              }
	 * @return
	 */
	public OGL3Texture getTextureAtlas_Normal() {
		return null;
	}

	/**
	 * R = Smoothness
	 * G = Reflectivity
	 * B = Ambient Occlusion
	 * A = Height Map (for parallax occlusion mapping)
	 * @return
	 */
	public OGL3Texture getTextureAtlas_Data() {
		return null;
	}
}
