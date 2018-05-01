package net.openvoxel.client.renderer.vk.pipeline;

import net.openvoxel.client.renderer.vk.core.VulkanDevice;
import net.openvoxel.client.renderer.vk.core.VulkanState;
import net.openvoxel.client.renderer.vk.core.VulkanUtility;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
import static org.lwjgl.vulkan.VK10.*;

/*
 * Global Render Pass:
 *  Render Method -- [Voxel based Global Illumination]:
 *   Render Pass(GEN_VOXEL): [Attachments: None]
 *    0: Opaque Chunks -> Voxel
 *    1: Transparency  -> Voxel
 *   Barrier(Voxel Image: Frag <-> Frag) TODO: LIGHT VOXEL PROPAGATION???
 *   Render Pass(SOLID_VOXEL): [Attachments: GBuffer,SRGB_Target][Depend: Voxel for Fragment]
 *    0: SkyBox Lights -> GBuffer
 *    1: Opaque Chunks -> GBuffer
 *    2: GBuffer,Voxel -> SRGB_Target
 *   Render Pass(TRANS_VOXEL): [Attachments: SRGB_Target,Target][Depend: GBuffer Lighting for Fragment]
 *    0: Transparency  -> SRGB_Target
 *    4: GUI, S-rgb    -> Target [performs gamma correction & corrects brightness]
 *
 *
 *  Render Method -- [Environmental Maps]:
 *   Render Pass(GEN_SHADOWS):
 *    0: Opaque Chunks -> Shadow Map
 *    1: Transparency  -> Shadow Map
 *   ...
 *
 *  Render Method -- [Screen Space Only]:
 *   0: Opaque Chunks -> GBuffer {Depend: None} {Free: None}
 *   ...
 *
 *  Render Method -- [Forward Renderer]:
 *   Render Pass(FORWARD):
 *    pass 0: Forward Renderer -> Target {Depend: None} {Free: None}
 *      Draw Opaque
 *      Draw Transparent
 *      Draw GUI
 *
 *  Valid Render Paths:
 *   0: ...
 *   1: Deferred - Voxel based GI
 *   2: Deferred - Environment Maps [Shadow & Reflection]
 *   3: Deferred - Screen Space Only
 *   4: Forward Renderer
 */
public class VulkanRenderPass {

	public long RenderPass;
	private int type;

	//Voxel Based
	public static final int RENDER_PASS_TYPE_VOXEL_GEN = 1;
	public static final int RENDER_PASS_TYPE_SOLID_VOXEL = 2;
	public static final int RENDER_PASS_TYPE_FINAL_VOXEL = 3;

	//Forward
	public static final int RENDER_PASS_TYPE_FORWARD = 64;

	//Image Formats
	public static boolean formatInit = false;
	public static int formatPresent;
	public static int formatSimpleDepth = VK_FORMAT_D32_SFLOAT;

	public static void LoadFormats(VulkanState state) {
		VulkanUtility.LogInfo("Choosing Image Formats");
		formatInit = true;
		formatPresent = state.getPresentImageFormat();
		formatSimpleDepth = state.findSupportedFormat(
				VK_IMAGE_TILING_OPTIMAL,
				VK_FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT,
				VK_FORMAT_D32_SFLOAT,
				VK_FORMAT_D32_SFLOAT_S8_UINT,
				VK_FORMAT_D24_UNORM_S8_UINT
		);
		//
		LogFormat(" - Swap Present",formatPresent);
		LogFormat(" - Simple Depth",formatSimpleDepth);
	}

	private static void LogFormat(String id,int format) {
		VulkanUtility.LogInfo(id+" = "+VulkanUtility.getFormatAsString(format));
	}

	public VulkanRenderPass(int type) {
		this.type = type;
	}

	public void generate(VkDevice device) {
		try(MemoryStack stack = stackPush()) {
			VkRenderPassCreateInfo createInfo = VkRenderPassCreateInfo.mallocStack(stack);
			createInfo.sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO);
			createInfo.pNext(VK_NULL_HANDLE);
			createInfo.flags(0);
			createInfo.pAttachments(getAttachments(stack));
			createInfo.pSubpasses(getSubPasses(stack));
			createInfo.pDependencies(getDependencies(stack));

			//Create
			LongBuffer returnVal = stack.mallocLong(1);
			int vkResult = vkCreateRenderPass(device,createInfo,null,returnVal);
			if(vkResult == VK_SUCCESS) {
				RenderPass = returnVal.get(0);
			}else{
				VulkanUtility.LogWarn("Failed to create render-pass: out of memory");
				VulkanUtility.CrashOnBadResult("Failed to create render-pass",vkResult);
			}
		}
	}

	private VkAttachmentDescription.Buffer getAttachments(MemoryStack stack) {
		if(type == RENDER_PASS_TYPE_VOXEL_GEN) {
			return null;
		}else if(type == RENDER_PASS_TYPE_SOLID_VOXEL) {
			return null;
		}else if(type == RENDER_PASS_TYPE_FINAL_VOXEL) {
			return null;
		}else if(type == RENDER_PASS_TYPE_FORWARD) {
			VkAttachmentDescription.Buffer attachmentList = VkAttachmentDescription.mallocStack(2,stack);
			attachmentList.position(0);
			if(type == RENDER_PASS_TYPE_FORWARD || type == RENDER_PASS_TYPE_FINAL_VOXEL) {
				//Attachment 0: Swap Color
				attachmentList.flags(0);//may alias bit Y/B
				attachmentList.format(formatPresent);
				attachmentList.samples(VK_SAMPLE_COUNT_1_BIT);
				attachmentList.loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR);
				attachmentList.storeOp(VK_ATTACHMENT_STORE_OP_STORE);
				attachmentList.stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE);
				attachmentList.stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE);
				attachmentList.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
				attachmentList.finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);
			}
			attachmentList.position(1);
			{
				//Attachment 1: Depth Buffer
				attachmentList.flags(0);
				attachmentList.format(formatSimpleDepth);
				attachmentList.samples(VK_SAMPLE_COUNT_1_BIT);
				attachmentList.loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR);
				attachmentList.storeOp(VK_ATTACHMENT_STORE_OP_STORE);
				attachmentList.stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE);
				attachmentList.stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE);
				attachmentList.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
				attachmentList.finalLayout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);
			}
			attachmentList.position(0);
			return attachmentList;
		}else {
			throw new RuntimeException("Invalid Render Pass Type");
		}
	}

	private VkSubpassDescription.Buffer getSubPasses(MemoryStack stack) {
		if(type == RENDER_PASS_TYPE_VOXEL_GEN) {
			throw new RuntimeException("NYI");
		}else if(type == RENDER_PASS_TYPE_SOLID_VOXEL) {
			throw new RuntimeException("NYI");
		}else if(type == RENDER_PASS_TYPE_FINAL_VOXEL) {
			throw new RuntimeException("NYI");
		}else if(type == RENDER_PASS_TYPE_FORWARD) {
			VkAttachmentReference.Buffer refPresent = VkAttachmentReference.mallocStack(1,stack);
			refPresent.attachment(0);
			refPresent.layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

			VkAttachmentReference refDepth = VkAttachmentReference.mallocStack(stack);
			refDepth.attachment(1);
			refDepth.layout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);

			VkSubpassDescription.Buffer subPassList = VkSubpassDescription.mallocStack(1, stack);
			subPassList.position(0);
			{
				subPassList.flags(0);
				subPassList.pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS);
				subPassList.pInputAttachments(null);
				subPassList.colorAttachmentCount(1);
				subPassList.pColorAttachments(refPresent);
				subPassList.pResolveAttachments(null);
				subPassList.pDepthStencilAttachment(refDepth);
				subPassList.pPreserveAttachments(null);
			}
			subPassList.position(0);
			return subPassList;
		}else{
			throw new RuntimeException("Invalid Render Pass Type");
		}
	}

	private VkSubpassDependency.Buffer getDependencies(MemoryStack stack) {
		if(type == RENDER_PASS_TYPE_VOXEL_GEN) {
			return null;
		}else if(type == RENDER_PASS_TYPE_SOLID_VOXEL) {
			return null;
		}else if(type == RENDER_PASS_TYPE_FINAL_VOXEL) {
			return null;
		}else if(type == RENDER_PASS_TYPE_FORWARD) {
			VkSubpassDependency.Buffer dependencyList = VkSubpassDependency.mallocStack(1, stack);
			dependencyList.position(0);
			{
				dependencyList.srcSubpass(VK_SUBPASS_EXTERNAL);
				dependencyList.dstSubpass(0);
				dependencyList.srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT);
				dependencyList.srcAccessMask(0);
				dependencyList.dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT);
				dependencyList.dstAccessMask();
				dependencyList.dependencyFlags(VK_ACCESS_COLOR_ATTACHMENT_READ_BIT |
						                               VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);
				dependencyList.dependencyFlags(0);
			}
			dependencyList.position(0);
			return dependencyList;
		}else{
			throw new RuntimeException("Invalid Render Pass Type");
		}
	}

	public void free(VkDevice device) {
		if(RenderPass != VK_NULL_HANDLE) {
			vkDestroyRenderPass(device,RenderPass,null);
		}
	}
}
