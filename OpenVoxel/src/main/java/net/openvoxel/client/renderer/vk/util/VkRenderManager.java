package net.openvoxel.client.renderer.vk.util;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkCommandPoolCreateInfo;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkFramebufferCreateInfo;
import org.lwjgl.vulkan.VkMemoryRequirements;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

class VkRenderManager {

	public VkRenderDevice renderDevice;

	//Swap Chain Image Info
	LongBuffer swapChainImages;
	LongBuffer swapChainImageViews;

	/*Chosen SwapChain Info*/
	public int chosenPresentMode;
	public int chosenImageFormat;
	public int chosenColourSpace;
	public int chosenImageCount;
	public VkExtent2D swapExtent = VkExtent2D.calloc();

	//Command Pools//
	private long command_pool_graphics;
	private long command_pool_transfer;

	//Command Buffers//
	private long command_buffer_main;

	//Memory Management//
	private LongBuffer staging_buffers = MemoryUtil.memAllocLong(3);

	//Frame Buffers//
	private LongBuffer targetFrameBuffers;

	void initFrameBuffers() {
		targetFrameBuffers = MemoryUtil.memAllocLong(swapChainImageViews.capacity());
		try(MemoryStack stack = stackPush()) {
			LongBuffer singleBuffer = stack.mallocLong(1);
			for (int i = 0; i < swapChainImageViews.capacity(); i++) {
				singleBuffer.put(0,swapChainImageViews.get(i));
				VkFramebufferCreateInfo framebufferCreateInfo = VkFramebufferCreateInfo.mallocStack(stack);
				framebufferCreateInfo.sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO);
				framebufferCreateInfo.pNext(VK_NULL_HANDLE);
				framebufferCreateInfo.renderPass(/**TODO**/VK_NULL_HANDLE);
				framebufferCreateInfo.pAttachments(singleBuffer);
				framebufferCreateInfo.width(swapExtent.width());
				framebufferCreateInfo.height(swapExtent.height());
				framebufferCreateInfo.layers(1);
				//TODO: add render passes & other information
			}
		}
	}

	void destroyFrameBuffers() {
		for(int i = 0; i < targetFrameBuffers.capacity(); i++) {
			//vkDestroyFramebuffer(renderDevice.device, targetFrameBuffers.get(i),null);
		}
		MemoryUtil.memFree(targetFrameBuffers);
	}


	/**
	 * Returns -1 on failure otherwise returns offset
	 */
	public long appendToStaging(ByteBuffer bufferData) {
		return -1;
	}

	void initCommandBuffers() {
		try(MemoryStack stack = stackPush()) {
			VkCommandPoolCreateInfo createPool = VkCommandPoolCreateInfo.mallocStack(stack);
			createPool.sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO);
			createPool.pNext(VK_NULL_HANDLE);
			createPool.queueFamilyIndex(renderDevice.queueFamilyIndexRender);
			createPool.flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT | VK_COMMAND_POOL_CREATE_TRANSIENT_BIT);
			LongBuffer result = stack.mallocLong(1);
			if(vkCreateCommandPool(renderDevice.device,createPool,null,result) != VK_SUCCESS) {
				throw new RuntimeException("Failed to create command pool");
			}
			command_pool_graphics = result.get(0);
			createPool.queueFamilyIndex(renderDevice.queueFamilyIndexTransfer);
			createPool.flags(VK_COMMAND_POOL_CREATE_TRANSIENT_BIT);
			if(vkCreateCommandPool(renderDevice.device,createPool,null,result) != VK_SUCCESS) {
				throw new RuntimeException("Failed to create command pool");
			}
			command_pool_transfer = result.get(0);
			//TODO: create main command buffer//
		}
	}
	void destroyCommandBuffers(MemoryStack stack) {
		//NO OP//
	}

	void destroyCommandPools() {
		vkDestroyCommandPool(renderDevice.device,command_pool_graphics,null);
		vkDestroyCommandPool(renderDevice.device,command_pool_transfer,null);
	}

	void initMemory() {
		//Create Staging Buffer//
		try(MemoryStack stack = stackPush()) {

		}
	}

	void destroyMemory() {
		//Destroy Staging Buffer//
	}

	void initRenderPasses() {

	}

	void destroyRenderPasses(MemoryStack stack) {

	}

	void initGraphicsPipeline() {

	}

	void destroyPipelineAndLayout(MemoryStack stack) {

	}

}
