package net.openvoxel.client.renderer.vk.shader;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.VK_NULL_HANDLE;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.vkCreateRenderPass;

/**
 * Created by James on 12/05/2017.
 *
 * Manage The Render Pass Used By The Vulkan Renderer
 */
public class VkUberRenderPass {

	private long vkRenderPassHandle;


	private void create(VkDevice device) {
		try(MemoryStack stack = MemoryStack.stackPush()) {
			VkRenderPassCreateInfo createInfo = VkRenderPassCreateInfo.mallocStack(stack);
			VkSubpassDependency.Buffer subpassDependencies = VkSubpassDependency.mallocStack(1,stack);
			VkSubpassDescription.Buffer subpassDescriptions = VkSubpassDescription.mallocStack(1,stack);
			VkAttachmentDescription.Buffer subpassAttachments = VkAttachmentDescription.mallocStack(1,stack);




			//Create Render Pass//
			createInfo.sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO);
			createInfo.pNext(VK_NULL_HANDLE);
			createInfo.flags(0);
			createInfo.pAttachments(subpassAttachments);
			createInfo.pDependencies(subpassDependencies);
			createInfo.pSubpasses(subpassDescriptions);
			LongBuffer handle = stack.callocLong(1);
			vkCreateRenderPass(device,createInfo,null,handle);
			vkRenderPassHandle = handle.get(0);
		}
	}


}
