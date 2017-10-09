package net.openvoxel.client.renderer.vk.util;

import net.openvoxel.client.renderer.vk.shader.VkOmniRenderPass;
import net.openvoxel.client.renderer.vk.shader.VkRenderConfig;
import net.openvoxel.client.renderer.vk.shader.VkShaderModuleCache;
import net.openvoxel.client.renderer.vk.shader.VkShaderPipelineDebug;
import net.openvoxel.common.resources.ResourceManager;
import net.openvoxel.common.resources.ResourceType;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.List;

import static org.lwjgl.system.MemoryStack.create;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRSwapchain.VK_STRUCTURE_TYPE_PRESENT_INFO_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.vkQueuePresentKHR;
import static org.lwjgl.vulkan.VK10.*;

class VkRenderManager {

	public VkRenderDevice renderDevice;

	//Rendering State//
	public VkOmniRenderPass renderPass = new VkOmniRenderPass();
	public VkShaderPipelineDebug debugPipeline = new VkShaderPipelineDebug(VkShaderModuleCache.debugShader);
	//Shader Pipeline GBuffer Make
	//Shader Pipeline Shadow Mapping
	//Shader Pipeline Draw Entity
	//Shader Pipeline
	public VkRenderConfig renderConfig = new VkRenderConfig();

	//Swap Chain Image Info
	LongBuffer swapChainImages;
	LongBuffer swapChainImageViews;
	int swapChainImageIndex = 0;

	/*Chosen SwapChain Info*/
	public int chosenPresentMode;
	public int chosenImageFormat;
	public int chosenColourSpace;
	public int chosenImageCount;
	public VkExtent2D swapExtent;

	//Command Pools//
	private long command_pool_graphics;
	private long command_pool_transfer;

	//Command Buffers//
	PointerBuffer command_buffers_main;

	//Memory Management//
	private LongBuffer staging_buffers;

	//Frame Buffers//
	private LongBuffer targetFrameBuffers;

	//Synchronisation//
	long semaphore_image_available;
	long semaphore_render_finished;

	void initSynchronisation() {
		try(MemoryStack stack = stackPush()) {
			VkSemaphoreCreateInfo createSemaphore = VkSemaphoreCreateInfo.mallocStack(stack);
			createSemaphore.sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);
			createSemaphore.pNext(VK_NULL_HANDLE);
			createSemaphore.flags(0);
			LongBuffer lb = stack.callocLong(1);
			if(vkCreateSemaphore(renderDevice.device,createSemaphore,null,lb) != VK_SUCCESS) {
				throw new RuntimeException("Failed to create semaphore");
			}
			semaphore_image_available = lb.get(0);
			if(vkCreateSemaphore(renderDevice.device,createSemaphore,null,lb) != VK_SUCCESS) {
				throw new RuntimeException("Failed to create semaphore");
			}
			semaphore_render_finished = lb.get(0);
		}
	}

	void destroySynchronisation() {
		vkDestroySemaphore(renderDevice.device,semaphore_image_available,null);
		vkDestroySemaphore(renderDevice.device,semaphore_render_finished, null);
	}

	void initFrameBuffers() {
		targetFrameBuffers = MemoryUtil.memAllocLong(swapChainImageViews.capacity());
		try(MemoryStack stack = stackPush()) {
			LongBuffer singleBuffer = stack.mallocLong(1);
			for (int i = 0; i < swapChainImageViews.capacity(); i++) {
				singleBuffer.put(0,swapChainImageViews.get(i));
				VkFramebufferCreateInfo framebufferCreateInfo = VkFramebufferCreateInfo.mallocStack(stack);
				framebufferCreateInfo.sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO);
				framebufferCreateInfo.pNext(VK_NULL_HANDLE);
				framebufferCreateInfo.flags(0);
				framebufferCreateInfo.renderPass(renderPass.render_pass);
				framebufferCreateInfo.pAttachments(singleBuffer);
				framebufferCreateInfo.width(swapExtent.width());
				framebufferCreateInfo.height(swapExtent.height());
				framebufferCreateInfo.layers(1);
				targetFrameBuffers.position(i);
				if(vkCreateFramebuffer(renderDevice.device,framebufferCreateInfo,null,targetFrameBuffers) != VK_SUCCESS) {
					throw new RuntimeException("Failed to create framebuffer");
				}
			}
		}
		targetFrameBuffers.position(0);
	}

	void destroyFrameBuffers() {
		for(int i = 0; i < targetFrameBuffers.capacity(); i++) {
			vkDestroyFramebuffer(renderDevice.device, targetFrameBuffers.get(i),null);
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
			//Create Main Command Pool//
			int mainCommandBufferCount = targetFrameBuffers.capacity();
			VkCommandBufferAllocateInfo allocateInfo = VkCommandBufferAllocateInfo.callocStack(stack);
			allocateInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
			allocateInfo.pNext(0);
			allocateInfo.commandPool(command_pool_graphics);
			allocateInfo.commandBufferCount(mainCommandBufferCount);
			allocateInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);
			command_buffers_main = MemoryUtil.memAllocPointer(mainCommandBufferCount);
			if(vkAllocateCommandBuffers(renderDevice.device,allocateInfo,command_buffers_main) != VK_SUCCESS) {
				throw new RuntimeException("Failed to allocate main command buffer");
			}
			//Record With Debug Info//
			for(int i = 0; i < mainCommandBufferCount; i++) {
				VkCommandBuffer cmdBuffer = new VkCommandBuffer(command_buffers_main.get(i),renderDevice.device);
				VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.callocStack(stack);
				beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
				beginInfo.flags(VK_COMMAND_BUFFER_USAGE_SIMULTANEOUS_USE_BIT);
				beginInfo.pNext(VK_NULL_HANDLE);
				beginInfo.pInheritanceInfo(null);
				vkBeginCommandBuffer(cmdBuffer,beginInfo);

				VkRenderPassBeginInfo renderPassInfo = VkRenderPassBeginInfo.callocStack(stack);
				renderPassInfo.sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO);
				renderPassInfo.pNext(VK_NULL_HANDLE);
				renderPassInfo.renderPass(renderPass.render_pass);
				renderPassInfo.framebuffer(targetFrameBuffers.get(i));
				VkRect2D screenRect = VkRect2D.callocStack(stack);
				screenRect.extent(swapExtent);
				renderPassInfo.renderArea(screenRect);

				VkClearValue.Buffer clearValues = VkClearValue.callocStack(1,stack);
				VkClearColorValue clearColorValue = VkClearColorValue.callocStack(stack);
				clearColorValue.float32(0,0.0f);
				clearColorValue.float32(1,0.0f);
				clearColorValue.float32(2,0.2f);
				clearColorValue.float32(3,1.0f);
				clearValues.color(clearColorValue);
				renderPassInfo.pClearValues(clearValues);

				vkCmdBeginRenderPass(cmdBuffer,renderPassInfo, VK_SUBPASS_CONTENTS_INLINE);

				vkCmdBindPipeline(cmdBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, debugPipeline.graphics_pipeline);

				vkCmdDraw(cmdBuffer, 3, 1, 0, 0);

				vkCmdEndRenderPass(cmdBuffer);

				if (vkEndCommandBuffer(cmdBuffer) != VK_SUCCESS) {
					throw new RuntimeException("Failed to record command buffer(main)");
				}
			}
		}
	}

	void destroyCommandBuffers() {

	}

	void destroyCommandPools() {
		vkDestroyCommandPool(renderDevice.device,command_pool_graphics,null);
		vkDestroyCommandPool(renderDevice.device,command_pool_transfer,null);
		MemoryUtil.memFree(command_buffers_main);
	}

	void initMemory() {
		//Create Staging Buffer//
		staging_buffers = MemoryUtil.memAllocLong(3);
	}

	void destroyMemory() {
		//Destroy Staging Buffer//
		MemoryUtil.memFree(staging_buffers);
	}

	void initRenderPasses() {
		renderPass.init(renderDevice.device,renderConfig,chosenImageFormat);
	}

	void destroyRenderPasses() {
		renderPass.cleanup(renderDevice.device);
	}

	void initGraphicsPipeline() {
		debugPipeline.init(renderDevice.device,renderPass.render_pass,0,0, List.of("DEBUG_VULKAN"));
	}

	void destroyPipelineAndLayout() {
		debugPipeline.destroy(renderDevice.device);
	}


}
