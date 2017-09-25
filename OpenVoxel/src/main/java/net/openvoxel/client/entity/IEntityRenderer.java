package net.openvoxel.client.entity;

import net.openvoxel.client.renderer.generic.EntityRenderer;
import net.openvoxel.common.entity.Entity;

/**
 * Created by James on 23/09/2016.
 *
 * Class that is used to render an entity
 */
public interface IEntityRenderer<T extends Entity> {

	/**
	 * Main Renderer Function
	 * @param entityRenderHandle the handle to the render api
	 */
	void render(T object,EntityRenderer entityRenderHandle);

}
