package net.openvoxel.client.renderer.vk.world.draw;

import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;
import net.openvoxel.client.renderer.vk.VulkanCache;
import net.openvoxel.client.renderer.vk.VulkanCommandHandler;
import net.openvoxel.client.renderer.vk.core.VulkanUtility;
import net.openvoxel.client.renderer.vk.world.VulkanWorldRenderer;
import net.openvoxel.client.renderer.vk.world.draw.snips.DrawUtility;
import net.openvoxel.world.client.ClientChunkSection;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferBeginInfo;
import org.lwjgl.vulkan.VkCommandBufferInheritanceInfo;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class WorldDrawForward implements IWorldDraw {

	private TLongList pools;
	private List<VkCommandBuffer> buffers;

	private static final int DRAW_COUNT = 2;

	public WorldDrawForward() {
		pools = new TLongArrayList();
		buffers = new ArrayList<>();
	}

	@Override
	public int getShadowCascadeCount() {
		return 0;
	}

	@Override
	public int getNearbyCullSize() {
		return 0;
	}

	@Override
	public void load(VulkanCommandHandler handler, int asyncCount) {
		DrawUtility.InitCommandPools(
				handler.getDevice(),
				handler.getDeviceManager().familyQueue,
				handler.getSwapSize(),
				asyncCount,
				DRAW_COUNT,
				pools,
				buffers
		);
	}

	@Override
	public void close(VulkanCommandHandler handler) {
		DrawUtility.DestroyCommandPools(handler.getDevice(),pools);
		pools.clear();
		buffers.clear();
	}


	@Override
	public void beginAsync(VulkanCommandHandler commandHandler,
	                       VulkanCache cache,
	                       VulkanWorldRenderer.VulkanAsyncWorldHandler asyncHandler) {
		asyncHandler.drawStandardOpaque = DrawUtility.getCommandBuffer(
				commandHandler.getSwapSize(),
				DRAW_COUNT,
				commandHandler.getSwapIndex(),
				asyncHandler.asyncID,
				0,
				buffers
		);
		asyncHandler.layoutStandardOpaque = cache.PIPELINE_LAYOUT_WORLD_STANDARD_INPUT;
		asyncHandler.drawStandardTransparent = DrawUtility.getCommandBuffer(
				commandHandler.getSwapSize(),
				DRAW_COUNT,
				commandHandler.getSwapIndex(),
				asyncHandler.asyncID,
				1,
				buffers
		);
		asyncHandler.layoutStandardTransparent = cache.PIPELINE_LAYOUT_WORLD_STANDARD_INPUT;
		try(MemoryStack stack = stackPush()) {
			VkCommandBufferInheritanceInfo inheritanceInfo = VkCommandBufferInheritanceInfo.mallocStack(stack);
			inheritanceInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_INHERITANCE_INFO);
			inheritanceInfo.pNext(VK_NULL_HANDLE);

			//TODO: IMPLEMENT CORRECT SELECTION
			inheritanceInfo.renderPass(cache.RENDER_PASS_FORWARD_ONLY.RenderPass);
			inheritanceInfo.subpass(0);
			inheritanceInfo.framebuffer(commandHandler.getFrameBuffer_ForwardOnly());

			//No Occlusion Queries
			inheritanceInfo.occlusionQueryEnable(false);
			inheritanceInfo.queryFlags(0);
			inheritanceInfo.pipelineStatistics(0);

			VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.mallocStack(stack);
			beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
			beginInfo.pNext(VK_NULL_HANDLE);
			beginInfo.flags(
					VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT |
					VK_COMMAND_BUFFER_USAGE_RENDER_PASS_CONTINUE_BIT
			);
			beginInfo.pInheritanceInfo(inheritanceInfo);

			int vkResult = vkBeginCommandBuffer(asyncHandler.drawStandardOpaque,beginInfo);
			VulkanUtility.ValidateSuccess("Failed to begin command buffer",vkResult);
			vkResult = vkBeginCommandBuffer(asyncHandler.drawStandardTransparent,beginInfo);
			VulkanUtility.ValidateSuccess("Failed to begin command buffer",vkResult);
		}
	}

	@Override
	public void endAsync(VulkanCommandHandler commandHandler,
        VulkanWorldRenderer.VulkanAsyncWorldHandler asyncHandler){
		int vkResult = vkEndCommandBuffer(asyncHandler.drawStandardOpaque);
		VulkanUtility.ValidateSuccess("Failed to end command buffer",vkResult);
		vkResult = vkEndCommandBuffer(asyncHandler.drawStandardTransparent);
		VulkanUtility.ValidateSuccess("Failed to end command buffer",vkResult);
	}

	@Override
	public void asyncDrawStandard(VulkanWorldRenderer.VulkanAsyncWorldHandler handler,
	                              ClientChunkSection section,
	                              int offsetX, int offsetY, int offsetZ) {

		try(MemoryStack stack = stackPush()) {
			if(section.Renderer_Size_Opaque != -1) {
				VkCommandBuffer buffer = handler.drawStandardOpaque;
				long opaqueBuffer = handler.getDeviceBuffer(section.Renderer_Info_Opaque);
				long opaqueOffset = handler.getDeviceOffset(section.Renderer_Info_Opaque);

				vkCmdBindVertexBuffers(
						buffer,
						0,
						stack.longs(opaqueBuffer),
						stack.longs(opaqueOffset)
				);
				vkCmdPushConstants(
						buffer,
						handler.layoutStandardOpaque,
						VK_SHADER_STAGE_VERTEX_BIT,
						0,
						stack.floats(offsetX, offsetY, offsetZ)
				);
				vkCmdDraw(
						buffer,
						section.Renderer_Size_Opaque / 32,
						1,
						0,
						0
				);
			}
			if(section.Renderer_Size_Transparent != -1) {
				VkCommandBuffer buffer = handler.drawStandardTransparent;
				long transparentBuffer = handler.getDeviceBuffer(section.Renderer_Info_Transparent);
				long transparentOffset = handler.getDeviceOffset(section.Renderer_Info_Transparent);

				vkCmdBindVertexBuffers(
						buffer,
						0,
						stack.longs(transparentBuffer),
						stack.longs(transparentOffset)
				);
				vkCmdPushConstants(
						buffer,
						handler.layoutStandardTransparent,
						VK_SHADER_STAGE_VERTEX_BIT,
						0,
						stack.floats(offsetX, offsetY, offsetZ)
				);
				vkCmdDraw(
						buffer,
						section.Renderer_Size_Transparent / 32,
						1,
						0,
						0
				);
			}
		}
	}

	@Override
	public void asyncDrawShadows(VulkanWorldRenderer.VulkanAsyncWorldHandler handler, ClientChunkSection section, int offsetX, int offsetY, int offsetZ) {
		//NO OP
	}

	@Override
	public void asyncDrawNearby(VulkanWorldRenderer.VulkanAsyncWorldHandler handler, ClientChunkSection section, int offsetX, int offsetY, int offsetZ) {
		//NO OP
	}

	@Override
	public void executeDrawCommands(VkCommandBuffer buffer, MemoryStack stack, List<VulkanWorldRenderer.VulkanAsyncWorldHandler> asyncList) {
		//TODO: CONDITIONAL ENABLE IF POST OR ANTI_ALIAS ENABLED!!
	}

	@Override
	public void drawForwardRenderer(VkCommandBuffer buffer, MemoryStack old_stack, List<VulkanWorldRenderer.VulkanAsyncWorldHandler> asyncList) {
		//TODO: OPTIONAL RENDER IN ANOTHER LOCATION
		try(MemoryStack stack = old_stack.push()) {
			PointerBuffer pCommands = stack.mallocPointer(asyncList.size());
			for(int i = 0; i < asyncList.size(); i++) {
				pCommands.put(i,asyncList.get(i).drawStandardOpaque);
			}
			vkCmdExecuteCommands(buffer,pCommands);
			for(int i = 0; i < asyncList.size(); i++) {
				pCommands.put(i,asyncList.get(i).drawStandardTransparent);
			}
			vkCmdExecuteCommands(buffer,pCommands);
		}
	}

}
