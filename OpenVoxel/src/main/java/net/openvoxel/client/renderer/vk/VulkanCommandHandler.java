package net.openvoxel.client.renderer.vk;

import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;
import net.openvoxel.client.renderer.vk.core.VulkanDevice;
import net.openvoxel.client.renderer.vk.core.VulkanMemory;
import net.openvoxel.client.renderer.vk.core.VulkanState;
import net.openvoxel.client.renderer.vk.core.VulkanUtility;
import net.openvoxel.client.renderer.vk.pipeline.VulkanRenderPass;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

/**
 * Vulkan: Manage Command Buffer submission & threading
 *       : Also Managed Render Pass Images & Attachments
 */
public class VulkanCommandHandler {

	//Current Frame Index:
	private int currentFrameIndex = 0;

	//Reference:
	private final VulkanState state;
	private final VulkanMemory memory;
	private final VulkanDevice device;
	private final VulkanCache cache;

	//Main Draw Depth...
	private long DepthImageMemory = VK_NULL_HANDLE;
	private  long DepthImage = VK_NULL_HANDLE;
	private  long DepthImageView = VK_NULL_HANDLE;

	//Frame Buffers...
	private TLongList FrameBuffers_ForwardOnly;

	//Command Pools...
	private long commandPoolMainThread = VK_NULL_HANDLE;
	private long commandPoolGuiAsync = VK_NULL_HANDLE;

	//Command Buffers...
	private List<VkCommandBuffer> commandBuffersMainThread;
	private List<VkCommandBuffer> commandBuffersGuiAsync;

	//Synchronisation
	private TLongList MainThreadFenceList;
	private TLongList MainThreadAcquireSemaphores;
	private TLongList MainThreadPresentSemaphores;

	VulkanCommandHandler(VulkanState state,VulkanCache cache) {
		this.state = state;
		this.memory = state.VulkanMemory;
		this.device = state.VulkanDevice;
		this.cache = cache;
		if(!VulkanRenderPass.formatInit) {
			throw new RuntimeException("Formats have not been initialized");
		}
		FrameBuffers_ForwardOnly = new TLongArrayList();
		commandBuffersMainThread = new ArrayList<>();
		commandBuffersGuiAsync = new ArrayList<>();
		MainThreadFenceList = new TLongArrayList();
		MainThreadAcquireSemaphores = new TLongArrayList();
		MainThreadPresentSemaphores = new TLongArrayList();
	}

	private void initImages(int width,int height) {
		try(MemoryStack stack = stackPush()) {
			//Depth Buffer...
			VkImageCreateInfo imageCreateInfo = VkImageCreateInfo.mallocStack(stack);
			imageCreateInfo.sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO);
			imageCreateInfo.pNext(VK_NULL_HANDLE);
			imageCreateInfo.flags(0);
			imageCreateInfo.imageType(VK_IMAGE_TYPE_2D);
			imageCreateInfo.format(VulkanRenderPass.formatSimpleDepth);
			imageCreateInfo.extent().set(width,height,1);
			imageCreateInfo.mipLevels(1);
			imageCreateInfo.arrayLayers(1);
			imageCreateInfo.samples(VK_SAMPLE_COUNT_1_BIT);
			imageCreateInfo.tiling(VK_IMAGE_TILING_OPTIMAL);
			imageCreateInfo.usage(VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT);
			imageCreateInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE);
			imageCreateInfo.pQueueFamilyIndices(null);
			imageCreateInfo.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);

			LongBuffer pReturn = stack.mallocLong(1);
			int vkResult = vkCreateImage(device.logicalDevice,imageCreateInfo,null,pReturn);
			if(vkResult == VK_SUCCESS) {
				DepthImage = pReturn.get(0);
			}else{
				//Not enough memory
				VulkanUtility.CrashOnBadResult("Failed to create depth-image",vkResult);
			}
			DepthImageMemory = memory.allocateDedicatedImage(DepthImage,VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);
			vkResult = vkBindImageMemory(device.logicalDevice,DepthImage,DepthImageMemory,0);
			if(vkResult != VK_SUCCESS) {
				//Not enough memory
				VulkanUtility.CrashOnBadResult("Failed to bind image-memory",vkResult);
			}

			VkImageViewCreateInfo imageViewCreateInfo = VkImageViewCreateInfo.mallocStack(stack);
			imageViewCreateInfo.sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO);
			imageViewCreateInfo.pNext(VK_NULL_HANDLE);
			imageViewCreateInfo.flags(0);
			imageViewCreateInfo.image(DepthImage);
			imageViewCreateInfo.viewType(VK_IMAGE_VIEW_TYPE_2D);
			imageViewCreateInfo.format(VulkanRenderPass.formatSimpleDepth);
			imageViewCreateInfo.components().set(
					VK_COMPONENT_SWIZZLE_IDENTITY,
					VK_COMPONENT_SWIZZLE_IDENTITY,
					VK_COMPONENT_SWIZZLE_IDENTITY,
					VK_COMPONENT_SWIZZLE_IDENTITY
			);
			imageViewCreateInfo.subresourceRange().set(
					VK_IMAGE_ASPECT_DEPTH_BIT,
					0,
					1,
					0,
					1
			);

			vkResult = vkCreateImageView(device.logicalDevice,imageViewCreateInfo,null,pReturn);
			if(vkResult == VK_SUCCESS) {
				DepthImageView = pReturn.get(0);
			}else{
				//Not enough memory
				VulkanUtility.CrashOnBadResult("Failed to create depth-image-view",vkResult);
			}
		}
	}

	private void destroyImages() {
		if(DepthImageView != VK_NULL_HANDLE) {
			vkDestroyImageView(device.logicalDevice, DepthImageView, null);
			DepthImageView = VK_NULL_HANDLE;
		}
		if(DepthImage != VK_NULL_HANDLE) {
			vkDestroyImage(device.logicalDevice, DepthImage, null);
			DepthImage = VK_NULL_HANDLE;
		}
		if(DepthImageMemory != VK_NULL_HANDLE) {
			memory.freeDedicatedMemory(DepthImageMemory);
			DepthImageMemory = VK_NULL_HANDLE;
		}
	}

	private void initFrameBuffers(int width,int height,int imageCount) {
		try(MemoryStack stack = stackPush()) {
			LongBuffer attachments = stack.mallocLong(2);
			attachments.put(0,VK_NULL_HANDLE);
			attachments.put(1,DepthImageView);

			VkFramebufferCreateInfo frameCreateInfo = VkFramebufferCreateInfo.mallocStack(stack);
			frameCreateInfo.sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO);
			frameCreateInfo.pNext(VK_NULL_HANDLE);
			frameCreateInfo.flags(0);
			frameCreateInfo.renderPass(cache.RENDER_PASS_FORWARD_ONLY.RenderPass);
			frameCreateInfo.pAttachments(attachments);
			frameCreateInfo.width(width);
			frameCreateInfo.height(height);
			frameCreateInfo.layers(1);

			LongBuffer pReturn = stack.mallocLong(1);
			for(int i = 0; i < imageCount; i++) {
				attachments.put(0,state.VulkanSwapChainImageViews.get(i));
				int vkResult = vkCreateFramebuffer(device.logicalDevice,frameCreateInfo,null,pReturn);
				if(vkResult == VK_SUCCESS) {
					FrameBuffers_ForwardOnly.add(pReturn.get(0));
				}else{
					//No Memory
					VulkanUtility.CrashOnBadResult("Failed to create FrameBuffers[Forward Only]",vkResult);
				}
			}
		}
	}

	private void destroyFrameBuffers() {
		for(int i = 0; i < FrameBuffers_ForwardOnly.size(); i++) {
			vkDestroyFramebuffer(device.logicalDevice,FrameBuffers_ForwardOnly.get(i),null);
		}
		FrameBuffers_ForwardOnly.clear();
	}

	private void initCommandBuffers(int swapSize) {
		try(MemoryStack stack = stackPush()) {
			VkCommandPoolCreateInfo commandPoolCreate = VkCommandPoolCreateInfo.mallocStack(stack);
			commandPoolCreate.sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO);
			commandPoolCreate.pNext(VK_NULL_HANDLE);
			commandPoolCreate.flags(
					VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT |
					VK_COMMAND_POOL_CREATE_TRANSIENT_BIT
			);
			commandPoolCreate.queueFamilyIndex(device.familyQueue);

			LongBuffer pResult = stack.mallocLong(1);
			int vkResult = vkCreateCommandPool(device.logicalDevice,commandPoolCreate,null,pResult);
			if(vkResult == VK_SUCCESS) {
				commandPoolMainThread = pResult.get(0);
			}else{
				//No Memory
				VulkanUtility.CrashOnBadResult("Failed to create Command Pool[Main Thread]",vkResult);
			}

			commandPoolCreate.flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT);

			vkResult = vkCreateCommandPool(device.logicalDevice,commandPoolCreate,null,pResult);
			if(vkResult == VK_SUCCESS) {
				commandPoolGuiAsync = pResult.get(0);
			}else{
				//No Memory
				VulkanUtility.CrashOnBadResult("Failed to create Command Pool[GUI Async]",vkResult);
			}

			VkCommandBufferAllocateInfo commandAllocateInfo = VkCommandBufferAllocateInfo.mallocStack(stack);
			commandAllocateInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
			commandAllocateInfo.pNext(VK_NULL_HANDLE);
			commandAllocateInfo.commandPool(commandPoolMainThread);
			commandAllocateInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);
			commandAllocateInfo.commandBufferCount(swapSize);

			PointerBuffer bufferResult = stack.mallocPointer(swapSize);
			vkResult = vkAllocateCommandBuffers(device.logicalDevice,commandAllocateInfo,bufferResult);
			if(vkResult == VK_SUCCESS) {
				for(int i = 0; i < swapSize; i++) {
					commandBuffersMainThread.add(new VkCommandBuffer(bufferResult.get(i),device.logicalDevice));
				}
			}else{
				//No Memory
				VulkanUtility.CrashOnBadResult("Failed to allocate Command Buffers[GUI Async]",vkResult);
			}

			commandAllocateInfo.commandPool(commandPoolGuiAsync);
			commandAllocateInfo.level(VK_COMMAND_BUFFER_LEVEL_SECONDARY);

			vkResult = vkAllocateCommandBuffers(device.logicalDevice,commandAllocateInfo,bufferResult);
			if(vkResult == VK_SUCCESS) {
				for(int i = 0; i < swapSize; i++) {
					commandBuffersGuiAsync.add(new VkCommandBuffer(bufferResult.get(i),device.logicalDevice));
				}
			}else{
				//No Memory
				VulkanUtility.CrashOnBadResult("Failed to allocate Command Buffers[GUI Async]",vkResult);
			}
		}
	}

	private void destroyCommandBuffers() {
		vkDestroyCommandPool(device.logicalDevice,commandPoolGuiAsync,null);
		vkDestroyCommandPool(device.logicalDevice,commandPoolMainThread,null);
		commandBuffersMainThread.clear();
		commandBuffersGuiAsync.clear();
	}

	private void initSynchronisation(int swapSize) {
		try(MemoryStack stack = stackPush()) {
			VkFenceCreateInfo fenceCreateInfo = VkFenceCreateInfo.mallocStack(stack);
			fenceCreateInfo.sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO);
			fenceCreateInfo.pNext(VK_NULL_HANDLE);
			fenceCreateInfo.flags(VK_FENCE_CREATE_SIGNALED_BIT);

			LongBuffer pResult = stack.mallocLong(1);
			for(int i = 0; i < swapSize; i++) {
				int vkResult = vkCreateFence(device.logicalDevice, fenceCreateInfo, null, pResult);
				if(vkResult == VK_SUCCESS) {
					MainThreadFenceList.add(pResult.get(0));
				}else{
					//Out of Memory
					VulkanUtility.CrashOnBadResult("Failed to create Fence[Main]",vkResult);
				}
			}

			VkSemaphoreCreateInfo semaphoreCreateInfo = VkSemaphoreCreateInfo.mallocStack(stack);
			semaphoreCreateInfo.sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);
			semaphoreCreateInfo.pNext(VK_NULL_HANDLE);
			semaphoreCreateInfo.flags(0);

			for(int i = 0; i < swapSize; i++) {
				int vkResult = vkCreateSemaphore(device.logicalDevice,semaphoreCreateInfo,null,pResult);
				if(vkResult == VK_SUCCESS) {
					MainThreadAcquireSemaphores.add(pResult.get(0));
				}else{
					//Out of Memory
					VulkanUtility.CrashOnBadResult("Failed to create Semaphore[Acquire Image]",vkResult);
				}
			}

			for(int i = 0; i < swapSize; i++) {
				int vkResult = vkCreateSemaphore(device.logicalDevice,semaphoreCreateInfo,null,pResult);
				if(vkResult == VK_SUCCESS) {
					MainThreadPresentSemaphores.add(pResult.get(0));
				}else{
					//Out of Memory
					VulkanUtility.CrashOnBadResult("Failed to create Semaphore[Acquire Image]",vkResult);
				}
			}
		}
	}

	private void destroySynchronisation() {
		for(int i = 0; i < MainThreadFenceList.size(); i++) {
			vkDestroyFence(device.logicalDevice,MainThreadFenceList.get(i),null);
		}
		MainThreadFenceList.clear();

		for(int i = 0; i < MainThreadAcquireSemaphores.size(); i++) {
			vkDestroySemaphore(device.logicalDevice,MainThreadAcquireSemaphores.get(i),null);
		}
		MainThreadAcquireSemaphores.clear();

		for(int i = 0; i < MainThreadPresentSemaphores.size(); i++) {
			vkDestroySemaphore(device.logicalDevice,MainThreadPresentSemaphores.get(i),null);
		}
		MainThreadPresentSemaphores.clear();
	}

	///////////////////////
	/// Control Methods ///
	///////////////////////

	private void initResizeable() {
		int width = state.chosenSwapExtent.width();
		int height = state.chosenSwapExtent.height();
		int imageCount = state.VulkanSwapChainSize;
		initImages(width,height);
		initFrameBuffers(width,height,imageCount);
	}

	private void destroyResizeable() {
		destroyFrameBuffers();
		destroyImages();
	}

	void init() {
		initResizeable();
		initCommandBuffers(state.VulkanSwapChainSize);
		initSynchronisation(state.VulkanSwapChainSize);
	}

	void reload() {
		destroyResizeable();
		initResizeable();
	}

	void close() {
		destroySynchronisation();
		destroyCommandBuffers();
		destroyResizeable();
	}


	///////////////////
	/// API Methods ///
	///////////////////

	public long getFrameBuffer_ForwardOnly() {
		return FrameBuffers_ForwardOnly.get(currentFrameIndex);
	}

	public VkCommandBuffer getSingleUseCommandBuffer() {
		return null;
	}

	public void SubmitSingleUseCommandBuffer() {
		//TODO:
	}

	/**
	 * @return The command buffer for the entire draw call
	 */
	public VkCommandBuffer getMainDrawCommandBuffer() {
		return commandBuffersMainThread.get(currentFrameIndex);
	}

	/**
	 * @return The command buffer for the async GUI Draw call
	 */
	public VkCommandBuffer getGuiDrawCommandBuffer() {
		return commandBuffersGuiAsync.get(currentFrameIndex);
	}

	/**
	 * @return The command buffer for the async GUI Image Transfer
	 */
	public VkCommandBuffer getGuiTransferCommandBuffer() {
		return null;
	}

	public void setCurrentFrameIndex(int idx) {
		currentFrameIndex = idx;
	}
}
