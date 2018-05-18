package net.openvoxel.client.renderer.vk.world;

import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;
import net.openvoxel.client.renderer.WorldDrawTask;
import net.openvoxel.client.renderer.base.BaseWorldRenderer;
import net.openvoxel.client.renderer.vk.VulkanCache;
import net.openvoxel.client.renderer.vk.VulkanCommandHandler;
import net.openvoxel.client.renderer.vk.core.VulkanDevice;
import net.openvoxel.client.renderer.vk.core.VulkanMemory;
import net.openvoxel.client.renderer.vk.core.VulkanUtility;
import net.openvoxel.utility.MathUtilities;
import net.openvoxel.world.client.ClientChunkSection;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanWorldRenderer extends BaseWorldRenderer {

	private VulkanCommandHandler command;
	private VulkanWorldMemoryManager memory;
	private VulkanMemory raw_memory;
	private VulkanCache cache;

	private long UniformDescriptorPool;
	private TLongList UniformDescriptorSetList;

	private long UniformBuffer;
	private long UniformBufferMemory;

	private int UniformAlignedSize;

	private long UniformStagingBuffer;
	private long UniformStagingMemory;
	private ByteBuffer UniformStagingMappedMemory;

	private static final int WORLD_UNIFORM_BUFFER_SIZE = 176;
	private static final int DEFAULT_MEMORY_SIZE = VulkanWorldMemoryPage.SUB_PAGE_SIZE;

	private final int MAX_TRANSFERS_PER_FRAME = WorldDrawTask.MAX_TRANSFER_CALLS_PER_FRAME;
	private final VkBufferMemoryBarrier.Buffer BufferTransferBarriers = VkBufferMemoryBarrier.malloc(MAX_TRANSFERS_PER_FRAME);
	private int bufferCopyCount = 0;
	private Lock bufferCopyLock = new ReentrantLock();

	public VulkanWorldRenderer(VulkanCommandHandler command,VulkanCache cache, VulkanDevice device, VulkanMemory memory) {
		this.command = command;
		this.cache = cache;
		this.raw_memory = memory;
		this.memory = new VulkanWorldMemoryManager(device,memory);
		UniformDescriptorSetList = new TLongArrayList();
		UniformAlignedSize = MathUtilities.padToAlign(
				WORLD_UNIFORM_BUFFER_SIZE,
				(int)device.properties.limits().minUniformBufferOffsetAlignment()
		);
		VulkanUtility.LogInfo("Padding World Uniform Buffer To: "+UniformAlignedSize+", from: "+WORLD_UNIFORM_BUFFER_SIZE);
		long uniformAllocSize = command.getSwapSize() * UniformAlignedSize;
		try(MemoryStack stack = stackPush()) {
			VkBufferCreateInfo createBuffer = VkBufferCreateInfo.mallocStack(stack);
			createBuffer.sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO);
			createBuffer.pNext(VK_NULL_HANDLE);
			createBuffer.flags(0);
			createBuffer.size(uniformAllocSize);
			createBuffer.usage(VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT);
			createBuffer.sharingMode(VK_SHARING_MODE_EXCLUSIVE);
			createBuffer.pQueueFamilyIndices(null);

			LongBuffer pReturn = stack.mallocLong(1);
			int vkResult = vkCreateBuffer(device.logicalDevice,createBuffer,null,pReturn);
			VulkanUtility.ValidateSuccess("Failed to create descriptor buffer",vkResult);
			UniformBuffer = pReturn.get(0);

			createBuffer.usage(VK_BUFFER_USAGE_TRANSFER_SRC_BIT);
			vkResult = vkCreateBuffer(device.logicalDevice,createBuffer,null,pReturn);
			VulkanUtility.ValidateSuccess("Failed to create descriptor staging buffer",vkResult);
			UniformStagingBuffer = pReturn.get(0);

			UniformBufferMemory = memory.allocateDedicatedBuffer(UniformBuffer,VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);
			if(UniformBufferMemory == VK_NULL_HANDLE) VulkanUtility.CrashOnBadResult("Failed to get uniform memory",-1);
			vkResult = vkBindBufferMemory(device.logicalDevice,UniformBuffer,UniformBufferMemory,0);
			VulkanUtility.ValidateSuccess("Failed to bind descriptor buffer memory!",vkResult);

			UniformStagingMemory = memory.allocateDedicatedBuffer(UniformStagingBuffer,
					VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT |
							VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
			if(UniformStagingMemory == VK_NULL_HANDLE) VulkanUtility.CrashOnBadResult("Failed to get uniform staging memory",-1);
			vkResult = vkBindBufferMemory(device.logicalDevice,UniformStagingBuffer,UniformStagingMemory,0);
			VulkanUtility.ValidateSuccess("Failed to bind descriptor staging buffer memory!",vkResult);

			VkDescriptorPoolSize.Buffer pPoolSizes = VkDescriptorPoolSize.mallocStack(1,stack);
			pPoolSizes.type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
			pPoolSizes.descriptorCount(3);

			VkDescriptorPoolCreateInfo createPool = VkDescriptorPoolCreateInfo.mallocStack(stack);
			createPool.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO);
			createPool.pNext(VK_NULL_HANDLE);
			createPool.flags(0);
			createPool.maxSets(command.getSwapSize());
			createPool.pPoolSizes(pPoolSizes);

			vkResult = vkCreateDescriptorPool(device.logicalDevice,createPool,null,pReturn);
			VulkanUtility.ValidateSuccess("Failed to create descriptor pool",vkResult);
			UniformDescriptorPool = pReturn.get(0);

			LongBuffer pSetLayouts = stack.mallocLong(command.getSwapSize());
			for(int i = 0; i < command.getSwapSize(); i++) {
				pSetLayouts.put(i,cache.DESCRIPTOR_SET_LAYOUT_WORLD_CONSTANTS);
			}

			VkDescriptorSetAllocateInfo allocateInfo = VkDescriptorSetAllocateInfo.mallocStack(stack);
			allocateInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO);
			allocateInfo.pNext(VK_NULL_HANDLE);
			allocateInfo.descriptorPool(UniformDescriptorPool);
			allocateInfo.pSetLayouts(pSetLayouts);

			LongBuffer pSets = stack.mallocLong(command.getSwapSize());

			vkResult = vkAllocateDescriptorSets(device.logicalDevice,allocateInfo,pSets);
			VulkanUtility.ValidateSuccess("Failed to allocate descriptor sets",vkResult);

			VkDescriptorBufferInfo.Buffer bufferInfo = VkDescriptorBufferInfo.mallocStack(1,stack);
			bufferInfo.buffer(UniformBuffer);
			bufferInfo.offset(0);
			bufferInfo.range(UniformAlignedSize);

			VkWriteDescriptorSet.Buffer pWrites = VkWriteDescriptorSet.mallocStack(1,stack);
			pWrites.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);
			pWrites.pNext(VK_NULL_HANDLE);
			pWrites.dstSet(VK_NULL_HANDLE);
			pWrites.dstBinding(0);
			pWrites.dstArrayElement(0);
			pWrites.descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
			pWrites.pBufferInfo(bufferInfo);

			UniformDescriptorSetList.clear();
			for(int i = 0; i < command.getSwapSize(); i++){
				long pSet = pSets.get(i);
				UniformDescriptorSetList.add(pSet);
				bufferInfo.offset(i * UniformAlignedSize);
				pWrites.dstSet(pSet);
				vkUpdateDescriptorSets(device.logicalDevice,pWrites,null);
			}

			PointerBuffer pMap = stack.mallocPointer(1);

			vkResult = vkMapMemory(
					device.logicalDevice,
					UniformStagingMemory,
					0,
					uniformAllocSize,
					0,
					pMap
			);
			VulkanUtility.ValidateSuccess("Failed to map uniform staging memory",vkResult);
			UniformStagingMappedMemory = pMap.getByteBuffer((int)uniformAllocSize);
		}
	}

	public void ResetForFrame() {
		bufferCopyCount = 0;
		//TODO: MOVE ELSEWHERE {CAN RUN ASYNC AFTER MEMORY HAS BEEN MANAGED!}
		memory.tick();
	}

	public void CmdTransferBufferData(VkCommandBuffer buffer, WorldDrawTask worldDrawInfo) {
		try(MemoryStack stack = stackPush()) {
			int offset = command.getSwapIndex() * UniformAlignedSize;
			//0
			UniformStagingMappedMemory.position(offset);
			worldDrawInfo.perspectiveMatrix.get(UniformStagingMappedMemory);
			//64
			UniformStagingMappedMemory.position(offset+64);
			worldDrawInfo.cameraMatrix.get(UniformStagingMappedMemory);
			//128
			UniformStagingMappedMemory.position(offset+128);
			UniformStagingMappedMemory.putFloat(worldDrawInfo.playerX);
			UniformStagingMappedMemory.putFloat(worldDrawInfo.playerY);
			UniformStagingMappedMemory.putFloat(worldDrawInfo.playerZ);
			//144
			UniformStagingMappedMemory.position(offset+144);
			UniformStagingMappedMemory.putFloat(0);
			UniformStagingMappedMemory.putFloat(-1);
			UniformStagingMappedMemory.putFloat(0);
			//160
			UniformStagingMappedMemory.position(offset+160);
			UniformStagingMappedMemory.putFloat(1);
			UniformStagingMappedMemory.putFloat(1);
			UniformStagingMappedMemory.putFloat(1);
			//172
			UniformStagingMappedMemory.position(offset+172);
			UniformStagingMappedMemory.putInt(0);
			//Fin
			UniformStagingMappedMemory.position(0);

			VkBufferCopy.Buffer pRegions = VkBufferCopy.mallocStack(1,stack);
			pRegions.srcOffset(command.getSwapIndex() * UniformAlignedSize);
			pRegions.dstOffset(command.getSwapIndex() * UniformAlignedSize);
			pRegions.size(UniformAlignedSize);

			vkCmdCopyBuffer(buffer, UniformStagingBuffer, UniformBuffer, pRegions);
		}
	}

	public void CmdUniformMemoryBarrier(VkCommandBuffer buffer,MemoryStack stack) {
		VkBufferMemoryBarrier.Buffer uniformBarrier = VkBufferMemoryBarrier.mallocStack(1,stack);
		uniformBarrier.sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER);
		uniformBarrier.pNext(VK_NULL_HANDLE);
		uniformBarrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
		uniformBarrier.dstAccessMask(VK_ACCESS_UNIFORM_READ_BIT);
		uniformBarrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
		uniformBarrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
		uniformBarrier.buffer(UniformBuffer);
		uniformBarrier.offset(command.getSwapIndex() * UniformAlignedSize);
		uniformBarrier.size(UniformAlignedSize);

		vkCmdPipelineBarrier(
			buffer,
			VK_PIPELINE_STAGE_TRANSFER_BIT,
			(
				VK_PIPELINE_STAGE_VERTEX_SHADER_BIT |
				VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT
			),
			0,
			null,
			uniformBarrier,
			null
		);
	}

	public void CmdTransferDataBarrier(VkCommandBuffer buffer) {
		if(bufferCopyCount != 0) {
			BufferTransferBarriers.position(0);
			BufferTransferBarriers.limit(bufferCopyCount);
			vkCmdPipelineBarrier(
				buffer,
				VK_PIPELINE_STAGE_TRANSFER_BIT,
				VK_PIPELINE_STAGE_VERTEX_INPUT_BIT,
				0,
				null,
				BufferTransferBarriers,
				null
			);
			BufferTransferBarriers.limit(BufferTransferBarriers.capacity());
		}
	}

	public void CmdBindDescriptorSet(VkCommandBuffer buffer) {
		try(MemoryStack stack = stackPush()){
			vkCmdBindDescriptorSets(
					buffer,
					VK_PIPELINE_BIND_POINT_GRAPHICS,
					cache.PIPELINE_LAYOUT_WORLD_STANDARD_INPUT,
					0,
					stack.longs(
							UniformDescriptorSetList.get(command.getSwapIndex()),
							cache.DESCRIPTOR_SET_ATLAS
					),
					null
			);
		}
	}

	public boolean hasWorld() {
		return this.theWorld != null;
	}

	public void close() {
		vkUnmapMemory(command.getDevice(),UniformStagingMemory);

		vkDestroyDescriptorPool(command.getDevice(),UniformDescriptorPool,null);

		vkDestroyBuffer(command.getDevice(),UniformBuffer,null);
		vkDestroyBuffer(command.getDevice(),UniformStagingBuffer,null);

		raw_memory.freeDedicatedMemory(UniformBufferMemory);
		raw_memory.freeDedicatedMemory(UniformStagingMemory);

		memory.close();

		BufferTransferBarriers.free();
	}

	@Override
	public void StartAsyncGenerate(AsyncWorldHandler handler,int asyncID) {
		try(MemoryStack stack = stackPush()) {
			VkCommandBuffer transfer = command.getAsyncTransferCommandBuffer(asyncID);
			VkCommandBuffer graphics = command.getAsyncMainCommandBuffer(asyncID);

			VkCommandBufferInheritanceInfo inheritanceInfo = VkCommandBufferInheritanceInfo.mallocStack(stack);
			inheritanceInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_INHERITANCE_INFO);
			inheritanceInfo.pNext(VK_NULL_HANDLE);
			inheritanceInfo.renderPass(0);
			inheritanceInfo.subpass(0);
			inheritanceInfo.framebuffer(0);
			inheritanceInfo.occlusionQueryEnable(false);
			inheritanceInfo.queryFlags(0);
			inheritanceInfo.pipelineStatistics(0);

			VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.mallocStack(stack);
			beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
			beginInfo.pNext(VK_NULL_HANDLE);
			beginInfo.flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
			beginInfo.pInheritanceInfo(inheritanceInfo);

			int vkResult = vkBeginCommandBuffer(transfer,beginInfo);
			VulkanUtility.ValidateSuccess("Failed to begin async transfer buffer",vkResult);

			beginInfo.flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT |
					                VK_COMMAND_BUFFER_RESET_RELEASE_RESOURCES_BIT);

			//TODO: IMPLEMENT PROPERLY
			beginInfo.flags(
					VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT |
					VK_COMMAND_BUFFER_USAGE_RENDER_PASS_CONTINUE_BIT
			);
			inheritanceInfo.renderPass(cache.RENDER_PASS_FORWARD_ONLY.RenderPass);
			inheritanceInfo.subpass(0);
			inheritanceInfo.framebuffer(command.getFrameBuffer_ForwardOnly());

			vkResult = vkBeginCommandBuffer(graphics,beginInfo);
			VulkanUtility.ValidateSuccess("Failed to begin async graphics buffer",vkResult);

			vkCmdBindPipeline(graphics,VK_PIPELINE_BIND_POINT_GRAPHICS, cache.PIPELINE_FORWARD_WORLD.getPipeline());

			VkViewport.Buffer pViewport = VkViewport.mallocStack(1,stack);
			pViewport.x(0);
			pViewport.y(0);
			pViewport.width(screenWidth);
			pViewport.height(screenHeight);
			pViewport.minDepth(0.0f);
			pViewport.maxDepth(1.0f);

			vkCmdSetViewport(graphics,0,pViewport);

			VkRect2D.Buffer pScissor = VkRect2D.mallocStack(1,stack);
			pScissor.offset().set(0,0);
			pScissor.extent().set(screenWidth,screenHeight);

			vkCmdSetScissor(graphics,0,pScissor);

			CmdBindDescriptorSet(graphics);
		}
	}

	@Override
	public void StopAsyncGenerate(AsyncWorldHandler handler,int asyncID) {
		VkCommandBuffer transfer = command.getAsyncTransferCommandBuffer(asyncID);
		VkCommandBuffer graphics = command.getAsyncMainCommandBuffer(asyncID);

		int vkResult = vkEndCommandBuffer(transfer);
		VulkanUtility.ValidateSuccess("Failed to end async transfer buffer",vkResult);
		vkResult = vkEndCommandBuffer(graphics);
		VulkanUtility.ValidateSuccess("Failed to end async graphics buffer",vkResult);
	}

	private void CmdDeviceTransfer(int asyncID,int from, int to, int size) {
		try(MemoryStack stack = stackPush()) {
			VkCommandBuffer transfer = command.getAsyncTransferCommandBuffer(asyncID);

			long hostOffset = memory.getOffsetForHost(from);
			long hostBuffer = memory.GetHostBuffer(from);

			long deviceOffset = memory.GetDeviceOffset(to);
			long deviceBuffer = memory.GetDeviceBuffer(to);

			VkBufferCopy.Buffer pRegions = VkBufferCopy.mallocStack(1,stack);
			pRegions.position(0);
			pRegions.srcOffset(hostOffset);
			pRegions.dstOffset(deviceOffset);
			pRegions.size(size);

			System.out.println("srcOffset="+pRegions.srcOffset()+", dstOffset="+pRegions.dstOffset()+", size="+pRegions.size());
			vkCmdCopyBuffer(transfer,hostBuffer,deviceBuffer,pRegions);

			//Create A Buffer Copy Barrier {...}
			bufferCopyLock.lock();
			int idx = bufferCopyCount;

			//TODO: PRE_GEN MORE OF THE VALUES SO THAT LESS OPERATIONS HAPPEN IN THE LOCK
			BufferTransferBarriers.position(idx);
			BufferTransferBarriers.sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER);
			BufferTransferBarriers.pNext(VK_NULL_HANDLE);
			BufferTransferBarriers.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
			BufferTransferBarriers.dstAccessMask(VK_ACCESS_VERTEX_ATTRIBUTE_READ_BIT);
			BufferTransferBarriers.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
			BufferTransferBarriers.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
			BufferTransferBarriers.buffer(deviceBuffer);
			BufferTransferBarriers.offset(deviceOffset);
			BufferTransferBarriers.size(size);
			BufferTransferBarriers.position(0);

			bufferCopyCount++;
			bufferCopyLock.unlock();
		}
	}


	@Override
	protected void AsyncDraw(AsyncWorldHandler handle, ClientChunkSection chunkSection, int asyncID) {
		float chunkOffsetX = 16.F * (chunkSection.getChunkX() - originX);
		float chunkOffsetY = 16.F * (chunkSection.getChunkY());
		float chunkOffsetZ = 16.F * (chunkSection.getChunkZ() - originZ);
		try(MemoryStack stack = stackPush()) {
			if(chunkSection.Renderer_Size_Opaque != -1) {
				VkCommandBuffer graphics = command.getAsyncMainCommandBuffer(asyncID);
				vkCmdBindVertexBuffers(
						graphics,
						0,
						stack.longs(memory.GetDeviceBuffer(chunkSection.Renderer_Info_Opaque)),
						stack.longs(memory.GetDeviceOffset(chunkSection.Renderer_Info_Opaque))
				);

				vkCmdPushConstants(
						graphics,
						cache.PIPELINE_LAYOUT_WORLD_STANDARD_INPUT,
						VK_SHADER_STAGE_VERTEX_BIT,
						0,
						stack.floats(chunkOffsetX,chunkOffsetY,chunkOffsetZ)
				);
				vkCmdDraw(
						graphics,
						chunkSection.Renderer_Size_Opaque / 32,
						1,
						0,
						0
				);
			}
			if(chunkSection.Renderer_Size_Transparent != -1) {
				VkCommandBuffer graphics = command.getAsyncMainCommandBuffer(asyncID);
				vkCmdBindVertexBuffers(
						graphics,
						0,
						stack.longs(memory.GetDeviceBuffer(chunkSection.Renderer_Info_Transparent)),
						stack.longs(memory.GetDeviceOffset(chunkSection.Renderer_Info_Transparent))
				);

				vkCmdPushConstants(
						graphics,
						cache.PIPELINE_LAYOUT_WORLD_STANDARD_INPUT,
						VK_SHADER_STAGE_VERTEX_BIT,
						0,
						stack.floats(chunkOffsetX,chunkOffsetY,chunkOffsetZ)
				);
				vkCmdDraw(
						graphics,
						chunkSection.Renderer_Size_Transparent / 32,
						1,
						0,
						0
				);
			}
		}
	}

	@Override
	public void InvalidateChunkSection(ClientChunkSection section) {
		if(section.Renderer_Size_Transparent != -1) {
			memory.FreeMemoryFromDevice(section.Renderer_Info_Transparent, command.getSwapSize());
		}
		if(section.Renderer_Size_Opaque != -1) {
			memory.FreeMemoryFromDevice(section.Renderer_Info_Opaque, command.getSwapSize());
		}
		section.Renderer_Size_Opaque = -1;
		section.Renderer_Size_Transparent = -1;
		section.Renderer_Info_Opaque = 0;
		section.Renderer_Info_Transparent = 0;
	}


	@Override
	protected void AllocateChunkMemory(AsyncWorldHandler handle, boolean isOpaque) {
		handle.memory_id = memory.allocHostMemory(DEFAULT_MEMORY_SIZE);
		handle.memoryMap = memory.mapHostMemory(handle.memory_id);
		handle.start_offset = (int)memory.getOffsetForHost(handle.memory_id);
		handle.write_offset = handle.start_offset;
		handle.end_offset = handle.start_offset + DEFAULT_MEMORY_SIZE;
	}

	@Override
	protected void ExpandChunkMemory(AsyncWorldHandler handle, boolean isOpaque) {
		int current_size = handle.write_offset - handle.start_offset;
		int new_size = current_size  + DEFAULT_MEMORY_SIZE;

		//Allocate New Memory
		int new_memory = memory.allocHostMemory(new_size);
		ByteBuffer new_mapping = memory.mapHostMemory(new_memory);
		int new_offset = (int)memory.getOffsetForHost(new_memory);

		//Copy Memory
		MemoryUtil.memCopy(
				MemoryUtil.memAddress(handle.memoryMap,handle.start_offset),
				MemoryUtil.memAddress(new_mapping,new_offset),
				current_size
		);

		//Invalidate Old Memory
		memory.unMapHostMemory(handle.memory_id);
		memory.InvalidateHostMemory(handle.memory_id);

		//Update Values
		handle.memory_id = new_memory;
		handle.memoryMap = new_mapping;
		handle.start_offset = new_offset;
		handle.write_offset = handle.start_offset + current_size;
		handle.end_offset = handle.start_offset + new_size;
	}

	@Override
	protected void FinalizeChunkMemory(AsyncWorldHandler handle,int asyncID,ClientChunkSection section, boolean isOpaque) {
		int actual_size = handle.write_offset - handle.start_offset;
		int invalidate_countdown = command.getSwapSize()+1;
		//Free Old Data
		if(isOpaque) {
			if (section.Renderer_Size_Opaque != -1) {
				memory.FreeMemoryFromDevice(section.Renderer_Info_Opaque, invalidate_countdown);
				section.Renderer_Size_Opaque = -1;
			}
		}else{
			if (section.Renderer_Size_Transparent != -1) {
				memory.FreeMemoryFromDevice(section.Renderer_Info_Transparent, invalidate_countdown);
				section.Renderer_Size_Transparent = -1;
			}
		}

		//Store New Data
		if(actual_size == 0) {
			memory.unMapHostMemory(handle.memory_id);
			memory.InvalidateHostMemory(handle.memory_id);
			if(isOpaque) {
				section.Renderer_Size_Opaque = -1;
				section.Renderer_Info_Opaque = 0;
			}else{
				section.Renderer_Size_Transparent = -1;
				section.Renderer_Info_Transparent = 0;
			}
		}else {
			memory.shrinkHostMemory(handle.memory_id, actual_size);
			memory.unMapHostMemory(handle.memory_id);
			if (isOpaque) {
				int device_memory = memory.GetDeviceMemory(handle.memory_id);
				CmdDeviceTransfer(asyncID, handle.memory_id, device_memory, actual_size);
				memory.FreeHostMemory(handle.memory_id, invalidate_countdown);
				section.Renderer_Info_Opaque = device_memory;
				section.Renderer_Size_Opaque = actual_size;
			} else {
				int device_memory = memory.GetDeviceMemory(handle.memory_id);
				CmdDeviceTransfer(asyncID, handle.memory_id, device_memory, actual_size);
				memory.FreeHostMemory(handle.memory_id, invalidate_countdown);
				section.Renderer_Info_Transparent = device_memory;
				section.Renderer_Size_Transparent = actual_size;
			}
		}
		//Clean-up
		handle.memory_id = 0;
		handle.memoryMap = null;
	}



}
