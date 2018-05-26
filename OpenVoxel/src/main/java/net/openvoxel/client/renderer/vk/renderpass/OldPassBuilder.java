package net.openvoxel.client.renderer.vk.renderpass;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import net.openvoxel.OpenVoxel;
import net.openvoxel.utility.CrashReport;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkAttachmentDescription;
import org.lwjgl.vulkan.VkRenderPassCreateInfo;
import org.lwjgl.vulkan.VkSubpassDependency;
import org.lwjgl.vulkan.VkSubpassDescription;

import java.io.Closeable;
import java.util.*;
import java.util.stream.Collectors;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class OldPassBuilder implements Closeable {

	//Constant to resize state to the size of the viewport
	public static final int SIZE_VIEWPORT = -1;
	private static final String FRAME_TARGET_ID = "frame_output";

	private enum AttachmentType {
		VOXEL_ATTACHMENT,   //Voxel 3D Image Data
		DEPTH_ATTACHMENT,   //Depth Attachment Data
		COLOUR_ATTACHMENT,  //Colour Attachment Data
		FRAME_TARGET,       //SwapChain Attachment Data
	}

	private static class PassAttachment {
		private String attachment_name; // the unique ID of this attachment
		private IPass write_pass;       // pass in which this is generated
		private AttachmentType type;    // type of attachment
		private int size;               // size of attachment data [square YxY or Viewport]
		private int attachment_id;      // id of colour attachment
		private int format;             // format of attachment
		private int layers;             // number of attachment layers
		private int samples;            // number of attachment samples
		private boolean can_read;       // if read access is needed while generating
		private boolean clear_on_load;  // if the attachment should be cleared on load!
		private String  load_data_from; // attachment from which to load data from
	}
	private static class PassDependency {
		private String dependency;      // id of the dependency
		private boolean isLocal;        // if the dependency is per pixel & layer only
		private boolean attachment;     // if this dependency is an attachment
	}
	private static class RenderPassMetadata {
		private int size;               // the size of all target attachments
		private int start_idx;          // the starting index of passes handled
		private int end_idx;            // the ending index of passes handled
	}

	///////////////////////
	///Pass Builder Code///
	///////////////////////

	//Dependency Metadata
	private Map<String,PassAttachment> attachmentMap = new HashMap<>();
	private Map<IPass,List<PassAttachment>> writeAttachments = new HashMap<>();
	private Map<IPass,List<PassDependency>> dependencyAttachments = new HashMap<>();

	//Pass Metadata
	private IPass currentPass;
	private Collection<IPass> passList = new ArrayList<>();

	//////////////////////
	/// Build Metadata ///
	//////////////////////

	private static class DependencyNode {
		private IPass pass;
		private List<DependencyNode> dependsOn = new ArrayList<>();
		private DependencyNode(IPass pass) {
			this.pass = pass;
		}
	}
	private List<DependencyNode> flattenedDependency = new ArrayList<>();

	private static class ImageResource {

		//Image Format Data
		private int size;
		private int format;
		private int layers;
		private int samples;
		private AttachmentType type;

		//Lifetime Markers [Begin->End] must preserve data
		private List<Lifetime> lifetimes = new ArrayList<>();
		private Lifetime lastLifetime = null;
		private static class Lifetime {
			private boolean clear_on_begin = false;
			private TIntList attachment_usage = new TIntArrayList();
			private TIntList sampled_usage = new TIntArrayList();
			private int begin = Integer.MIN_VALUE;
			private int end = Integer.MAX_VALUE;
		}

		//Vulkan Graphics State
		private long vulkan_image;
		private long vulkan_image_view;
		private long vulkan_image_memory;
	}


	private void flatten_dependency(Set<DependencyNode> visited,DependencyNode node) {
		visited.add(node);
		for(DependencyNode dependency : node.dependsOn) {
			if(!flattenedDependency.contains(dependency)) {
				if(visited.contains(dependency)) {
					CrashReport crash = new CrashReport("Found Circular Dependency in Pass Builder");
					crash.invalidState("Circular Dependency: "+node.pass.toString()+" -> "+dependency.pass.toString());
					OpenVoxel.reportCrash(crash);
				}
				flatten_dependency(visited, dependency);
			}
		}
		flattenedDependency.add(node);
	}

	private boolean is_valid_image(ImageResource freeImage, PassAttachment target) {
		boolean same_swap = freeImage.type == target.type;
		boolean same_size = freeImage.size == target.size;
		boolean same_layer = freeImage.layers == target.layers;
		boolean same_samples = freeImage.samples == target.samples;
		boolean same_format = freeImage.format == target.format;
		return (same_swap && same_size && same_layer && same_samples && same_format);
	}

	private ImageResource create_image(PassAttachment target) {
		ImageResource resource = new ImageResource();
		resource.type = target.type;
		resource.size = target.size;
		resource.layers = target.layers;
		resource.samples = target.samples;
		resource.format = target.format;
		return resource;
	}

	private void assign_images(Map<String,ImageResource> boundImages) {

		//List of all images that have been freed to usage
		Set<ImageResource> freeImages = new HashSet<>();

		//Iterate through passes in reverse order
		ListIterator<DependencyNode> iterator = flattenedDependency.listIterator(flattenedDependency.size());
		while(iterator.hasPrevious()) {
			int node_index = iterator.previousIndex();
			DependencyNode node = iterator.previous();

			//Consider Write Attachments
			List<PassAttachment> write = writeAttachments.get(node.pass);
			for(PassAttachment target : write) {
				ImageResource resource = boundImages.get(target.attachment_name);

				//Create new resource if not already assigned
				if(resource == null) resource = create_image(target);

				//Bind image to self
				boundImages.putIfAbsent(target.attachment_name,resource);

				//Assign earlier availability
				resource.lastLifetime.attachment_usage.add(node_index);
				if(target.load_data_from != null) {
					if(boundImages.containsKey(target.load_data_from)) {
						throw new RuntimeException("Loading data from target read from later");
					}
					boundImages.put(target.load_data_from,resource);
				}else{
					resource.lastLifetime.clear_on_begin = target.clear_on_load;
					resource.lastLifetime.begin = node_index;
					resource.lastLifetime = null;
					freeImages.add(resource);
				}
			}

			//Consider Dependency Attachments
			List<PassDependency> read = dependencyAttachments.get(node.pass);
			for(PassDependency target : read) {
				ImageResource resource = boundImages.get(target.dependency);
				PassAttachment attachment = attachmentMap.get(target.dependency);

				//Find OR create new image if not already assigned
				if(resource == null) {
					resource = freeImages.stream()
						           .filter(x -> is_valid_image(x, attachment))
						           .findAny().orElseGet(() -> create_image(attachment));
					ImageResource.Lifetime lifetime = new ImageResource.Lifetime();
					lifetime.end = node_index;
					resource.lifetimes.add(lifetime);
					resource.lastLifetime = lifetime;
					freeImages.remove(resource);
				}

				//Mark as sampled
				resource.lastLifetime.sampled_usage.add(node_index);

				//Bind image to dependency
				boundImages.putIfAbsent(target.dependency,resource);
			}
		}
	}

	private void split_render_passes(TIntList startIndices) {

		//List of dependencies that can only be read locally
		Set<String> localUseOnlyImages = new HashSet<>();

		//Skip External Dependency Node
		startIndices.add(1);
		for(int i = 1; i < flattenedDependency.size(); i++) {
			DependencyNode node = flattenedDependency.get(i);
			boolean requireSplit = false;

			//Consider Write Attachments
			List<PassAttachment> write = writeAttachments.get(node.pass);
			for(PassAttachment target : write) {
				if(target.load_data_from != null) {
					localUseOnlyImages.remove(target.load_data_from);
				}
				localUseOnlyImages.add(target.attachment_name);
			}

			//Consider Read Attachments
			List<PassDependency> read = dependencyAttachments.get(node.pass);
			for(PassDependency target : read) {
				if(!target.isLocal && localUseOnlyImages.contains(target.dependency)) {
					requireSplit = true;
				}
			}

			//If non local dependency exists - split the passes
			if(requireSplit) {
				startIndices.add(i);
				localUseOnlyImages.clear();
			}
		}
	}

	private void get_referenced_attachments(int begin ,int end, Set<String> usedAttachments) {
		for(int i = begin; i < end; i++) {
			DependencyNode node = flattenedDependency.get(i);
			//Consider Write Attachments
			List<PassAttachment> write = writeAttachments.get(node.pass);
			for(PassAttachment target : write) {
				if(target.load_data_from != null) usedAttachments.add(target.load_data_from);
				usedAttachments.add(target.attachment_name);
			}

			//Consider Read Attachments
			List<PassDependency> read = dependencyAttachments.get(node.pass);
			for(PassDependency target : read) {
				usedAttachments.add(target.dependency);
			}
		}
	}
/*
	private boolean is_depth_format(int format) {
		if(format == VK_FORMAT_D32_SFLOAT) return true;
		if(format == VK_FORMAT_D32_SFLOAT_S8_UINT) return true;
		if(format == VK_FORMAT_D24_UNORM_S8_UINT) return true;
		if(format == VK_FORMAT_D16_UNORM) return true;
		if(format == VK_FORMAT_D16_UNORM_S8_UINT) return true;
		return format == VK_FORMAT_X8_D24_UNORM_PACK32;
	}

	private void assign_attachments(int begin, int end,Set<ImageResource> used, TObjectIntMap<ImageResource> idMap, VkAttachmentDescription.Buffer attachments) {
		int i = 0;
		for(ImageResource resource : used) {

			//Skip Voxel Attachments (..)
			if(resource.type == AttachmentType.VOXEL_ATTACHMENT) continue;

			//Assign Identifiers
			VkAttachmentDescription description = attachments.get(i);
			idMap.put(resource,i);

			//Initial Resource Data
			description.flags(0);
			description.format(resource.format);
			description.samples(resource.samples);
			boolean is_depth_format = is_depth_format(resource.format);

			//Find dependency between render passes
			ImageResource.Lifetime beginLifetime = null;
			ImageResource.Lifetime firstUseLifetime = null;
			ImageResource.Lifetime endLifetime = null;
			for(ImageResource.Lifetime lifetime : resource.lifetimes) {
				if(lifetime.begin < begin && lifetime.end >= begin) beginLifetime = lifetime;
				if(lifetime.end > end && lifetime.begin <= end) endLifetime = lifetime;
				if(lifetime.begin >= begin && lifetime.begin <= end) {
					if(firstUseLifetime == null) firstUseLifetime = lifetime;
					else if(lifetime.begin < firstUseLifetime.begin) firstUseLifetime = lifetime;
				}
			}
			boolean first_used_as_attachment = false;
			boolean next_used_as_attachment = false;

			//Set Shader Load State
			if(beginLifetime == null) {
				description.loadOp(firstUseLifetime.clear_on_begin ? VK_ATTACHMENT_LOAD_OP_CLEAR : VK_ATTACHMENT_LOAD_OP_DONT_CARE);
				description.initialLayout(is_depth_format ? VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL : VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
			}else{
				description.loadOp(VK_ATTACHMENT_LOAD_OP_LOAD);
				if(is_depth_format) {
					description.initialLayout(first_used_as_attachment ? VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL : VK_IMAGE_LAYOUT_DEPTH_STENCIL_READ_ONLY_OPTIMAL);
				}else{
					description.initialLayout(first_used_as_attachment ? VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL : VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
				}
			}
			if(endLifetime == null) {
				description.storeOp(VK_ATTACHMENT_STORE_OP_DONT_CARE);
				description.finalLayout(VK_IMAGE_LAYOUT_UNDEFINED);
			}else{
				description.storeOp(VK_ATTACHMENT_STORE_OP_STORE);
				if(is_depth_format) {
					description.finalLayout(next_used_as_attachment ? VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL : VK_IMAGE_LAYOUT_DEPTH_STENCIL_READ_ONLY_OPTIMAL);
				}else{
					description.finalLayout(next_used_as_attachment ? VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL : VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
				}
			}
			description.stencilLoadOp(description.loadOp());
			description.stencilStoreOp(description.storeOp());
			i += 1;
		}
	}*/

	/**
	 *
	 * @param begin inclusive starting render pass
	 * @param end exclusive ending render pass
	 */
	private void create_render_pass(int begin, int end, Map<String,ImageResource> boundImages) {
		try(MemoryStack stack = stackPush()) {
			VkRenderPassCreateInfo passCreate = VkRenderPassCreateInfo.mallocStack(stack);
			passCreate.sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO);
			passCreate.pNext(VK_NULL_HANDLE);
			passCreate.flags(0);

			//Get Metadata
			Set<String> usedAttachments = new HashSet<>();
			get_referenced_attachments(begin,end,usedAttachments);

			//Assign Used Attachments
			Set<ImageResource> usedResources = usedAttachments.stream().map(boundImages::get).collect(Collectors.toSet());
			TObjectIntMap<ImageResource> attachmentIdMap = new TObjectIntHashMap<>();
			VkAttachmentDescription.Buffer attachments = VkAttachmentDescription.mallocStack(usedResources.size(),stack);
			//assign_attachments(begin,end,usedResources,attachmentIdMap,attachments);

			//Assign SubPass List
			VkSubpassDescription.Buffer subPasses = VkSubpassDescription.mallocStack(begin-end,stack);
			for(int i = begin; i < end; i++) {
				VkSubpassDescription subPass = subPasses.get(i-begin);
				subPass.flags(0);
				subPass.pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS);
				subPass.pInputAttachments();//TODO:
				subPass.pColorAttachments();//TODO:
				subPass.pResolveAttachments(null);//TODO: IMPLEMENT AND ALLOW RESOLVE ATTACHMENTS
				subPass.pDepthStencilAttachment();//TODO:
				subPass.pPreserveAttachments();//TODO:
			}

			//Assign SubPass Dependency List
			VkSubpassDependency dependencyList;
			//passDependencies.set
		}
	}

	/**
	 * Call on pass create:
	 *  - Generate the entire render pass dependency list
	 *  - Create all render passes
	 *  - Create all required images
	 *  - Create all required frame buffers
	 */
	public void build() {
		//Get all metadata
		for(IPass pass : passList) {
			currentPass = pass;
			pass.configure(this);
		}
		currentPass = null;

		//Calculate Dependencies
		DependencyNode initDependency = new DependencyNode(null);
		Map<IPass,DependencyNode> nodeMap = new HashMap<>();
		for(IPass pass : passList) {
			DependencyNode node = new DependencyNode(pass);

			//All Depend on global
			node.dependsOn.add(initDependency);
			nodeMap.put(pass,node);
		}
		for(IPass pass : passList) {
			DependencyNode node = nodeMap.get(pass);
			for(PassDependency dependency : dependencyAttachments.computeIfAbsent(pass,k -> new ArrayList<>())) {
				IPass dependPass = attachmentMap.get(dependency.dependency).write_pass;

				//Add non global dependency
				node.dependsOn.add(nodeMap.get(dependPass));
				node.dependsOn.remove(initDependency);
			}
		}

		//Flatten Dependencies
		Set<DependencyNode> visitedNodes = new HashSet<>();
		flatten_dependency(visitedNodes,initDependency);

		//Assign semi-unique images to each attachment (duplication where applicable)
		Map<String,ImageResource> boundImages = new HashMap<>();
		assign_images(boundImages);

		//Split into multiple render passes
		TIntList startIndices = new TIntArrayList();
		split_render_passes(startIndices);

		//Create Render Passes
		int prev_index = startIndices.get(0);
		for(int i = 1; i < startIndices.size(); i++) {
			int next_index = startIndices.get(i);
			create_render_pass(prev_index,next_index,boundImages);
			prev_index = next_index;
		}
		create_render_pass(prev_index,flattenedDependency.size(),boundImages);
		//Create Image Resources

		//Create Frame Buffers

		//Create Descriptor Sets [For binding non attachment resources]
	}

	/**
	 * Call on SwapChain Recreate:
	 *  - Recreate applicable images
	 *  - Recreate applicable frame buffers
	 *  - Recreate applicable descriptor sets
	 */
	public void onSwapRecreate() {
		//Teardown Frame Buffers

		//Teardown Images

		//Create Images

		//Create Frame Buffers
	}

	/**
	 * Call on Pass Teardown:
	 *  - Release all available resources
	 */
	@Override
	public void close() {


		//Clean-up Stored Values
		attachmentMap.clear();
		writeAttachments.clear();
		attachmentMap.clear();
		passList.clear();
		flattenedDependency.clear();
	}

	public void registerPass(IPass pass) {
		passList.add(pass);
	}

	//////////////////////
	///Pass Builder API///
	//////////////////////

	private void attachment_put(String id, PassAttachment attach) {
		attach.attachment_name = id;
		attach.write_pass = currentPass;
		if(!attachmentMap.containsKey(id)) {
			attachmentMap.put(id,attach);
			List<PassAttachment> lists = writeAttachments.computeIfAbsent(currentPass, k -> new ArrayList<>());
			lists.add(attach);
		}else{
			CrashReport crash = new CrashReport("Multiple dependencies with same name");
			crash.invalidState(id);
			OpenVoxel.reportCrash(crash);
		}
		if(attach.load_data_from != null) {
			PassDependency dependency = new PassDependency();
			dependency.dependency = attach.load_data_from;
			dependency.isLocal = false;
			dependency.attachment = true;
			dependency_put(dependency);
		}
	}

	private void dependency_put(PassDependency dependency) {
		List<PassDependency> lists = dependencyAttachments.computeIfAbsent(currentPass, k -> new ArrayList<>());
		lists.add(dependency);
	}

	public void setVoxelDependency(String name,int size, int format) {
		PassAttachment attachment = new PassAttachment();
		attachment.type = AttachmentType.VOXEL_ATTACHMENT;
		attachment.size = size;
		attachment.attachment_id = -1;
		attachment.format = format;
		attachment.layers = 1;
		attachment.samples = VK_SAMPLE_COUNT_1_BIT;
		attachment.can_read = true;
		attachment.clear_on_load = false;
		attachment.load_data_from = null;
		attachment_put(name,attachment);
	}

	public void setFrameTargetOutput(int attachment_id, boolean read, boolean clear) {
		PassAttachment attachment = new PassAttachment();
		attachment.type = AttachmentType.FRAME_TARGET;
		attachment.size = SIZE_VIEWPORT;
		attachment.attachment_id = attachment_id;
		attachment.format = -1;
		attachment.layers = 1;
		attachment.samples = VK_SAMPLE_COUNT_1_BIT;
		attachment.can_read = read;
		attachment.clear_on_load = clear;
		attachment.load_data_from = null;
		attachment_put(FRAME_TARGET_ID,attachment);
	}

	public void setColourAttachment(String name, int size, int attachment_id, int format, int layers, int samples, boolean read, boolean clear, String load) {
		PassAttachment attachment = new PassAttachment();
		attachment.type = AttachmentType.COLOUR_ATTACHMENT;
		attachment.size = size;
		attachment.attachment_id = attachment_id;
		attachment.format = format;
		attachment.layers = layers;
		attachment.samples = samples;
		attachment.can_read = read;
		attachment.clear_on_load = clear;
		attachment.load_data_from = load;
		attachment_put(name,attachment);
	}

	public void setDepthAttachment(String name, int size, int format, int layers, int samples, boolean read, boolean clear, String load) {
		PassAttachment attachment = new PassAttachment();
		attachment.type = AttachmentType.DEPTH_ATTACHMENT;
		attachment.size = size;
		attachment.attachment_id = -1;
		attachment.format = format;
		attachment.layers = layers;
		attachment.samples = samples;
		attachment.can_read = read;
		attachment.clear_on_load = clear;
		attachment.load_data_from = load;
		attachment_put(name,attachment);
	}

	public void addDependency(String id, boolean isLocal) {
		PassDependency dependency = new PassDependency();
		dependency.dependency = id;
		dependency.isLocal = isLocal;
		dependency.attachment = false;
		dependency_put(dependency);
	}

	public interface IPass {
		void configure(OldPassBuilder builder);
	}
}
