package net.openvoxel.client.renderer.vk.world.draw;

import net.openvoxel.client.renderer.vk.VulkanCache;
import net.openvoxel.client.renderer.vk.VulkanCommandHandler;
import net.openvoxel.client.renderer.vk.world.VulkanWorldRenderer;
import net.openvoxel.world.client.ClientChunkSection;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;

import java.util.List;

/**
 *
 * Abstract Interface around a world Draw Method!
 *
 */
public interface IWorldDraw {

	/**
	 *
	 * @return the number of shadow cascades
	 */
	int getShadowCascadeCount();

	/**
	 * @return the size of nearby chunks to be called
	 */
	int getNearbyCullSize();

	/**
	 * Initialize the new World Draw Method
	 */
	void load(VulkanCommandHandler handler, int asyncCount);

	/**
	 * Free resources of the World Draw Method
	 */
	void close(VulkanCommandHandler handler);


	/**
	 */
	void beginAsync(VulkanCommandHandler commandHandler,
	                VulkanCache cache,
	                VulkanWorldRenderer.VulkanAsyncWorldHandler asyncHandler);

	/**
	 */
	void endAsync(VulkanCommandHandler commandHandler, VulkanWorldRenderer.VulkanAsyncWorldHandler asyncHandler);


	void asyncDrawStandard(VulkanWorldRenderer.VulkanAsyncWorldHandler handler,
	                       ClientChunkSection section,
	                       int offsetX, int offsetY, int offsetZ);

	void asyncDrawShadows(VulkanWorldRenderer.VulkanAsyncWorldHandler handler,
	                      ClientChunkSection section,
	                      int offsetX, int offsetY, int offsetZ);

	void asyncDrawNearby(VulkanWorldRenderer.VulkanAsyncWorldHandler handler,
	                     ClientChunkSection section,
	                     int offsetX, int offsetY, int offsetZ);

	/**
	 * Called before the GUI is drawn:
	 *  The Forward Render Pass has been bound
	 * @param buffer the Main Graphics Command Buffer
	 * @param stack the Memory Stack in use
	 */
	void drawForwardRenderer(VkCommandBuffer buffer, MemoryStack stack, List<VulkanWorldRenderer.VulkanAsyncWorldHandler> asyncList);

	/**
	 * Called before the GUI is drawn:
	 *  No bound state
	 * @param buffer the Main Graphics Command Buffer
	 * @param stack the Memory Stack in use
	 */
	void executeDrawCommands(VkCommandBuffer buffer, MemoryStack stack, List<VulkanWorldRenderer.VulkanAsyncWorldHandler> asyncList);

}
