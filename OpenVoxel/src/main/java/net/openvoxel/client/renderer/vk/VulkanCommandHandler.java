package net.openvoxel.client.renderer.vk;

import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;
import net.openvoxel.OpenVoxel;
import net.openvoxel.client.renderer.vk.core.VulkanDevice;
import net.openvoxel.client.renderer.vk.core.VulkanMemory;
import net.openvoxel.client.renderer.vk.core.VulkanState;
import net.openvoxel.client.renderer.vk.core.VulkanUtility;
import net.openvoxel.client.renderer.vk.pipeline.VulkanRenderPass;
import net.openvoxel.utility.CrashReport;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

/**
 * Vulkan: Manage Command Buffer submission & threading
 *       : Also Managed Render Pass Images & Attachments
 */
public final class VulkanCommandHandler {

	//Current Frame Index:
	private int currentFrameIndex = 0;

	//Reference:
	private final VulkanState state;
	private final VulkanMemory memory;
	private final VulkanDevice device;
	private final VulkanCache cache;

	//Main Draw Depth...
	private long DepthImageMemory = VK_NULL_HANDLE;
	private long DepthImage = VK_NULL_HANDLE;
	private long DepthImageView = VK_NULL_HANDLE;

	//Frame Buffers...
	private TLongList FrameBuffers_ForwardOnly;

	//Command Pools...
	private long commandPoolMainThread = VK_NULL_HANDLE;
	private long commandPoolGuiAsync = VK_NULL_HANDLE;
	private long commandPoolTransfer = VK_NULL_HANDLE;
	private TLongList commandPoolsAsync;
	private TLongList commandPoolsTransferAsync;

	//Command Buffers...
	private List<VkCommandBuffer> commandBuffersMainThread;
	private List<VkCommandBuffer> commandBuffersGuiAsync;
	private List<VkCommandBuffer> commandBuffersTransfer;
	//NB: idx = pool*swapSize + swapImage
	private List<VkCommandBuffer> commandBuffersAsync;
	private List<VkCommandBuffer> commandBuffersTransferAsync;

	//Synchronisation
	private TLongList MainThreadFenceList;
	private TLongList TransferFenceList;

	private long MainThreadAcquireFence;
	private long MainThreadAcquireSemaphore;

	private TLongList MainTransferSemaphores;
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

		commandPoolsAsync = new TLongArrayList();
		commandPoolsTransferAsync = new TLongArrayList();

		commandBuffersMainThread = new ArrayList<>();
		commandBuffersGuiAsync = new ArrayList<>();
		commandBuffersTransfer = new ArrayList<>();
		commandBuffersAsync = new ArrayList<>();
		commandBuffersTransferAsync = new ArrayList<>();

		MainThreadFenceList = new TLongArrayList();
		TransferFenceList = new TLongArrayList();
		MainThreadAcquireSemaphore = VK_NULL_HANDLE;
		MainTransferSemaphores = new TLongArrayList();
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

	private void initCommandBuffers(int swapSize,int asyncPoolSize) {
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

			commandPoolCreate.flags(
					VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT |
					VK_COMMAND_POOL_CREATE_TRANSIENT_BIT
			);
			commandPoolCreate.queueFamilyIndex(device.familyTransfer);

			vkResult = vkCreateCommandPool(device.logicalDevice,commandPoolCreate,null,pResult);
			if(vkResult == VK_SUCCESS) {
				commandPoolTransfer = pResult.get(0);
			}else{
				//No Memory
				VulkanUtility.CrashOnBadResult("Failed to create Command Pool[Transfer]",vkResult);
			}

			commandPoolCreate.queueFamilyIndex(device.familyQueue);

			for(int pool = 0; pool < asyncPoolSize; pool++) {
				vkResult = vkCreateCommandPool(device.logicalDevice,commandPoolCreate,null,pResult);
				if(vkResult == VK_SUCCESS) {
					commandPoolsAsync.add(pResult.get(0));
				}else{
					//No Memory
					VulkanUtility.CrashOnBadResult("Failed to create Command Pool[Async-"+pool+"]",vkResult);
				}
			}

			commandPoolCreate.queueFamilyIndex(device.familyTransfer);

			for(int pool = 0; pool < asyncPoolSize; pool++) {
				vkResult = vkCreateCommandPool(device.logicalDevice,commandPoolCreate,null,pResult);
				if(vkResult == VK_SUCCESS) {
					commandPoolsTransferAsync.add(pResult.get(0));
				}else{
					//No Memory
					VulkanUtility.CrashOnBadResult("Failed to create Command Pool[Transfer Async-"+pool+"]",vkResult);
				}
			}

			////////////////////////////////////////////////////////////////////////////////////////////////

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

			commandAllocateInfo.commandPool(commandPoolTransfer);
			commandAllocateInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);

			vkResult = vkAllocateCommandBuffers(device.logicalDevice,commandAllocateInfo,bufferResult);
			if(vkResult == VK_SUCCESS) {
				for(int i = 0; i < swapSize; i++) {
					commandBuffersTransfer.add(new VkCommandBuffer(bufferResult.get(i),device.logicalDevice));
				}
			}else{
				//No Memory
				VulkanUtility.CrashOnBadResult("Failed to allocate Command Buffers[Transfer]",vkResult);
			}

			commandAllocateInfo.level(VK_COMMAND_BUFFER_LEVEL_SECONDARY);

			for(int pool = 0; pool < asyncPoolSize; pool++) {
				commandAllocateInfo.commandPool(commandPoolsAsync.get(pool));

				vkResult = vkAllocateCommandBuffers(device.logicalDevice,commandAllocateInfo,bufferResult);
				if(vkResult == VK_SUCCESS) {
					for (int i = 0; i < swapSize; i++) {
						commandBuffersAsync.add(new VkCommandBuffer(bufferResult.get(i),device.logicalDevice));
					}
				}else{
					//No Memory
					VulkanUtility.CrashOnBadResult("Failed to allocate Command Buffers[Async..]",vkResult);
				}

				commandAllocateInfo.commandPool(commandPoolsTransferAsync.get(pool));

				vkResult = vkAllocateCommandBuffers(device.logicalDevice,commandAllocateInfo,bufferResult);
				if(vkResult == VK_SUCCESS) {
					for(int i = 0; i < swapSize; i++) {
						commandBuffersTransferAsync.add(new VkCommandBuffer(bufferResult.get(i),device.logicalDevice));
					}
				}else{
					//No Memory
					VulkanUtility.CrashOnBadResult("Failed to allocate Command Buffers[Async..]",vkResult);
				}
			}
		}
	}

	private void destroyCommandBuffers() {
		vkDestroyCommandPool(device.logicalDevice,commandPoolGuiAsync,null);
		vkDestroyCommandPool(device.logicalDevice,commandPoolMainThread,null);
		vkDestroyCommandPool(device.logicalDevice,commandPoolTransfer,null);

		for(int i = 0; i < commandPoolsAsync.size(); i++) {
			vkDestroyCommandPool(device.logicalDevice,commandPoolsAsync.get(i),null);
			vkDestroyCommandPool(device.logicalDevice,commandPoolsTransferAsync.get(i),null);
		}
		commandPoolsAsync.clear();
		commandPoolsTransferAsync.clear();

		//Clear Command Buffers:
		commandBuffersMainThread.clear();
		commandBuffersGuiAsync.clear();
		commandBuffersTransfer.clear();
		commandBuffersAsync.clear();
		commandBuffersTransferAsync.clear();
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

			for(int i = 0; i < swapSize; i++) {
				int vkResult = vkCreateFence(device.logicalDevice,fenceCreateInfo,null,pResult);
				if(vkResult == VK_SUCCESS) {
					TransferFenceList.add(pResult.get(0));
				}else{
					//Out of Memory
					VulkanUtility.CrashOnBadResult("Failed to create Fence[Transfer]",vkResult);
				}
			}

			{
				int vkResult = vkCreateFence(device.logicalDevice,fenceCreateInfo,null,pResult);
				if(vkResult == VK_SUCCESS) {
					MainThreadAcquireFence = pResult.get(0);
				}else{
					//Out of Memory
					VulkanUtility.CrashOnBadResult("Failed to create acquire fence",vkResult);
				}
			}

			VkSemaphoreCreateInfo semaphoreCreateInfo = VkSemaphoreCreateInfo.mallocStack(stack);
			semaphoreCreateInfo.sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);
			semaphoreCreateInfo.pNext(VK_NULL_HANDLE);
			semaphoreCreateInfo.flags(0);

			{
				int vkResult = vkCreateSemaphore(device.logicalDevice, semaphoreCreateInfo, null, pResult);
				if (vkResult == VK_SUCCESS) {
					MainThreadAcquireSemaphore = pResult.get(0);
				} else {
					//Out of Memory
					VulkanUtility.CrashOnBadResult("Failed to create Semaphore[Acquire Image]", vkResult);
				}
			}

			for(int i = 0; i < swapSize; i++) {
				int vkResult = vkCreateSemaphore(device.logicalDevice,semaphoreCreateInfo,null,pResult);
				if(vkResult == VK_SUCCESS) {
					MainTransferSemaphores.add(pResult.get(0));
				}else{
					//Out of Memory
					VulkanUtility.CrashOnBadResult("Failed to create Semaphore[Transfer]",vkResult);
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

		for(int i = 0; i < TransferFenceList.size(); i++) {
			vkDestroyFence(device.logicalDevice,TransferFenceList.get(i),null);
		}
		TransferFenceList.clear();

		vkDestroyFence(device.logicalDevice,MainThreadAcquireFence,null);

		vkDestroySemaphore(device.logicalDevice,MainThreadAcquireSemaphore,null);

		for(int i = 0; i < MainTransferSemaphores.size(); i++) {
			vkDestroySemaphore(device.logicalDevice,MainTransferSemaphores.get(i),null);
		}
		MainTransferSemaphores.clear();

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

	void init(int asyncCount) {
		initResizeable();
		initCommandBuffers(state.VulkanSwapChainSize,asyncCount);
		initSynchronisation(state.VulkanSwapChainSize);
	}

	void reload() {
		//WaitForFence(MainThreadAcquireFence,50*1000*1000);
		destroySynchronisation();
		destroyResizeable();
		initResizeable();
		initSynchronisation(state.VulkanSwapChainSize);
	}

	void close() {
		WaitForFence(MainThreadAcquireFence,50*1000*1000);
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

	//TODO: GET OTHER FRAME BUFFER TYPES


	////////////////////////////////////////////////////////////////////////////////////////////////////////

	public VkCommandBuffer getSingleUseCommandBuffer() {
		return null;
	}

	public void SubmitSingleUseCommandBuffer() {
		//TODO:
	}

	private void WaitForFence(long fence,long timeout) {
		//TODO: REMOVE DEBUG TIMEOUT
		long _time1 = System.currentTimeMillis();
		int result = vkWaitForFences(device.logicalDevice, fence, true, timeout);
		long _time2 = System.currentTimeMillis();
		long _delta = _time2 - _time1;
		if(_delta > timeout / 2 || result == VK_TIMEOUT) {
			if (fence == MainThreadAcquireFence) {
				VulkanUtility.LogDebug("Acquire Fence: " + _delta);
			} else if (MainThreadFenceList.contains(fence)) {
				VulkanUtility.LogDebug("Main Fence: " + _delta);
			} else {
				VulkanUtility.LogDebug("Transfer Fence: " + _delta);
			}
		}
		if (result == VK_TIMEOUT) {
			//Fallback Fence Timeout...
			VulkanUtility.LogWarn("Fence Timed-out!!!");
			vkDeviceWaitIdle(device.logicalDevice);
			result = vkWaitForFences(device.logicalDevice,fence,true,100);
			if(result  != VK_SUCCESS) {
				VulkanUtility.CrashOnBadResult("Fence still in use after vkDeviceWaitIdle(...)",result);
			}
		} else if (result != VK_SUCCESS) {
			//No Memory
			VulkanUtility.CrashOnBadResult("Failed to wait for fence",result);
		}
		result = vkResetFences(device.logicalDevice, fence);
		if(result != VK_SUCCESS) {
			//No Memory
			VulkanUtility.CrashOnBadResult("Failed to reset fence",result);
		}
	}

	/*
	 * Wait till This Frames Graphics Queue is Valid
	 */
	void AwaitGraphicsFence(long timeout) {
		WaitForFence(MainThreadFenceList.get(currentFrameIndex), timeout);
	}

	/*
	 * Wait till This Frames Transfer Queue is Valid
	 */
	void AwaitTransferFence(long timeout) {
		WaitForFence(TransferFenceList.get(currentFrameIndex),timeout);
	}

/*
	TODO: REMOVE IF UNEEDED - DOESNT SEEM TO FIX THE ISSUE
	void TESTING_0(long timeout) {
		WaitForFence(MainThreadAcquireFence,timeout);
	}

	void TESTING_1() {
		vkDestroyFence(device.logicalDevice,MainThreadAcquireFence,null);
		MainThreadAcquireFence = VK_NULL_HANDLE;
		try(MemoryStack stack = stackPush()) {
			VkFenceCreateInfo fenceCreate = VkFenceCreateInfo.mallocStack(stack);
			fenceCreate.sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO);
			fenceCreate.pNext(VK_NULL_HANDLE);
			fenceCreate.flags(VK_FENCE_CREATE_SIGNALED_BIT);
			LongBuffer pReturn = stack.mallocLong(1);
			int vk_result = vkCreateFence(device.logicalDevice,fenceCreate,null,pReturn);
			MainThreadAcquireFence = pReturn.get(0);
			if(vk_result != VK_SUCCESS) {
				VulkanUtility.CrashOnBadResult("Failed to recreate acquire fence",vk_result);
			}
		}
	}
*/


	/**
	 * @return if the acquire succeeded, otherwise the swap-chain needs recreating
	 */
	boolean AcquireNextImage(long timeout) {
		try(MemoryStack stack = stackPush()) {
			WaitForFence(MainThreadAcquireFence, timeout);
			IntBuffer pImageIndex = stack.mallocInt(1);
			int vkResult = KHRSwapchain.vkAcquireNextImageKHR(
					device.logicalDevice,
					state.VulkanSwapChain,
					timeout,
					MainThreadAcquireSemaphore,
					MainThreadAcquireFence,
					pImageIndex
			);
			currentFrameIndex = pImageIndex.get(0);
			if (vkResult != VK_SUCCESS) {
				if (vkResult == VK_SUBOPTIMAL_KHR) {
					VulkanUtility.LogWarn("Sub-Optimal Swap-Chain");
					return true;
				}else if(vkResult == VK_ERROR_OUT_OF_DATE_KHR) {
					VulkanUtility.LogWarn("Out-Of-Date Swap-Chain");
					return false;
				}else{
					VulkanUtility.CrashOnBadResult("Failed to acquire swap-chain",vkResult);
					return false;
				}
			}else{
				return true;
			}
		}
	}

	/**
	 * @return if the present succeeded, otherwise the swap-chain needs recreating
	 */
	boolean PresentImage() {
		try(MemoryStack stack = stackPush()) {
			VkPresentInfoKHR presentInfo = VkPresentInfoKHR.mallocStack(stack);
			presentInfo.sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR);
			presentInfo.pNext(VK_NULL_HANDLE);
			presentInfo.pWaitSemaphores(stack.longs(
					MainThreadPresentSemaphores.get(currentFrameIndex)
			));
			presentInfo.swapchainCount(1);
			presentInfo.pSwapchains(stack.longs(
					state.VulkanSwapChain
			));
			presentInfo.pImageIndices(stack.ints(
					currentFrameIndex
			));
			presentInfo.pResults(null);

			int vkResult = vkQueuePresentKHR(device.allQueue,presentInfo);
			if(vkResult == VK_ERROR_OUT_OF_DATE_KHR) {
				VulkanUtility.LogWarn("Out of Date Present");
				return false;
			}else if(vkResult == VK_SUBOPTIMAL_KHR) {
				VulkanUtility.LogWarn("Sub Optimal Present");
				return true;
			}else if(vkResult != VK_SUCCESS) {
				VulkanUtility.CrashOnBadResult("Failed to present image",vkResult);
				return false;
			}else {
				return true;
			}
		}
	}

	/*
	 * Submit This Frames Command Queue
	 */
	void SubmitCommandGraphics(VkCommandBuffer submit) {
		try(MemoryStack stack = stackPush()) {
			VkSubmitInfo.Buffer submitInfo = VkSubmitInfo.mallocStack(1,stack);
			submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);
			submitInfo.pNext(VK_NULL_HANDLE);
			submitInfo.waitSemaphoreCount(2);
			submitInfo.pWaitSemaphores(stack.longs(
					MainThreadAcquireSemaphore,
					MainTransferSemaphores.get(currentFrameIndex)
			));
			submitInfo.pWaitDstStageMask(stack.ints(
					VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT,
					VK_PIPELINE_STAGE_DRAW_INDIRECT_BIT
			));
			submitInfo.pCommandBuffers(stack.pointers(
					submit
			));
			submitInfo.pSignalSemaphores(stack.longs(
					MainThreadPresentSemaphores.get(currentFrameIndex)
			));
			vkQueueSubmit(device.allQueue,submitInfo,MainThreadFenceList.get(currentFrameIndex));
		}
	}

	/*
	 * Submit This Frames Transfer Queue
	 */
	void SubmitCommandTransfer(VkCommandBuffer submit) {
		try(MemoryStack stack = stackPush()) {
			VkSubmitInfo.Buffer submitInfo = VkSubmitInfo.mallocStack(1,stack);
			submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);
			submitInfo.pNext(VK_NULL_HANDLE);
			submitInfo.waitSemaphoreCount(0);
			submitInfo.pWaitSemaphores(null);
			submitInfo.pWaitDstStageMask(null);
			submitInfo.pCommandBuffers(stack.pointers(
					submit
			));
			submitInfo.pSignalSemaphores(stack.longs(
					MainTransferSemaphores.get(currentFrameIndex)
			));
			vkQueueSubmit(device.transferQueue,submitInfo,TransferFenceList.get(currentFrameIndex));
		}
	}

	////////////////////////////////////////////////////////////


	/**
	 * @return The command buffer for the entire draw call
	 */
	VkCommandBuffer getMainDrawCommandBuffer() {
		return commandBuffersMainThread.get(currentFrameIndex);
	}

	/**
	 * @return The command buffer for the async GUI Draw call
	 */
	public VkCommandBuffer getGuiDrawCommandBuffer() {
		return commandBuffersGuiAsync.get(currentFrameIndex);
	}

	/**
	 * @return The command buffer for the async Data Transfer
	 */
	VkCommandBuffer getTransferCommandBuffer() {
		return commandBuffersTransfer.get(currentFrameIndex);
	}

	/**
	 * @return The command buffer for the poolID'th Async Task
	 */
	public VkCommandBuffer getAsyncMainCommandBuffer(int poolID) {
		int idx = (poolID * state.VulkanSwapChainSize) + currentFrameIndex;
		return commandBuffersAsync.get(idx);
	}

	public VkCommandBuffer getAsyncTransferCommandBuffer(int poolID) {
		int idx = (poolID * state.VulkanSwapChainSize) + currentFrameIndex;
		return commandBuffersTransferAsync.get(idx);
	}

}
