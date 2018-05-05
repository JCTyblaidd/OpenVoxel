package net.openvoxel.client.renderer.vk.world;

import net.openvoxel.client.renderer.base.BaseWorldRenderer;
import net.openvoxel.client.renderer.vk.VulkanCache;
import net.openvoxel.client.renderer.vk.VulkanCommandHandler;
import net.openvoxel.client.renderer.vk.core.VulkanDevice;
import net.openvoxel.client.renderer.vk.core.VulkanMemory;
import net.openvoxel.client.renderer.vk.core.VulkanUtility;
import net.openvoxel.world.client.ClientChunkSection;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanWorldRenderer extends BaseWorldRenderer {

	private VulkanCommandHandler command;
	private VulkanWorldMemoryManager memory;
	private VulkanCache cache;

	private static final int DEFAULT_MEMORY_SIZE = VulkanWorldMemoryPage.SUB_PAGE_SIZE * 4;


	public VulkanWorldRenderer(VulkanCommandHandler command,VulkanCache cache, VulkanDevice device, VulkanMemory memory) {
		this.command = command;
		this.cache = cache;
		this.memory = new VulkanWorldMemoryManager(device,memory);
	}

	public void close() {
		memory.close();
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
			inheritanceInfo.renderPass(cache.RENDER_PASS_FORWARD_ONLY.RenderPass);
			inheritanceInfo.subpass(0);
			inheritanceInfo.framebuffer(command.getFrameBuffer_ForwardOnly());

			vkResult = vkBeginCommandBuffer(graphics,beginInfo);
			VulkanUtility.ValidateSuccess("Failed to begin async graphics buffer",vkResult);
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
			VkBufferCopy.Buffer pRegions = VkBufferCopy.mallocStack(1,stack);
			pRegions.srcOffset(memory.getOffsetForHost(from));
			pRegions.dstOffset(memory.GetDeviceOffset(to));
			pRegions.size(size);
			vkCmdCopyBuffer(transfer,memory.GetHostBuffer(from),memory.GetDeviceBuffer(to),pRegions);
		}
	}


	@Override
	protected void AsyncDraw(AsyncWorldHandler handle, ClientChunkSection chunkSection, int asyncID) {
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
						VK_PIPELINE_STAGE_ALL_GRAPHICS_BIT,
						0,
						stack.floats(originX,16.F * chunkSection.yIndex,originZ)
				);
				vkCmdDraw(
						graphics,
						chunkSection.Renderer_Size_Opaque / 32,
						1,
						0,
						0
				);
			}
		}
	}

	@Override
	public void InvalidateChunkSection(ClientChunkSection section) {
		if(section.Renderer_Info_Opaque != -1) {
			memory.FreeMemoryFromDevice(section.Renderer_Info_Opaque, command.getSwapSize());
		}
		if(section.Renderer_Size_Opaque != -1) {
			memory.FreeMemoryFromDevice(section.Renderer_Info_Transparent, command.getSwapSize());
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
		handle.end_offset = handle.start_offset + DEFAULT_MEMORY_SIZE;
	}

	@Override
	protected void ExpandChunkMemory(AsyncWorldHandler handle, boolean isOpaque) {
		int current_size = handle.write_offset - handle.start_offset;
		int new_size = current_size  + DEFAULT_MEMORY_SIZE;
		int new_memory = memory.allocHostMemory(new_size);
		ByteBuffer new_mapping = memory.mapHostMemory(new_memory);
		int new_offset = (int)memory.getOffsetForHost(new_memory);
		MemoryUtil.memCopy(
				MemoryUtil.memAddress(handle.memoryMap,handle.write_offset),
				MemoryUtil.memAddress(new_mapping,new_offset),
				current_size
		);
		memory.unMapHostMemory(handle.memory_id);
		memory.InvalidateHostMemory(handle.memory_id);
		handle.memory_id = new_memory;
		handle.memoryMap = new_mapping;
		handle.start_offset = new_offset;
		handle.end_offset = handle.start_offset + new_size;
	}

	@Override
	protected void FinalizeChunkMemory(AsyncWorldHandler handle,int asyncID,ClientChunkSection section, boolean isOpaque) {
		int actual_size = handle.write_offset - handle.start_offset;
		if(actual_size == 0) {
			memory.unMapHostMemory(handle.memory_id);
			memory.InvalidateHostMemory(handle.memory_id);
		}
		memory.shrinkHostMemory(handle.memory_id,actual_size);
		memory.unMapHostMemory(handle.memory_id);
		if(isOpaque) {
			if(section.Renderer_Size_Opaque != -1) {
				memory.FreeMemoryFromDevice(section.Renderer_Info_Opaque,command.getSwapSize());
				section.Renderer_Size_Opaque = -1;
			}
			//TRANSFER & UPDATE MEMORY
			int device_memory = memory.GetDeviceMemory(handle.memory_id);
			CmdDeviceTransfer(asyncID,handle.memory_id,device_memory,actual_size);
			memory.FreeHostMemory(handle.memory_id,command.getSwapSize());
			section.Renderer_Info_Opaque = device_memory;
			section.Renderer_Size_Opaque = actual_size;
		}else{
			if(section.Renderer_Size_Transparent != -1) {
				memory.FreeMemoryFromDevice(section.Renderer_Info_Transparent,command.getSwapSize());
				section.Renderer_Size_Transparent = -1;
			}
			int device_memory = memory.GetDeviceMemory(handle.memory_id);
			CmdDeviceTransfer(asyncID,handle.memory_id,device_memory,actual_size);
			memory.FreeHostMemory(handle.memory_id,command.getSwapSize());
			section.Renderer_Info_Transparent = device_memory;
			section.Renderer_Size_Transparent = actual_size;
		}
		//Clean-up
		handle.memory_id = 0;
		handle.memoryMap = null;
	}



}
