package net.openvoxel.client.renderer.vk;

import net.openvoxel.client.renderer.vk.pipeline.VulkanRenderPass;
import org.lwjgl.vulkan.VkCommandBuffer;

/**
 * Vulkan: Manage Command Buffer submission & threading
 *       : Also Managed Render Pass Images & Attachments
 */
public class VulkanCommandHandler {

	public VulkanCommandHandler() {
		if(!VulkanRenderPass.formatInit) {
			throw new RuntimeException("Formats have not been initialized");
		}
	}

	public void init(int newImageCount) {

	}

	public void reload() {

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
