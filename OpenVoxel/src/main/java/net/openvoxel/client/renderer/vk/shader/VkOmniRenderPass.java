package net.openvoxel.client.renderer.vk.shader;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
import static org.lwjgl.vulkan.VK10.*;

/**
 * Attachments:
 *  - final_image_colour{0}
 * Dependency:
 *  #NONE YET#
 * Subpass:
 *  - gui_gen_subpass
 *
 * Planned Passes:
 *             //depth>> gbuffer_depth_peel
 *  - gbuffer_solid                     \\>>gbuffer_merge>>post_process
 *               \\                     //          ..>>>//
 *  - shadow_map >>> gbuffer_solid_colour          //
 *  - draw_entity>>>>>//                          //
 *  - draw_gui  >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>//
 *
 *  Required Targets:
 *   gbuffer_diff,
 *   gbuffer_pbr,
 *   gbuffer_norm,
 *   shadow_ma
 *
 *
 *   Alternative:
 *                  -gbuffer_alpha_buffer
 *   - gbuffer_solid                            -gbuffer_merge      -post_process
 *   - shadow_map(w_close_col)   -gbuffer_solid_colour
 *   - draw_entity
 *   - draw_gui
 */
public class VkOmniRenderPass {

	public long render_pass;

	public void init(VkDevice device, VkRenderConfig config,int swapChainImageFormat) {
		try(MemoryStack stack = stackPush()) {
			VkRenderPassCreateInfo createInfo = VkRenderPassCreateInfo.callocStack(stack);
			VkSubpassDependency.Buffer dependencyList = VkSubpassDependency.callocStack(calc_dependency_count(config),stack);
			VkSubpassDescription.Buffer subPassList = VkSubpassDescription.callocStack(calc_subpass_count(config),stack);
			VkAttachmentDescription.Buffer attachmentList = VkAttachmentDescription.callocStack(calc_attachment_count(config),stack);
			initDependency(dependencyList,config);
			initDescription(stack,subPassList,config);
			initAttachment(attachmentList,swapChainImageFormat,config);
			createInfo.sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO);
			createInfo.pNext(VK_NULL_HANDLE);
			createInfo.flags(0);
			createInfo.pDependencies(dependencyList);
			createInfo.pAttachments(attachmentList);
			createInfo.pSubpasses(subPassList);
			LongBuffer lb = stack.callocLong(1);
			if(vkCreateRenderPass(device,createInfo,null,lb) != VK_SUCCESS) {
				throw new RuntimeException("Failed to create render pass");
			}
			render_pass = lb.get(0);
		}
	}

	private int calc_dependency_count(VkRenderConfig config) {
		return 1;
		//return 2;
	}

	private int calc_subpass_count(VkRenderConfig config) {
		return 1;
	}

	private int calc_attachment_count(VkRenderConfig config) {

		return 1;
	}


	//Generation//

	public void cleanup(VkDevice device) {
		vkDestroyRenderPass(device,render_pass,null);
	}

	private void initDependency(VkSubpassDependency.Buffer buffer,VkRenderConfig config) {
		buffer.position(0);
		buffer.srcSubpass(VK_SUBPASS_EXTERNAL);
		buffer.dstSubpass(0);
		buffer.srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT);
		buffer.srcAccessMask(0);
		buffer.dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT);
		buffer.dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_READ_BIT | VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);
		buffer.dependencyFlags(0);
	}

	private void initDescription(MemoryStack stack,VkSubpassDescription.Buffer buffer,VkRenderConfig config) {
		VkAttachmentReference.Buffer references = VkAttachmentReference.callocStack(1,stack);
		references.position(0);
		references.attachment(0);
		references.layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
		buffer.position(0);
		buffer.pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS);
		buffer.colorAttachmentCount(1);
		buffer.pColorAttachments(references);
	}

	private void initAttachment(VkAttachmentDescription.Buffer buffer, int swapChainImageFormat,VkRenderConfig config) {
		buffer.position(0);
		buffer.format(swapChainImageFormat);
		buffer.samples(VK_SAMPLE_COUNT_1_BIT);
		buffer.loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR);
		buffer.storeOp(VK_ATTACHMENT_STORE_OP_STORE);
		buffer.stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE);
		buffer.stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE);
		buffer.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
		buffer.finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);
	}

}
