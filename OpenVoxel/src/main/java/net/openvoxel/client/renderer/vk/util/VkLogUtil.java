package net.openvoxel.client.renderer.vk.util;

import net.openvoxel.api.logger.Logger;
import org.lwjgl.vulkan.VkDebugReportCallbackCreateInfoEXT;
import org.lwjgl.vulkan.VkDebugReportCallbackEXT;

import static org.lwjgl.vulkan.EXTDebugReport.*;
import static org.lwjgl.vulkan.VK10.VK_FALSE;

/**
 * Created by James on 16/04/2017.
 *
 * Vulkan Debug Logger Utility : Debug Report
 */
public class VkLogUtil extends VkDebugReportCallbackEXT{

	private Logger vulkanLogger = Logger.getLogger("Vulkan").getSubLogger("Debug Report");

	public static void Init() {
		VkDebugReportCallbackCreateInfoEXT createInfoEXT;
	}

	@Override
	public int invoke(int flags, int objectType, long object, long location, int messageCode, long pLayerPrefix, long pMessage, long pUserData) {
		String message = getString(pMessage);
		if((flags & VK_DEBUG_REPORT_ERROR_BIT_EXT) != 0) {
			vulkanLogger.Severe(message);
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
