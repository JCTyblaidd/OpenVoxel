package net.openvoxel.client.renderer.vk.core;

import gnu.trove.list.TIntList;
import net.openvoxel.OpenVoxel;
import net.openvoxel.api.logger.Logger;
import net.openvoxel.api.util.Version;
import net.openvoxel.client.ClientInput;
import net.openvoxel.utility.CrashReport;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkSurfaceCapabilities2KHR;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;

import java.nio.ByteBuffer;
import java.util.List;

import static org.lwjgl.vulkan.EXTDebugReport.*;
import static org.lwjgl.vulkan.EXTDebugReport.VK_DEBUG_REPORT_INFORMATION_BIT_EXT;
import static org.lwjgl.vulkan.EXTDebugReport.VK_DEBUG_REPORT_PERFORMANCE_WARNING_BIT_EXT;
import static org.lwjgl.vulkan.EXTGlobalPriority.VK_ERROR_NOT_PERMITTED_EXT;
import static org.lwjgl.vulkan.KHRDisplaySwapchain.VK_ERROR_INCOMPATIBLE_DISPLAY_KHR;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_SUBOPTIMAL_KHR;
import static org.lwjgl.vulkan.NVGLSLShader.VK_ERROR_INVALID_SHADER_NV;
import static org.lwjgl.vulkan.VK10.*;

final class VulkanUtility {

	private static final Logger VulkanLog = Logger.getLogger("Vulkan");
	private static final Logger DebugReportLog = VulkanLog.getSubLogger("Debug Report");

	static void LogSevere(String str) {
		VulkanLog.Severe(str);
	}

	static void LogWarn(String str) {
		VulkanLog.Warning(str);
	}

	static void LogInfo(String str) {
		VulkanLog.Info(str);
	}

	static void LogDebug(String str) {
		VulkanLog.Debug(str);
	}

	static void CrashOnBadResult(String str,int res) {
		String resString = vkResultToString(res);
		VulkanLog.Severe(str,resString);
		CrashReport report = new CrashReport(str).invalidState(resString);
		OpenVoxel.reportCrash(report);
	}

	static void ValidateSuccess(String str,int res) {
		if(res != VK_SUCCESS) {
			CrashOnBadResult(str,res);
		}
	}

	static int createVersion(@NotNull Version version) {
		return VK_MAKE_VERSION(version.getMajor(),version.getMinor(),version.getPatch());
	}

	static String getNvidiaDriverVersionString(int ver) {
		final int major = VK_VERSION_MAJOR(ver);
		final int minor = (ver >> 14) & 0x0ff;
		final int secondaryBranch = (ver >> 6) & 0x0ff;
		final int tertiaryBranch = (ver) & 0x003f;
		return major + "." + minor + "." + secondaryBranch + "." + tertiaryBranch;
	}

	static String getVersionAsString(int ver) {
		return VK_VERSION_MAJOR(ver) + "." + VK_VERSION_MINOR(ver) + "." + VK_VERSION_PATCH(ver);
	}

	static PointerBuffer toPointerBuffer(MemoryStack stack, List<ByteBuffer> entryList) {
		PointerBuffer output = stack.callocPointer(entryList.size());
		for(ByteBuffer buffer : entryList) {
			output.put(buffer);
		}
		output.position(0);
		return output;
	}

	////////////////////////////////////
	/// Default Choice Functionality ///
	////////////////////////////////////

	public static int chooseDefaultPresentMode(TIntList validPresentModes) {
		if(validPresentModes.contains(VK_PRESENT_MODE_MAILBOX_KHR)) {
			LogInfo("Chosen Present Mode: Mailbox");
			return VK_PRESENT_MODE_MAILBOX_KHR;
		}
		LogInfo("Chosen Present Mode: FIFO");
		return VK_PRESENT_MODE_FIFO_KHR;
	}

	public static int chooseSurfaceFormat(VkSurfaceFormatKHR.Buffer validFormats,boolean getImageFormat) {
		if (validFormats.capacity() == 1 && validFormats.get(0).format() == VK_FORMAT_UNDEFINED) {
			if(getImageFormat) {
				LogInfo("Chosen Surface Format: Default[Universal]");
				return VK_FORMAT_B8G8R8A8_UNORM;
			}else{
				LogInfo("Chosen Colour Space: Default[Universal]");
				return VK_COLOR_SPACE_SRGB_NONLINEAR_KHR;
			}
		} else {
			for (int i = 0; i < validFormats.capacity(); i++) {
				validFormats.position(i);
				if (validFormats.format() == VK_FORMAT_B8G8R8A8_UNORM && validFormats.colorSpace() == VK_COLOR_SPACE_SRGB_NONLINEAR_KHR) {
					if(getImageFormat) {
						LogInfo("Chosen Surface Format: Default[Found]");
						return validFormats.format();
					}else{
						LogInfo("Chosen Colour Space: Default[Found]");
						return validFormats.colorSpace();
					}
				}
			}
			validFormats.position(0);
			if(getImageFormat) {
				LogInfo("Chosen Surface Format: Fallback[#"+Integer.toHexString(validFormats.format()));
				return validFormats.format();
			}else{
				LogInfo("Chosen Colour Space: Fallback[#"+Integer.toHexString(validFormats.colorSpace()));
				return validFormats.colorSpace();
			}
		}
	}

	public static void chooseSwapExtent(VkSurfaceCapabilitiesKHR capabilities, VkExtent2D swapExtent) {
		if (capabilities.currentExtent().width() != 0xFFFFFFFF) {
			swapExtent.set(capabilities.currentExtent());
		} else {
			int width = ClientInput.currentWindowWidth.get();
			int height = ClientInput.currentWindowHeight.get();
			width = Math.min(Math.max(width, capabilities.minImageExtent().width()), capabilities.maxImageExtent().width());
			height = Math.min(Math.max(height, capabilities.minImageExtent().height()), capabilities.maxImageExtent().height());
			swapExtent.set(width, height);
		}
		LogInfo("Chosen Swap Extent: ("+ swapExtent.width()+","+swapExtent.height()+")");
	}

	public static int chooseImageCount(VkSurfaceCapabilitiesKHR capabilities) {
		int imageCount = capabilities.minImageCount() + 1;
		if (capabilities.maxImageCount() > 0 && imageCount > capabilities.maxImageCount()) {
			LogInfo("Chosen Image Count: " + capabilities.maxImageCount());
			return capabilities.maxImageCount();
		}else{
			LogInfo("Chosen Image Count: " + imageCount);
			return imageCount;
		}
	}

	//////////////////////////////////
	/// Internal Utility Functions ///
	//////////////////////////////////

	private static String vkResultToString(int result) {
		switch(result) {
			case VK_SUCCESS:                        return "VK_SUCCESS";
			case VK_NOT_READY:                      return "VK_NOT_READY";
			case VK_TIMEOUT:                        return "VK_TIMEOUT";
			case VK_EVENT_SET:                      return "VK_EVENT_SET";
			case VK_EVENT_RESET:                    return "VK_EVENT_RESET";
			case VK_INCOMPLETE:                     return "VK_INCOMPLETE";
			case VK_ERROR_OUT_OF_HOST_MEMORY:       return "VK_ERROR_OUT_OF_HOST_MEMORY";
			case VK_ERROR_OUT_OF_DEVICE_MEMORY:     return "VK_ERROR_OUT_OF_DEVICE_MEMORY";
			case VK_ERROR_INITIALIZATION_FAILED:    return "VK_ERROR_INITIALIZATION_FAILED";
			case VK_ERROR_DEVICE_LOST:              return "VK_ERROR_DEVICE_LOST";
			case VK_ERROR_MEMORY_MAP_FAILED:        return "VK_ERROR_MEMORY_MAP_FAILED";
			case VK_ERROR_LAYER_NOT_PRESENT:        return "VK_ERROR_LAYER_NOT_PRESENT";
			case VK_ERROR_EXTENSION_NOT_PRESENT:    return "VK_ERROR_EXTENSION_NOT_PRESENT";
			case VK_ERROR_FEATURE_NOT_PRESENT:      return "VK_ERROR_FEATURE_NOT_PRESENT";
			case VK_ERROR_INCOMPATIBLE_DRIVER:      return "VK_ERROR_INCOMPATIBLE_DRIVER";
			case VK_ERROR_TOO_MANY_OBJECTS:         return "VK_ERROR_TOO_MANY_OBJECTS";
			case VK_ERROR_FORMAT_NOT_SUPPORTED:     return "VK_ERROR_FORMAT_NOT_SUPPORTED";
			case VK_ERROR_FRAGMENTED_POOL:          return "VK_ERROR_FRAGMENTED_POOL";
			//case VK_ERROR_OUT_OF_POOL_MEMORY:       return "VK_ERROR_OUT_OF_POOL_MEMORY";
			//case VK_ERROR_INVALID_EXTERNAL_HANDLE:  return "VK_ERROR_INVALID_EXTERNAL_HANDLE";
			case VK_ERROR_SURFACE_LOST_KHR:         return "VK_ERROR_SURFACE_LOST_KHR";
			case VK_ERROR_NATIVE_WINDOW_IN_USE_KHR: return "VK_ERROR_NATIVE_WINDOW_IN_USE_KHR";
			case VK_SUBOPTIMAL_KHR:                 return "VK_SUBOPTIMAL_KHR";
			case VK_ERROR_OUT_OF_DATE_KHR:          return "VK_ERROR_OUT_OF_DATE_KHR";
			case VK_ERROR_INCOMPATIBLE_DISPLAY_KHR: return "VK_ERROR_INCOMPATIBLE_DISPLAY_KHR";
			case VK_ERROR_VALIDATION_FAILED_EXT:    return "VK_ERROR_VALIDATION_FAILED_EXT";
			case VK_ERROR_INVALID_SHADER_NV:        return "VK_ERROR_INVALID_SHADER_NV";
			//case VK_ERROR_FRAGMENTATION_EXT:        return "VK_ERROR_FRAGMENTATION_EXT";
			case VK_ERROR_NOT_PERMITTED_EXT:        return "VK_ERROR_NOT_PERMITTED_EXT";
			default: return "VK_UNKNOWN_RESULT - #"+Integer.toHexString(result);
		}
	}

	static String getVendorAsString(int vendorID) {
		switch(vendorID) {
			case 0x1002: return "AMD";
			case 0x1010: return "ImgTec";
			case 0x10DE: return "NVIDIA";
			case 0x13B5: return "ARM";
			case 0x5143: return "Qualcomm";
			case 0x8086: return "INTEL";
			default:
				return "UnknownVendor-#"+Integer.toHexString(vendorID);
		}
	}

	private static String getObjectTypeAsString(int type) {
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
			default: return "Unknown Type-#" + Integer.toHexString(type);
		}
	}

	static int CallDebugReport(int flags, String message, int objectType) {
		String objectName = getObjectTypeAsString(objectType);
		if((flags & VK_DEBUG_REPORT_ERROR_BIT_EXT) != 0) {
			//if(objectType == VK_OBJECT_TYPE_COMMAND_BUFFER && message.startsWith("vkCmdPipelineBarrier():")) {
			//	return VK_FALSE;
			//}
			//if(objectType == VK_OBJECT_TYPE_RENDER_PASS && message.startsWith("vkCmdPipelineBarrier():")) {
			//	return VK_FALSE;
			//}
			DebugReportLog.Severe(message,": ",message);
			System.exit(0);
		}else if((flags & VK_DEBUG_REPORT_WARNING_BIT_EXT) != 0 || (flags & VK_DEBUG_REPORT_PERFORMANCE_WARNING_BIT_EXT) != 0) {
			DebugReportLog.Warning(objectName ,": ",message);
		}else if((flags & VK_DEBUG_REPORT_INFORMATION_BIT_EXT) != 0) {
			DebugReportLog.Info(message);
		}else {
			DebugReportLog.Debug(message);
		}
		return VK_FALSE;
	}

}
