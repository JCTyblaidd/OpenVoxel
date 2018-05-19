package net.openvoxel.client.renderer.vk.world.draw;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;

import java.io.Closeable;

/**
 *
 * Abstract Interface around a world Draw Method!
 *
 */
public interface IWorldDraw extends Closeable {


	/**
	 * Initialize the new World Draw Method
	 */
	void load();

	/**
	 * Free resources of the World Draw Method
	 */
	@Override
	void close();

	/**
	 * Called before the GUI is drawn:
	 *  The Forward Render Pass has been bound
	 * @param buffer the Main Graphics Command Buffer
	 * @param stack the Memory Stack in use
	 */
	void drawForwardRenderer(VkCommandBuffer buffer, MemoryStack stack);

	/**
	 * Called before the GUI is drawn:
	 *  No bound state
	 * @param buffer the Main Graphics Command Buffer
	 * @param stack the Memory Stack in use
	 */
	void executeDrawCommands(VkCommandBuffer buffer, MemoryStack stack);

}
