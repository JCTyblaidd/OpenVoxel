package net.openvoxel.client.renderer.vk.util;

import net.openvoxel.client.renderer.vk.shader.*;
import net.openvoxel.client.renderer.vk.world.VkWorldRenderManager;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;
import java.util.List;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class VkRenderManager {

	public VkRenderDevice renderDevice;

	//Rendering State//
	public VkOmniRenderPass renderPass = new VkOmniRenderPass();
	public VkShaderPipelineGUI guiPipeline = new VkShaderPipelineGUI(VkShaderModuleCache.guiShader);
	public VkWorldRenderManager worldRenderManager;
	public VkRenderConfig renderConfig;

	public VkMemoryManager memoryMgr;

	//Swap Chain Image Info
	LongBuffer swapChainImages;
	public LongBuffer swapChainImageViews;
	public int swapChainImageIndex = 0;

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
	public PointerBuffer command_buffers_main;
	public PointerBuffer command_buffers_gui;
	public PointerBuffer command_buffers_gui_transfer;

	//Frame Buffers//
	public LongBuffer targetFrameBuffers;
	//TODO: render target FrameBuffers

	//Descriptor Pools//
	private LongBuffer descriptorPools;

	//Synchronisation//
	long semaphore_image_available;
	long semaphore_render_finished;
	long semaphore_gui_data_updated;
	LongBuffer submit_wait_fences_draw;
	LongBuffer submit_wait_fences_transfer;

	VkRenderManager() {
		renderConfig = new VkRenderConfig();
		renderConfig.load();
		renderConfig.save();
		worldRenderManager = new VkWorldRenderManager(this);
	}

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
			if(vkCreateSemaphore(renderDevice.device,createSemaphore,null,lb) != VK_SUCCESS) {
				throw new RuntimeException("Failed to create semaphore");
			}
			semaphore_gui_data_updated = lb.get(0);
		}
	}

	void initSwapChainSynchronisation() {
		submit_wait_fences_draw = MemoryUtil.memAllocLong(swapChainImageViews.capacity());
		submit_wait_fences_transfer = MemoryUtil.memAllocLong(swapChainImageViews.capacity());
		try(MemoryStack stack = stackPush()) {
			VkFenceCreateInfo createFence = VkFenceCreateInfo.mallocStack(stack);
			createFence.sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO);
			createFence.pNext(VK_NULL_HANDLE);
			createFence.flags(VK_FENCE_CREATE_SIGNALED_BIT);
			for(int i = 0; i < swapChainImageViews.capacity(); i++) {
				submit_wait_fences_draw.position(i);
				submit_wait_fences_transfer.position(i);
				if(vkCreateFence(renderDevice.device,createFence,null,submit_wait_fences_draw) != VK_SUCCESS) {
					throw new RuntimeException("Failed to create fence");
				}
				if(vkCreateFence(renderDevice.device,createFence,null,submit_wait_fences_transfer) != VK_SUCCESS) {
					throw new RuntimeException("Failed to create fence");
				}
			}
			submit_wait_fences_draw.position(0);
			submit_wait_fences_transfer.position(0);
		}
	}

	void destroySwapChainSynchronisation() {
		for(int i = 0; i < swapChainImageViews.capacity(); i++) {
			vkDestroyFence(renderDevice.device,submit_wait_fences_draw.get(i),null);
			vkDestroyFence(renderDevice.device,submit_wait_fences_transfer.get(i),null);
		}
		MemoryUtil.memFree(submit_wait_fences_draw);
		MemoryUtil.memFree(submit_wait_fences_transfer);
	}

	void destroySynchronisation() {
		vkDestroySemaphore(renderDevice.device,semaphore_image_available,null);
		vkDestroySemaphore(renderDevice.device,semaphore_render_finished, null);
		vkDestroySemaphore(renderDevice.device,semaphore_gui_data_updated,null);
	}

	void initFrameBuffers() {
		worldRenderManager.initFrameBuffers();
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
		worldRenderManager.initRenderTargets();
	}

	void destroyFrameBuffers() {
		worldRenderManager.destroyRenderTargets();
		worldRenderManager.destroyFrameBuffers();
		for(int i = 0; i < targetFrameBuffers.capacity(); i++) {
			vkDestroyFramebuffer(renderDevice.device, targetFrameBuffers.get(i),null);
		}
		MemoryUtil.memFree(targetFrameBuffers);
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
			command_buffers_gui_transfer = MemoryUtil.memAllocPointer(mainCommandBufferCount);
			allocateInfo.commandPool(command_pool_transfer);
			if(vkAllocateCommandBuffers(renderDevice.device,allocateInfo,command_buffers_gui_transfer) != VK_SUCCESS) {
				throw new RuntimeException("Failed to allocate transfer GUI command buffers");
			}
			command_buffers_gui = MemoryUtil.memAllocPointer(mainCommandBufferCount);
			allocateInfo.commandPool(command_pool_graphics);
			allocateInfo.level(VK_COMMAND_BUFFER_LEVEL_SECONDARY);
			if(vkAllocateCommandBuffers(renderDevice.device,allocateInfo,command_buffers_gui) != VK_SUCCESS) {
				throw new RuntimeException("Failed to allocated Secondary GUI Draw Command Buffer Bit");
			}
		}
	}

	void destroyCommandBuffers() {

	}

	void destroyCommandPools() {
		vkDestroyCommandPool(renderDevice.device,command_pool_graphics,null);
		vkDestroyCommandPool(renderDevice.device,command_pool_transfer,null);
		MemoryUtil.memFree(command_buffers_main);
		MemoryUtil.memFree(command_buffers_gui);
		MemoryUtil.memFree(command_buffers_gui_transfer);
	}

	void initMemory() {
		memoryMgr.initStandardMemory();
	}

	void recreateMemory() {
		memoryMgr.recreateStandardMemory();
	}

	void destroyMemory() {
		memoryMgr.clearStandardMemory();
	}

	void initRenderPasses() {
		renderPass.init(renderDevice.device,renderConfig,chosenImageFormat);
	}

	void destroyRenderPasses() {
		renderPass.cleanup(renderDevice.device);
	}

	void initGraphicsPipeline() {
		List<String> defines = List.of("DEBUG_VULKAN");
		guiPipeline.init(renderDevice.device,renderPass.render_pass,0,0, defines);
		worldRenderManager.initPipelines(defines);
	}

	void destroyPipelineAndLayout() {
		guiPipeline.destroy(renderDevice.device);
		worldRenderManager.destroyPipelines();
	}


}
