package net.openvoxel.client.renderer.vk.util;

import net.openvoxel.OpenVoxel;
import net.openvoxel.api.logger.Logger;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

/**
 * Created by James on 17/04/2017.
 *
 * Wrapper around a physical device and a device
 */
public class VkRenderDevice{

	private VkDeviceState state;
	VkPhysicalDevice physicalDevice;
	private VkPhysicalDeviceFeatures deviceFeatures = VkPhysicalDeviceFeatures.calloc();
	private VkPhysicalDeviceMemoryProperties memoryProperties = VkPhysicalDeviceMemoryProperties.calloc();
	private VkPhysicalDeviceProperties properties = VkPhysicalDeviceProperties.calloc();
	private VkQueueFamilyProperties.Buffer queueFamilyProperties;

	/*Created Device State Information*/
	public VkDevice device;
	public VkQueue renderQueue;
	public VkQueue asyncTransferQueue;
	public boolean asyncTransfer;

	/*State Information About Queues*/
	public int queueIndexRender;
	public int queueFamilyIndexTransfer, queueIndexTransfer;


	int queueFamilyIndexRender;//needed for surface check TODO: remove

	VkRenderDevice(VkDeviceState state,long handle) {
		this.state = state;
		physicalDevice = new VkPhysicalDevice(handle, state.instance);
		vkGetPhysicalDeviceFeatures(physicalDevice,deviceFeatures);
		vkGetPhysicalDeviceMemoryProperties(physicalDevice,memoryProperties);
		vkGetPhysicalDeviceProperties(physicalDevice,properties);
		int[] buffer = new int[1];
		vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice,buffer,null);
		queueFamilyProperties = VkQueueFamilyProperties.calloc(buffer[0]);
		vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice,buffer,queueFamilyProperties);
	}


	private PointerBuffer enabledLayers(MemoryStack stack) {
		IntBuffer sizeRef = stack.callocInt(1);
		state.success(vkEnumerateDeviceLayerProperties(physicalDevice,sizeRef,null),"Error Enumerating Device Layers");
		VkLayerProperties.Buffer layerPropertyList = VkLayerProperties.callocStack(sizeRef.get(0),stack);
		state.success(vkEnumerateDeviceLayerProperties(physicalDevice,sizeRef,layerPropertyList),"Error Enumerating Device Layers");
		List<ByteBuffer> enabledLayers = new ArrayList<>();
		for(int i = 0; i < sizeRef.get(0); i++) {
			layerPropertyList.position(i);
			if(VkDeviceState.vulkanDebug) {
				state.vkLogger.Debug("Device Layer: " + layerPropertyList.layerNameString());
			}
			if(VkDeviceState.vulkanDebug) {
				if(layerPropertyList.layerNameString().equals("VK_LAYER_LUNARG_standard_validation")) {
					enabledLayers.add(layerPropertyList.layerName());
					state.vkLogger.Info("Enabled Device Standard Validation Layer");
					continue;
				}
			}
			if(VkDeviceState.vulkanRenderDoc) {
				if(layerPropertyList.layerNameString().equals("VK_LAYER_RENDERDOC_Capture")) {
					enabledLayers.add(layerPropertyList.layerName());
					state.vkLogger.Info("Enabled Device RenderDoc Layer");
					continue;
				}
			}
			if(OpenVoxel.getLaunchParameters().hasFlag("-VKLayer:"+layerPropertyList.layerNameString())) {
				enabledLayers.add(layerPropertyList.layerName());
				state.vkLogger.Info("Enabled Custom Layer: " + layerPropertyList.layerNameString());
			}
		}
		PointerBuffer pointerBuffer = stack.callocPointer(enabledLayers.size());
		for(ByteBuffer buffer : enabledLayers) {
			pointerBuffer.put(buffer);
		}
		pointerBuffer.position(0);
		return pointerBuffer;
	}

	private PointerBuffer enabledExtensions(MemoryStack stack) {
		IntBuffer sizeRef = stack.callocInt(1);
		state.success(vkEnumerateDeviceExtensionProperties(physicalDevice,(ByteBuffer)null,sizeRef,null),"Error Enumerating Device Extensions");
		VkExtensionProperties.Buffer extensionPropertyList = VkExtensionProperties.callocStack(sizeRef.get(0),stack);
		state.success(vkEnumerateDeviceExtensionProperties(physicalDevice,(ByteBuffer)null,sizeRef,extensionPropertyList),"Error Enumerating Device Extensions");
		List<ByteBuffer> enabledExtensions = new ArrayList<>();
		enabledExtensions.add(stack.UTF8("VK_KHR_swapchain"));
		for(int i = 0; i < sizeRef.get(0); i++) {
			extensionPropertyList.position(i);
			if(VkDeviceState.vulkanDebug) {
				state.vkLogger.Debug("Device Extension: " + extensionPropertyList.extensionNameString());
			}
			if(VkDeviceState.vulkanDebug) {
				if(extensionPropertyList.extensionNameString().equals("VK_EXT_debug_marker")) {
					enabledExtensions.add(extensionPropertyList.extensionName());
					state.vkLogger.Debug("Enabled Debug Marker");
				}
			}
		}
		PointerBuffer pointerBuffer = stack.callocPointer(enabledExtensions.size());
		for(ByteBuffer buffer : enabledExtensions) {
			pointerBuffer.put(buffer);
		}
		pointerBuffer.position(0);
		return pointerBuffer;
	}

	private VkDeviceQueueCreateInfo.Buffer enabledQueues(MemoryStack stack) {
		int allQueue = -1;
		int allQueueLim = 0;
		int transferOnlyQueue = -1;
		for(int i = 0; i < queueFamilyProperties.capacity(); i++) {
			queueFamilyProperties.position(i);
			boolean presentSupport = GLFWVulkan.glfwGetPhysicalDevicePresentationSupport(state.instance,physicalDevice,i);
			boolean allType = queueFamilyProperties.queueFlags() ==
					                  (VK_QUEUE_GRAPHICS_BIT | VK_QUEUE_TRANSFER_BIT | VK_QUEUE_COMPUTE_BIT | VK_QUEUE_SPARSE_BINDING_BIT);
			boolean hasTransfer = (queueFamilyProperties.queueFlags() & VK_QUEUE_TRANSFER_BIT) != 0;
			boolean onlyTransfer = queueFamilyProperties.queueFlags() == VK_QUEUE_TRANSFER_BIT;
			if(allType && presentSupport) {
				allQueue = i;
				allQueueLim = queueFamilyProperties.queueCount();
			}
			if(hasTransfer && transferOnlyQueue == -1) {
				transferOnlyQueue = i;
			}
			if(onlyTransfer) {
				transferOnlyQueue = i;
			}
		}
		if(allQueue == -1) {
			throw new RuntimeException("Error Creating Vulkan Device");
		}
		asyncTransfer = !(transferOnlyQueue == allQueue && allQueueLim <= 1);
		state.vkLogger.Info("Async Transfer Queue : " + (asyncTransfer ? "Enabled" : "Disabled"));
		queueFamilyIndexRender = allQueue;
		queueIndexRender = 0;
		boolean dualQueue = transferOnlyQueue == allQueue;
		if(asyncTransfer) {
			queueFamilyIndexTransfer = transferOnlyQueue;
			if(dualQueue) {
				queueIndexTransfer = 1;
			}else{
				queueIndexTransfer = 0;
			}
		}else{
			queueIndexTransfer = 0;
			queueFamilyIndexTransfer = 0;
		}
		//Create//
		VkDeviceQueueCreateInfo.Buffer queueInfo = VkDeviceQueueCreateInfo.callocStack(dualQueue ? 1 : 2, stack);
		{
			queueInfo.position(0);
			queueInfo.sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO);
			queueInfo.pNext(VK_NULL_HANDLE);
			queueInfo.flags(0);
			queueInfo.queueFamilyIndex(queueFamilyIndexRender);
			if(dualQueue) {
				if(asyncTransfer) {
					queueInfo.pQueuePriorities(stack.floats(1.0f, 0.0f));
				}else{
					queueInfo.pQueuePriorities(stack.floats(1.0f));
				}
			}else {
				queueInfo.pQueuePriorities(stack.floats(1.0F));
			}
		}
		if(!dualQueue) {
			queueInfo.position(1);
			queueInfo.sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO);
			queueInfo.pNext(VK_NULL_HANDLE);
			queueInfo.flags(0);
			queueInfo.queueFamilyIndex(queueFamilyIndexTransfer);
			queueInfo.pQueuePriorities(stack.floats(0.0f));
		}
		queueInfo.position(0);
		return queueInfo;
	}


	void createDevice() {
		try(MemoryStack stack = stackPush()) {
			PointerBuffer pointer = stack.callocPointer(1);
			VkDeviceCreateInfo createInfo = VkDeviceCreateInfo.callocStack(stack);
			createInfo.sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO);
			createInfo.pNext(VK_NULL_HANDLE);
			createInfo.flags(0);
			createInfo.ppEnabledLayerNames(enabledLayers(stack));
			createInfo.ppEnabledExtensionNames(enabledExtensions(stack));
			createInfo.pQueueCreateInfos(enabledQueues(stack));
			createInfo.pEnabledFeatures(deviceFeatures);
			state.success(vkCreateDevice(physicalDevice,createInfo,null,pointer),"Error Creating Vulkan Device");
			device = new VkDevice(pointer.get(0),physicalDevice,createInfo);
			vkGetDeviceQueue(device,queueFamilyIndexRender,queueIndexRender,pointer);
			renderQueue = new VkQueue(pointer.get(0),device);
			if(asyncTransfer) {
				vkGetDeviceQueue(device, queueFamilyIndexTransfer, queueIndexTransfer, pointer);
				asyncTransferQueue = new VkQueue(pointer.get(0), device);
			}else{
				asyncTransferQueue = null;
			}
		}
	}

	boolean isValidDevice() {
		if(!deviceFeatures.geometryShader()) {
			return false;
		}
		if(!deviceFeatures.dualSrcBlend()) {
			return false;
		}
		if(!deviceFeatures.tessellationShader()) {
			return false;
		}
		if(!deviceFeatures.samplerAnisotropy()) {
			return false;
		}
		//TODO: implement more validation checks
		return true;
	}

	int rateDevice() {
		int SCORE = 0;
		if(properties.deviceType() == VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU) {
			SCORE += 10000;
		}
		for(int i = 0; i < memoryProperties.memoryHeapCount(); i++) {
			SCORE += memoryProperties.memoryHeaps(i).size();
		}
		//Sorta Compute Power Check//
		SCORE += properties.limits().maxComputeWorkGroupInvocations();
		return SCORE;
	}


	String getDeviceInfo() {
		if(properties == null) return null;
		return properties.deviceNameString() + " : " + VK_VERSION_MAJOR(properties.apiVersion())
				+ "." + VK_VERSION_MINOR(properties.apiVersion()) + "." + VK_VERSION_PATCH(properties.apiVersion())
				+ "(driver: " + VK_VERSION_MAJOR(properties.driverVersion()) + "." + VK_VERSION_MINOR(properties.driverVersion())
				+ "." + VK_VERSION_PATCH(properties.driverVersion()) + ")";
	}

	void printDetailedDeviceInfo() {
		Logger log = state.vkLogger;
		log.Info("Device Features:");
		//TODO:
		//log.Info("Device Properties:");
		//properties.
		log.Info("Device Queue Family Info:");
		//
		log.Info("Device Memory:");
		for(int i = 0; i < memoryProperties.memoryTypeCount(); i++) {
			int propFlags = memoryProperties.memoryTypes(i).propertyFlags();
			int heapIndex = memoryProperties.memoryTypes(i).heapIndex();
			int memFlags = memoryProperties.memoryHeaps(heapIndex).flags();
			List<String> propArray = new ArrayList<>();
			if((propFlags & VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT) != 0) {
				propArray.add("Device-Local");
			}
			if((propFlags & VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT) != 0) {
				propArray.add("Host-Visible");
			}
			if((propFlags & VK_MEMORY_PROPERTY_HOST_COHERENT_BIT) != 0) {
				propArray.add("Host-Coherent");
			}
			if((propFlags & VK_MEMORY_PROPERTY_HOST_CACHED_BIT) != 0) {
				propArray.add("Host-Cached");
			}
			if((propFlags & VK_MEMORY_PROPERTY_LAZILY_ALLOCATED_BIT) != 0) {
				propArray.add("Lazy-Alloc");
			}
			if(memFlags == VK_MEMORY_HEAP_DEVICE_LOCAL_BIT) {
				log.Info("Device-Local || " + String.join(":",propArray));
			}else {
				log.Info("None || " + String.join(":", propArray));
			}
		}
	}

	void freeInitial() {
		deviceFeatures.free();
		properties.free();
		queueFamilyProperties.free();
		memoryProperties.free();
	}

	void freeDevice() {
		freeInitial();
		vkDestroyDevice(device,null);
	}

	public int findMemoryType(int typeFilter,int memoryPropertyFlags) {
		//TODO: improve//
		for (int i = 0; i < memoryProperties.memoryTypeCount(); i++) {
			if ((typeFilter & (1 << i)) != 0 &&
				    (memoryProperties.memoryTypes(i).propertyFlags() & memoryPropertyFlags) == memoryPropertyFlags) {
				return i;
			}
		}
		state.vkLogger.Severe("Failed to find valid memory type");
		//TODO: handle better
		throw new RuntimeException("Failure");
	}
}
