package net.openvoxel.client.textureatlas;

import net.openvoxel.common.resources.ResourceHandle;
import net.openvoxel.common.resources.ResourceManager;

/**
 * Created by James on 10/09/2016.
 *
 * Texture Atlas
 */
public interface IconAtlas {

	/**
	 * Register Textures
	 * @param handle_diffuse Diffuse Map (RGB = Color, A = Transparency)
	 * @param handle_normal Normal Map (RGB = Normals Tangent Space)
	 * @param handle_pbr PBR Information Map (R = Metal, G = Roughness, B = Ambient Occlusion, A = HeightMap)
	 * @return an opaque icon handle
	 */
	Icon register(ResourceHandle handle_diffuse,ResourceHandle handle_normal, ResourceHandle handle_pbr);

	/**
	 * Register Wrapped, ResourceManager.getImage(Handle)
	 * @param diff the diffuse map name
	 * @param normal the normal map name
	 * @param pbr the pbr map name
	 * @return an opaque icon handle
	 */
	default Icon register(String diff,String normal,String pbr) {
		return register(ResourceManager.getImage(diff),
						ResourceManager.getImage(normal),
						ResourceManager.getImage(pbr));
	}

	//TODO: implement GUI & Particle & Entity
	//Icon registerGUITex(ResourceHandle handleTex);

	//Icon registerParticleTex(ResourceHandle handleParticle);

	//Icon registerEntityTex(ResourceHandle handle_diffuse, ResourceHandle handle_normal, ResourceHandle handle_pbr);

	void performStitch();
}
