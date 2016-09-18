package net.openvoxel.client.textureatlas;

import net.openvoxel.common.resources.ResourceHandle;
import net.openvoxel.common.resources.ResourceManager;

/**
 * Created by James on 10/09/2016.
 *
 * Texture Atlas
 */
public interface IconAtlas {

	Icon register(ResourceHandle handle_diffuse,ResourceHandle handle_normal, ResourceHandle handle_pbr);

	default Icon register(String diff,String normal,String pbr) {
		return register(ResourceManager.getImage(diff),
						ResourceManager.getImage(normal),
						ResourceManager.getImage(pbr));
	}
}
