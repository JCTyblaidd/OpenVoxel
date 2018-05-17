package net.openvoxel.client.renderer.vk;

import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;
import net.openvoxel.client.STBITexture;
import net.openvoxel.client.renderer.vk.core.VulkanDevice;
import net.openvoxel.client.renderer.vk.core.VulkanMemory;
import net.openvoxel.client.renderer.vk.core.VulkanState;
import net.openvoxel.client.renderer.vk.core.VulkanUtility;
import net.openvoxel.client.renderer.vk.pipeline.VulkanRenderPass;
import net.openvoxel.statistics.SystemStatistics;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.CallbackI;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
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

	//Query Pool N x [start_graphics,start_gui,end_gui], -> 3N
	private long queryPool;
	private long lastTimestampGraphics = 0;
	private long queryGetDelay = 0;

	//Reference:
	private final VulkanState state;
	private final VulkanMemory memory;
	private final VulkanDevice device;
	private final VulkanCache cache;

	//Staging Buffer for Single Use...
	private long StagingBuffer;
	private long StagingBufferMemory;

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

			PointerBuffer bufferResult = stack.mallocPointer(swapSize * 2);
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
			commandAllocateInfo.commandBufferCount(swapSize * 2);

			vkResult = vkAllocateCommandBuffers(device.logicalDevice,commandAllocateInfo,bufferResult);
			if(vkResult == VK_SUCCESS) {
				for(int i = 0; i < swapSize*2; i++) {
					commandBuffersGuiAsync.add(new VkCommandBuffer(bufferResult.get(i),device.logicalDevice));
				}
			}else{
				//No Memory
				VulkanUtility.CrashOnBadResult("Failed to allocate Command Buffers[GUI Async]",vkResult);
			}

			commandAllocateInfo.commandBufferCount(swapSize);
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

	private void initStaging() {
		try(MemoryStack stack = stackPush()) {
			VkBufferCreateInfo bufferCreate = VkBufferCreateInfo.mallocStack(stack);
			bufferCreate.sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO);
			bufferCreate.pNext(VK_NULL_HANDLE);
			bufferCreate.flags(0);
			bufferCreate.size(VulkanMemory.MEMORY_PAGE_SIZE);
			bufferCreate.usage(VK_BUFFER_USAGE_TRANSFER_SRC_BIT);
			bufferCreate.sharingMode(VK_SHARING_MODE_EXCLUSIVE);
			bufferCreate.pQueueFamilyIndices(null);

			LongBuffer pReturn = stack.mallocLong(1);
			int vkResult = vkCreateBuffer(device.logicalDevice,bufferCreate,null,pReturn);
			VulkanUtility.ValidateSuccess("Failed to create single use staging buffer",vkResult);
			StagingBuffer = pReturn.get(0);

			StagingBufferMemory = memory.allocateDedicatedBuffer(StagingBuffer,
					VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
			if(StagingBufferMemory == VK_NULL_HANDLE) {
				VulkanUtility.CrashOnBadResult("Failed to allocate staging buffer memory",-1);
			}
			vkResult = vkBindBufferMemory(device.logicalDevice,StagingBuffer,StagingBufferMemory,0);
			VulkanUtility.ValidateSuccess("Failed to bind staging buffer memory",vkResult);
		}
	}

	private void destroyStaging() {
		vkDestroyBuffer(device.logicalDevice,StagingBuffer,null);
		memory.freeDedicatedMemory(StagingBufferMemory);
	}

	private void initQueryPool(int swapSize) {
		try(MemoryStack stack = stackPush()) {
			VkQueryPoolCreateInfo queryPoolCreate = VkQueryPoolCreateInfo.mallocStack(stack);
			queryPoolCreate.sType(VK_STRUCTURE_TYPE_QUERY_POOL_CREATE_INFO);
			queryPoolCreate.pNext(VK_NULL_HANDLE);
			queryPoolCreate.flags(0);
			queryPoolCreate.queryType(VK_QUERY_TYPE_TIMESTAMP);
			queryPoolCreate.queryCount(3 * swapSize);
			queryPoolCreate.pipelineStatistics(0);

			LongBuffer pReturn = stack.mallocLong(1);
			int result = vkCreateQueryPool(device.logicalDevice,queryPoolCreate,null,pReturn);
			if(result == VK_SUCCESS) {
				queryPool = pReturn.get(0);
				queryGetDelay = swapSize;
				lastTimestampGraphics = 0;
			}else{
				//NO Memory
				VulkanUtility.CrashOnBadResult("Failed to create query pool",result);
			}
		}
	}

	private void destroyQueryPool() {
		vkDestroyQueryPool(device.logicalDevice,queryPool,null);
	}

	void CmdResetTimstamps(VkCommandBuffer buffer) {
		vkCmdResetQueryPool(
				buffer,
				queryPool,
				3*currentFrameIndex,
				3
		);
	}

	void CmdWriteTimestamp(VkCommandBuffer buffer,int queryOffset,int pipelineStage) {
		vkCmdWriteTimestamp(
				buffer,
				pipelineStage,
				queryPool,
				3*currentFrameIndex+queryOffset
		);
	}

	void UpdateTimestamp() {
		if(queryGetDelay > 0) {
			queryGetDelay -= 1;
			return;
		}
		try(MemoryStack stack = stackPush()) {
			LongBuffer pData = stack.mallocLong(3);
			vkGetQueryPoolResults(
					device.logicalDevice,
					queryPool,
					3*currentFrameIndex,
					3,
					pData,
					0,
					VK_QUERY_RESULT_64_BIT
			);


			long start_graphics = pData.get(0);
			long delta_world = pData.get(1) - start_graphics;
			long delta_gui = pData.get(2) - pData.get(1);
			long delta_all_graphics = pData.get(2) - start_graphics;
			long delta_graphics_total = pData.get(2) - lastTimestampGraphics;

			double graphics_usage = (double)delta_all_graphics / delta_graphics_total;
			double world_usage = (double)delta_world / delta_graphics_total;
			double gui_usage = (double)delta_gui / delta_graphics_total;

			SystemStatistics.graphics_history[SystemStatistics.write_index] = graphics_usage;
			SystemStatistics.graphics_world_history[SystemStatistics.write_index] = world_usage;
			SystemStatistics.graphics_gui_history[SystemStatistics.write_index] = gui_usage;

			lastTimestampGraphics = pData.get(2);
		}
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
		initStaging();
		initResizeable();
		initCommandBuffers(state.VulkanSwapChainSize,asyncCount);
		initSynchronisation(state.VulkanSwapChainSize);
		initQueryPool(state.VulkanSwapChainSize);
	}

	void reload() {
		//WaitForFence(MainThreadAcquireFence,50*1000*1000);
		destroySynchronisation();
		destroyResizeable();
		initResizeable();
		initSynchronisation(state.VulkanSwapChainSize);
	}

	void close() {
		//WaitForFence(MainThreadAcquireFence,50*1000*1000);
		destroyQueryPool();
		destroySynchronisation();
		destroyCommandBuffers();
		destroyResizeable();
		destroyStaging();;
	}


	///////////////////
	/// API Methods ///
	///////////////////

	public long getFrameBuffer_ForwardOnly() {
		return FrameBuffers_ForwardOnly.get(currentFrameIndex);
	}

	//TODO: GET OTHER FRAME BUFFER TYPES


	////////////////////////////////////////////////////////////////////////////////////////////////////////

	public VkCommandBuffer GetSingleUseCommandBuffer() {
		try(MemoryStack stack = stackPush()) {
			vkDeviceWaitIdle(device.logicalDevice);

			VkCommandBufferAllocateInfo allocateInfo = VkCommandBufferAllocateInfo.mallocStack(stack);
			allocateInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
			allocateInfo.pNext(VK_NULL_HANDLE);
			allocateInfo.commandPool(commandPoolMainThread);
			allocateInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);
			allocateInfo.commandBufferCount(1);

			PointerBuffer pCommand = stack.mallocPointer(1);
			int vkResult = vkAllocateCommandBuffers(device.logicalDevice,allocateInfo,pCommand);
			VulkanUtility.ValidateSuccess("Failed to alloc single-use command buffer",vkResult);

			VkCommandBuffer commandBuffer = new VkCommandBuffer(pCommand.get(0),device.logicalDevice);

			VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.mallocStack(stack);
			beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
			beginInfo.pNext(VK_NULL_HANDLE);
			beginInfo.flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
			beginInfo.pInheritanceInfo(null);

			vkResult = vkBeginCommandBuffer(commandBuffer,beginInfo);
			VulkanUtility.ValidateSuccess("Failed to begin single-use command buffer",vkResult);

			return commandBuffer;
		}
	}

	public void SubmitSingleUseCommandBuffer(VkCommandBuffer buffer) {
		try(MemoryStack stack = stackPush()) {
			int vkResult = vkEndCommandBuffer(buffer);
			VulkanUtility.ValidateSuccess("Failed to end single-use command buffer",vkResult);

			VkSubmitInfo submit = VkSubmitInfo.mallocStack(stack);
			submit.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);
			submit.pNext(VK_NULL_HANDLE);
			submit.waitSemaphoreCount(0);
			submit.pWaitSemaphores(null);
			submit.pWaitDstStageMask(null);
			submit.pCommandBuffers(stack.pointers(buffer));
			submit.pSignalSemaphores(null);

			vkQueueSubmit(device.allQueue,submit,VK_NULL_HANDLE);
			vkQueueWaitIdle(device.allQueue);
		}
	}

	public void SingleUseImagePopulate(long Image, STBITexture texture) {
		SingleUseImagePopulate(Image,texture,false);
	}

	public void SingleUseImagePopulate(long Image, STBITexture texture,boolean alphaOnly) {
		SingleUseImagePopulate(Image,texture.pixels,texture.width,texture.height,0,0,alphaOnly);
	}

	public void SingleUseImagePopulate(long Image, ByteBuffer pixels, int width, int height,int baseArrayLayer, int mipLevel) {
		SingleUseImagePopulate(Image,pixels,width,height,baseArrayLayer,mipLevel,false);
	}

	public void SingleUseImagePopulate(long Image, ByteBuffer pixels, int width, int height,int baseArrayLayer, int mipLevel,boolean alphaOnly) {
		try(MemoryStack stack = stackPush()) {
			VkCommandBuffer command = GetSingleUseCommandBuffer();

			VkImageMemoryBarrier.Buffer imgBarrier = VkImageMemoryBarrier.mallocStack(1, stack);
			imgBarrier.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
			imgBarrier.pNext(VK_NULL_HANDLE);
			imgBarrier.srcAccessMask(0);
			imgBarrier.dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
			imgBarrier.oldLayout(VK_IMAGE_LAYOUT_UNDEFINED);
			imgBarrier.newLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
			imgBarrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
			imgBarrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
			imgBarrier.image(Image);
			imgBarrier.subresourceRange().set(
					VK_IMAGE_ASPECT_COLOR_BIT,
					mipLevel,
					1,
					baseArrayLayer,
					1
			);

			vkCmdPipelineBarrier(command,
					VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT,
					VK_PIPELINE_STAGE_TRANSFER_BIT,
					0,
					null,
					null,
					imgBarrier);


			PointerBuffer pMapping = stack.mallocPointer(1);
			int vkResult = vkMapMemory(device.logicalDevice,StagingBufferMemory,0,VulkanMemory.MEMORY_PAGE_SIZE,0,pMapping);
			VulkanUtility.ValidateSuccess("Failed to map staging buffer",vkResult);

			ByteBuffer data = pMapping.getByteBuffer((int)VulkanMemory.MEMORY_PAGE_SIZE);
			if(alphaOnly) {
				//Only store alpha channel
				for(int i = 0; i < width*height; i++) {
					byte val = pixels.get(4*i+3);
					data.put(i,val);
				}
			}else {
				data.put(pixels);
			}
			vkUnmapMemory(device.logicalDevice,StagingBufferMemory);

			VkBufferImageCopy.Buffer regions = VkBufferImageCopy.mallocStack(1,stack);
			regions.bufferOffset(0);
			regions.bufferRowLength(width);
			regions.bufferImageHeight(height);
			regions.imageSubresource().set(
					VK_IMAGE_ASPECT_COLOR_BIT,
					mipLevel,
					baseArrayLayer,
					1
			);
			regions.imageOffset().set(0,0,0);
			regions.imageExtent().set(width,height,1);

			vkCmdCopyBufferToImage(command,StagingBuffer, Image, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, regions);

			imgBarrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
			imgBarrier.dstAccessMask(VK_ACCESS_SHADER_READ_BIT);
			imgBarrier.oldLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
			imgBarrier.newLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);

			vkCmdPipelineBarrier(command,
					VK_PIPELINE_STAGE_TRANSFER_BIT,
					VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT,
					0,
					null,
					null,
					imgBarrier);

			SubmitSingleUseCommandBuffer(command);
		}
	}

	private void WaitForFence(long fence,long timeout) {
		/*
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
		*/
		//HAXXY TODO: RETURN TO PROPER IMPLEMENTATION
		vkDeviceWaitIdle(device.logicalDevice);
		vkResetFences(device.logicalDevice,fence);
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
			//TODO: is this fence needed - Y / N
			//WaitForFence(MainThreadAcquireFence, timeout);
			IntBuffer pImageIndex = stack.mallocInt(1);
			int vkResult = KHRSwapchain.vkAcquireNextImageKHR(
					device.logicalDevice,
					state.VulkanSwapChain,
					timeout,
					MainThreadAcquireSemaphore,
					VK_NULL_HANDLE,//MainThreadAcquireFence,
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

	public VkDevice getDevice() {
		return device.logicalDevice;
	}

	VulkanDevice getDeviceManager() {
		return device;
	}

	public int getSwapSize() {
		return state.VulkanSwapChainSize;
	}

	public int getSwapIndex() {
		return currentFrameIndex;
	}

	/**
	 * @return The command buffer for the entire draw call
	 */
	VkCommandBuffer getMainDrawCommandBuffer() {
		return commandBuffersMainThread.get(currentFrameIndex);
	}

	/**
	 * @return The command buffer for the async GUI Draw call
	 */
	public VkCommandBuffer getGuiDrawCommandBuffer(boolean isTransfer) {
		int offset = isTransfer ? 0 : state.VulkanSwapChainSize;
		return commandBuffersGuiAsync.get(currentFrameIndex + offset);
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
