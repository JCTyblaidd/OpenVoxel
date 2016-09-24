package net.openvoxel.client.entity;

import net.openvoxel.common.entity.Entity;

/**
 * Created by James on 23/09/2016.
 *
 * Class that is used to render an entity
 */
public interface IEntityRenderer<T extends Entity> {

	/**
	 * Main Renderer Function
	 * @param entityRenderHandle
	 */
	void render(T object,Object entityRenderHandle);

}
