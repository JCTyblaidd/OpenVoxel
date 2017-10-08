package net.openvoxel.client.renderer.vk.util;

import net.openvoxel.api.logger.Logger;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDebugReportCallbackCreateInfoEXT;
import org.lwjgl.vulkan.VkDebugReportCallbackEXT;
import org.lwjgl.vulkan.VkInstance;

import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.EXTDebugReport.*;
import static org.lwjgl.vulkan.VK10.VK_FALSE;
import static org.lwjgl.vulkan.VK10.VK_NULL_HANDLE;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;

/**
 * Created by James on 16/04/2017.
 *
 * Vulkan Debug Logger Utility : Debug Report
 */
public class VkLogUtil extends VkDebugReportCallbackEXT{

	private Logger vulkanLogger = Logger.getLogger("Vulkan").getSubLogger("Debug Report");
	private static VkLogUtil instance = new VkLogUtil();

	public static long Init(VkInstance vkInstance,boolean enableDetail) {
		try(MemoryStack stack = stackPush()) {
			VkDebugReportCallbackCreateInfoEXT createInfoEXT = VkDebugReportCallbackCreateInfoEXT.mallocStack(stack);
			createInfoEXT.sType(VK_STRUCTURE_TYPE_DEBUG_REPORT_CALLBACK_CREATE_INFO_EXT);
			createInfoEXT.pNext(VK_NULL_HANDLE);
			createInfoEXT.flags(
					VK_DEBUG_REPORT_ERROR_BIT_EXT |
					VK_DEBUG_REPORT_WARNING_BIT_EXT |
					VK_DEBUG_REPORT_PERFORMANCE_WARNING_BIT_EXT |
						(enableDetail ? VK_DEBUG_REPORT_INFORMATION_BIT_EXT |
						VK_DEBUG_REPORT_DEBUG_BIT_EXT : 0)
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
