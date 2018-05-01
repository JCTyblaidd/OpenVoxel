package net.openvoxel.client.renderer.vk.core;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import net.openvoxel.OpenVoxel;
import net.openvoxel.client.ClientInput;
import net.openvoxel.client.renderer.glfw.GLFWEventHandler;
import net.openvoxel.client.renderer.vk.VulkanCommandHandler;
import net.openvoxel.utility.CrashReport;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWVulkan.glfwCreateWindowSurface;
import static org.lwjgl.glfw.GLFWVulkan.glfwGetRequiredInstanceExtensions;
import static org.lwjgl.system.MemoryStack.create;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.EXTDebugReport.*;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

public final class VulkanState {

	///Flags
	private static boolean flag_vulkanDetailedDeviceInfo = OpenVoxel.getLaunchParameters().hasFlag("vkDeviceInfo");
	private static boolean flag_vulkanDetailLog = OpenVoxel.getLaunchParameters().hasFlag("vkDebugDetailed");
	private static boolean flag_vulkanDumpAPI = OpenVoxel.getLaunchParameters().hasFlag("vkDumpAPI");
	static boolean flag_vulkanDebug = OpenVoxel.getLaunchParameters().hasFlag("vkDebug") || flag_vulkanDetailLog;
	static boolean flag_vulkanRenderDoc = OpenVoxel.getLaunchParameters().hasFlag("vkRenderDoc");

	///State
	public final long GLFWWindow;
	private final VkInstance VulkanInstance;
	public final VulkanDevice VulkanDevice;
	public final VulkanMemory VulkanMemory;
	private final long VulkanSurface;

	///Dynamic State
	public long VulkanSwapChain;
	public LongBuffer VulkanSwapChainImages;
	public LongBuffer VulkanSwapChainImageViews;
	public int VulkanSwapChainSize;


	//SwapChain Configuration
	public TIntList validPresentModes = new TIntArrayList();
	public VkSurfaceFormatKHR.Buffer validSurfaceFormats;
	public VkSurfaceCapabilitiesKHR surfaceCapabilities;
	//Chosen SwapChain Values
	private VkExtent2D chosenSwapExtent;
	public int chosenPresentMode;
	private int chosenImageFormat;
	private int chosenColourSpace;
	private int chosenImageCount;

	//Configuration Settings
	private boolean HasDebugReport = false;
	private long DebugReportCallback = 0;
	private VkDebugReportCallbackEXT DebugReportCallbackFunc = null;


	/**
	 * Allocate all resources
	 */
	public VulkanState() {
		////Create Constants////
		GLFWWindow = createWindow();
		GLFWEventHandler.Load(GLFWWindow);
		VulkanInstance = createInstance();
		createDebugReport();
		VulkanSurface = createSurface();
		////Create Managers////
		VulkanDevice = new VulkanDevice(VulkanInstance,VulkanSurface);
		if(flag_vulkanDetailedDeviceInfo) {
			VulkanDevice.printDetailedDeviceInfo();
		}
		VulkanMemory = new VulkanMemory(VulkanDevice);
		///Create Swap-Chain///
		createSwapChain(false);
	}

	public VkDevice getLogicalDevice() {
		return VulkanDevice.logicalDevice;
	}

	/**
	 * Recreates the swapchain...
	 * @return If the image count changed?
	 */
	public boolean recreate() {
		int oldSize = VulkanSwapChainSize;
		//Destroy///

		//ReCreate//
		createSwapChain(true);
		//Create//

		return oldSize != chosenImageCount;
	}

	/*
	 * Cleanup all allocated resources
	 */
	public void close() {
		///Destroy Swap-Chain///
		destroySwapChain();
		////Destroy Managers////
		VulkanMemory.close();
		VulkanDevice.close();
		////Destroy Constants////
		destroySurface();
		destroyDebugReport();
		destroyInstance();
		GLFWEventHandler.Unload();
		destroyWindow();
	}

	///////////////////////////////
	/// Other API Functionality ///
	///////////////////////////////

	public int getPresentImageFormat() {
		return chosenImageFormat;
	}

	public int findSupportedFormat(int imageTiling,int features,int... all_formats) {
		try(MemoryStack stack = stackPush()) {
			VkFormatProperties props = VkFormatProperties.mallocStack(stack);
			for (int format : all_formats) {
				vkGetPhysicalDeviceFormatProperties(VulkanDevice.physicalDevice, format, props);
				if(imageTiling == VK_IMAGE_TILING_LINEAR && (props.linearTilingFeatures() & features) == features) {
					return format;
				}else if(imageTiling == VK_IMAGE_TILING_OPTIMAL && (props.optimalTilingFeatures() & features) == features) {
					return format;
				}
			}
			CrashReport crashReport = new CrashReport("Failed to find valid Vulkan Image Format");
			for (int format : all_formats) {
				crashReport.invalidState("Format: #" + Integer.toHexString(format) + " = Failure");
			}
			OpenVoxel.reportCrash(crashReport);
			return 0;
		}
	}

	/////////////////////////////////////
	/// Management of Other Resources ///
	/////////////////////////////////////

	private long createWindow() {
		glfwDefaultWindowHints();
		glfwWindowHint(GLFW_CLIENT_API,GLFW_NO_API);
		long window = glfwCreateWindow(ClientInput.currentWindowWidth.get(), ClientInput.currentWindowHeight.get(), "Open Voxel " + OpenVoxel.currentVersion.getValString(), 0, 0);
		try(MemoryStack stack = stackPush()) {
			IntBuffer windowWidth = stack.mallocInt(1);
			IntBuffer windowHeight = stack.mallocInt(1);
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
				if (layerList.layerNameString().equals("VK_LAYER_LUNARG_standard_validation")) {
					enabledLayers.add(layerList.layerName());
					VulkanUtility.LogInfo("Enabled Layer: Standard Validation");
					continue;
				}
			}
			if(flag_vulkanDetailLog) {
				if (layerList.layerNameString().equals("VK_LAYER_LUNARG_assistant_layer")) {
					enabledLayers.add(layerList.layerName());
					VulkanUtility.LogInfo("Enabled Layer: Assistant Layer");
					continue;
				}
			}
			if(flag_vulkanDumpAPI) {
				if(layerList.layerNameString().equals("VK_LAYER_LUNARG_api_dump")) {
					enabledLayers.add(layerList.layerName());
					VulkanUtility.LogInfo("Enabled Layer: API DUMP...");
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

	//////////////////////
	/// Vulkan Surface ///
	//////////////////////

	private long createSurface() {
		try(MemoryStack stack = stackPush()) {
			LongBuffer tmpSurface = stack.mallocLong(1);
			int res = glfwCreateWindowSurface(VulkanInstance,GLFWWindow,null,tmpSurface);
			VulkanUtility.ValidateSuccess("Error creating window surface",res);
			return tmpSurface.get(0);
		}
	}

	private void destroySurface() {
		vkDestroySurfaceKHR(VulkanInstance,VulkanSurface,null);
	}

	////////////////////////
	/// Vulkan SwapChain ///
	////////////////////////

	private void createSwapChain(boolean isRecreated) {
		try(MemoryStack stack = stackPush()) {
			if(!isRecreated) {
				//Init Memory
				chosenSwapExtent = VkExtent2D.malloc();
				surfaceCapabilities = VkSurfaceCapabilitiesKHR.malloc();
				IntBuffer sizeRef = stack.mallocInt(1);

				//Load Information
				vkGetPhysicalDeviceSurfaceCapabilitiesKHR(VulkanDevice.physicalDevice,VulkanSurface,surfaceCapabilities);
				vkGetPhysicalDeviceSurfaceFormatsKHR(VulkanDevice.physicalDevice,VulkanSurface,sizeRef,null);
				validSurfaceFormats = VkSurfaceFormatKHR.malloc(sizeRef.get(0));
				vkGetPhysicalDeviceSurfaceFormatsKHR(VulkanDevice.physicalDevice,VulkanSurface,sizeRef,validSurfaceFormats);
				vkGetPhysicalDeviceSurfacePresentModesKHR(VulkanDevice.physicalDevice,VulkanSurface,sizeRef,null);
				IntBuffer presentModeBuffer = stack.mallocInt(sizeRef.get(0));
				vkGetPhysicalDeviceSurfacePresentModesKHR(VulkanDevice.physicalDevice,VulkanSurface,sizeRef,presentModeBuffer);
				validPresentModes.clear();
				for(int i = 0; i < sizeRef.get(0); i++) {
					validPresentModes.add(presentModeBuffer.get(i));
				}

				//Choose Defaults
				VulkanUtility.chooseSwapExtent(surfaceCapabilities,chosenSwapExtent);
				chosenPresentMode = VulkanUtility.chooseDefaultPresentMode(validPresentModes);
				chosenImageFormat = VulkanUtility.chooseSurfaceFormat(validSurfaceFormats,true);
				chosenColourSpace = VulkanUtility.chooseSurfaceFormat(validSurfaceFormats,false);
				chosenImageCount  = VulkanUtility.chooseImageCount(surfaceCapabilities);
			}else{
				//Destroy Old Swap Chain Image Views
				{
					MemoryUtil.memFree(VulkanSwapChainImages);
					VulkanSwapChainImages = null;
				}
				for(int i = 0; i < VulkanSwapChainSize; i++) {
					vkDestroyImageView(VulkanDevice.logicalDevice,VulkanSwapChainImageViews.get(i),null);
				}
				{
					MemoryUtil.memFree(VulkanSwapChainImageViews);
					VulkanSwapChainImageViews = null;
				}
			}

			//Update Surface Capabilities
			vkGetPhysicalDeviceSurfaceCapabilitiesKHR(VulkanDevice.physicalDevice,VulkanSurface,surfaceCapabilities);
			VulkanUtility.chooseSwapExtent(surfaceCapabilities,chosenSwapExtent);

			//Skip if invalid
			if(ClientInput.currentWindowHeight.get() == 0 || ClientInput.currentWindowWidth.get() == 0) {
				return;
			}

			//Create Swap Chain
			VkSwapchainCreateInfoKHR swapCreateInfo = VkSwapchainCreateInfoKHR.mallocStack(stack);
			swapCreateInfo.sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR);
			swapCreateInfo.pNext(VK_NULL_HANDLE);
			swapCreateInfo.flags(0);
			swapCreateInfo.surface(VulkanSurface);
			swapCreateInfo.minImageCount(chosenImageCount);
			swapCreateInfo.imageFormat(chosenImageFormat);
			swapCreateInfo.imageColorSpace(chosenColourSpace);
			swapCreateInfo.imageExtent(chosenSwapExtent);
			swapCreateInfo.imageArrayLayers(1);
			swapCreateInfo.imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT);
			swapCreateInfo.imageSharingMode(VK_SHARING_MODE_EXCLUSIVE);
			swapCreateInfo.pQueueFamilyIndices(null);
			swapCreateInfo.preTransform(surfaceCapabilities.currentTransform());
			swapCreateInfo.compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR);
			swapCreateInfo.presentMode(chosenPresentMode);
			swapCreateInfo.clipped(true);
			swapCreateInfo.oldSwapchain(isRecreated ? VulkanSwapChain : 0L);

			LongBuffer pLongValue = stack.mallocLong(1);
			int vkResult = vkCreateSwapchainKHR(VulkanDevice.logicalDevice,swapCreateInfo,null,pLongValue);
			if(vkResult == VK_SUCCESS) {
				VulkanSwapChain = pLongValue.get(0);
			}else if(vkResult == VK_ERROR_OUT_OF_HOST_MEMORY || vkResult == VK_ERROR_OUT_OF_DEVICE_MEMORY) {
				VulkanUtility.LogWarn("Failed to create swap-chain: Out of Memory");
				//TODO: RETRY AFTER FREE MEMORY
				VulkanUtility.CrashOnBadResult("Failed to create swap-chain",vkResult);
				return;
			}else{
				VulkanUtility.CrashOnBadResult("Failed to create swap-chain",vkResult);
				return;
			}

			//Get Swap Chain Images
			IntBuffer returnVal = stack.mallocInt(1);
			vkResult = vkGetSwapchainImagesKHR(VulkanDevice.logicalDevice,VulkanSwapChain,returnVal,null);
			if(vkResult != VK_SUCCESS) {
				VulkanUtility.CrashOnBadResult("Failed to get swap-chain images",vkResult);
				return;
			}
			VulkanSwapChainSize = returnVal.get(0);
			VulkanSwapChainImages = MemoryUtil.memAllocLong(VulkanSwapChainSize);
			vkResult = vkGetSwapchainImagesKHR(VulkanDevice.logicalDevice,VulkanSwapChain,returnVal,VulkanSwapChainImages);
			if(vkResult == VK_INCOMPLETE) {
				VulkanUtility.LogWarn("Loading incomplete selection of swap-chain images!");
			}else if(vkResult != VK_SUCCESS) {
				VulkanUtility.LogWarn("Failed to get swap-chain images: Out of Memory");
				//TODO: RETRY AFTER FREE MEMORY
				VulkanUtility.CrashOnBadResult("Failed to get swap-chain images",vkResult);
				return;
			}

			//Temp Data Structures
			VkComponentMapping components = VkComponentMapping.mallocStack(stack);
			components.set(
					VK_COMPONENT_SWIZZLE_IDENTITY,
					VK_COMPONENT_SWIZZLE_IDENTITY,
					VK_COMPONENT_SWIZZLE_IDENTITY,
					VK_COMPONENT_SWIZZLE_IDENTITY
			);
			VkImageSubresourceRange subResource = VkImageSubresourceRange.mallocStack(stack);
			subResource.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
			subResource.baseMipLevel(0);
			subResource.levelCount(1);
			subResource.baseArrayLayer(0);
			subResource.layerCount(1);

			//Create Swap Chain Image Views
			VulkanSwapChainImageViews = MemoryUtil.memCallocLong(VulkanSwapChainSize);
			VkImageViewCreateInfo createImageView = VkImageViewCreateInfo.mallocStack(stack);
			createImageView.sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO);
			createImageView.pNext(VK_NULL_HANDLE);
			createImageView.flags(0);
			createImageView.viewType(VK_IMAGE_VIEW_TYPE_2D);
			createImageView.format(chosenImageFormat);
			createImageView.components(components);
			createImageView.subresourceRange(subResource);
			for(int i = 0; i < VulkanSwapChainSize; i++) {
				createImageView.image(VulkanSwapChainImages.get(i));
				vkResult = vkCreateImageView(VulkanDevice.logicalDevice,createImageView,null,pLongValue);
				if(vkResult != VK_SUCCESS) {
					VulkanUtility.LogWarn("Failed to create swap-chain image view: Out of Memory");
					//TODO: RETRY AFTER FREE MEMORY
					VulkanUtility.CrashOnBadResult("Failed to get swap-chain images",vkResult);
				}
				VulkanSwapChainImageViews.put(i,pLongValue.get(0));
			}
		}
	}

	private void destroySwapChain() {
		if(VulkanSwapChainImages != null) {
			MemoryUtil.memFree(VulkanSwapChainImages);
			VulkanSwapChainImages = null;
		}else{
			VulkanUtility.LogWarn("Unexpected: Swap-chain image list == null");
		}
		if(VulkanSwapChainImageViews != null) {
			for (int i = 0; i < VulkanSwapChainSize; i++) {
				if (VulkanSwapChainImageViews.get(i) != VK_NULL_HANDLE) {
					vkDestroyImageView(VulkanDevice.logicalDevice, VulkanSwapChainImageViews.get(i), null);
				} else {
					VulkanUtility.LogWarn("Unexpected: Swap-chain image view == VK_NULL_HANDLE");
				}
			}
			MemoryUtil.memFree(VulkanSwapChainImageViews);
			VulkanSwapChainImageViews = null;
		}else{
			VulkanUtility.LogWarn("Unexpected: Swap-chain image view list == null");
		}
		vkDestroySwapchainKHR(VulkanDevice.logicalDevice,VulkanSwapChain,null);

		//Free Memory
		validPresentModes.clear();
		validSurfaceFormats.free();
		surfaceCapabilities.free();
		chosenSwapExtent.free();
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