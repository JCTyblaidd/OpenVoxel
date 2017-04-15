package net.openvoxel.client.renderer.vk.util;

import net.openvoxel.OpenVoxel;
import net.openvoxel.api.logger.Logger;
import net.openvoxel.api.util.Version;
import net.openvoxel.utility.CrashReport;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.vulkan.VK10.*;

/**
 * Created by James on 03/09/2016.
 *
 * Vulkan Global State Information
 */
public class VkDeviceState {

	public VkInstance instance;
	public VkPhysicalDevice physicalDevice;
	public VkDevice device;

	public VkDeviceMemory memoryManager;
	public Logger vkLogger;


	private static int vkVersion(Version version) {
		return VK_MAKE_VERSION(version.getMajor(),version.getMinor(),version.getPatch());
	}

	private void initApplicationInfo(VkApplicationInfo appInfo,ByteBuffer appNameBuf) {
		appInfo.sType(VK_STRUCTURE_TYPE_APPLICATION_INFO);
		appInfo.pNext(VK_NULL_HANDLE);
		appInfo.apiVersion(VK_MAKE_VERSION(1,0,0));
		appInfo.applicationVersion(vkVersion(OpenVoxel.currentVersion));
		appInfo.engineVersion(vkVersion(OpenVoxel.currentVersion));
		appInfo.pApplicationName(appNameBuf);
		appInfo.pEngineName(appNameBuf);
	}

	private void initInstanceCreateInfo(VkInstanceCreateInfo createInfo, VkApplicationInfo appInfo,MemoryStack stack) {
		createInfo.sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO);
		createInfo.pNext(VK_NULL_HANDLE);
		createInfo.flags(0);
		createInfo.pApplicationInfo(appInfo);
		//Choose Enabled Layer & Extensions//

	}

	public void initInstance() {
		vkLogger = Logger.getLogger("Vulkan");
		try(MemoryStack stack = MemoryStack.stackPush()) {
			PointerBuffer pointer = stack.callocPointer(1);
			ByteBuffer appNameBuf = stack.UTF8("Open Voxel");
			VkApplicationInfo appInfo = VkApplicationInfo.mallocStack(stack);
			VkInstanceCreateInfo createInfo = VkInstanceCreateInfo.mallocStack(stack);
			initApplicationInfo(appInfo,appNameBuf);
			initInstanceCreateInfo(createInfo, appInfo,stack);
			int vkResult = vkCreateInstance(createInfo,null,pointer);
			if(vkResult == VK_SUCCESS) {
				vkLogger.Info("Created Instance");
			}else{
				vkLogger.Severe("Failed to Create Instance: " + vkResult);
				CrashReport report = new CrashReport("Failed to create vulkan instance")
						                     .invalidState("vkResult = " + vkResult);
				OpenVoxel.reportCrash(report);
			}
			instance = new VkInstance(pointer.get(),createInfo);
		}
	}

	public void initDevice() {
		try(MemoryStack stack = MemoryStack.stackPush()) {
			IntBuffer deviceCount = stack.mallocInt(1);
			vkEnumeratePhysicalDevices(instance,deviceCount,null);
			PointerBuffer devices = stack.callocPointer(deviceCount.get(0));
			//vkEnumeratePhysicalDevices(instance,)
		}
	}

}
