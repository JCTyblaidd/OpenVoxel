package net.openvoxel.client.renderer.vk.renderpass;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import net.openvoxel.OpenVoxel;
import net.openvoxel.utility.CrashReport;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static net.openvoxel.client.renderer.vk.renderpass.PassEntry.FRAME_ID;
import static net.openvoxel.client.renderer.vk.renderpass.PassResource.VIEWPORT_SIZE;
import static org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
import static org.lwjgl.vulkan.VK10.*;

public class PassBuilder {

	List<PassEntry> entryList;
	Map<String,PassEntry> resourceToCreatorPassMap;
	TObjectIntMap<String> resourceToLastConsumerMap;
	Map<String,PassEntry.OutputAttachment> resourceToAssignmentPassMap;
	Map<String,PassResource> resourceMapping;
	final PassResource FRAME_RESOURCE;

	public PassBuilder() {
		entryList = new ArrayList<>();
		resourceToCreatorPassMap = new HashMap<>();
		resourceToLastConsumerMap = new TObjectIntHashMap<>();
		resourceToAssignmentPassMap = new HashMap<>();
		resourceMapping = new HashMap<>();
		FRAME_RESOURCE = new PassResource(
				VIEWPORT_SIZE,
				VIEWPORT_SIZE,
				1,
				1,
				-1,
				VK_SAMPLE_COUNT_1_BIT
		);
	}

	public void register(PassEntry entry) {
		entryList.add(entry);
	}

	public void build() {

		//Calculate Dependencies
		build_dependency_graph();
		flatten_dependency_graph();

		//Assign Images
		assign_images();
		resourceMapping.forEach((id,resource) -> resource.calculateLayouts(entryList.size()));

		//Split into RenderPasses [start..end)
		TIntList startList = new TIntArrayList();
		split_into_render_passes(startList);

		//Generate RenderPasses
		List<PassRender> renderList = new ArrayList<>();
		int prev_index = startList.get(0);
		for(int i = 1; i < startList.size(); i++) {
			int next_index = startList.get(i);
			renderList.add(new PassRender(this,prev_index,next_index));
			prev_index = next_index;
		}
		renderList.add(new PassRender(this,prev_index,entryList.size()));
	}

	/////////////////////////////
	/// Utility Building Code ///
	/////////////////////////////

	private void split_into_render_passes(@NotNull TIntList startList) {
		Set<String> localUseOnlyResource = new HashSet<>();
		int current_width = entryList.get(0).output_width;
		int current_height = entryList.get(0).output_height;
		int current_layers = entryList.get(0).output_layers;

		//Skip External Dependency Node
		startList.add(0);
		for(int i = 1; i < entryList.size(); i++) {
			boolean requireSplit = false;
			PassEntry entry = entryList.get(i);

			//Consider Write Attachments
			for(PassEntry.OutputAttachment output : entry.outputAttachments) {
				if(output.load_to_create != null) {
					localUseOnlyResource.remove(output.load_to_create);
				}
				localUseOnlyResource.add(output.identifier);
			}

			//Consider Read Attachments
			for(PassEntry.InputAttachment input : entry.inputAttachments) {
				if(!input.local_dependency && localUseOnlyResource.contains(input.identifier)) {
					requireSplit = true;
				}
			}

			//Consider FrameBuffer Dimensions!!
			if(current_width != entry.output_width) requireSplit = true;
			if(current_height != entry.output_height) requireSplit = true;
			if(current_layers != entry.output_layers) requireSplit = true;

			//If non local dependency exists - split the passes
			if(requireSplit) {
				startList.add(i);
				localUseOnlyResource.clear();
			}
		}
	}

	private boolean compatible_resource(@NotNull PassResource resource,@NotNull PassEntry.OutputAttachment output) {
		if(PassEntry.AttachmentType.isFrame(output.type)) return false;
		return resource.is_same_dimensions(output);
	}

	@NotNull
	private PassResource create_resource(@NotNull PassEntry.OutputAttachment output) {
		return new PassResource(output.width,output.height,output.depth,output.layers,output.format,output.samples);
	}

	private void assign_last_consumer(String id,int index) {
		int last = resourceToLastConsumerMap.get(id);
		if(last == resourceToLastConsumerMap.getNoEntryValue()) {
			resourceToLastConsumerMap.put(id,index);
		}else if(index > last) {
			resourceToLastConsumerMap.put(id,index);
		}
	}

	private void assign_images() {

		//Clear Frame Resource Usage
		FRAME_RESOURCE.resetRequires();
		FRAME_RESOURCE.setRequirePrevious(entryList.size(),true);
		FRAME_RESOURCE.setRequiredLayout(entryList.size(),VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);
		resourceToLastConsumerMap.put(FRAME_ID,entryList.size());

		//Set of all images that are free for aliased assignment
		Set<PassResource> freeImages = new HashSet<>();

		//Iterate through passes in reverse order
		ListIterator<PassEntry> iterator = entryList.listIterator(entryList.size());
		while(iterator.hasPrevious()) {
			int pass_index = iterator.previousIndex();
			PassEntry pass = iterator.previous();

			//Consider Write Attachments
			for(PassEntry.OutputAttachment write_target : pass.outputAttachments) {
				PassResource resource = resourceMapping.get(write_target.identifier);

				//Create New Resource if does not exist & is not frame output
				if(resource == null){
					if(PassEntry.AttachmentType.isFrame(write_target.type)) {
						resource = FRAME_RESOURCE;
					}else {
						resource = create_resource(write_target);
					}
				}

				//Assign last consumer
				assign_last_consumer(write_target.identifier,pass_index);

				//Bind resource to identifier
				resourceMapping.putIfAbsent(write_target.identifier,resource);

				//Mark image format
				if (resource.is_depth_format() && !PassEntry.AttachmentType.isFrame(write_target.type)) {
					resource.setRequiredLayout(pass_index, VK_IMAGE_LAYOUT_DEPTH_STENCIL_READ_ONLY_OPTIMAL);
				} else {
					resource.setRequiredLayout(pass_index, VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
				}

				//Assign information on earlier availability
				if(write_target.load_to_create != null) {
					if(resourceMapping.containsKey(write_target.load_to_create)) {
						throw new RuntimeException("Loading from existing data");
					}
					assign_last_consumer(write_target.load_to_create,pass_index);
					resource.setRequirePrevious(pass_index,true);
					resourceMapping.put(write_target.load_to_create,resource);
				}else{
					if(write_target.clear_on_create) resource.markClearAt(pass_index);
					resource.setRequirePrevious(pass_index,false);
					freeImages.add(resource);
				}
			}

			for(PassEntry.InputAttachment input_target : pass.inputAttachments) {
				PassResource resource = resourceMapping.get(input_target.identifier);
				PassEntry.OutputAttachment write_attachment = resourceToAssignmentPassMap.get(input_target.identifier);

				//Find OR create image if not already assigned
				if(resource == null) {
					resource = freeImages.stream()
                        .filter(x -> compatible_resource(x,write_attachment))
						.findAny().orElseGet(() -> create_resource(write_attachment));
					freeImages.remove(resource);
				}

				//Assign last consumer
				assign_last_consumer(input_target.identifier,pass_index);

				//Mark image format
				if(resource.is_depth_format()) {
					resource.setRequiredLayout(pass_index, VK_IMAGE_LAYOUT_DEPTH_STENCIL_READ_ONLY_OPTIMAL);
				}else{
					resource.setRequiredLayout(pass_index, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
				}
				resource.setRequirePrevious(pass_index,true);

				//Mark resource metadata
				if(input_target.local_dependency) resource.markUsesLocal();

				//Bind image to dependency
				resourceMapping.putIfAbsent(input_target.identifier,resource);
			}
		}
	}

	/**
	 * Convert entryList into a sorted form that
	 *  follows the dependency graph
	 */
	private void flatten_dependency_graph() {
		//Initial Setup
		Set<PassEntry> visited = new HashSet<>();
		List<PassEntry> global_dependency = new ArrayList<>(entryList);
		entryList.clear();

		//Global Iteration
		for(PassEntry entry : global_dependency) {
			if(!entryList.contains(entry)) {
				flatten_dependency(visited,entry);
			}
		}
	}

	/**
	 * Recursively flatten the dependency graph
	 * @param visited set of already visited passes
	 * @param node the pass to visited
	 */
	private void flatten_dependency(Set<PassEntry> visited, PassEntry node) {
		visited.add(node);
		for(PassEntry dependency : node.passDependencies) {
			if(!entryList.contains(dependency)) {
				if(visited.contains(dependency)) {
					CrashReport crash = new CrashReport("Found Circular Dependency in Pass Builder");
					crash.invalidState("Circular Dependency: "+node.toString()+" -> "+dependency.toString());
					OpenVoxel.reportCrash(crash);
				}
				flatten_dependency(visited, dependency);
			}
		}
		entryList.add(node);
	}

	/**
	 * Build the dependency graph
	 *  between frame information
	 */
	private void build_dependency_graph() {

		//Map Creating Resources with
		resourceToCreatorPassMap.clear();
		for(PassEntry entry : entryList) {
			for(String output : entry.outputIdList) {
				resourceToCreatorPassMap.put(output,entry);
			}
			for(PassEntry.OutputAttachment output : entry.outputAttachments) {
				resourceToAssignmentPassMap.put(output.identifier,output);
			}
		}

		//Create Dependency Listings
		for(PassEntry entry : entryList) {
			for(String input : entry.inputIdList) {
				entry.passDependencies.add(resourceToCreatorPassMap.get(input));
			}
		}
	}

}
