package net.openvoxel.client.renderer.vk.util;

import net.openvoxel.api.logger.Logger;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDebugReportCallbackCreateInfoEXT;
import org.lwjgl.vulkan.VkDebugReportCallbackEXT;
import org.lwjgl.vulkan.VkInstance;

import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.EXTDebugReport.*;
import static org.lwjgl.vulkan.VK10.*;

/**
 * Created by James on 16/04/2017.
 *
 * Vulkan Debug Logger Utility : Debug Report
 */
public class VkLogUtil extends VkDebugReportCallbackEXT{

	private Logger vulkanLogger = Logger.getLogger("Vulkan").getSubLogger("Debug Report");
	private static VkLogUtil instance;

	public static long Init(VkInstance vkInstance,boolean enableDetail) {
		instance = new VkLogUtil();
		try(MemoryStack stack = stackPush()) {
			VkDebugReportCallbackCreateInfoEXT createInfoEXT = VkDebugReportCallbackCreateInfoEXT.mallocStack(stack);
			createInfoEXT.sType(VK_STRUCTURE_TYPE_DEBUG_REPORT_CALLBACK_CREATE_INFO_EXT);
			createInfoEXT.pNext(VK_NULL_HANDLE);
			createInfoEXT.flags(
					VK_DEBUG_REPORT_ERROR_BIT_EXT |
					VK_DEBUG_REPORT_WARNING_BIT_EXT |
					VK_DEBUG_REPORT_PERFORMANCE_WARNING_BIT_EXT |
						(enableDetail ? VK_DEBUG_REPORT_INFORMATION_BIT_EXT : 0)
			);
			createInfoEXT.pfnCallback(instance);
			createInfoEXT.pUserData(VK_NULL_HANDLE);
			LongBuffer lb = stack.callocLong(1);
			if(vkCreateDebugReportCallbackEXT(vkInstance,createInfoEXT,null,lb) != VK_SUCCESS) {
				Logger.getLogger("Error loading debug report");
				return 0;
			}else{
				return lb.get(0);
			}
		}
	}

	public static void Cleanup() {
		instance.free();
	}

	private String get_object_type(int type) {
		switch (type) {
			case VK_OBJECT_TYPE_BUFFER:                     return "Buffer";
			case VK_OBJECT_TYPE_BUFFER_VIEW:                return "Buffer-View";
			case VK_OBJECT_TYPE_COMMAND_BUFFER:             return "Command-Buffer";
			case VK_OBJECT_TYPE_COMMAND_POOL:               return "Command-Pool";
			case VK_OBJECT_TYPE_DEBUG_REPORT_CALLBACK_EXT:  return "Debug-Report-Callback";
			case VK_OBJECT_TYPE_DESCRIPTOR_POOL:            return "Descriptor-Pool";
			case VK_OBJECT_TYPE_DESCRIPTOR_SET:             return "Descriptor-Set";
			case VK_OBJECT_TYPE_DESCRIPTOR_SET_LAYOUT:      return "Descriptor-Set-Layout";
			case VK_OBJECT_TYPE_DEVICE:                     return "Device";
			case VK_OBJECT_TYPE_DEVICE_MEMORY:              return "Device-Memory";
			case VK_OBJECT_TYPE_EVENT:                      return "Event";
			case VK_OBJECT_TYPE_FENCE:                      return "Fence";
			case VK_OBJECT_TYPE_FRAMEBUFFER:                return "FrameBuffer";
			case VK_OBJECT_TYPE_IMAGE:                      return "Image";
			case VK_OBJECT_TYPE_IMAGE_VIEW:                 return "Image-View";
			case VK_OBJECT_TYPE_INSTANCE:                   return "Instance";
			case VK_OBJECT_TYPE_PHYSICAL_DEVICE:            return "Physical-Device";
			case VK_OBJECT_TYPE_PIPELINE:                   return "Pipeline";
			case VK_OBJECT_TYPE_PIPELINE_CACHE:             return "Pipeline-Cache";
			case VK_OBJECT_TYPE_PIPELINE_LAYOUT:            return "Pipeline-Layout";
			case VK_OBJECT_TYPE_QUERY_POOL:                 return "Query-Pool";
			case VK_OBJECT_TYPE_QUEUE:                      return "Queue";
			case VK_OBJECT_TYPE_RENDER_PASS:                return "Render-Pass";
			case VK_OBJECT_TYPE_SAMPLER:                    return "Sampler";
			case VK_OBJECT_TYPE_SEMAPHORE:                  return "Semaphore";
			case VK_OBJECT_TYPE_SHADER_MODULE:              return "Shader-Module";
			case VK_OBJECT_TYPE_UNKNOWN:                    return "Unknown";
		}
		return "Invalid-Type";
	}

	@Override
	public int invoke(int flags, int objectType, long object, long location, int messageCode, long pLayerPrefix, long pMessage, long pUserData) {
		String message = getString(pMessage);
		if((flags & VK_DEBUG_REPORT_ERROR_BIT_EXT) != 0) {
			if(objectType == VK_OBJECT_TYPE_COMMAND_BUFFER && message.startsWith("vkCmdPipelineBarrier():")) {
				return VK_FALSE;
			}
			//if(objectType == VK_OBJECT_TYPE_RENDER_PASS && message.startsWith("vkCmdPipelineBarrier():")) {
			//	return VK_FALSE;
			//}
			vulkanLogger.Severe(message);
			vulkanLogger.Severe(get_object_type(objectType));
			vulkanLogger.Severe("Detected Vulkan Error : Terminating");
			System.exit(0);
		}else if((flags & VK_DEBUG_REPORT_WARNING_BIT_EXT) != 0 || (flags & VK_DEBUG_REPORT_PERFORMANCE_WARNING_BIT_EXT) != 0) {
			vulkanLogger.Warning(message);
		}else if((flags & VK_DEBUG_REPORT_INFORMATION_BIT_EXT) != 0) {
			vulkanLogger.Info(message);
		}else {
			vulkanLogger.Debug(message);
		}
		return VK_FALSE;
	}
}
