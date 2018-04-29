package net.openvoxel.client.renderer.vk.core;

import org.lwjgl.vulkan.VkCommandBuffer;

/**
 * Vulkan: Manage Command Buffer submission & threading
 */
public class VulkanCommandHandler {

	public VulkanCommandHandler() {

	}

	public void init(int newImageCount) {

	}

	public void close() {

	}

	/**
	 * @return The command buffer for the entire draw call
	 */
	public VkCommandBuffer getMainDrawCommandBuffer() {
		return null;
	}

	/**
	 * @return The command buffer for the async GUI Draw call
	 */
	public VkCommandBuffer getGuiDrawCommandBuffer() {
		return null;
	}

	/**
	 * @return The command buffer for the async GUI Image Transfer
	 */
	public VkCommandBuffer getGuiTransferCommandBuffer() {
		return null;
	}
}
