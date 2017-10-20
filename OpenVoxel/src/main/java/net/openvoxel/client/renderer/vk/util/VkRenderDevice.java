package net.openvoxel.client.renderer.vk.util;

import net.openvoxel.OpenVoxel;
import net.openvoxel.api.logger.Logger;
import net.openvoxel.client.gui.ScreenDebugInfo;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
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
	public VkPhysicalDevice physicalDevice;
	private VkPhysicalDeviceFeatures deviceFeatures = VkPhysicalDeviceFeatures.calloc();
	public VkPhysicalDeviceMemoryProperties memoryProperties = VkPhysicalDeviceMemoryProperties.calloc();
	private VkPhysicalDeviceProperties properties = VkPhysicalDeviceProperties.calloc();
	VkQueueFamilyProperties.Buffer queueFamilyProperties;

	/*Created Device State Information*/
	public VkDevice device;
	public VkQueue renderQueue;
	public VkQueue asyncTransferQueue;
	public boolean asyncTransfer;

	/*State Information About Queues*/
	public int queueIndexRender, queueFamilyIndexRender;
	public int queueFamilyIndexTransfer, queueIndexTransfer;


	private String get_version(int ver,boolean custom_flag) {
		if(custom_flag) {
			//NVIDIA CUSTOM
			int major = VK_VERSION_MAJOR(ver);
			int minor = (ver >> 14) & 0x0ff;
			int secondaryBranch = (ver >> 6) & 0x0ff;
			int tertiaryBranch = (ver) & 0x003f;
			return major + "." + minor + "." + secondaryBranch + "." + tertiaryBranch;
		}else {
			return VK_VERSION_MAJOR(ver) + "." + VK_VERSION_MINOR(ver) + "." + VK_VERSION_PATCH(ver);
		}
	}

	private String get_vendor(int vendorID) {
		switch(vendorID) {
			case 0x1002: return "AMD";
			case 0x1010: return "ImgTec";
			case 0x10DE: return "NVIDIA";
			case 0x13B5: return "ARM";
			case 0x5143: return "Qualcomm";
			case 0x8086: return "INTEL";
			default:
				return "Vendor-"+Integer.toHexString(vendorID);
		}
	}

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
		//Update Debug Screen With Information//
		ScreenDebugInfo.RendererType = "Vulkan "+get_version(properties.apiVersion(),false) + " ";
		ScreenDebugInfo.RendererVendor = get_vendor(properties.vendorID()) + " ";
		ScreenDebugInfo.RendererDriver = properties.deviceNameString() + " "
             + get_version(properties.driverVersion(),properties.vendorID() == 0x10DE) + " ";
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
			//Enable Standard Useful Extensions
			String extName = extensionPropertyList.extensionNameString();
			if(extName.equals("VK_KHR_maintenance1") || extName.equals("VK_KHR_maintenance2")) {
				enabledExtensions.add(extensionPropertyList.extensionName());
				state.vkLogger.Debug("Enabled KHR Maintenance Extension: " + extName);
			}
			if(extName.equals("VK_NV_geometry_shader_passthrough")) {
				enabledExtensions.add(extensionPropertyList.extensionName());
				state.vkLogger.Debug("Enabled NV Geometry Shader Passthrough");
			}
			if(extName.equals("VK_EXT_blend_operation_advanced")) {
				enabledExtensions.add(extensionPropertyList.extensionName());
				state.vkLogger.Debug("Enabled EXT Advanced Blend Operations");
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
			state.vkLogger.Info("Acquired Rendering Queue");
			if(asyncTransfer) {
				vkGetDeviceQueue(device, queueFamilyIndexTransfer, queueIndexTransfer, pointer);
				asyncTransferQueue = new VkQueue(pointer.get(0), device);
				state.vkLogger.Info("Acquired Transfer Queue");
			}else{
				state.vkLogger.Info("Doubling up render queue as async transfer queue");
				asyncTransferQueue = renderQueue;
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

	int findMemoryType(int memoryTypeBits,int memoryPropertyFlags) {
		for (int i = 0; i < memoryProperties.memoryTypeCount(); i++) {
			boolean filter = (memoryTypeBits & (1 << i)) != 0;
			int mem_props = memoryProperties.memoryTypes(i).propertyFlags();
			boolean valid_props = (mem_props & memoryPropertyFlags) == memoryPropertyFlags;
			if (filter && valid_props) {
				return i;
			}
		}
		if((memoryPropertyFlags & VK_MEMORY_PROPERTY_HOST_CACHED_BIT) == VK_MEMORY_PROPERTY_HOST_CACHED_BIT) {
			//Fallback - No Caching//
			int new_props = memoryPropertyFlags & ~VK_MEMORY_PROPERTY_HOST_CACHED_BIT;
			return findMemoryType(memoryTypeBits,new_props);
		}
		state.vkLogger.Severe("Failed to find valid memory type");
		throw new RuntimeException("Failure");
	}
}
