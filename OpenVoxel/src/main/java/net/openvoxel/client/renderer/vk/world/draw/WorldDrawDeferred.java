package net.openvoxel.client.renderer.vk.world.draw;

import net.openvoxel.client.renderer.vk.VulkanCache;
import net.openvoxel.client.renderer.vk.VulkanCommandHandler;
import net.openvoxel.client.renderer.vk.world.VulkanWorldRenderer;
import net.openvoxel.world.client.ClientChunkSection;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;

import java.nio.LongBuffer;
import java.util.List;

public class WorldDrawDeferred extends BaseWorldDraw{


	@Override
	public int getNearbyCullSize() {
		return 0;
	}

	@Override
	public void load(VulkanCommandHandler handler, int asyncCount) {

	}

	@Override
	public void close(VulkanCommandHandler handler) {

	}

	@Override
	public void beginAsync(VulkanCommandHandler commandHandler, VulkanCache cache, VulkanWorldRenderer.VulkanAsyncWorldHandler asyncHandler, int screenWidth, int screenHeight, LongBuffer descriptorSets) {

	}

	@Override
	public void endAsync(VulkanCommandHandler commandHandler, VulkanWorldRenderer.VulkanAsyncWorldHandler asyncHandler) {

	}

	@Override
	public void asyncDrawShadows(VulkanWorldRenderer.VulkanAsyncWorldHandler handler, ClientChunkSection section, float offsetX, float offsetY, float offsetZ) {
		//NO OP (TODO: IMPL...)
	}

	@Override
	public void asyncDrawNearby(VulkanWorldRenderer.VulkanAsyncWorldHandler handler, ClientChunkSection section, float offsetX, float offsetY, float offsetZ) {
		//NO OP (TODO: IMPL...)
	}

	@Override
	public void drawForwardRenderer(VkCommandBuffer buffer, MemoryStack stack, List<VulkanWorldRenderer.VulkanAsyncWorldHandler> asyncList) {

	}

	@Override
	public void executeDrawCommands(VkCommandBuffer buffer, MemoryStack stack, List<VulkanWorldRenderer.VulkanAsyncWorldHandler> asyncList) {

	}
}
