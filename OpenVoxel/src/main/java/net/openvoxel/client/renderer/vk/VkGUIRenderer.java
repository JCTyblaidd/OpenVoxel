package net.openvoxel.client.renderer.vk;

import gnu.trove.list.TByteList;
import gnu.trove.list.array.TByteArrayList;
import net.openvoxel.client.ClientInput;
import net.openvoxel.client.STBITexture;
import net.openvoxel.client.gui_framework.Screen;
import net.openvoxel.client.renderer.generic.GUIRenderer;
import net.openvoxel.client.renderer.vk.shader.VkOmniRenderPass;
import net.openvoxel.client.renderer.vk.util.VkDeviceState;
import net.openvoxel.client.renderer.vk.util.VkMemoryManager;
import net.openvoxel.common.resources.ResourceHandle;
import net.openvoxel.common.resources.ResourceManager;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.*;
import java.util.stream.Collectors;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

/**
 * Created by James on 17/04/2017.
 *
 * Vulkan GUI Renderer
 */
public class VkGUIRenderer implements GUIRenderer, GUIRenderer.GUITessellator {

	//vulkan buffer information
	private static final int GUI_ELEMENT_SIZE = (4 + 4 + 4 + 4 + 4);
	private static final int GUI_TRIANGLE_COUNT = 4096;
	private static final int GUI_STATE_CHANGE_LIMIT = 512;
	public static final int GUI_BUFFER_SIZE = GUI_ELEMENT_SIZE * GUI_TRIANGLE_COUNT;
	public static final int GUI_IMAGE_BLOCK_SIZE = 128 * 64;
	public static final int GUI_IMAGE_BLOCK_COUNT = 512;
	public static final int GUI_IMAGE_CACHE_SIZE = GUI_IMAGE_BLOCK_SIZE * GUI_IMAGE_BLOCK_COUNT;

	//Implementation Flags//
	public static final boolean GUI_USE_COHERENT_MEMORY = VkImplFlags.gui_use_coherent_memory();
	private static final boolean GUI_USE_PERMANENT_MAPPING = VkImplFlags.gui_use_coherent_memory();
	private static final boolean GUI_DIRECT_TO_NON_COHERENT_MEMORY = VkImplFlags.gui_direct_to_non_coherent_memory();
	private static final boolean GUI_ALLOW_DRAW_CACHING = VkImplFlags.gui_allow_draw_caching();

	private VkDeviceState state;
	private VkMemoryManager mgr;
	private int screenWidth;
	private int screenHeight;
	private int drawCount;
	private ByteBuffer writeTarget, imgStagingTarget;


	private Set<ResourceHandle> requestedImages;
	private Matrix4f matrixStackHead = new Matrix4f();
	private List<ResourceHandle> requestedImageStack;
	private FloatBuffer matrixArrayStack;
	private int stateChangeCount, lastStateChange;
	private IntBuffer offsetTransitionStack;
	private TByteList imageEnableStateStack;

	//Bound Image Info//
	private static class BoundResourceHandle {
		public long image;
		long image_view;
		long image_sampler;
		int image_index;
		int offset;
		int count;
		int usageCount;
	}
	private Map<ResourceHandle,BoundResourceHandle> imageBindings;
	private long descriptorPool;
	private LongBuffer imageDescriptorSets;
	private ByteBuffer mappedGUIStaging;
	private int imageCleanupCountdown = MAX_IMAGE_CLEANUP_COUNTDOWN;
	private static final int MAX_IMAGE_CLEANUP_COUNTDOWN = 200;

	private int rewriteDescriptorSetCountdown = 0;
	private int dirtyDrawUpdateCountdown = 0;

	/**
	 * Construct from the main vulkan state
	 */
	VkGUIRenderer(VkDeviceState state) {
		this.state = state;
		this.mgr = state.memoryMgr;
		if(GUI_USE_PERMANENT_MAPPING && GUI_DIRECT_TO_NON_COHERENT_MEMORY) {
			writeTarget = MemoryUtil.memAlloc(GUI_BUFFER_SIZE);
		}
		imgStagingTarget = MemoryUtil.memAlloc(GUI_IMAGE_CACHE_SIZE);

		requestedImages = new HashSet<>();
		requestedImageStack = new ArrayList<>();
		matrixArrayStack = MemoryUtil.memAllocFloat(16 * GUI_STATE_CHANGE_LIMIT);
		offsetTransitionStack = MemoryUtil.memAllocInt(GUI_STATE_CHANGE_LIMIT);
		imageEnableStateStack = new TByteArrayList(GUI_STATE_CHANGE_LIMIT);
		imageBindings = new HashMap<>();
		if(!GUI_USE_PERMANENT_MAPPING) {
			try (MemoryStack stack = stackPush()) {
				mappedGUIStaging = mgr.mapMemory(mgr.memGuiStaging.get(1), 0, GUI_BUFFER_SIZE + GUI_IMAGE_CACHE_SIZE, stack);
			}
			if(GUI_DIRECT_TO_NON_COHERENT_MEMORY) {
				writeTarget = mappedGUIStaging;
			}
		}
		create_descriptor_sets();
	}

	@Override
	public boolean supportDirty() {
		return GUI_ALLOW_DRAW_CACHING && dirtyDrawUpdateCountdown == 0;
	}

	public void create_descriptor_sets() {
		dirtyDrawUpdateCountdown = state.swapChainImageViews.capacity() + 1;
		rewriteDescriptorSetCountdown = state.swapChainImageViews.capacity() + 1;
		imageDescriptorSets = MemoryUtil.memAllocLong(state.swapChainImageViews.capacity());
		try(MemoryStack stack = stackPush()) {
			LongBuffer res = stack.mallocLong(1);

			VkDescriptorPoolSize.Buffer poolSizes = VkDescriptorPoolSize.mallocStack(1,stack);
			poolSizes.type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
			poolSizes.descriptorCount(32);

			VkDescriptorPoolCreateInfo poolCreateInfo = VkDescriptorPoolCreateInfo.mallocStack(stack);
			poolCreateInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO);
			poolCreateInfo.pNext(VK_NULL_HANDLE);
			poolCreateInfo.flags(0);
			poolCreateInfo.maxSets(state.swapChainImageViews.capacity());
			poolCreateInfo.pPoolSizes(poolSizes);

			if(vkCreateDescriptorPool(state.renderDevice.device,poolCreateInfo,null,res) != VK_SUCCESS) {
				throw new RuntimeException("Failed to create Descriptor Pool");
			}
			descriptorPool = res.get(0);

			VkDescriptorSetAllocateInfo setAllocateInfo = VkDescriptorSetAllocateInfo.mallocStack(stack);
			setAllocateInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO);
			setAllocateInfo.pNext(VK_NULL_HANDLE);
			setAllocateInfo.descriptorPool(descriptorPool);
			setAllocateInfo.pSetLayouts(stack.longs(state.guiPipeline.DescriptorSetLayout));
			for(int i = 0; i < state.swapChainImageViews.capacity(); i++) {
				if (vkAllocateDescriptorSets(state.renderDevice.device, setAllocateInfo, res) != VK_SUCCESS) {
					throw new RuntimeException("Failed to allocate descriptor set");
				}
				imageDescriptorSets.put(i,res.get(0));
			}
		}
	}

	public void destroy_descriptors() {
		vkResetDescriptorPool(state.renderDevice.device,descriptorPool,0);
		vkDestroyDescriptorPool(state.renderDevice.device,descriptorPool, null);
		MemoryUtil.memFree(imageDescriptorSets);
	}

	/**
	 * Destroy all required images
	 */
	void cleanup() {
		if(!GUI_USE_PERMANENT_MAPPING) {
			mgr.unMapMemory(mgr.memGuiStaging.get(1));
		}
		for(BoundResourceHandle handle : imageBindings.values()) {
			vkDestroySampler(state.renderDevice.device,handle.image_sampler,null);
			vkDestroyImageView(state.renderDevice.device,handle.image_view,null);
			vkDestroyImage(state.renderDevice.device,handle.image,null);
		}
		destroy_descriptors();
		if(GUI_USE_PERMANENT_MAPPING && GUI_DIRECT_TO_NON_COHERENT_MEMORY) {
			MemoryUtil.memFree(writeTarget);
		}
		MemoryUtil.memFree(imgStagingTarget);
		MemoryUtil.memFree(matrixArrayStack);
		MemoryUtil.memFree(offsetTransitionStack);
	}

	@Override
	public void DisplayScreen(Screen screen) {
		screen.DrawScreen(this);
	}

	@Override
	public void beginDraw() {
		screenWidth = ClientInput.currentWindowWidth.get();
		screenHeight = ClientInput.currentWindowHeight.get();
		drawCount = 0;
		requestedImages.clear();

		imageEnableStateStack.clear();
		matrixArrayStack.position(0);
		offsetTransitionStack.position(0);
		requestedImageStack.clear();

		imageEnableStateStack.add((byte)0);
		requestedImageStack.add(null);
		matrixStackHead.set(identity_matrix);
		offsetTransitionStack.put(0);

		stateChangeCount = 0;
		lastStateChange = 0;
	}

	/**
	 * Destroy Image is ok since it hasn't been requested in last 100 frames & has yet to be requested for use...
	 */
	private boolean tickImageCleanupCountdown() {
		imageCleanupCountdown--;
		if(imageCleanupCountdown == 0) {
			imageCleanupCountdown = MAX_IMAGE_CLEANUP_COUNTDOWN;
			List<ResourceHandle> to_clean = new ArrayList<>();
			for(Map.Entry<ResourceHandle,BoundResourceHandle> handle : imageBindings.entrySet()) {
				if(handle.getValue().usageCount == 0) {
					vkDestroySampler(state.renderDevice.device,handle.getValue().image_sampler,null);
					vkDestroyImageView(state.renderDevice.device,handle.getValue().image_view,null);
					vkDestroyImage(state.renderDevice.device,handle.getValue().image,null);
					to_clean.add(handle.getKey());
				}else{
					handle.getValue().usageCount = 0;
				}
			}
			to_clean.forEach(imageBindings::remove);
			return to_clean.size() != 0;
		}
		return false;
	}

	/**
	 * Write an image layout transition to the command buffer
	 */
	private void cmdTransitionImageLayout(MemoryStack stack,VkCommandBuffer buffer,long image,int oldLayout, int newLayout) {
		VkImageSubresourceRange subresourceRange = VkImageSubresourceRange.mallocStack(stack);
		subresourceRange.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
		subresourceRange.baseMipLevel(0);
		subresourceRange.levelCount(1);
		subresourceRange.baseArrayLayer(0);
		subresourceRange.layerCount(1);

		VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.mallocStack(1,stack);
		barrier.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
		barrier.pNext(VK_NULL_HANDLE);
		barrier.oldLayout(oldLayout);
		barrier.newLayout(newLayout);
		barrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
		barrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
		barrier.image(image);
		barrier.subresourceRange(subresourceRange);

		int srcStage;
		int dstStage;

		if(oldLayout == VK_IMAGE_LAYOUT_UNDEFINED && newLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL) {
			barrier.srcAccessMask(0);
			barrier.dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
			srcStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
			dstStage = VK_PIPELINE_STAGE_TRANSFER_BIT;
		}else if(oldLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL && newLayout == VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL) {
			barrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
			barrier.dstAccessMask(VK_ACCESS_SHADER_READ_BIT);
			srcStage = VK_PIPELINE_STAGE_TRANSFER_BIT;
			dstStage = VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT;
		}else{
			throw new RuntimeException("Non-Implemented Transition");
		}

		vkCmdPipelineBarrier(buffer,srcStage,dstStage,0,
				null,null,barrier);
	}


	/**
	 * Build the entirety of the draw command
	 * and append it to the required command buffers
	 * with some batching to minimise the number of draw calls
	 */
	@Override
	public void finishDraw(boolean dirtyDraw) {
		if(!dirtyDraw) {
			try(MemoryStack stack = stackPush()) {
				VkCommandBuffer transferBuffer = new VkCommandBuffer(state.command_buffers_gui_transfer.get(state.swapChainImageIndex), state.renderDevice.device);
				vkResetCommandBuffer(transferBuffer, VK_COMMAND_BUFFER_RESET_RELEASE_RESOURCES_BIT);
				VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.mallocStack(stack);
				beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
				beginInfo.pNext(VK_NULL_HANDLE);
				beginInfo.flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
				beginInfo.pInheritanceInfo(null);
				vkBeginCommandBuffer(transferBuffer, beginInfo);
				vkEndCommandBuffer(transferBuffer);
			}
			return;//NO DRAW NEEDED//
		}else{
			dirtyDrawUpdateCountdown = imageDescriptorSets.capacity() + 1;
		}
		offsetTransitionStack.put(drawCount / GUI_ELEMENT_SIZE);
		try(MemoryStack stack = stackPush()) {
			boolean rewrite_descriptor_set = tickImageCleanupCountdown();
			VkBufferImageCopy.Buffer copyImg = VkBufferImageCopy.callocStack(requestedImages.size(),stack);
			LongBuffer dstImages = stack.callocLong(requestedImages.size());
			copyImg.limit(0);
			copyImg.position(0);
			imgStagingTarget.position(0);
			for(ResourceHandle handle : requestedImages) {
				BoundResourceHandle boundHandle = imageBindings.get(handle);
				boolean dirty = handle.checkIfChanged();
				STBITexture texture = null;
				if (boundHandle == null) {
					rewrite_descriptor_set = true;
					handle.reloadData();
					texture = new STBITexture(handle.getByteData());
					LongBuffer retValue = stack.longs(0,0,0);
					mgr.AllocateGuiImage(VK_FORMAT_R8G8B8A8_UNORM,
							texture.width,texture.height,
							VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT,
							VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT,
							retValue,stack,true);
					boundHandle = new BoundResourceHandle();
					boundHandle.image = retValue.get(0);
					boundHandle.offset = (int)retValue.get(1);
					boundHandle.count = (int)retValue.get(2);
					boundHandle.usageCount = 1;
					imageBindings.put(handle,boundHandle);
					dirty = true;
					VkComponentMapping componentMapping = VkComponentMapping.mallocStack(stack);
					componentMapping.set(VK_COMPONENT_SWIZZLE_IDENTITY,
							VK_COMPONENT_SWIZZLE_IDENTITY,
							VK_COMPONENT_SWIZZLE_IDENTITY,
							VK_COMPONENT_SWIZZLE_IDENTITY);
					VkImageSubresourceRange subResourceRange = VkImageSubresourceRange.mallocStack(stack);
					subResourceRange.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
					subResourceRange.baseMipLevel(0);
					subResourceRange.levelCount(1);
					subResourceRange.baseArrayLayer(0);
					subResourceRange.layerCount(1);
					VkImageViewCreateInfo imageViewCreate = VkImageViewCreateInfo.mallocStack(stack);
					imageViewCreate.sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO);
					imageViewCreate.pNext(VK_NULL_HANDLE);
					imageViewCreate.flags(0);
					imageViewCreate.image(boundHandle.image);
					imageViewCreate.viewType(VK_IMAGE_VIEW_TYPE_2D);
					imageViewCreate.format(VK_FORMAT_R8G8B8A8_UNORM);
					imageViewCreate.components(componentMapping);
					imageViewCreate.subresourceRange(subResourceRange);
					if(vkCreateImageView(state.renderDevice.device,imageViewCreate,null,retValue) != VK_SUCCESS) {
						throw new RuntimeException("Failed to create image view[gui]");
					}
					boundHandle.image_view = retValue.get(0);
					VkSamplerCreateInfo samplerCreateInfo = VkSamplerCreateInfo.mallocStack(stack);
					samplerCreateInfo.sType(VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO);
					samplerCreateInfo.pNext(VK_NULL_HANDLE);
					samplerCreateInfo.flags(0);
					samplerCreateInfo.magFilter(VK_FILTER_LINEAR);
					samplerCreateInfo.minFilter(VK_FILTER_LINEAR);
					samplerCreateInfo.addressModeU(VK_SAMPLER_ADDRESS_MODE_REPEAT);
					samplerCreateInfo.addressModeV(VK_SAMPLER_ADDRESS_MODE_REPEAT);
					samplerCreateInfo.addressModeW(VK_SAMPLER_ADDRESS_MODE_REPEAT);
					samplerCreateInfo.anisotropyEnable(true);
					samplerCreateInfo.maxAnisotropy(8);
					samplerCreateInfo.borderColor(VK_BORDER_COLOR_INT_OPAQUE_BLACK);
					samplerCreateInfo.unnormalizedCoordinates(false);
					samplerCreateInfo.compareEnable(false);
					samplerCreateInfo.compareOp(VK_COMPARE_OP_ALWAYS);
					samplerCreateInfo.mipmapMode(VK_SAMPLER_MIPMAP_MODE_LINEAR);
					samplerCreateInfo.mipLodBias(0.0f);
					samplerCreateInfo.minLod(0.0f);
					samplerCreateInfo.maxLod(0.0f);

					if(vkCreateSampler(state.renderDevice.device,samplerCreateInfo,null,retValue) != VK_SUCCESS) {
						throw new RuntimeException("Failed to create sampler");
					}
					boundHandle.image_sampler = retValue.get(0);
				}else if(dirty) {
					handle.reloadData();
					texture = new STBITexture(handle.getByteData());
				}else{
					boundHandle.usageCount++;
				}
				if(dirty) {
					//Append to staging buffer//
					VkImageSubresourceLayers sub_resource = VkImageSubresourceLayers.mallocStack(stack);
					sub_resource.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
					sub_resource.baseArrayLayer(0);
					sub_resource.mipLevel(0);
					sub_resource.layerCount(1);

					VkExtent3D extent = VkExtent3D.mallocStack(stack);
					extent.width(texture.width);
					extent.height(texture.height);
					extent.depth(1);
					VkOffset3D i_offset = VkOffset3D.mallocStack(stack);
					i_offset.x(0);
					i_offset.y(0);
					i_offset.z(0);

					if(handle.getResourceID().equals("font/font")) {
						//TODO: disable and actually change the texture//
						for(int i = 0; i < texture.pixels.capacity(); i+= 4) {
							byte alpha = texture.pixels.get(i);
							texture.pixels.put(i,(byte)0xFF);
							texture.pixels.put(i+1,(byte)0xFF);
							texture.pixels.put(i+2,(byte)0xFF);
							texture.pixels.put(i+3,alpha);
						}
					}

					int offset = imgStagingTarget.position();
					imgStagingTarget.put(texture.pixels);
					int lim = copyImg.limit() + 1;
					copyImg.limit(lim);
					copyImg.bufferOffset(GUI_BUFFER_SIZE+offset);
					copyImg.bufferImageHeight(0);
					copyImg.bufferRowLength(0);
					copyImg.imageExtent(extent);
					copyImg.imageOffset(i_offset);
					copyImg.imageSubresource(sub_resource);
					copyImg.position(lim);
					dstImages.put(boundHandle.image);
				}
				if(texture != null) {
					state.vkLogger.Warning("TODO: FIX MEMORY LEAKING");
					//TODO: find out how this causes crashes??
					//texture.Free();
				}
			}
			if(rewrite_descriptor_set) {
				rewriteDescriptorSetCountdown = imageDescriptorSets.capacity();
				dirtyDrawUpdateCountdown = rewriteDescriptorSetCountdown;
			}else{
				if(rewriteDescriptorSetCountdown > 0) {
					rewriteDescriptorSetCountdown--;
				}
				if(dirtyDrawUpdateCountdown > 0) {
					dirtyDrawUpdateCountdown--;
				}
			}
			if(rewriteDescriptorSetCountdown > 0) {
				List<BoundResourceHandle> imageTargets = requestedImages.stream().map(imageBindings::get).collect(Collectors.toList());
				if(requestedImages.size() > 32) {
					state.vkLogger.Severe("Larger than maximum images requested");
					return;
				}
				VkDescriptorImageInfo.Buffer imageInfo = VkDescriptorImageInfo.mallocStack(32,stack);
				for(int i = 0; i < requestedImages.size(); i++) {
					BoundResourceHandle boundHandle = imageTargets.get(i);
					imageInfo.position(i);
					imageInfo.sampler(boundHandle.image_sampler);
					imageInfo.imageLayout(VK_IMAGE_LAYOUT_GENERAL);
					imageInfo.imageView(boundHandle.image_view);
					boundHandle.image_index = i;
				}
				BoundResourceHandle ignoreBinding = imageTargets.get(0);
				for(int i = requestedImages.size(); i < 32; i++) {
					imageInfo.position(i);
					imageInfo.sampler(ignoreBinding.image_sampler);
					imageInfo.imageLayout(VK_IMAGE_LAYOUT_GENERAL);
					imageInfo.imageView(ignoreBinding.image_view);
				}
				imageInfo.position(0);
				VkWriteDescriptorSet.Buffer descWrites = VkWriteDescriptorSet.mallocStack(1,stack);
				descWrites.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);
				descWrites.pNext(VK_NULL_HANDLE);
				descWrites.dstSet(imageDescriptorSets.get(state.swapChainImageIndex));
				descWrites.dstBinding(0);
				descWrites.dstArrayElement(0);
				descWrites.descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
				descWrites.pImageInfo(imageInfo);
				vkUpdateDescriptorSets(state.renderDevice.device,descWrites,null);
			}
			copyImg.position(0);
			dstImages.position(0);

			VkCommandBuffer transferBuffer = new VkCommandBuffer(state.command_buffers_gui_transfer.get(state.swapChainImageIndex),state.renderDevice.device);
			vkResetCommandBuffer(transferBuffer,VK_COMMAND_BUFFER_RESET_RELEASE_RESOURCES_BIT);
			VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.mallocStack(stack);
			beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
			beginInfo.pNext(VK_NULL_HANDLE);
			beginInfo.flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
			beginInfo.pInheritanceInfo(null);
			vkBeginCommandBuffer(transferBuffer,beginInfo);

			if(drawCount != 0) {
				boolean has_image_transfer = copyImg.remaining() != 0;
				if(GUI_USE_PERMANENT_MAPPING) {
					ByteBuffer memMapping = mgr.mapMemory(mgr.memGuiStaging.get(1), 0, GUI_BUFFER_SIZE + GUI_IMAGE_CACHE_SIZE, stack);
					writeTarget.position(0);
					memMapping.put(writeTarget);
					imgStagingTarget.position(0);
					memMapping.position(GUI_BUFFER_SIZE);
					memMapping.put(imgStagingTarget);
					memMapping.position(0);
					mgr.unMapMemory(mgr.memGuiStaging.get(1));
				}else {
					int img_lim = imgStagingTarget.position();
					if(GUI_DIRECT_TO_NON_COHERENT_MEMORY) {
						if(img_lim != 0) {
							imgStagingTarget.position(0);
							mappedGUIStaging.position(GUI_BUFFER_SIZE);
							imgStagingTarget.limit(img_lim);
							mappedGUIStaging.put(imgStagingTarget);
							imgStagingTarget.limit(imgStagingTarget.capacity());
						}
						mappedGUIStaging.position(0);
					}else {
						writeTarget.position(0);
						mappedGUIStaging.position(0);
						mappedGUIStaging.put(writeTarget);
						imgStagingTarget.position(0);
						mappedGUIStaging.position(GUI_BUFFER_SIZE);
						mappedGUIStaging.put(imgStagingTarget);
					}

					VkMappedMemoryRange.Buffer memoryRanges = VkMappedMemoryRange.mallocStack(has_image_transfer ? 2 : 1, stack);
					memoryRanges.position(0);
					memoryRanges.sType(VK_STRUCTURE_TYPE_MAPPED_MEMORY_RANGE);
					memoryRanges.pNext(VK_NULL_HANDLE);
					memoryRanges.memory(mgr.memGuiStaging.get(1));
					memoryRanges.offset(0);
					memoryRanges.size(drawCount);
					if (has_image_transfer) {
						memoryRanges.position(1);
						memoryRanges.sType(VK_STRUCTURE_TYPE_MAPPED_MEMORY_RANGE);
						memoryRanges.pNext(VK_NULL_HANDLE);
						memoryRanges.memory(mgr.memGuiStaging.get(1));
						memoryRanges.offset(GUI_BUFFER_SIZE);
						memoryRanges.size(img_lim);
					}
					vkFlushMappedMemoryRanges(state.renderDevice.device, memoryRanges);
				}

				VkBufferCopy.Buffer copyInfo = VkBufferCopy.mallocStack(1,stack);
				copyInfo.position(0);
				copyInfo.srcOffset(0);
				copyInfo.dstOffset(0);
				copyInfo.size(drawCount);
				vkCmdCopyBuffer(transferBuffer, mgr.memGuiStaging.get(0), mgr.memGuiDrawing.get(0), copyInfo);

				if(has_image_transfer) {
					int lim = copyImg.limit();
					for(int i = 0; i < lim; i++) {
						copyImg.position(i);
						copyImg.limit(i+1);
						long dstImage = dstImages.get(i);
						cmdTransitionImageLayout(stack,transferBuffer,dstImage,VK_IMAGE_LAYOUT_UNDEFINED,VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
						vkCmdCopyBufferToImage(transferBuffer,mgr.memGuiStaging.get(0), dstImage,VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,copyImg);
						cmdTransitionImageLayout(stack,transferBuffer,dstImage,VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
					}
				}
			}


			if(vkEndCommandBuffer(transferBuffer) != VK_SUCCESS) {
				throw new RuntimeException("Failed to record transfer buffer");
			}

			VkCommandBuffer cmdBuffer = new VkCommandBuffer(state.command_buffers_gui.get(state.swapChainImageIndex),state.renderDevice.device);
			vkResetCommandBuffer(cmdBuffer,VK_COMMAND_BUFFER_RESET_RELEASE_RESOURCES_BIT);
			VkCommandBufferInheritanceInfo inheritance = VkCommandBufferInheritanceInfo.mallocStack(stack);
			inheritance.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_INHERITANCE_INFO);
			inheritance.pNext(VK_NULL_HANDLE);
			inheritance.renderPass(state.renderPass.render_pass);
			inheritance.subpass(VkOmniRenderPass.GUI_DRAW_SUB_PASS_INDEX);
			inheritance.framebuffer(state.targetFrameBuffers.get(state.swapChainImageIndex));
			inheritance.occlusionQueryEnable(false);
			inheritance.queryFlags(0);
			inheritance.pipelineStatistics(0);
			beginInfo.pInheritanceInfo(inheritance);
			if(GUI_ALLOW_DRAW_CACHING) {
				beginInfo.flags(VK_COMMAND_BUFFER_USAGE_RENDER_PASS_CONTINUE_BIT);
			}else {
				beginInfo.flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT | VK_COMMAND_BUFFER_USAGE_RENDER_PASS_CONTINUE_BIT);
			}
			vkBeginCommandBuffer(cmdBuffer,beginInfo);

			vkCmdBindPipeline(cmdBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, state.guiPipeline.graphics_pipeline);

			if(drawCount != 0) {
				LongBuffer descriptorSets = stack.longs(imageDescriptorSets.get(state.swapChainImageIndex));
				vkCmdBindDescriptorSets(cmdBuffer, 0, state.guiPipeline.graphics_pipeline_layout,
						0, descriptorSets, null);
			}

			VkRect2D.Buffer scissor = VkRect2D.mallocStack(1,stack);
			VkOffset2D offset = VkOffset2D.callocStack(stack);
			scissor.offset(offset);
			VkExtent2D extent = VkExtent2D.mallocStack(stack);
			extent.set(screenWidth,screenHeight);
			scissor.extent(extent);

			vkCmdSetScissor(cmdBuffer,0,scissor);

			if(drawCount != 0) {
				vkCmdBindVertexBuffers(cmdBuffer, 0, stack.longs(mgr.memGuiDrawing.get(0)), stack.longs(0));
				IntBuffer pushConstantsBuffer = stack.callocInt(2);
				for(int i = 0; i <= stateChangeCount; i++) {
					int offsetStart = offsetTransitionStack.get(i);
					int offsetEnd = offsetTransitionStack.get(i+1);
					int drawLen = offsetEnd - offsetStart;
					if(drawLen != 0) {
						boolean enable_draw = imageEnableStateStack.get(i) != 0;
						ResourceHandle resHandle = requestedImageStack.get(i);
						int selected_image_index = resHandle == null ? 0 : imageBindings.get(resHandle).image_index;
						pushConstantsBuffer.put(0, selected_image_index);
						pushConstantsBuffer.put(1, enable_draw ? 1 : 0);
						vkCmdPushConstants(cmdBuffer, state.guiPipeline.graphics_pipeline_layout,
								VK_SHADER_STAGE_FRAGMENT_BIT, 0, pushConstantsBuffer);
						vkCmdSetScissor(cmdBuffer, 0, scissor);//TODO: change values when applicable
						vkCmdDraw(cmdBuffer, drawLen, 1, offsetStart, 0);
					}
				}
			}

			if(vkEndCommandBuffer(cmdBuffer) != VK_SUCCESS) {
				throw new RuntimeException("Failed to draw command buffer");
			}
		}
	}

	private final Matrix4f identity_matrix = new Matrix4f().identity();
	@Override
	public void Begin() {
		SetTexture(null);
		EnableTexture(false);
		SetMatrix(identity_matrix);
	}

	@Override
	public void Draw() {
		//NO OP//
	}

	private boolean has_drawn_since_state_change() {
		return lastStateChange != drawCount;
	}

	private void state_change() {
		offsetTransitionStack.put(drawCount / GUI_ELEMENT_SIZE);
		lastStateChange = drawCount;
		stateChangeCount++;
	}

	@Override
	public void SetTexture(ResourceHandle handle) {
		if(!requestedImages.contains(handle) && handle != null) {
			requestedImages.add(handle);
		}
		ResourceHandle latest = requestedImageStack.get(stateChangeCount);
		if(handle != null && !handle.equals(latest)) {
			if(has_drawn_since_state_change() && latest != null) {
				requestedImageStack.add(handle);
				imageEnableStateStack.add(imageEnableStateStack.get(stateChangeCount));
				int start = stateChangeCount * 16;
				for(int i = 0; i < 16; i++) {
					matrixArrayStack.put(start+16+i,matrixArrayStack.get(start+i));
				}
				state_change();
			}else{
				requestedImageStack.set(stateChangeCount,handle);
			}
		}
	}

	@Override
	public void EnableTexture(boolean enabled) {
		byte change = enabled ? (byte)1 : (byte)0;
		if(imageEnableStateStack.get(stateChangeCount) != change) {
			if(has_drawn_since_state_change()) {
				requestedImageStack.add(requestedImageStack.get(stateChangeCount));
				imageEnableStateStack.add(change);
				int start = stateChangeCount * 16;
				for(int i = 0; i < 16; i++) {
					matrixArrayStack.put(start+16+i,matrixArrayStack.get(start+i));
				}
				state_change();
			}else{
				imageEnableStateStack.set(stateChangeCount,change);
			}
		}
	}

	@Override
	public void SetMatrix(@NotNull Matrix4f mat) {
		if(!matrixStackHead.equals(mat)) {
			if(has_drawn_since_state_change()) {
				requestedImageStack.add(requestedImageStack.get(stateChangeCount));
				imageEnableStateStack.add(imageEnableStateStack.get(stateChangeCount));
				int pos = matrixArrayStack.position();
				mat.set(matrixArrayStack);
				matrixArrayStack.position(pos + 16);
				state_change();
			}else{
				int pos = matrixArrayStack.position();
				matrixArrayStack.position(pos - 16);
				mat.set(matrixArrayStack);
				matrixArrayStack.position(pos);
			}
		}
	}

	@Override
	public void Vertex(float x, float y) {
		VertexWithColUV(x,y,0,0,0xFFFFFFFF);
	}

	@Override
	public void VertexWithUV(float x, float y, float u, float v) {
		VertexWithColUV(x,y,u,v,0xFFFFFFFF);
	}


	@Override
	public void VertexWithCol(float x, float y, int RGB) {
		VertexWithColUV(x,y,0,0,RGB);
	}


	@Override
	public void VertexWithColUV(float x, float y, float u, float v, int RGB) {
		writeTarget.putFloat(drawCount,x*2-1);
		writeTarget.putFloat(drawCount+4,y*2-1);
		writeTarget.putFloat(drawCount+8,u);
		writeTarget.putFloat(drawCount+12,v);
		writeTarget.putInt(drawCount+16,RGB);
		drawCount += GUI_ELEMENT_SIZE;
	}


	@Override
	public void DrawText(float x, float y, float height, String text) {
		DrawText(x,y,height,text,0xFFFFFFFF,0xFFFFFFFF);
	}

	@Override
	public void DrawItem(float x, float y, float width, float height) {
		//TODO:
	}

	@Override
	public float getScreenWidth() {
		return screenWidth;
	}

	@Override
	public float getScreenHeight() {
		return screenHeight;
	}

	@Override
	public void resetScissor() {
		//TODO:
	}

	@Override
	public void scissor(int x, int y, int w, int h) {
		//TODO:
	}


	private static final ResourceHandle FONT_IMAGE = ResourceManager.getImage("font/font");
	private static final float[] FONT_CHAR_SIZES = new float[]{0,13,13,13,13,13,13,13,13,13,13,13,13,0,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,
			6,8,10,12,13,18,17,6,8,8,12,12,6,8,6,10,13,13,13,13,13,13,13,13,13,13,7,7,12,12,12,12,22,14,14,13,15,12,11,16,16,6,8,13,11,21,
			16,17,13,17,14,11,12,16,14,22,13,12,12,8,10,8,12,12,7,12,13,11,13,12,8,12,13,6,6,11,6,20,13,13,13,13,9,10,8,13,11,18,11,11,10,
			8,12,8,12,13,13,13,6,8,10,17,12,12,10,26,11,8,22,13,12,13,13,6,6,10,10,12,12,23,11,18,10,8,21,13,10,12,6,8,12,13,12,13,12,12,
			10,21,10,13,12,8,13,10,8,12,8,8,7,14,15,6,8,6,11,13,16,17,17,12,14,14,14,14,14,14,19,13,12,12,12,12,6,6,6,6,16,16,17,17,17,17,
			17,12,17,16,16,16,16,12,13,13,12,12,12,12,12,12,19,11,12,12,12,12,6,6,6,6,13,13,13,13,13,13,13,12,13,13,13,13,13,11,13,11};

	private static final int FONT_SHEET_WIDTH = 16;
	private static final int FONT_SHEET_HEIGHT = 16;
	private static final float FONT_CELL_WIDTH = 1.0F / (FONT_SHEET_WIDTH);
	private static final float FONT_CELL_HEIGHT = 1.0F / (FONT_SHEET_HEIGHT);
	private static final float FONT_WIDTH_SCALE = 32;


	private float DrawChar(float X, float Y, float Height,char c,float aspectRatio,int col) {
		int charID = ((int)c)-32;

		int YCell = charID / FONT_SHEET_HEIGHT;
		int XCell = charID - (YCell * FONT_SHEET_HEIGHT);

		float minU = XCell * FONT_CELL_WIDTH;
		float Width = (FONT_CHAR_SIZES[charID+32] / FONT_WIDTH_SCALE);
		float maxU = minU + (Width * FONT_CELL_WIDTH);
		float maxV = YCell * FONT_CELL_HEIGHT;
		float minV = maxV + FONT_CELL_HEIGHT;

		float realWidth = Height * Width * aspectRatio;
		float maxY = Y - Height;
		float maxX = X + realWidth;

		VertexWithColUV(X,Y,minU,minV,col);
		VertexWithColUV(X,maxY,minU,maxV,col);
		VertexWithColUV(maxX,maxY,maxU,maxV,col);

		VertexWithColUV(X,Y,minU,minV,col);
		VertexWithColUV(maxX,maxY,maxU,maxV,col);
		VertexWithColUV(maxX,Y,maxU,minV,col);

		return realWidth;
	}

	@Override
	public void DrawText(float x, float y, float height, String text, int col, int colOutline) {
		//IMPORTANT NOTE: colOutline --> IGNORED
		ResourceHandle oldRes = requestedImageStack.get(stateChangeCount);
		boolean oldEnabled = imageEnableStateStack.get(stateChangeCount) != 0;
		SetTexture(FONT_IMAGE);
		EnableTexture(true);
		//Draw//
		final int SIZE = text.length();
		float runningOffset = 0;
		final float aspect = (float)screenHeight / screenWidth;
		for(int i = 0; i < SIZE; i++){
			char c = text.charAt(i);
			runningOffset += DrawChar(x+runningOffset,y,height,c,aspect,col);
		}
		//Cleanup//
		SetTexture(oldRes);
		EnableTexture(oldEnabled);
	}

	@Override
	public float GetTextWidthRatio(String text) {
		final int SIZE = text.length();
		final float aspect = (float)screenHeight / screenWidth;
		float runningOffset = 0;
		for(int i = 0; i < SIZE; i++){
			char c = text.charAt(i);
			runningOffset += (FONT_CHAR_SIZES[(int)c] / FONT_WIDTH_SCALE) * aspect;
		}
		return runningOffset;
	}
}
