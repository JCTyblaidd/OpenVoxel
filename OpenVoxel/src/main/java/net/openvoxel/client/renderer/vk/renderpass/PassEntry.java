package net.openvoxel.client.renderer.vk.renderpass;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static net.openvoxel.client.renderer.vk.renderpass.PassResource.VIEWPORT_SIZE;
import static org.lwjgl.vulkan.VK10.VK_SAMPLE_COUNT_1_BIT;

public abstract class PassEntry {

	static final String FRAME_ID = "frame_output_attachment";

	List<PassEntry> passDependencies            = new ArrayList<>(); //Pass Dependency
	List<String>    outputIdList                = new ArrayList<>(); //Resource Ids Created
	List<String>    inputIdList                 = new ArrayList<>(); //Resource Ids Consumed
	List<OutputAttachment>  outputAttachments   = new ArrayList<>(); //Resources Created
	List<InputAttachment>   inputAttachments    = new ArrayList<>(); //Resources Consumed

	int output_width;
	int output_height;
	int output_layers;

	enum AttachmentType {
		COLOUR_ATTACHMENT,          //colour output attachment
		DEPTH_ATTACHMENT,           //depth output attachment
		RESOLVE_ATTACHMENT,         //multi sample resolve attachment
		EXTERNAL_ATTACHMENT,        //external un-managed attachment
		FRAME_ATTACHMENT,           //frame target attachment
		FRAME_RESOLVE_ATTACHMENT;   //frame target attachment(resolve operation)

		static boolean isFrame(AttachmentType type) {
			return type == FRAME_ATTACHMENT || type == FRAME_RESOLVE_ATTACHMENT;
		}
	}

	static class OutputAttachment {
		AttachmentType type;        //type of created attachment
		String identifier;          //identifier of attachment
		int binding;                //output binding of attachment

		//Image Configuration
		int width;                  //width or PassResource.VIEWPORT_SIZE
		int height;                 //height or PassResource.VIEWPORT_SIZE
		int depth;                  //depth or PassResource.VIEWPORT_SIZE
		int layers;                 //layer count
		int format;                 //format used
		int samples;                //samples used

		//Initial Data Loading
		boolean read_on_create;     //blend operations required (reads existing values)
		boolean clear_on_create;    //clear before usage
		String  load_to_create;     //copy existing values from previous attachment
	}
	static class InputAttachment {
		String identifier;          //identifier used
		boolean local_dependency;   //if local dependency is needed
		int bound_index;            //binding of input attachment
	}

	final void configureState() {
		passDependencies.clear();
		configure();

		//Find and validate output sizing
		boolean has_assigned = false;
		output_width = -100;
		output_height = -100;
		output_layers = -100;
		for(OutputAttachment output : outputAttachments) {
			if(output.type == AttachmentType.EXTERNAL_ATTACHMENT) continue;
			if(!has_assigned) {
				output_width = output.width;
				output_height = output.height;
				output_layers = output.layers;
				has_assigned = true;
			}else{
				if(output_width != output.width) throw new RuntimeException("Differing Pass Output");
				if(output_height != output.height) throw new RuntimeException("Differing Pass Output");
				if(output_layers != output.layers) throw new RuntimeException("Differing Pass Output");
			}
		}

		//List identifiers
		outputIdList.clear();
		inputIdList.clear();
		outputAttachments.stream().map(output -> output.identifier).forEach(outputIdList::add);
		outputAttachments.stream().map(output -> output.load_to_create).filter(Objects::nonNull).forEach(inputIdList::add);
		inputAttachments.stream().map(input -> input.identifier).forEach(inputIdList::add);
	}

	/////////////////////////////////
	/// API Used By Super Classes ///
	/////////////////////////////////

	protected void setDepthAttachment(String name, int width, int height, int format, int layers, int samples, boolean read, boolean clear, String load) {
		OutputAttachment output = new OutputAttachment();
		output.type = AttachmentType.DEPTH_ATTACHMENT;
		output.identifier = name;
		output.binding = -1;
		output.width = width;
		output.height = height;
		output.depth = 1;
		output.layers = layers;
		output.format = format;
		output.samples = samples;
		output.read_on_create = read;
		output.clear_on_create = clear;
		output.load_to_create = load;
		outputAttachments.add(output);
	}

	protected void setColourAttachment(String name, int width, int height, int binding, int format, int layers, int samples, boolean read, boolean clear, String load) {
		OutputAttachment output = new OutputAttachment();
		output.type = AttachmentType.COLOUR_ATTACHMENT;
		output.identifier = name;
		output.binding = binding;
		output.width = width;
		output.height = height;
		output.depth = 1;
		output.layers = layers;
		output.format = format;
		output.samples = samples;
		output.read_on_create = read;
		output.clear_on_create = clear;
		output.load_to_create = load;
		outputAttachments.add(output);
	}

	protected void setFrameAttachment(int binding, boolean read, boolean clear) {
		OutputAttachment output = new OutputAttachment();
		output.type = AttachmentType.FRAME_ATTACHMENT;
		output.identifier = FRAME_ID;
		output.binding = binding;
		output.width = VIEWPORT_SIZE;
		output.height = VIEWPORT_SIZE;
		output.depth = 1;
		output.layers = 1;
		output.format = -1;//Unknown Depends on Frame
		output.samples = VK_SAMPLE_COUNT_1_BIT;
		output.read_on_create = read;
		output.clear_on_create = clear;
		outputAttachments.add(output);
	}

	protected void setResolveAttachment(String name, int width, int height, int binding, int format, int layers) {
		OutputAttachment output = new OutputAttachment();
		output.type = AttachmentType.RESOLVE_ATTACHMENT;
		output.identifier = name;
		output.binding = binding;
		output.width = width;
		output.height = height;
		output.depth = 1;
		output.layers = layers;
		output.format = format;
		output.samples = VK_SAMPLE_COUNT_1_BIT;
		output.read_on_create = false;
		output.clear_on_create = false;
		outputAttachments.add(output);
	}

	protected void setFrameResolveAttachment(int binding) {
		OutputAttachment output = new OutputAttachment();
		output.type = AttachmentType.FRAME_RESOLVE_ATTACHMENT;
		output.identifier = FRAME_ID;
		output.binding = binding;
		output.width = VIEWPORT_SIZE;
		output.height = VIEWPORT_SIZE;
		output.depth = 1;
		output.layers = 1;
		output.format = -1;//Unknown Depends on Frame
		output.samples = VK_SAMPLE_COUNT_1_BIT;
		output.read_on_create = false;
		output.clear_on_create = false;
		outputAttachments.add(output);
	}

	protected void setExternalAttachment(String name, int binding, int width, int height,int depth, int format, int layers, int samples) {
		OutputAttachment output = new OutputAttachment();
		output.type = AttachmentType.EXTERNAL_ATTACHMENT;
		output.identifier = name;
		output.binding = binding; //References Descriptor Binding NOT Output Binding
		output.width = width;
		output.height = height;
		output.depth = depth;
		output.layers = layers;
		output.format = format;
		output.samples = samples;
		output.read_on_create = false;//Not Applicable
		output.clear_on_create = false;
		outputAttachments.add(output);
	}

	protected void setAttachmentDependency(String name, int binding, boolean localDependency) {
		InputAttachment input = new InputAttachment();
		input.identifier = name;
		input.bound_index = binding;
		input.local_dependency = localDependency;
		inputAttachments.add(input);
	}

	/**
	 * Build & Configure dependency information
	 */
	protected abstract void configure();

}
