package net.openvoxel.client.renderer.vk.renderpass;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class PassRender {

	private long render_pass;
	private TObjectIntMap<PassResource> resourceAttachmentMap = new TObjectIntHashMap<>();
	private TIntObjectMap<PassResource> attachmentResourceMap = new TIntObjectHashMap<>();

	public PassRender(PassBuilder builder, int begin, int end) {
		try(MemoryStack stack = stackPush()) {
			VkRenderPassCreateInfo renderPass = VkRenderPassCreateInfo.mallocStack(stack);
			renderPass.sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO);
			renderPass.pNext(VK_NULL_HANDLE);
			renderPass.flags(0);
			renderPass.pAttachments(get_attachments(stack,builder,begin,end));
			renderPass.pSubpasses(get_sub_pass_list(stack,builder,begin,end));
			renderPass.pDependencies(get_sub_pass_depends(stack,builder,begin,end));
		}
	}

	private int get_sub_pass(PassBuilder builder, String id, int begin, int end) {
		PassEntry entry = builder.resourceToCreatorPassMap.get(id);
		int entry_index = builder.entryList.indexOf(entry);
		if(entry_index < begin) return VK_SUBPASS_EXTERNAL;
		if(entry_index >= end) return VK_SUBPASS_EXTERNAL;
		return entry_index - begin;
	}

	private int get_access_mask_create(PassBuilder builder, String id) {
		PassResource resource = builder.resourceMapping.get(id);
		PassEntry entry = builder.resourceToCreatorPassMap.get(id);
		PassEntry.OutputAttachment output = entry.outputAttachments.stream().filter(out -> out.identifier.equals(id)).findFirst().orElse(null);
		if(resource.is_depth_format()) {
			if(output == null || output.read_on_create) {
				return VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_READ_BIT | VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT;
			}else{
				return VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT;
			}
		}else{
			if(output == null || output.read_on_create) {
				return VK_ACCESS_COLOR_ATTACHMENT_READ_BIT | VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT;
			}else{
				return VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT;
			}
		}
	}

	private VkSubpassDependency.Buffer get_sub_pass_depends(MemoryStack stack, PassBuilder builder, int begin, int end) {

		//Count the dependencies
		int dependencyCount = 0;
		for(int i = begin; i < end; i++) {
			PassEntry entry = builder.entryList.get(i);
			for(PassEntry.InputAttachment input : entry.inputAttachments) {
				if(input.local_dependency) dependencyCount += 1;
			}
			for(PassEntry.OutputAttachment output : entry.outputAttachments) {
				if(output.load_to_create != null) dependencyCount += 1;
				if(builder.resourceToLastConsumerMap.get(output) >= end) dependencyCount += 1;
			}
		}

		//Assign the dependencies
		VkSubpassDependency.Buffer dependencies = VkSubpassDependency.mallocStack(dependencyCount,stack);
		int index = 0;
		for(int i = begin; i < end; i++) {
			PassEntry entry = builder.entryList.get(i);
			for(PassEntry.InputAttachment input : entry.inputAttachments) {

				//Input Attachment Dependency
				if(input.local_dependency) {
					VkSubpassDependency dependency = dependencies.get(index);
					dependency.srcSubpass(get_sub_pass(builder,input.identifier,begin,end));
					dependency.dstSubpass(i-begin);
					dependency.srcStageMask(VK_SHADER_STAGE_FRAGMENT_BIT);
					dependency.dstStageMask(VK_SHADER_STAGE_FRAGMENT_BIT);
					dependency.srcAccessMask(get_access_mask_create(builder,input.identifier));
					dependency.dstAccessMask(VK_ACCESS_INPUT_ATTACHMENT_READ_BIT);
					dependency.dependencyFlags(VK_DEPENDENCY_BY_REGION_BIT);
					index += 1;
				}
			}
			for(PassEntry.OutputAttachment output : entry.outputAttachments) {

				//Load Previous SubPass Output Dependency
				if(output.load_to_create != null) {
					VkSubpassDependency dependency = dependencies.get(index);
					dependency.srcSubpass(get_sub_pass(builder,output.load_to_create,begin,end));
					dependency.dstSubpass(i-begin);
					dependency.srcStageMask(VK_SHADER_STAGE_FRAGMENT_BIT);
					dependency.dstStageMask(VK_SHADER_STAGE_FRAGMENT_BIT);
					dependency.srcAccessMask(get_access_mask_create(builder,output.load_to_create));
					dependency.dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_READ_BIT | VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);
					dependency.dependencyFlags(VK_DEPENDENCY_BY_REGION_BIT);
					index += 1;
				}

				//Required by external Dependency
				if(builder.resourceToLastConsumerMap.get(output) >= end) {
					VkSubpassDependency dependency = dependencies.get(index);
					dependency.srcSubpass(i-begin);
					dependency.dstSubpass(VK_SUBPASS_EXTERNAL);
					dependency.srcStageMask(VK_SHADER_STAGE_FRAGMENT_BIT);
					dependency.dstStageMask(VK_SHADER_STAGE_FRAGMENT_BIT);
					dependency.srcAccessMask(get_access_mask_create(builder,output.identifier));
					dependency.dstAccessMask(dependency.srcAccessMask());
					index += 1;
				}
			}
		}

		return dependencies;
	}

	private VkSubpassDescription.Buffer get_sub_pass_list(MemoryStack stack,PassBuilder builder, int begin, int end) {
		VkSubpassDescription.Buffer subPassList = VkSubpassDescription.mallocStack(end-begin,stack);
		for(int i = begin; i < end; i++) {
			VkSubpassDescription description = subPassList.get(i-begin);
			PassEntry entry = builder.entryList.get(i);
			description.flags(0);
			description.pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS);

			//Input Attachments
			{
				List<PassEntry.InputAttachment> inputAttachments = entry.inputAttachments.stream()
						.filter(in -> in.local_dependency).collect(Collectors.toList());
				if(inputAttachments.size() != 0) {
					VkAttachmentReference.Buffer inputReference = VkAttachmentReference.mallocStack(inputAttachments.size(),stack);
					for(PassEntry.InputAttachment input : inputAttachments) {
						VkAttachmentReference reference = inputReference.get(input.bound_index);
						PassResource resource = builder.resourceMapping.get(input.identifier);
						reference.attachment(resourceAttachmentMap.get(resource));
						if(resource.is_depth_format()) {
							reference.layout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_READ_ONLY_OPTIMAL);
						}else {
							reference.layout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
						}
					}
					description.pInputAttachments(inputReference);
				}else{
					description.pInputAttachments(null);
				}
			}

			//Output Attachments
			{
				List<PassEntry.OutputAttachment> colorAttachments = entry.outputAttachments.stream()
                    .filter(o -> o.type == PassEntry.AttachmentType.COLOUR_ATTACHMENT  ||
                                 o.type == PassEntry.AttachmentType.FRAME_ATTACHMENT)
                    .collect(Collectors.toList());
				description.colorAttachmentCount(colorAttachments.size());
				if(colorAttachments.size() != 0) {
					VkAttachmentReference.Buffer colorReference = VkAttachmentReference.mallocStack(colorAttachments.size(), stack);
					for (PassEntry.OutputAttachment output : colorAttachments) {
						VkAttachmentReference reference = colorReference.get(output.binding);
						reference.attachment(resourceAttachmentMap.get(builder.resourceMapping.get(output.identifier)));
						reference.layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
					}
					description.pColorAttachments(colorReference);
				}else{
					description.pColorAttachments(null);
					description.pResolveAttachments(null);
				}
			}

			//Resolve Attachments
			if(description.colorAttachmentCount() != 0) {
				List<PassEntry.OutputAttachment> resolveAttachments = entry.outputAttachments.stream()
						.filter(o -> o.type == PassEntry.AttachmentType.RESOLVE_ATTACHMENT ||
									 o.type == PassEntry.AttachmentType.FRAME_RESOLVE_ATTACHMENT)
						.collect(Collectors.toList());
				if(resolveAttachments.size() == 0) {
					description.pResolveAttachments(null);
				}else {
					VkAttachmentReference.Buffer resolveReference = VkAttachmentReference.mallocStack(description.colorAttachmentCount(),stack);
					for(int j = 0; j < description.colorAttachmentCount(); j++) {
						resolveReference.get(j).attachment(VK_ATTACHMENT_UNUSED);
						resolveReference.get(j).layout(VK_IMAGE_LAYOUT_UNDEFINED);
					}
					for(PassEntry.OutputAttachment output : resolveAttachments) {
						VkAttachmentReference reference = resolveReference.get(output.binding);
						reference.attachment(resourceAttachmentMap.get(builder.resourceMapping.get(output.identifier)));
						reference.layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
					}
					description.pResolveAttachments(resolveReference);
				}
			}
			//Depth Attachments
			{
				PassEntry.OutputAttachment depthAttachment = entry.outputAttachments.stream()
                     .filter(o -> o.type == PassEntry.AttachmentType.DEPTH_ATTACHMENT).findAny().orElse(null);
				if(depthAttachment == null) {
					description.pDepthStencilAttachment(null);
				}else {
					VkAttachmentReference depthReference = VkAttachmentReference.mallocStack(stack);
					depthReference.attachment(resourceAttachmentMap.get(builder.resourceMapping.get(depthAttachment.identifier)));
					depthReference.layout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);
					description.pDepthStencilAttachment(depthReference);
				}
			}

			//Preserve Attachments TODO: IMPLEMENT PRESERVE ATTACHMENTS!!!
			description.pPreserveAttachments(null);
		}
		return subPassList;
	}

	private VkAttachmentDescription.Buffer get_attachments(MemoryStack stack,PassBuilder builder, int begin, int end) {
		Set<PassResource> referencedAttachments = new HashSet<>();
		for(int i = begin; i < end; i++) {
			PassEntry entry = builder.entryList.get(i);
			for(PassEntry.OutputAttachment output : entry.outputAttachments) {
				referencedAttachments.add(builder.resourceMapping.get(output.identifier));
			}
			for(PassEntry.InputAttachment input : entry.inputAttachments) {
				if(input.local_dependency) {
					referencedAttachments.add(builder.resourceMapping.get(input.identifier));
				}
			}
		}
		int index = 0;
		for(PassResource resource : referencedAttachments) {
			resourceAttachmentMap.put(resource,index);
			attachmentResourceMap.put(index,resource);
			index += 1;
		}
		VkAttachmentDescription.Buffer attachments = VkAttachmentDescription.mallocStack(index,stack);
		for(int i = 0; i < index; i++) {
			VkAttachmentDescription description = attachments.get(i);
			PassResource resource = attachmentResourceMap.get(i);
			description.flags();
			description.format(resource.format);
			description.samples(resource.samples);

			if(resource.requirePrevious(begin)) {
				description.loadOp(VK_ATTACHMENT_LOAD_OP_LOAD);
			}else if(resource.clearAt(begin)){
				description.loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR);
			}else{
				description.loadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE);
			}
			if(resource.requirePrevious(end)) {
				description.storeOp(VK_ATTACHMENT_STORE_OP_STORE);
			}else{
				description.storeOp(VK_ATTACHMENT_STORE_OP_DONT_CARE);
			}
			description.stencilLoadOp(description.loadOp());
			description.stencilStoreOp(description.storeOp());
			description.initialLayout(resource.getLayout(begin));
			description.finalLayout(resource.getLayout(end));
		}
		return attachments;
	}

}
