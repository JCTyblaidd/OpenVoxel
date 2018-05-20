package net.openvoxel.client.renderer.vk.world.draw;

import net.openvoxel.client.renderer.vk.core.VulkanMemory;
import net.openvoxel.client.renderer.vk.core.VulkanUtility;
import net.openvoxel.client.renderer.vk.pipeline.VulkanRenderPass;
import net.openvoxel.client.renderer.vk.world.VulkanWorldRenderer;
import net.openvoxel.world.client.ClientChunkSection;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

/**
 *  World Draw with the implementation of draw call resource management
 */
public abstract class BaseWorldDraw implements IWorldDraw {

	//
	// Static Configuration
	//
	private static final boolean LAZY_BIND = true;

	//
	// Configuration
	//
	protected boolean ShadowCascadeColoured = false;
	private   int     ShadowCascadeCount = 0;

	//Shadow Cascade Variables
	protected long ShadowCascadeImageColour;
	protected long ShadowCascadeImage;
	protected long ShadowCascadeImageViewColour;
	protected long ShadowCascadeImageView;
	protected long ShadowCascadeImageMemoryColour;
	protected long ShadowCascadeImageMemory;
	protected long ShadowCascadeFramebuffer;

	protected void LoadShadowCascades(VkDevice device, VulkanMemory memory, int count, int size,boolean coloured,long renderPass) {
		ShadowCascadeCount = count;
		ShadowCascadeColoured = coloured;
		try(MemoryStack stack = stackPush()) {
			VkImageCreateInfo imageCreate = VkImageCreateInfo.mallocStack(stack);
			imageCreate.sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO);
			imageCreate.pNext(VK_NULL_HANDLE);
			imageCreate.flags(0);
			imageCreate.imageType(VK_IMAGE_TYPE_2D);
			imageCreate.format(VulkanRenderPass.formatSimpleDepthSampled);
			imageCreate.extent().set(size,size,1);
			imageCreate.mipLevels(1);
			imageCreate.arrayLayers(ShadowCascadeCount);
			imageCreate.samples(VK_SAMPLE_COUNT_1_BIT);
			imageCreate.tiling(VK_IMAGE_TILING_OPTIMAL);
			imageCreate.usage(
					VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT |
					VK_IMAGE_USAGE_SAMPLED_BIT
			);
			imageCreate.sharingMode(VK_SHARING_MODE_EXCLUSIVE);
			imageCreate.pQueueFamilyIndices(null);
			imageCreate.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);

			LongBuffer pResult = stack.mallocLong(1);
			int vkResult = vkCreateImage(device,imageCreate,null,pResult);
			VulkanUtility.ValidateSuccess("Failed to create Cascade Image",vkResult);
			ShadowCascadeImage = pResult.get(0);
			ShadowCascadeImageMemory = memory.allocateDedicatedImage(
				ShadowCascadeImage,
				VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT
			);
			vkResult = vkBindImageMemory(device,ShadowCascadeImage,ShadowCascadeImageMemory,0L);
			VulkanUtility.ValidateSuccess("Failed to bind Cascade Image",vkResult);

			if(coloured) {
				imageCreate.format(VulkanRenderPass.formatSimpleAttachmentSampled);
				imageCreate.usage(
						VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT |
						VK_IMAGE_USAGE_SAMPLED_BIT
				);

				vkResult = vkCreateImage(device,imageCreate,null,pResult);
				VulkanUtility.ValidateSuccess("Failed to create Cascade Image",vkResult);
				ShadowCascadeImageColour = pResult.get(0);
				ShadowCascadeImageMemoryColour = memory.allocateDedicatedImage(
					ShadowCascadeImageColour,
					VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT
				);
				vkResult = vkBindImageMemory(device,ShadowCascadeImageColour,
						ShadowCascadeImageMemoryColour,0L);
				VulkanUtility.ValidateSuccess("Failed to bind Cascade Image",vkResult);
			}


			VkImageViewCreateInfo viewCreate = VkImageViewCreateInfo.mallocStack(stack);
			viewCreate.sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO);
			viewCreate.pNext(VK_NULL_HANDLE);
			viewCreate.flags(0);
			viewCreate.image(ShadowCascadeImage);
			viewCreate.viewType(VK_IMAGE_VIEW_TYPE_2D_ARRAY);
			viewCreate.format(VulkanRenderPass.formatSimpleDepthSampled);
			viewCreate.components().set(
					VK_COMPONENT_SWIZZLE_IDENTITY,
					VK_COMPONENT_SWIZZLE_IDENTITY,
					VK_COMPONENT_SWIZZLE_IDENTITY,
					VK_COMPONENT_SWIZZLE_IDENTITY
			);
			viewCreate.subresourceRange().set(
					VK_IMAGE_ASPECT_DEPTH_BIT,
					0,
					1,
					0,
					ShadowCascadeCount
			);
			vkResult = vkCreateImageView(device,viewCreate,null,pResult);
			VulkanUtility.ValidateSuccess("Failed to create Cascade Image View",vkResult);
			ShadowCascadeImageView = pResult.get(0);

			if(coloured) {
				viewCreate.image(ShadowCascadeImageColour);
				viewCreate.format(VulkanRenderPass.formatSimpleAttachmentSampled);
				viewCreate.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);

				vkResult = vkCreateImageView(device,viewCreate,null,pResult);
				VulkanUtility.ValidateSuccess("Failed to create Cascade Image View",vkResult);
				ShadowCascadeImageViewColour = pResult.get(0);
			}

			VkFramebufferCreateInfo framebufferCreate = VkFramebufferCreateInfo.mallocStack(stack);
			framebufferCreate.sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO);
			framebufferCreate.pNext(VK_NULL_HANDLE);
			framebufferCreate.flags(0);
			framebufferCreate.renderPass(renderPass);
			framebufferCreate.pAttachments(
					stack.longs(
							ShadowCascadeImageViewColour,
							ShadowCascadeImageView
					)
			);
			framebufferCreate.width(size);
			framebufferCreate.height(size);
			framebufferCreate.layers(count);

			vkResult = vkCreateFramebuffer(device,framebufferCreate,null,pResult);
			VulkanUtility.ValidateSuccess("Failed to create Cascade Frame Buffer",vkResult);
			ShadowCascadeFramebuffer = pResult.get(0);
		}
	}

	protected void FreeShadowCascades(VkDevice device, VulkanMemory memory) {
		if(ShadowCascadeCount > 0) {
			vkDestroyFramebuffer(device,ShadowCascadeFramebuffer,null);
			vkDestroyImageView(device,ShadowCascadeImageView,null);
			vkDestroyImage(device,ShadowCascadeImage,null);
			memory.freeDedicatedMemory(ShadowCascadeImageMemory);
			if(ShadowCascadeColoured) {
				vkDestroyImageView(device,ShadowCascadeImageViewColour,null);
				vkDestroyImage(device,ShadowCascadeImageColour,null);
				memory.freeDedicatedMemory(ShadowCascadeImageMemoryColour);
			}
			ShadowCascadeCount = 0;
		}
	}

	@Override
	public int getShadowCascadeCount() {
		return ShadowCascadeCount;
	}


	

	///
	/// Implementation Of Standard Draw Call Code
	///

	@Override
	public void asyncDrawStandard(VulkanWorldRenderer.VulkanAsyncWorldHandler handler,
	                              ClientChunkSection section,
	                              float offsetX, float offsetY, float offsetZ) {

		try(MemoryStack stack = stackPush()) {
			if(section.Renderer_Size_Opaque != -1) {
				VkCommandBuffer buffer = handler.drawStandardOpaque;
				long opaqueBuffer = handler.getDeviceBuffer(section.Renderer_Info_Opaque);
				long opaqueOffset = handler.getDeviceOffset(section.Renderer_Info_Opaque);
				int firstVertex;
				int vertexCount = section.Renderer_Size_Opaque / 32;
				if(LAZY_BIND) {
					if(opaqueBuffer != handler.lastBoundBufferStandardOpaque) {
						vkCmdBindVertexBuffers(
								buffer,
								0,
								stack.longs(opaqueBuffer),
								stack.longs(0L)
						);
						handler.lastBoundBufferStandardOpaque = opaqueBuffer;
					}
					firstVertex = (int)opaqueOffset / 32;
				}else {
					vkCmdBindVertexBuffers(
							buffer,
							0,
							stack.longs(opaqueBuffer),
							stack.longs(opaqueOffset)
					);
					firstVertex = 0;
				}
				vkCmdPushConstants(
						buffer,
						handler.layoutStandardOpaque,
						VK_SHADER_STAGE_VERTEX_BIT,
						0,
						stack.floats(offsetX, offsetY, offsetZ)
				);
				vkCmdDraw(
						buffer,
						vertexCount,
						1,
						firstVertex,
						0
				);
			}
			if(section.Renderer_Size_Transparent != -1) {
				VkCommandBuffer buffer = handler.drawStandardTransparent;
				long transparentBuffer = handler.getDeviceBuffer(section.Renderer_Info_Transparent);
				long transparentOffset = handler.getDeviceOffset(section.Renderer_Info_Transparent);
				int firstVertex;
				int vertexCount = section.Renderer_Size_Transparent / 32;
				if(LAZY_BIND) {
					if(transparentBuffer != handler.lastBoundBufferStandardTransparent) {
						vkCmdBindVertexBuffers(
								buffer,
								0,
								stack.longs(transparentBuffer),
								stack.longs(0L)
						);
						handler.lastBoundBufferStandardTransparent = transparentBuffer;
					}
					firstVertex = (int)transparentOffset / 32;
				}else {
					vkCmdBindVertexBuffers(
							buffer,
							0,
							stack.longs(transparentBuffer),
							stack.longs(transparentOffset)
					);
					firstVertex = 0;
				}
				vkCmdPushConstants(
						buffer,
						handler.layoutStandardTransparent,
						VK_SHADER_STAGE_VERTEX_BIT,
						0,
						stack.floats(offsetX, offsetY, offsetZ)
				);
				vkCmdDraw(
						buffer,
						vertexCount,
						1,
						firstVertex,
						0
				);
			}
		}
	}
}
