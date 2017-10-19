package net.openvoxel.client.renderer.vk.util;

import net.openvoxel.OpenVoxel;
import net.openvoxel.api.logger.Logger;
import net.openvoxel.api.util.Version;
import net.openvoxel.client.ClientInput;
import net.openvoxel.utility.CrashReport;
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
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.EXTDebugReport.vkDestroyDebugReportCallbackEXT;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

/**
 * Created by James on 03/09/2016.
 *
 * Vulkan Global State Information : Stores all required information for drawing
 */
public class VkDeviceState extends VkRenderManager {

	public VkInstance instance;
	public long glfw_window;
	private long window_surface;
	private long window_swapchain;

	private long debug_report_callback_ext;

	public Logger vkLogger;

	static boolean vulkanDetailLog = OpenVoxel.getLaunchParameters().hasFlag("vkDebugDetailed");
	static boolean vulkanDebug = OpenVoxel.getLaunchParameters().hasFlag("vkDebug") || vulkanDetailLog;
	static boolean vulkanRenderDoc = OpenVoxel.getLaunchParameters().hasFlag("vkRenderDoc");
	private static boolean vulkanDetailedDeviceInfo = OpenVoxel.getLaunchParameters().hasFlag("vkDeviceInfo");

	/*Surface and SwapChain Information*/
	private VkSurfaceCapabilitiesKHR surfaceCapabilities;
	public VkSurfaceFormatKHR.Buffer surfaceFormats;
	public IntBuffer presentModes;


	public VkDeviceState() {
		vkLogger = Logger.getLogger("Vulkan");
		swapExtent = VkExtent2D.calloc();
		surfaceCapabilities = VkSurfaceCapabilitiesKHR.calloc();
		create_window();
		initInstance();
		choosePhysicalDevice();
		renderDevice.createDevice();
		memoryMgr = new VkMemoryManager(this);
		initSynchronisation();
		initSurface();
		initSwapChain();
		initRenderPasses();
		initGraphicsPipeline();
		initFrameBuffers();
		initCommandBuffers();
		initMemory();
	}


	public void acquireNextImage(boolean rebootSync) {
		try(MemoryStack stack = stackPush()) {
			IntBuffer index = stack.callocInt(1);
			int result = vkAcquireNextImageKHR(renderDevice.device,window_swapchain,-1L,semaphore_image_available,VK_NULL_HANDLE,index);
			if(result == VK_ERROR_OUT_OF_DATE_KHR) {
				recreateSwapChain();
			}else if(result != VK_SUCCESS && result != VK_SUBOPTIMAL_KHR) {
				System.out.println(Integer.toString(result));
				throw new RuntimeException("Failed to get next image");
			}
			swapChainImageIndex =  index.get(0);
		}
	}

	public void submitNewWork() {
		try(MemoryStack stack = stackPush()) {
			VkSubmitInfo submitInfo = VkSubmitInfo.mallocStack(stack);
			submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);
			submitInfo.pNext(VK_NULL_HANDLE);
			submitInfo.pWaitSemaphores(null);
			submitInfo.waitSemaphoreCount(0);
			submitInfo.pWaitDstStageMask(stack.ints(VK_PIPELINE_STAGE_TRANSFER_BIT));
			submitInfo.pSignalSemaphores(stack.longs(semaphore_gui_data_updated));
			submitInfo.pCommandBuffers(stack.pointers(command_buffers_gui_transfer.get(swapChainImageIndex)));
			if(vkQueueSubmit(renderDevice.asyncTransferQueue,submitInfo,0) != VK_SUCCESS) {
				throw new RuntimeException("Failed to execute transfer queue");
			}

			VkCommandBuffer mainBuffer = new VkCommandBuffer(command_buffers_main.get(swapChainImageIndex),renderDevice.device);
			vkResetCommandBuffer(mainBuffer,VK_COMMAND_BUFFER_RESET_RELEASE_RESOURCES_BIT);
			VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.mallocStack(stack);
			beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
			beginInfo.pNext(VK_NULL_HANDLE);
			beginInfo.flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
			beginInfo.pInheritanceInfo(null);
			vkBeginCommandBuffer(mainBuffer,beginInfo);

			VkRenderPassBeginInfo renderPassInfo = VkRenderPassBeginInfo.callocStack(stack);
			renderPassInfo.sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO);
			renderPassInfo.pNext(VK_NULL_HANDLE);
			renderPassInfo.renderPass(renderPass.render_pass);
			renderPassInfo.framebuffer(targetFrameBuffers.get(swapChainImageIndex));
			VkRect2D screenRect = VkRect2D.callocStack(stack);
			screenRect.extent(swapExtent);
			renderPassInfo.renderArea(screenRect);
			VkClearValue.Buffer clearValues = VkClearValue.callocStack(1,stack);
			VkClearColorValue clearColorValue = VkClearColorValue.callocStack(stack);
			clearColorValue.float32(0,0.3f);
			clearColorValue.float32(1,0.0f);
			clearColorValue.float32(2,0.2f);
			clearColorValue.float32(3,1.0f);
			clearValues.color(clearColorValue);
			renderPassInfo.pClearValues(clearValues);
			vkCmdBeginRenderPass(mainBuffer,renderPassInfo,VK_SUBPASS_CONTENTS_SECONDARY_COMMAND_BUFFERS);

			vkCmdExecuteCommands(mainBuffer,stack.pointers(command_buffers_gui.get(swapChainImageIndex)));

			vkCmdEndRenderPass(mainBuffer);

			if(vkEndCommandBuffer(mainBuffer) != VK_SUCCESS) {
				throw new RuntimeException("Failed to generate main command buffer");
			}

			LongBuffer semaphores = stack.longs(semaphore_image_available,semaphore_gui_data_updated);
			LongBuffer signalSemaphores = stack.longs(semaphore_render_finished);
			PointerBuffer cmdBuffers = stack.pointers(command_buffers_main.get(swapChainImageIndex));
			IntBuffer waitStages = stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT,VK_PIPELINE_STAGE_VERTEX_INPUT_BIT);
			submitInfo.pWaitSemaphores(semaphores);
			submitInfo.waitSemaphoreCount(2);
			submitInfo.pWaitDstStageMask(waitStages);
			submitInfo.pCommandBuffers(cmdBuffers);
			submitInfo.pSignalSemaphores(signalSemaphores);
			if(vkQueueSubmit(renderDevice.renderQueue,submitInfo,VK_NULL_HANDLE) != VK_SUCCESS) {
				throw new RuntimeException("Failed to submit queue info");
			}
		}
	}

	public void presentOnCompletion() {
		try(MemoryStack stack = stackPush()) {
			LongBuffer swapChains = stack.longs(window_swapchain);
			LongBuffer semaphores = stack.longs(semaphore_render_finished);
			IntBuffer imageIndices = stack.ints(swapChainImageIndex);
			VkPresentInfoKHR presentInfo = VkPresentInfoKHR.callocStack(stack);
			presentInfo.sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR);
			presentInfo.pNext(VK_NULL_HANDLE);
			presentInfo.swapchainCount(1);
			presentInfo.pSwapchains(swapChains);
			presentInfo.pWaitSemaphores(semaphores);
			presentInfo.pImageIndices(imageIndices);
			presentInfo.pResults(null);
			int res = vkQueuePresentKHR(renderDevice.renderQueue,presentInfo);
			if(res != VK_SUCCESS) {
				if(res != VK_ERROR_OUT_OF_DATE_KHR && res != VK_SUBOPTIMAL_KHR) {
					throw new RuntimeException("Failed to present queue");
				}else{
					recreateSwapChain();
				}
			}
			vkQueueWaitIdle(renderDevice.renderQueue);
		}
	}


	private void create_window() {
		glfwDefaultWindowHints();
		glfwWindowHint(GLFW_CLIENT_API,GLFW_NO_API);
		glfw_window = glfwCreateWindow(ClientInput.currentWindowWidth.get(), ClientInput.currentWindowHeight.get(), "Open Voxel " + OpenVoxel.currentVersion.getValString(), 0, 0);
		try(MemoryStack stack = stackPush()) {
			IntBuffer width = stack.mallocInt(1);
			IntBuffer height = stack.mallocInt(1);
			glfwGetWindowSize(glfw_window,width,height);
			ClientInput.currentWindowHeight.set(height.get(0));
			ClientInput.currentWindowWidth.set(width.get(0));
		}
	}

	void success(int result,String err) {
		if(result != VK_SUCCESS) {
			vkLogger.Severe(err + " : " + result);
			throw new RuntimeException(err);
		}
	}


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


	private void initInstance() {
		try(MemoryStack stack = stackPush()) {
			boolean enabled_debug_report_extension = false;
			PointerBuffer pointer = stack.callocPointer(1);
			ByteBuffer appNameBuf = stack.UTF8("Open Voxel");
			VkApplicationInfo appInfo = VkApplicationInfo.mallocStack(stack);
			VkInstanceCreateInfo createInfo = VkInstanceCreateInfo.mallocStack(stack);
			initApplicationInfo(appInfo,appNameBuf);
			createInfo.sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO);
			createInfo.pNext(VK_NULL_HANDLE);
			createInfo.flags(0);
			createInfo.pApplicationInfo(appInfo);
			//Choose Enabled Layers//
			IntBuffer sizeRef = stack.callocInt(1);
			success(vkEnumerateInstanceLayerProperties(sizeRef,null),"Error Enumerating Instance Layers");
			VkLayerProperties.Buffer layerPropertyList = VkLayerProperties.mallocStack(sizeRef.get(0),stack);
			success(vkEnumerateInstanceLayerProperties(sizeRef,layerPropertyList),"Error Enumerating Instance Layers");
			List<ByteBuffer> enabledLayers = new ArrayList<>();
			for(int i = 0; i < sizeRef.get(0); i++) {
				layerPropertyList.position(i);
				if(vulkanDebug) {
					vkLogger.Debug("Instance Layer: " + layerPropertyList.layerNameString());
				}
				if(vulkanDebug) {
					if(layerPropertyList.layerNameString().equals("VK_LAYER_LUNARG_standard_validation")) {
						enabledLayers.add(layerPropertyList.layerName());
						vkLogger.Info("Enabled Standard Validation Layer");
						continue;
					}
				}
				if(vulkanRenderDoc) {
					if(layerPropertyList.layerNameString().equals("VK_LAYER_RENDERDOC_Capture")) {
						enabledLayers.add(layerPropertyList.layerName());
						vkLogger.Info("Enabled RenderDoc Capture Layer");
						continue;
					}
				}
				if(OpenVoxel.getLaunchParameters().hasFlag("-VKLayer:"+layerPropertyList.layerNameString())) {
					enabledLayers.add(layerPropertyList.layerName());
					vkLogger.Info("Enabled Custom Layer: " + layerPropertyList.layerNameString());
				}
			}
			PointerBuffer enabledLayerBuffer = stack.callocPointer(enabledLayers.size());
			for(ByteBuffer buffer : enabledLayers){
				enabledLayerBuffer.put(buffer);
			}
			enabledLayerBuffer.position(0);
			createInfo.ppEnabledLayerNames(enabledLayerBuffer);
			//Choose Enabled Extensions//
			success(vkEnumerateInstanceExtensionProperties((ByteBuffer)null,sizeRef,null),"Error Enumerating Instance Extensions");
			VkExtensionProperties.Buffer extensionPropertyList = VkExtensionProperties.callocStack(sizeRef.get(0),stack);
			success(vkEnumerateInstanceExtensionProperties((ByteBuffer)null,sizeRef,extensionPropertyList),"Error Enumerating Instance Extensions");
			List<ByteBuffer> enabledExtensions = new ArrayList<>();
			PointerBuffer requiredExtensions = glfwGetRequiredInstanceExtensions();
			for(int i = 0; i < requiredExtensions.capacity(); i++) {
				enabledExtensions.add(requiredExtensions.getByteBuffer(i));
				if(vulkanDebug) {
					vkLogger.Debug("Extension[GLFW Required]: " + MemoryUtil.memUTF8(requiredExtensions.get(i)));
				}
			}
			vkLogger.Info("Added GLFW Extensions");
			for(int i = 0; i < sizeRef.get(0); i++) {
				extensionPropertyList.position(i);
				if(vulkanDebug) {
					vkLogger.Debug("Instance Extension: " + extensionPropertyList.extensionNameString());
				}
				if(vulkanDebug) {
					if(extensionPropertyList.extensionNameString().equals("VK_EXT_debug_report")) {
						enabledExtensions.add(extensionPropertyList.extensionName());
						vkLogger.Info("Enabled Debug Report");
						enabled_debug_report_extension = true;
					}
				}
			}
			PointerBuffer enabledExtensionBuffer = stack.callocPointer(enabledExtensions.size());
			for(ByteBuffer buffer : enabledExtensions) {
				enabledExtensionBuffer.put(buffer);
			}
			enabledExtensionBuffer.position(0);
			createInfo.ppEnabledExtensionNames(enabledExtensionBuffer);
			//Create Instance//
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
			int vk_version = instance.getCapabilities().apiVersion;
			vkLogger.Info("Instance Version: " +
					              VK_VERSION_MAJOR(vk_version) +
					              "." + VK_VERSION_MINOR(vk_version) +
					              "." + VK_VERSION_PATCH(vk_version));
			if(enabled_debug_report_extension) {
				vkLogger.Info("Creating Debug Report Callback");
				debug_report_callback_ext = VkLogUtil.Init(instance,vulkanDetailLog);
			}else{
				debug_report_callback_ext = VK_NULL_HANDLE;
			}
		}catch (RuntimeException ex) {
			CrashReport report = new CrashReport("Error Initializing Vulkan").
										 caughtException(ex);
			OpenVoxel.reportCrash(report);
		}
	}

	private void initSurface() {
		try(MemoryStack stack = stackPush()) {
			LongBuffer target = stack.callocLong(1);
			success(glfwCreateWindowSurface(instance, glfw_window, null, target),"Error Creating Window Surface");
			window_surface = target.get(0);
			IntBuffer supported = stack.callocInt(1);
			success(vkGetPhysicalDeviceSurfaceSupportKHR(renderDevice.physicalDevice,renderDevice.queueFamilyIndexRender,window_surface,supported),"Error Checking Surface Support");
			if(supported.get(0) != VK_TRUE) {
				throw new RuntimeException("Error Creating Surface : Not Supported in Queue");
			}
		}
	}

	private void initSwapChain() {
		try(MemoryStack stack = stackPush()) {
			vkGetPhysicalDeviceSurfaceCapabilitiesKHR(renderDevice.physicalDevice,window_surface,surfaceCapabilities);
			IntBuffer sizeRef = stack.callocInt(1);
			vkGetPhysicalDeviceSurfaceFormatsKHR(renderDevice.physicalDevice,window_surface,sizeRef,null);
			surfaceFormats = VkSurfaceFormatKHR.calloc(sizeRef.get(0));
			vkGetPhysicalDeviceSurfaceFormatsKHR(renderDevice.physicalDevice,window_surface,sizeRef,surfaceFormats);
			vkGetPhysicalDeviceSurfacePresentModesKHR(renderDevice.physicalDevice,window_surface,sizeRef,null);
			presentModes = MemoryUtil.memAllocInt(sizeRef.get(0));
			vkGetPhysicalDeviceSurfacePresentModesKHR(renderDevice.physicalDevice,window_surface,sizeRef, presentModes);
			//Choose Initial SwapChain Info//
			//Choose Swapchain Format//
			if(surfaceFormats.capacity() == 1 && surfaceFormats.get(0).format() == VK_FORMAT_UNDEFINED) {
				//All are Valid//
				chosenImageFormat = VK_FORMAT_B8G8R8A8_UNORM;
				chosenColourSpace = VK_COLOR_SPACE_SRGB_NONLINEAR_KHR;
				vkLogger.Debug("Universal Format Validation");
				vkLogger.Info("Image Format: B8G8R8A8_UNORM");
				vkLogger.Info("Colour Space: SRGB_NONLINEAR");
			}else{
				boolean found = false;
				for(int i = 0; i < surfaceFormats.capacity(); i++) {
					surfaceFormats.position(i);
					if(surfaceFormats.format() == VK_FORMAT_B8G8R8A8_UNORM && surfaceFormats.colorSpace() == VK_COLOR_SPACE_SRGB_NONLINEAR_KHR) {
						chosenImageFormat = VK_FORMAT_B8G8R8A8_UNORM;
						chosenColourSpace = VK_COLOR_SPACE_SRGB_NONLINEAR_KHR;
						vkLogger.Info("Image Format: B8G8R8A8_UNORM");
						vkLogger.Info("Colour Space: SRGB_NONLINEAR");
						found = true;
					}
				}
				if(!found) {
					surfaceFormats.position(0);
					chosenImageFormat = surfaceFormats.format();
					chosenColourSpace = surfaceFormats.colorSpace();
					vkLogger.Info("Fallback Image Format: " + chosenImageFormat);
					vkLogger.Info("Fallback Colour Space: " + chosenColourSpace);
				}
			}
			//Choose Present Mode//
			chosenPresentMode = -1;
			{
				vkLogger.Info("Valid Present Modes:");
				for (int i = 0; i < presentModes.capacity(); i++) {
					String res;
					switch (presentModes.get(i)) {
						case VK_PRESENT_MODE_IMMEDIATE_KHR:
							res = "Immediate";
							break;
						case VK_PRESENT_MODE_FIFO_KHR:
							res = "FIFO";
							break;
						case VK_PRESENT_MODE_FIFO_RELAXED_KHR:
							res = "FIFO Relaxed";
							break;
						case VK_PRESENT_MODE_MAILBOX_KHR:
							res = "Mailbox";
							break;
						default:
							res = "Unknown:{" + presentModes.get(i) + "}";
							break;
					}
					vkLogger.Info(" - " + res);
				}
			}
			boolean fallback_immediate = false;
			for(int i = 0; i < presentModes.capacity(); i++) {
				if(presentModes.get(i) == VK_PRESENT_MODE_MAILBOX_KHR) {
					vkLogger.Info("Chosen Present Mode: Mailbox");
					chosenPresentMode = VK_PRESENT_MODE_MAILBOX_KHR;
				}
				if(presentModes.get(i) == VK_PRESENT_MODE_IMMEDIATE_KHR) {
					fallback_immediate = true;
				}
			}
			if(chosenPresentMode == -1) {
				String os_type = System.getProperty("os.name").toLowerCase();
				if(!os_type.contains("win")  && !os_type.contains("mac") && fallback_immediate) {
					chosenPresentMode = VK_PRESENT_MODE_IMMEDIATE_KHR;
					vkLogger.Info("Chosen Present Mode: Immediate(Linux Fix)");
				}else {
					chosenPresentMode = VK_PRESENT_MODE_FIFO_KHR;
					vkLogger.Info("Chosen Present Mode: FIFO");
				}
			}
			//Choose Swap Extent//
			if(surfaceCapabilities.currentExtent().width() != 0xFFFFFFFF) {
				swapExtent.set(surfaceCapabilities.currentExtent());
			}else{
				int width = ClientInput.currentWindowWidth.get();
				int height = ClientInput.currentWindowHeight.get();
				width = Math.min(Math.max(width,surfaceCapabilities.minImageExtent().width()),surfaceCapabilities.maxImageExtent().width());
				height = Math.min(Math.max(height,surfaceCapabilities.minImageExtent().height()),surfaceCapabilities.maxImageExtent().height());
				swapExtent.set(width,height);
			}
			//Choose Image Count//
			chosenImageCount = surfaceCapabilities.minImageCount() + (chosenPresentMode == VK_PRESENT_MODE_MAILBOX_KHR ? 1 : 0);
			if (surfaceCapabilities.maxImageCount() > 0 && chosenImageCount > surfaceCapabilities.maxImageCount()) {
				chosenImageCount = surfaceCapabilities.maxImageCount();
			}
			vkLogger.Info("SwapChain Image Range " + surfaceCapabilities.minImageCount() + "<x<" + surfaceCapabilities.maxImageCount());
			vkLogger.Info("Chosen SwapChain Image Count : " + chosenImageCount);
			//Create The SwapChain//
			LongBuffer swapChainBuf = stack.callocLong(1);
			VkSwapchainCreateInfoKHR createInfoKHR = VkSwapchainCreateInfoKHR.callocStack(stack);
			createInfoKHR.sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR);
			createInfoKHR.pNext(VK_NULL_HANDLE);
			createInfoKHR.flags(0);
			createInfoKHR.surface(window_surface);
			createInfoKHR.minImageCount(chosenImageCount);
			createInfoKHR.imageFormat(chosenImageFormat);
			createInfoKHR.imageColorSpace(chosenColourSpace);
			createInfoKHR.imageExtent(swapExtent);
			createInfoKHR.imageArrayLayers(1);
			createInfoKHR.imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT);
			createInfoKHR.imageSharingMode(VK_SHARING_MODE_EXCLUSIVE);
			createInfoKHR.pQueueFamilyIndices(null);
			createInfoKHR.preTransform(surfaceCapabilities.currentTransform());
			createInfoKHR.compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR);
			createInfoKHR.presentMode(chosenPresentMode);
			createInfoKHR.clipped(true);
			createInfoKHR.oldSwapchain(0);
			int create = vkCreateSwapchainKHR(renderDevice.device,createInfoKHR,null,swapChainBuf);
			if(create < 0) {
				throw new RuntimeException("Error Creating SwapChain");
			}else if(create == VK_SUBOPTIMAL_KHR) {
				vkLogger.Warning("SwapChain Created SubOptimally");
			}
			window_swapchain = swapChainBuf.get(0);
			//Retrieve Images//
			success(vkGetSwapchainImagesKHR(renderDevice.device,window_swapchain,sizeRef,null),"Error Loading SwapChain Images");
			swapChainImages = MemoryUtil.memAllocLong(sizeRef.get(0));
			swapChainImageViews = MemoryUtil.memAllocLong(sizeRef.get(0));
			success(vkGetSwapchainImagesKHR(renderDevice.device,window_swapchain,sizeRef,swapChainImages),"Error Loading SwapChain Images");
			//Create Image Views//
			VkImageViewCreateInfo imageViewCreateInfo = VkImageViewCreateInfo.callocStack(stack);
			VkComponentMapping componentMapping = VkComponentMapping.callocStack(stack);
			componentMapping.set(VK_COMPONENT_SWIZZLE_IDENTITY,VK_COMPONENT_SWIZZLE_IDENTITY,VK_COMPONENT_SWIZZLE_IDENTITY,VK_COMPONENT_SWIZZLE_IDENTITY);
			VkImageSubresourceRange subResourceRange = VkImageSubresourceRange.callocStack(stack);
			subResourceRange.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
			subResourceRange.baseMipLevel(0);
			subResourceRange.levelCount(1);
			subResourceRange.baseArrayLayer(0);
			subResourceRange.layerCount(1);
			LongBuffer imageViewResult = stack.callocLong(1);
			for(int i = 0; i < sizeRef.get(0); i++) {
				imageViewCreateInfo.sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO);
				imageViewCreateInfo.pNext(VK_NULL_HANDLE);
				imageViewCreateInfo.flags(0);
				imageViewCreateInfo.image(swapChainImages.get(i));
				imageViewCreateInfo.viewType(VK_IMAGE_VIEW_TYPE_2D);
				imageViewCreateInfo.format(chosenImageFormat);
				imageViewCreateInfo.components(componentMapping);
				imageViewCreateInfo.subresourceRange(subResourceRange);
				success(vkCreateImageView(renderDevice.device, imageViewCreateInfo, null,imageViewResult),"Error Creating SwapChain Image View");
				swapChainImageViews.put(i,imageViewResult.get(0));
			}
			swapChainImageIndex = 0;
		}
	}

	private void destroySwapChain() {
		destroyFrameBuffers();
		destroyPipelineAndLayout();
		destroyRenderPasses();
		destroyCommandBuffers();
		for(int i = 0; i < swapChainImageViews.capacity();i++) {
			vkDestroyImageView(renderDevice.device,swapChainImageViews.get(i),null);
		}
		MemoryUtil.memFree(swapChainImages);
		MemoryUtil.memFree(swapChainImageViews);
		MemoryUtil.memFree(presentModes);
		surfaceFormats.free();
		vkDestroySwapchainKHR(renderDevice.device,window_swapchain,null);
		window_swapchain = VK_NULL_HANDLE;
	}

	public void recreateSwapChain() {
		vkDeviceWaitIdle(renderDevice.device);
		destroySwapChain();
		destroyCommandPools();
		initSwapChain();
		initRenderPasses();
		initGraphicsPipeline();
		initFrameBuffers();
		initCommandBuffers();
		recreateMemory();
	}

	private void choosePhysicalDevice() {
		try(MemoryStack stack = stackPush()) {
			IntBuffer deviceCount = stack.mallocInt(1);
			success(vkEnumeratePhysicalDevices(instance,deviceCount,null),"Error Enumerating Physical Devices");
			PointerBuffer devices = stack.callocPointer(deviceCount.get(0));
			success(vkEnumeratePhysicalDevices(instance,deviceCount,devices),"Error Enumerating Physical Devices");
			int validDeviceCount = 0;
			List<VkRenderDevice> deviceList = new ArrayList<>();
			for(int i = 0; i < deviceCount.get(0); i++) {
				deviceList.add(new VkRenderDevice(this,devices.get(i)));
			}
			List<VkRenderDevice> invalidDevices = new ArrayList<>();
			for(VkRenderDevice device : deviceList) {
				if(device.isValidDevice()) {
					validDeviceCount++;
				}else{
					invalidDevices.add(device);
				}
			}
			deviceList.removeAll(invalidDevices);
			if(validDeviceCount == 0) {
				throw new RuntimeException("No Valid Vulkan Device");
			}
			//FIND BEST DEVICE//
			VkRenderDevice bestDevice = deviceList.get(0);
			int bestScore = Integer.MIN_VALUE;
			for(VkRenderDevice device : deviceList) {
				int newScore = device.rateDevice();
				if(newScore > bestScore) {
					bestScore = newScore;
					bestDevice = device;
				}
			}
			deviceList.remove(bestDevice);
			deviceList.forEach(VkRenderDevice::freeInitial);
			vkLogger.Info("Chosen Device = " + bestDevice.getDeviceInfo());
			if(vulkanDetailedDeviceInfo) {
				bestDevice.printDetailedDeviceInfo();
			}
			this.renderDevice = bestDevice;
		}
	}


	public void terminateAndFree() {
		vkDeviceWaitIdle(renderDevice.device);
		destroySwapChain();
		destroyCommandPools();
		destroySynchronisation();
		destroyMemory();
		vkDestroySurfaceKHR(instance,window_surface,null);
		memoryMgr.cleanup();
		renderDevice.freeDevice();
		if(debug_report_callback_ext != VK_NULL_HANDLE) {
			vkLogger.Info("Disabling Debug Report");
			vkDestroyDebugReportCallbackEXT(instance,debug_report_callback_ext,null);
			VkLogUtil.Cleanup();
		}
		vkLogger.Info("Destroying Instance");
		vkDestroyInstance(instance,null);
		glfwDestroyWindow(glfw_window);
		glfwTerminate();
		vkLogger.Info("Cleaning Up Memory");
		surfaceCapabilities.free();
		swapExtent.free();
	}

}
