package net.openvoxel.client.renderer.vk;

import gnu.trove.list.TIntList;
import gnu.trove.list.TLongList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.TIntLongMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TIntLongHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.map.hash.TObjectLongHashMap;
import net.openvoxel.client.STBITexture;
import net.openvoxel.client.renderer.base.BaseGuiRenderer;
import net.openvoxel.client.renderer.vk.core.VulkanDevice;
import net.openvoxel.client.renderer.vk.core.VulkanMemory;
import net.openvoxel.client.renderer.vk.core.VulkanUtility;
import net.openvoxel.client.renderer.vk.pipeline.VulkanRenderPass;
import net.openvoxel.common.resources.ResourceHandle;
import net.openvoxel.utility.MathUtilities;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanGuiRenderer extends BaseGuiRenderer {

	//Memory State Core...
	private VulkanCache cache;
	private VulkanCommandHandler command;
	private VulkanMemory memory;
	private VulkanTextRenderer vulkanTextRender;

	//Vertex Buffer State...
	private long VertexBuffer;
	private boolean VertexMemoryCoherent;//TODO: ADD CONDITION FOR NON-COHERENT MEMORY
	private long VertexMemory;

	//Descriptor Pool...
	private long DescriptorPool;
	private TLongList DescriptorSetList = new TLongArrayList();

	//Image Device Storage {memory_type -> page}
	private TIntLongMap imagePages = new TIntLongHashMap();
	private TIntObjectMap<boolean[]> pageUsage = new TIntObjectHashMap<>();
	private static final int PAGE_COUNT = 256;
	private static final int PAGE_SIZE = (int)(VulkanMemory.MEMORY_PAGE_SIZE / PAGE_COUNT);

	//Memory Map...
	private ByteBuffer memoryMap;

	//Memory Partition...
	private int vertexSectionLength;
	private int uniformSectionLength;//TODO: IS UNIFORM BUFFER NEEDED?? (if so setup...) (else remove...)
	private TIntList offsetVertexBuffers = new TIntArrayList();
	private TIntList offsetUniformBuffers = new TIntArrayList();
	//Memory Transfer Partition...
	private TIntList dynamicTransferReadIndex = new TIntArrayList();
	private int transferMemoryStart = 0;
	private int dynamicTransferWriteIndex = 0;

	//Sampler..
	private long DefaultSampler = VK_NULL_HANDLE;

	private Set<ResourceHandle> resourceUsageSet = new HashSet<>();
	private TObjectIntMap<ResourceHandle> resourceCountdownMap = new TObjectIntHashMap<>();
	private TObjectLongMap<ResourceHandle> resourceImageMap = new TObjectLongHashMap<>();
	private TObjectLongMap<ResourceHandle> resourceImageViewMap = new TObjectLongHashMap<>();
	private TObjectIntMap<ResourceHandle> resourceImageMemoryType = new TObjectIntHashMap<>();
	private TObjectIntMap<ResourceHandle> resourceImageMemoryOffset = new TObjectIntHashMap<>();
	private TObjectIntMap<ResourceHandle> resourceImageMemorySize = new TObjectIntHashMap<>();

	VulkanGuiRenderer(VulkanCache cache,VulkanCommandHandler command, VulkanMemory memory,VulkanTextRenderer text) {
		super(text);
		this.cache = cache;
		this.command = command;
		this.memory = memory;
		vulkanTextRender = text;


		try(MemoryStack stack = stackPush()) {
			VkBufferCreateInfo bufferCreate = VkBufferCreateInfo.mallocStack(stack);
			bufferCreate.sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO);
			bufferCreate.pNext(VK_NULL_HANDLE);
			bufferCreate.flags(0);
			bufferCreate.size(VulkanMemory.MEMORY_PAGE_SIZE);
			bufferCreate.usage(
					VK_BUFFER_USAGE_VERTEX_BUFFER_BIT |
					VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT |
					VK_BUFFER_USAGE_TRANSFER_SRC_BIT
			);
			bufferCreate.sharingMode(VK_SHARING_MODE_EXCLUSIVE);
			bufferCreate.pQueueFamilyIndices(null);
			LongBuffer pReturn = stack.mallocLong(1);
			int vkResult = vkCreateBuffer(command.getDevice(),bufferCreate,null,pReturn);
			if(vkResult == VK_SUCCESS) {
				VertexBuffer = pReturn.get(0);
			}else{
				//No Memory
				VulkanUtility.CrashOnBadResult("Failed to create GUI Vertex Buffer",vkResult);
			}
			VertexMemory = memory.allocateDedicatedBuffer(
					VertexBuffer,
					(VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT |
					VK_MEMORY_PROPERTY_HOST_CACHED_BIT |
					VK_MEMORY_PROPERTY_HOST_COHERENT_BIT)
			);
			if(VertexMemory == VK_NULL_HANDLE) {
				VertexMemory = memory.allocateDedicatedBuffer(
						VertexBuffer,
						(VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT|
						VK_MEMORY_PROPERTY_HOST_CACHED_BIT)
				);
				if(VertexMemory == VK_NULL_HANDLE) {
					VulkanUtility.CrashOnBadResult("Failed to allocate memory for GUI Vertex Buffer",-1);
				}
				VertexMemoryCoherent = false;
			}else{
				VertexMemoryCoherent = true;
			}
			vkBindBufferMemory(command.getDevice(),VertexBuffer,VertexMemory,0);

			PointerBuffer pMap = stack.mallocPointer(1);
			vkResult = vkMapMemory(command.getDevice(),VertexMemory,0,bufferCreate.size(),0,pMap);
			VulkanUtility.ValidateSuccess("Failed to map GUI Vertex Memory",vkResult);
			memoryMap = pMap.getByteBuffer((int)bufferCreate.size());

			handleSwapChainSizeChange();

			VkSamplerCreateInfo samplerCreate = VkSamplerCreateInfo.mallocStack(stack);
			samplerCreate.sType(VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO);
			samplerCreate.pNext(VK_NULL_HANDLE);
			samplerCreate.flags(0);
			samplerCreate.magFilter(VK_FILTER_LINEAR);
			samplerCreate.minFilter(VK_FILTER_LINEAR);
			samplerCreate.mipmapMode(VK_SAMPLER_MIPMAP_MODE_LINEAR);
			samplerCreate.addressModeU(VK_SAMPLER_ADDRESS_MODE_REPEAT);
			samplerCreate.addressModeV(VK_SAMPLER_ADDRESS_MODE_REPEAT);
			samplerCreate.addressModeW(VK_SAMPLER_ADDRESS_MODE_REPEAT);
			samplerCreate.mipLodBias(0.0f);
			samplerCreate.anisotropyEnable(false);
			samplerCreate.maxAnisotropy(1.0f);
			samplerCreate.compareEnable(false);
			samplerCreate.compareOp(VK_COMPARE_OP_ALWAYS);
			samplerCreate.minLod(0.0f);
			samplerCreate.maxLod(0.0f);
			samplerCreate.borderColor(0);
			samplerCreate.unnormalizedCoordinates(false);

			vkResult = vkCreateSampler(command.getDevice(),samplerCreate,null,pReturn);
			if(vkResult == VK_SUCCESS) {
				DefaultSampler = pReturn.get(0);
			}else{
				VulkanUtility.CrashOnBadResult("Failed to create default-gui sampler",vkResult);
			}

			VkDescriptorPoolSize.Buffer descriptorPoolSizes = VkDescriptorPoolSize.mallocStack(1,stack);
			descriptorPoolSizes.type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
			descriptorPoolSizes.descriptorCount(32 * command.getSwapSize());

			VkDescriptorPoolCreateInfo poolCreateInfo = VkDescriptorPoolCreateInfo.mallocStack(stack);
			poolCreateInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO);
			poolCreateInfo.pNext(VK_NULL_HANDLE);
			poolCreateInfo.flags(0);
			poolCreateInfo.maxSets(command.getSwapSize());
			poolCreateInfo.pPoolSizes(descriptorPoolSizes);

			vkResult = vkCreateDescriptorPool(command.getDevice(),poolCreateInfo,null,pReturn);
			if(vkResult == VK_SUCCESS) {
				DescriptorPool = pReturn.get(0);
			}else{
				VulkanUtility.CrashOnBadResult("Failed to create gui-descriptor pool",vkResult);
			}

			LongBuffer pSetLayouts = stack.mallocLong(command.getSwapSize());
			for(int i = 0; i < command.getSwapSize(); i++) {
				pSetLayouts.put(i,cache.DESCRIPTOR_SET_LAYOUT_GUI_TEXTURE_ARRAY);
			}

			VkDescriptorSetAllocateInfo allocateInfo = VkDescriptorSetAllocateInfo.mallocStack(stack);
			allocateInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO);
			allocateInfo.pNext(VK_NULL_HANDLE);
			allocateInfo.descriptorPool(DescriptorPool);
			allocateInfo.pSetLayouts(pSetLayouts);

			LongBuffer pSets = stack.mallocLong(command.getSwapSize());
			vkResult = vkAllocateDescriptorSets(command.getDevice(),allocateInfo,pSets);
			if(vkResult == VK_SUCCESS) {
				for(int i = 0; i < command.getSwapSize(); i++) {
					DescriptorSetList.add(pSets.get(i));
				}
			}else{
				VulkanUtility.CrashOnBadResult("Failed to create gui-descriptor sets",vkResult);
			}
		}
	}


	private void partitionBuffer() {
		VulkanDevice device = command.getDeviceManager();
		long uniformAlign = device.properties.limits().minUniformBufferOffsetAlignment();
		long copyAlign = device.properties.limits().optimalBufferCopyOffsetAlignment();
		int alignment = (int)Math.max(uniformAlign,copyAlign);
		int swapSize = command.getSwapSize();

		//Choose Memory sizes
		vertexSectionLength = MathUtilities.padToAlign(256 * 1024,alignment);
		uniformSectionLength = MathUtilities.padToAlign(256,alignment);

		//Choose Offsets
		int _runningOffset = 0;
		for(int i = 0; i < swapSize; i++) {
			offsetVertexBuffers.add(_runningOffset);
			_runningOffset += vertexSectionLength;
		}
		for(int i = 0; i < swapSize; i++) {
			offsetUniformBuffers.add(_runningOffset);
			_runningOffset += uniformSectionLength;
		}
		transferMemoryStart = _runningOffset;
		for(int i = 0; i < swapSize; i++) {
			dynamicTransferReadIndex.add(0);
		}
	}

	@Override
	public void close() {
		for(Object val: resourceImageMap.keys()) {
			ResourceHandle handle = (ResourceHandle)val;
			long imageView = resourceImageViewMap.get(handle);
			long image = resourceImageMap.get(handle);
			vkDestroyImageView(command.getDevice(),imageView,null);
			vkDestroyImage(command.getDevice(),image,null);
		}
		resourceImageMap.clear();
		resourceImageViewMap.clear();
		resourceImageMemoryType.clear();
		resourceImageMemoryOffset.clear();
		resourceImageMemorySize.clear();

		vkDestroyDescriptorPool(command.getDevice(),DescriptorPool,null);
		DescriptorSetList.clear();

		for(int mem_type : imagePages.keys()) {
			long MemoryPage = imagePages.get(mem_type);
			memory.freeMemoryPage(MemoryPage);
		}
		imagePages.clear();

		vkDestroySampler(command.getDevice(),DefaultSampler,null);
		vkDestroyBuffer(command.getDevice(),VertexBuffer,null);
		memory.freeDedicatedMemory(VertexMemory);
		super.close();
	}

	void handleSwapChainSizeChange() {
		dynamicTransferReadIndex.clear();
		offsetVertexBuffers.clear();
		offsetUniformBuffers.clear();
		partitionBuffer();
		transferMemoryStart = 0;
		dynamicTransferWriteIndex = 0;
	}

	void invalidateCommands() {

	}

	//Returns Image
	private long allocateResourceMemory(ResourceHandle handle,VkImageMemoryBarrier initBarrier,VkBufferImageCopy copy,VkImageMemoryBarrier barrier) {
		STBITexture texture = new STBITexture(handle);
		int texture_format = VulkanRenderPass.formatSimpleReadImage;
		try(MemoryStack stack = stackPush()) {
			VkImageCreateInfo imageCreateInfo = VkImageCreateInfo.mallocStack(stack);
			imageCreateInfo.sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO);
			imageCreateInfo.pNext(VK_NULL_HANDLE);
			imageCreateInfo.flags(0);
			imageCreateInfo.imageType(VK_IMAGE_TYPE_2D);
			imageCreateInfo.format(texture_format);
			imageCreateInfo.extent().set(texture.width,texture.height,1);
			imageCreateInfo.mipLevels(1);
			imageCreateInfo.arrayLayers(1);
			imageCreateInfo.samples(VK_SAMPLE_COUNT_1_BIT);
			imageCreateInfo.tiling(VK_IMAGE_TILING_OPTIMAL);
			imageCreateInfo.usage(VK_IMAGE_USAGE_SAMPLED_BIT | VK_IMAGE_USAGE_TRANSFER_DST_BIT);
			imageCreateInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE);
			imageCreateInfo.pQueueFamilyIndices(null);
			imageCreateInfo.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);

			LongBuffer pReturn = stack.mallocLong(1);
			int vkResult = vkCreateImage(command.getDevice(),imageCreateInfo,null,pReturn);
			long image = pReturn.get(0);
			if(vkResult != VK_SUCCESS) {
				//Memory Issues
				VulkanUtility.CrashOnBadResult("Failed to create GUI Image",vkResult);
			}


			VkMemoryRequirements requirements = VkMemoryRequirements.mallocStack(stack);
			vkGetImageMemoryRequirements(command.getDevice(),image,requirements);

			int memoryType = memory.findMemoryType(requirements.memoryTypeBits(),VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);

			if(!imagePages.containsKey(memoryType)) {
				long page = memory.allocateMemoryPage(memoryType);
				imagePages.put(memoryType,page);
				pageUsage.put(memoryType,new boolean[PAGE_COUNT]);
			}

			int align_size = MathUtilities.padToAlign((int)requirements.size(),(int)requirements.alignment());
			int page_size = MathUtilities.padToAlign(align_size,PAGE_SIZE);
			int page_count = page_size / PAGE_SIZE;

			boolean[] pageInfo = pageUsage.get(memoryType);
			int page_offset = 0;
			int valid_page_count = 0;
			for(int i = 0; i < PAGE_COUNT;i++) {
				if(valid_page_count == page_count) {
					break;
				}else if(pageInfo[i]) {
					valid_page_count = 0;
					page_offset = i + 1;
				}else{
					valid_page_count++;
				}
			}
			if(valid_page_count != page_count) {
				VulkanUtility.CrashOnBadResult("Failed to allocate GUI Image in Pages",-1);
			}

			vkResult = vkBindImageMemory(command.getDevice(),image,imagePages.get(memoryType),page_offset * PAGE_SIZE);
			if(vkResult != VK_SUCCESS) {
				VulkanUtility.CrashOnBadResult("Failed to bind GUI Image Memory",vkResult);
			}
			for(int i = 0; i < page_count; i++) {
				if(pageInfo[page_offset + i]) {
					//TODO: REMOVE DEBUG CODE
					throw new RuntimeException("GUI Image Usage State Changed!!");
				}
				pageInfo[page_offset + i] = true;
			}

			VkImageViewCreateInfo imageViewCreate = VkImageViewCreateInfo.mallocStack(stack);
			imageViewCreate.sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO);
			imageViewCreate.pNext(VK_NULL_HANDLE);
			imageViewCreate.flags(0);
			imageViewCreate.image(image);
			imageViewCreate.viewType(VK_IMAGE_VIEW_TYPE_2D);
			imageViewCreate.format(texture_format);
			imageViewCreate.components().set(
					VK_COMPONENT_SWIZZLE_IDENTITY,
					VK_COMPONENT_SWIZZLE_IDENTITY,
					VK_COMPONENT_SWIZZLE_IDENTITY,
					VK_COMPONENT_SWIZZLE_IDENTITY
			);
			imageViewCreate.subresourceRange().set(
					VK_IMAGE_ASPECT_COLOR_BIT,
					0,
					1,
					0,
					1
			);

			vkResult = vkCreateImageView(command.getDevice(),imageViewCreate,null,pReturn);
			if(vkResult != VK_SUCCESS) {
				VulkanUtility.CrashOnBadResult("Failed to create GUI Image View",vkResult);
			}
			long imageView = pReturn.get(0);

			resourceUsageSet.add(handle);
			resourceCountdownMap.put(handle,0);
			resourceImageMap.put(handle,image);
			resourceImageViewMap.put(handle,imageView);
			resourceImageMemoryType.put(handle,memoryType);
			resourceImageMemoryOffset.put(handle,page_offset * PAGE_SIZE);
			resourceImageMemorySize.put(handle,page_size);
			useResource(handle);

			//TODO: HANDLE BETTER
			int storeOffset = dynamicTransferWriteIndex;
			dynamicTransferWriteIndex = dynamicTransferWriteIndex + texture.pixels.capacity()
					                            % ((int)VulkanMemory.MEMORY_PAGE_SIZE - transferMemoryStart);
			memoryMap.position(storeOffset);
			memoryMap.put(texture.pixels);
			memoryMap.position(0);

			copy.bufferOffset(storeOffset);
			copy.bufferRowLength(texture.width);
			copy.bufferImageHeight(texture.height);
			copy.imageSubresource().set(
					VK_IMAGE_ASPECT_COLOR_BIT,
					0,
					0,
					1
			);
			copy.imageOffset().set(0,0,0);
			copy.imageExtent().set(texture.width,texture.height,1);

			initBarrier.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
			initBarrier.pNext(VK_NULL_HANDLE);
			initBarrier.srcAccessMask(0);
			initBarrier.dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
			initBarrier.oldLayout(VK_IMAGE_LAYOUT_UNDEFINED);
			initBarrier.newLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
			initBarrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
			initBarrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
			initBarrier.image(image);
			initBarrier.subresourceRange().set(
					VK_IMAGE_ASPECT_COLOR_BIT,
					0,
					1,
					0,
					1
			);

			barrier.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
			barrier.pNext(VK_NULL_HANDLE);
			barrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
			barrier.dstAccessMask(VK_ACCESS_SHADER_READ_BIT);
			barrier.oldLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
			barrier.newLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
			barrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
			barrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
			barrier.image(image);
			barrier.subresourceRange().set(
					VK_IMAGE_ASPECT_COLOR_BIT,
					0,
					1,
					0,
					1
			);

			return image;
		}finally{
			texture.Free();
		}
	}

	private void reduceResourceTicks() {
		List<ResourceHandle> toDestroy = new ArrayList<>();
		for(ResourceHandle resource : resourceUsageSet) {
			int tick = resourceCountdownMap.get(resource) - 1;
			if(tick <= 0) {
				toDestroy.add(resource);
			}else{
				resourceCountdownMap.put(resource,tick);
			}
		}
		for(ResourceHandle destroy : toDestroy) {
			resourceUsageSet.remove(destroy);
			resourceCountdownMap.remove(destroy);
			long imageView = resourceImageViewMap.remove(destroy);
			vkDestroyImageView(command.getDevice(),imageView,null);
			long image = resourceImageMap.remove(destroy);
			vkDestroyImage(command.getDevice(),image,null);
			int memoryType = resourceImageMemoryType.remove(destroy);
			int offset = resourceImageMemoryOffset.remove(destroy);
			int size = resourceImageMemorySize.remove(destroy);
			int page_count = size / PAGE_SIZE;
			int page_offset = offset / PAGE_SIZE;
			boolean[] usage = pageUsage.get(memoryType);
			for(int i = 0; i < page_count; i++) {
				usage[page_offset + i] = false;
			}
		}
	}

	private boolean isResourceAllocated(ResourceHandle handle) {
		return resourceUsageSet.contains(handle);
	}

	private void useResource(ResourceHandle handle) {
		resourceCountdownMap.put(handle,command.getSwapSize() * 2);
	}

	/////////////////////////////
	/// Actual Rendering Code ///
	/////////////////////////////


	private int currentVertexOffset = 0;

	@Override
	protected void preDraw() {
		//Clear Unneeded Resources
		reduceResourceTicks();
		currentVertexOffset = offsetVertexBuffers.get(command.getSwapIndex());
	}

	@Override
	protected void store(int offset, float x, float y, float u, float v, int RGB) {
		final int VERTEX_SIZE = 4 + 4 + 4 + 4 + 4;
		int store_offset = offset + currentVertexOffset;
		if(offset+VERTEX_SIZE >= vertexSectionLength) {
			VulkanUtility.CrashOnBadResult("Too Many Points for GUI Draw["+offset+"]",-1);
		}
		memoryMap.putFloat(store_offset,x);
		memoryMap.putFloat(store_offset+4,y);
		memoryMap.putFloat(store_offset+8,u);
		memoryMap.putFloat(store_offset+12,v);
		memoryMap.putInt(store_offset+16,RGB);
	}

	@Override
	protected void redrawOld() {
		//TODO: RUN THE SAME COMMAND BUFFER {ENSURE VALID SUBMISSION}
		//TODO: SET TRANSFER BUFFER TO USELESS VERSION...
		throw new RuntimeException("Not Yet Implemented");
	}

	@Override
	protected void createNewDraw() {
		VkCommandBuffer transfer = command.getGuiDrawCommandBuffer(true);
		VkCommandBuffer drawing = command.getGuiDrawCommandBuffer(false);

		dynamicTransferReadIndex.set(command.getSwapIndex(),dynamicTransferWriteIndex);

		List<ResourceHandle> toAllocate = new ArrayList<>();
		for(ResourceHandle handle : requestedHandles) {
			if(isResourceAllocated(handle)) {
				useResource(handle);
			}else if(handle != vulkanTextRender.handle){
				toAllocate.add(handle);
			}
		}
		try(MemoryStack stack = stackPush()) {
			VkCommandBufferInheritanceInfo inheritanceInfo = VkCommandBufferInheritanceInfo.mallocStack(stack);
			inheritanceInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_INHERITANCE_INFO);
			inheritanceInfo.pNext(VK_NULL_HANDLE);
			inheritanceInfo.renderPass(VK_NULL_HANDLE);
			inheritanceInfo.subpass(0);
			inheritanceInfo.framebuffer(VK_NULL_HANDLE);
			inheritanceInfo.occlusionQueryEnable(false);
			inheritanceInfo.queryFlags(0);
			inheritanceInfo.pipelineStatistics(0);

			VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.mallocStack(stack);
			beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
			beginInfo.pNext(VK_NULL_HANDLE);
			beginInfo.flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
			beginInfo.pInheritanceInfo(inheritanceInfo);

			int vkResult = vkBeginCommandBuffer(transfer,beginInfo);
			VulkanUtility.ValidateSuccess("Failed to begin command buffer",vkResult);
			if(toAllocate.size() != 0) {
				int stack_pointer = stack.getPointer();
				VkImageMemoryBarrier.Buffer preImageBarrier = VkImageMemoryBarrier.mallocStack(toAllocate.size(),stack);
				VkBufferImageCopy.Buffer imageTransfer = VkBufferImageCopy.mallocStack(toAllocate.size(), stack);
				VkImageMemoryBarrier.Buffer imageBarrier = VkImageMemoryBarrier.mallocStack(toAllocate.size(),stack);
				LongBuffer imageBuffer = stack.mallocLong(toAllocate.size());
				for(int i = 0; i < toAllocate.size(); i++) {
					long img = allocateResourceMemory(toAllocate.get(i),preImageBarrier.get(i),imageTransfer.get(i),imageBarrier.get(i));
					imageBuffer.put(i,img);
				}

				vkCmdPipelineBarrier(
					transfer,
					VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT,
					VK_PIPELINE_STAGE_TRANSFER_BIT,
					0,
					null,
					null,
					preImageBarrier
				);

				//Copy Commands//
				for(int i = 0; i < toAllocate.size(); i++) {
					imageTransfer.position(i);
					imageTransfer.limit(i+1);
					vkCmdCopyBufferToImage(
						transfer,
						VertexBuffer,
						imageBuffer.get(i),
						VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
						imageTransfer
					);
				}
				//Pipeline Barrier//
				vkCmdPipelineBarrier(
					transfer,
					VK_PIPELINE_STAGE_TRANSFER_BIT,
					VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT,
					0,
					null,
					null,
					imageBarrier
				);
				stack.setPointer(stack_pointer);
			}
			vkResult = vkEndCommandBuffer(transfer);
			VulkanUtility.ValidateSuccess("Failed to end command buffer",vkResult);

			if(requestedHandles.size() >= 32) {
				VulkanUtility.CrashOnBadResult("Gui Uses too many images = {"+requestedHandles.size()+"}",-1);
			}

			TObjectIntMap<ResourceHandle> imageIndexMapping = new TObjectIntHashMap<>();
			VkDescriptorImageInfo.Buffer descriptorImageInfo
					= VkDescriptorImageInfo.mallocStack(32,stack);//TODO: CONVERT BACK FROM 32

			//Text Renderer...
			descriptorImageInfo.position(0);
			descriptorImageInfo.sampler(vulkanTextRender.ImageSampler);
			descriptorImageInfo.imageView(vulkanTextRender.ImageView);
			descriptorImageInfo.imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
			imageIndexMapping.put(vulkanTextRender.handle,0);

			int _idx = 1;
			for(ResourceHandle handle : requestedHandles) {
				if(handle != vulkanTextRender.handle) {
					imageIndexMapping.put(handle, _idx);
					descriptorImageInfo.position(_idx);
					descriptorImageInfo.sampler(DefaultSampler);
					descriptorImageInfo.imageView(resourceImageViewMap.get(handle));
					descriptorImageInfo.imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
					_idx++;
				}
			}
			{
				//TODO: REMOVE DEBUG CODE!?!?
				while (_idx < 32) {
					descriptorImageInfo.position(_idx);
					descriptorImageInfo.sampler(vulkanTextRender.ImageSampler);
					descriptorImageInfo.imageView(vulkanTextRender.ImageView);
					descriptorImageInfo.imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
					_idx++;
				}
			}
			descriptorImageInfo.position(0);

			long descriptorSet = DescriptorSetList.get(command.getSwapIndex());

			VkWriteDescriptorSet.Buffer pDescriptorWrites = VkWriteDescriptorSet.mallocStack(1,stack);
			pDescriptorWrites.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);
			pDescriptorWrites.pNext(VK_NULL_HANDLE);
			pDescriptorWrites.dstSet(descriptorSet);
			pDescriptorWrites.dstBinding(0);
			pDescriptorWrites.dstArrayElement(0);
			pDescriptorWrites.descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
			pDescriptorWrites.pImageInfo(descriptorImageInfo);
			pDescriptorWrites.pBufferInfo(null);
			pDescriptorWrites.pTexelBufferView(null);

			if(requestedHandles.size() != 0) {
				vkUpdateDescriptorSets(command.getDevice(), pDescriptorWrites, null);
			}
			///////////////////////////////////////////////////////////////////////////////////////////////////

			//TODO: IMPROVE AUTO-SELECT
			inheritanceInfo.renderPass(cache.RENDER_PASS_FORWARD_ONLY.RenderPass);
			inheritanceInfo.subpass(0);
			inheritanceInfo.framebuffer(command.getFrameBuffer_ForwardOnly());

			beginInfo.flags(VK_COMMAND_BUFFER_USAGE_RENDER_PASS_CONTINUE_BIT);

			vkResult = vkBeginCommandBuffer(drawing,beginInfo);
			VulkanUtility.ValidateSuccess("Failed to init Gui Draw Buffer",vkResult);

			vkCmdBindPipeline(drawing,VK_PIPELINE_BIND_POINT_GRAPHICS,cache.PIPELINE_FORWARD_GUI.getPipeline());
			boolean usingNormalPipeline = true;

			vkCmdBindDescriptorSets(drawing,
				VK_PIPELINE_BIND_POINT_GRAPHICS,
				cache.PIPELINE_LAYOUT_GUI_STANDARD_INPUT,
				0,
				stack.longs(
						descriptorSet
				),
				null
			);

			vkCmdBindVertexBuffers(drawing,
				0,
				stack.longs(VertexBuffer),
				stack.longs(offsetVertexBuffers.get(command.getSwapIndex()))
			);

			VkViewport.Buffer pViewport = VkViewport.mallocStack(1);
			pViewport.x(0);
			pViewport.y(0);
			pViewport.width(screenWidth);
			pViewport.height(screenHeight);
			pViewport.minDepth(0.0f);
			pViewport.maxDepth(0.0f);
			vkCmdSetViewport(drawing,0,pViewport);

			//Draw Code
			VkRect2D.Buffer pScissor = VkRect2D.mallocStack(1,stack);
			pScissor.offset().set(0,0);
			pScissor.extent().set(screenWidth,screenHeight);
			vkCmdSetScissor(drawing,0,pScissor);

			IntBuffer pPushConstants = stack.mallocInt(2);
			pPushConstants.put(0,0);
			pPushConstants.put(1,0);

			for(int sIdx = 0; sIdx <= stateIndex; sIdx++) {
				int scissorIdx = sIdx * 4;
				int scissorX = scissorStateList.get(scissorIdx);
				int scissorY = scissorStateList.get(scissorIdx+1);
				int scissorW = scissorStateList.get(scissorIdx+2);
				int scissorH = scissorStateList.get(scissorIdx+3);
				boolean changed = (
						scissorX != pScissor.offset().x() ||
						scissorY != pScissor.offset().y() ||
						scissorW != pScissor.extent().width() ||
						scissorH != pScissor.extent().height()
				);
				pScissor.offset().set(
					scissorX,
					scissorY
				);
				pScissor.extent().set(
					scissorW,
					scissorH
				);

				byte usesTexture = useTexStateList.get(sIdx);

				if(resourceStateList[sIdx] == vulkanTextRender.handle && usingNormalPipeline) {
					vkCmdBindPipeline(drawing,VK_PIPELINE_BIND_POINT_GRAPHICS,cache.PIPELINE_FORWARD_TEXT.getPipeline());
					usingNormalPipeline = false;
				}else if((resourceStateList[sIdx] != vulkanTextRender.handle || usesTexture == 0)&& !usingNormalPipeline) {
					vkCmdBindPipeline(drawing,VK_PIPELINE_BIND_POINT_GRAPHICS,cache.PIPELINE_FORWARD_GUI.getPipeline());
					usingNormalPipeline = true;
				}

				if(changed) {
					vkCmdSetScissor(drawing, 0, pScissor);
				}

				pPushConstants.put(0,imageIndexMapping.get(resourceStateList[sIdx]));
				pPushConstants.put(1,usesTexture);
				vkCmdPushConstants(
						drawing,
						cache.PIPELINE_LAYOUT_GUI_STANDARD_INPUT,
						VK_SHADER_STAGE_FRAGMENT_BIT,
						0,
						pPushConstants
				);

				int vertex = offsetStateList.get(sIdx);
				int offset = sIdx == stateIndex ? writeIndex : offsetStateList.get(sIdx+1);
				int draw_count = offset - vertex;
				if(draw_count > 0) {
					vkCmdDraw(drawing, draw_count, 1, vertex, 0);
				}
			}

			vkResult = vkEndCommandBuffer(drawing);
			VulkanUtility.ValidateSuccess("Failed to end Gui Draw Buffer",vkResult);
		}
	}

	@Override
	public boolean allowDrawCaching() {
		return false;
	}
}
