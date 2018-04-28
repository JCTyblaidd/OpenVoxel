package net.openvoxel.client.renderer.vk.core;

import net.openvoxel.OpenVoxel;
import net.openvoxel.client.ClientInput;
import net.openvoxel.utility.CrashReport;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWVulkan.glfwGetRequiredInstanceExtensions;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.EXTDebugReport.*;
import static org.lwjgl.vulkan.VK10.*;

public final class VulkanState implements Closeable {

	///Flags
	private static boolean flag_vulkanDetailedDeviceInfo = OpenVoxel.getLaunchParameters().hasFlag("vkDeviceInfo");
	private static boolean flag_vulkanDetailLog = OpenVoxel.getLaunchParameters().hasFlag("vkDebugDetailed");
	static boolean flag_vulkanDebug = OpenVoxel.getLaunchParameters().hasFlag("vkDebug") || flag_vulkanDetailLog;
	static boolean flag_vulkanRenderDoc = OpenVoxel.getLaunchParameters().hasFlag("vkRenderDoc");

	///State
	private final long GLFWWindow;
	private final VkInstance VulkanInstance;
	private final VulkanDevice VulkanDevice;
	private final VulkanMemory VulkanMemory;

	private boolean HasDebugReport = false;
	private long DebugReportCallback = 0;
	private VkDebugReportCallbackEXT DebugReportCallbackFunc = null;


	/**
	 * Allocate all resources
	 */
	public VulkanState() {
		GLFWWindow = createWindow();
		VulkanInstance = createInstance();
		createDebugReport();
		VulkanDevice = new VulkanDevice(VulkanInstance);
		if(flag_vulkanDetailedDeviceInfo) {
			VulkanDevice.printDetailedDeviceInfo();
		}
		VulkanMemory = new VulkanMemory(VulkanDevice);
	}

	/*
	 * Cleanup all allocated resources
	 */
	@Override
	public void close() {
		VulkanMemory.close();
		VulkanDevice.close();
		destroyDebugReport();
		destroyInstance();
		destroyWindow();
	}

	/////////////////////////////////////
	/// Management of Other Resources ///
	/////////////////////////////////////

	private long createWindow() {
		glfwDefaultWindowHints();
		glfwWindowHint(GLFW_CLIENT_API,GLFW_NO_API);
		long window = glfwCreateWindow(ClientInput.currentWindowWidth.get(), ClientInput.currentWindowHeight.get(), "Open Voxel " + OpenVoxel.currentVersion.getValString(), 0, 0);
		try(MemoryStack stack = stackPush()) {
			IntBuffer windowWidth = stack.mallocInt(0);
			IntBuffer windowHeight = stack.mallocInt(0);
			glfwGetWindowSize(window,windowWidth,windowHeight);
			ClientInput.currentWindowWidth.set(windowWidth.get(0));
			ClientInput.currentWindowHeight.set(windowHeight.get(0));
		}
		return window;
	}

	private void destroyWindow() {
		glfwDestroyWindow(GLFWWindow);
	}

	///////////////////////
	/// Vulkan Instance ///
	///////////////////////

	private void setApplicationInfo(@NotNull MemoryStack stack,@NotNull VkApplicationInfo app) {
		ByteBuffer appNameBuf = stack.UTF8("Open Voxel");
		app.sType(VK_STRUCTURE_TYPE_APPLICATION_INFO);
		app.pNext(VK_NULL_HANDLE);
		app.apiVersion(VK_MAKE_VERSION(1,0,0));
		app.applicationVersion(VulkanUtility.createVersion(OpenVoxel.currentVersion));
		app.engineVersion(VulkanUtility.createVersion(OpenVoxel.currentVersion));
		app.pApplicationName(appNameBuf);
		app.pEngineName(appNameBuf);
	}

	private PointerBuffer chooseEnabledExtensions(MemoryStack stack) {
		IntBuffer sizeRef = stack.callocInt(1);
		final String error = "Error Enumerating Instance Extensions";
		VulkanUtility.ValidateSuccess(error,
				vkEnumerateInstanceExtensionProperties((ByteBuffer)null,sizeRef,null));
		VkExtensionProperties.Buffer extensionList = VkExtensionProperties.callocStack(sizeRef.get(0),stack);
		VulkanUtility.ValidateSuccess(error,
				vkEnumerateInstanceExtensionProperties((ByteBuffer)null,sizeRef,extensionList));

		List<ByteBuffer> enabledExtensions = new ArrayList<>();
		PointerBuffer requiredExtensions = glfwGetRequiredInstanceExtensions();
		if(requiredExtensions == null) {
			CrashReport crash = new CrashReport("Failed to get GLFW extensions");
			crash.invalidState("glfwGetRequiredInstanceExtensions() == null");
			OpenVoxel.reportCrash(crash);
			return null;
		}
		for(int i = 0; i < requiredExtensions.capacity(); i++) {
			enabledExtensions.add(requiredExtensions.getByteBuffer(i));
			if(flag_vulkanDebug) {
				VulkanUtility.LogDebug("GLFW Extension: " + MemoryUtil.memUTF8(requiredExtensions.get(i)));
			}
		}
		VulkanUtility.LogInfo("Added GLFW Extensions");
		for(int i = 0; i < sizeRef.get(0); i++) {
			extensionList.position(i);
			if(flag_vulkanDebug) {
				VulkanUtility.LogDebug("Instance Extension: " + extensionList.extensionNameString());
			}
			if(flag_vulkanDebug) {
				if(extensionList.extensionNameString().equals("VK_EXT_debug_report")) {
					enabledExtensions.add(extensionList.extensionName());
					VulkanUtility.LogInfo("Enabled Ext: Debug Report");
					//Enable Debug Report Flag
					HasDebugReport = true;
				}
			}
			if(extensionList.extensionNameString().equals("VK_KHR_display")) {
				enabledExtensions.add(extensionList.extensionName());
				VulkanUtility.LogInfo("Enabled Ext: KHR Display");
			}
			if(extensionList.extensionNameString().equals("VK_KHR_display_swapchain")) {
				enabledExtensions.add(extensionList.extensionName());
				VulkanUtility.LogInfo("Enabled Ext: KHR Display SwapChain");
			}
		}
		return VulkanUtility.toPointerBuffer(stack,enabledExtensions);
	}

	private PointerBuffer chooseEnabledLayers(MemoryStack stack) {
		IntBuffer sizeRef = stack.callocInt(1);
		final String error = "Error Enumerating Instance Layers";
		VulkanUtility.ValidateSuccess(error,vkEnumerateInstanceLayerProperties(sizeRef,null));
		VkLayerProperties.Buffer layerList = VkLayerProperties.mallocStack(sizeRef.get(0),stack);
		VulkanUtility.ValidateSuccess(error,vkEnumerateInstanceLayerProperties(sizeRef,layerList));

		List<ByteBuffer> enabledLayers = new ArrayList<>();
		for(int i = 0; i < sizeRef.get(0); i++) {
			layerList.position(i);
			if(flag_vulkanDebug) {
				VulkanUtility.LogDebug("Instance Layer: " + layerList.layerNameString());
			}
			if(flag_vulkanDebug) {
				if(layerList.layerNameString().equals("VK_LAYER_LUNARG_standard_validation")) {
					enabledLayers.add(layerList.layerName());
					VulkanUtility.LogInfo("Enabled Layer: Standard Validation");
					continue;
				}
			}
			if(flag_vulkanRenderDoc) {
				if(layerList.layerNameString().equals("VK_LAYER_RENDERDOC_Capture")) {
					enabledLayers.add(layerList.layerName());
					VulkanUtility.LogInfo("Enabled Layer: RenderDoc Capture");
					continue;
				}
			}
			if(OpenVoxel.getLaunchParameters().hasFlag("-VKLayer:"+layerList.layerNameString())) {
				enabledLayers.add(layerList.layerName());
				VulkanUtility.LogInfo("Enabled Layer: " + layerList.layerNameString());
			}
		}
		return VulkanUtility.toPointerBuffer(stack,enabledLayers);
	}

	private VkInstance createInstance() {
		VkInstance res;
		try(MemoryStack stack = stackPush()){
			VkApplicationInfo appInfo = VkApplicationInfo.mallocStack(stack);
			setApplicationInfo(stack,appInfo);

			VkInstanceCreateInfo createInfo = VkInstanceCreateInfo.mallocStack(stack);
			createInfo.sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO);
			createInfo.pNext(VK_NULL_HANDLE);
			createInfo.flags(0);
			createInfo.pApplicationInfo(appInfo);
			createInfo.ppEnabledLayerNames(chooseEnabledLayers(stack));
			createInfo.ppEnabledExtensionNames(chooseEnabledExtensions(stack));

			PointerBuffer pointer = stack.mallocPointer(1);
			int result = vkCreateInstance(createInfo,null,pointer);
			if(result == VK_SUCCESS) {
				VulkanUtility.LogInfo("Created Instance");
			}else {
				VulkanUtility.CrashOnBadResult("Failed to create instance",result);
			}
			res = new VkInstance(pointer.get(0),createInfo);
		}
		return res;
	}

	private void destroyInstance() {
		vkDestroyInstance(VulkanInstance,null);
	}

	////////////////////
	/// Vulkan Debug ///
	////////////////////

	private void createDebugReport() {
		if(HasDebugReport) {
			try(MemoryStack stack = stackPush()) {
				VkDebugReportCallbackCreateInfoEXT createInfoEXT = VkDebugReportCallbackCreateInfoEXT.mallocStack(stack);
				createInfoEXT.sType(VK_STRUCTURE_TYPE_DEBUG_REPORT_CALLBACK_CREATE_INFO_EXT);
				createInfoEXT.pNext(VK_NULL_HANDLE);
				createInfoEXT.flags(
						VK_DEBUG_REPORT_ERROR_BIT_EXT |
						VK_DEBUG_REPORT_WARNING_BIT_EXT |
						VK_DEBUG_REPORT_PERFORMANCE_WARNING_BIT_EXT |
						(flag_vulkanDetailLog ? VK_DEBUG_REPORT_INFORMATION_BIT_EXT : 0)
				);
				DebugReportCallbackFunc = new VkDebugReportCallbackEXT() {
					@Override
					public int invoke(int flags, int objectType, long object, long location,
					                  int messageCode, long pLayerPrefix, long pMessage, long pUserData) {
						String message = getString(pMessage);
						return VulkanUtility.CallDebugReport(flags,message,objectType);
					}
				};
				createInfoEXT.pfnCallback(DebugReportCallbackFunc);
				createInfoEXT.pUserData(VK_NULL_HANDLE);
				LongBuffer lb = stack.callocLong(1);
				if(vkCreateDebugReportCallbackEXT(VulkanInstance,createInfoEXT,null,lb) != VK_SUCCESS) {
					VulkanUtility.LogWarn("Debug Report: Error on Initialization");
				}else{
					DebugReportCallback = lb.get(0);
				}
			}
		}
	}

	private void destroyDebugReport() {
		if(DebugReportCallback != 0) {
			vkDestroyDebugReportCallbackEXT(VulkanInstance,DebugReportCallback,null);
		}
		if(DebugReportCallbackFunc != null) {
			DebugReportCallbackFunc.free();
		}
	}

}