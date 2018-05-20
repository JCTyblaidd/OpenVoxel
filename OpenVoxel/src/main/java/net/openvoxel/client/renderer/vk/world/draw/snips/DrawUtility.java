package net.openvoxel.client.renderer.vk.world.draw.snips;

import gnu.trove.list.TLongList;
import net.openvoxel.client.renderer.vk.core.VulkanUtility;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo;
import org.lwjgl.vulkan.VkCommandPoolCreateInfo;
import org.lwjgl.vulkan.VkDevice;

import java.nio.LongBuffer;
import java.util.List;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public abstract class DrawUtility {

	public static void InitCommandPools(VkDevice device, int queueFamilyIndex,
	                                    int swapSize, int asyncCount, int drawCount,
	                                    @NotNull TLongList poolList,
	                                    @NotNull List<VkCommandBuffer> bufferList) {
		try(MemoryStack stack = stackPush()) {
			VkCommandPoolCreateInfo poolCreate = VkCommandPoolCreateInfo.mallocStack(stack);
			poolCreate.sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO);
			poolCreate.pNext(VK_NULL_HANDLE);
			poolCreate.flags(
					VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT |
					VK_COMMAND_POOL_CREATE_TRANSIENT_BIT
			);
			poolCreate.queueFamilyIndex(queueFamilyIndex);

			poolList.clear();
			LongBuffer pResult = stack.mallocLong(1);
			for(int i = 0; i < asyncCount; i++) {
				int vkResult = vkCreateCommandPool(device,poolCreate,null,pResult);
				VulkanUtility.ValidateSuccess("Failed to create async command pool",vkResult);
				poolList.add(pResult.get(0));
			}

			bufferList.clear();
			PointerBuffer pCommands = stack.mallocPointer(swapSize*drawCount);
			for(int i = 0; i < asyncCount; i++) {
				VkCommandBufferAllocateInfo bufferAllocate = VkCommandBufferAllocateInfo.mallocStack(stack);
				bufferAllocate.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
				bufferAllocate.pNext(VK_NULL_HANDLE);
				bufferAllocate.commandPool(poolList.get(i));
				bufferAllocate.level(VK_COMMAND_BUFFER_LEVEL_SECONDARY);
				bufferAllocate.commandBufferCount(swapSize*drawCount);

				int vkResult = vkAllocateCommandBuffers(device,bufferAllocate,pCommands);
				VulkanUtility.ValidateSuccess("Failed to allocate async command buffer",vkResult);

				for(int j = 0; j < (swapSize*drawCount); j++) {
					bufferList.add(new VkCommandBuffer(pCommands.get(j),device));
				}
			}
		}
	}

	public static void DestroyCommandPools(VkDevice device,TLongList poolList) {
		for(int i = 0; i < poolList.size(); i++) {
			vkDestroyCommandPool(device,poolList.get(i),null);
		}
		poolList.clear();
	}

	public static VkCommandBuffer getCommandBuffer(int swapSize,int drawCount,
	                                               int swapID, int asyncID, int drawID,
	                                               List<VkCommandBuffer> bufferList) {
		int offset = (asyncID) * (swapSize * drawCount) + (drawID * swapSize) + swapID;
		return bufferList.get(offset);
	}

}
