package net.openvoxel.client.renderer.vk.core;

import net.openvoxel.OpenVoxel;
import net.openvoxel.client.gui.ScreenDebugInfo;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

//////////////////////////////////////
/// Wrapper around a vulkan device ///
//////////////////////////////////////
public final class VulkanDevice implements Closeable {

	private final VkPhysicalDevice physicalDevice;
	private final VkDevice logicalDevice;

	private final VkPhysicalDeviceProperties properties = VkPhysicalDeviceProperties.malloc();
	private final  VkPhysicalDeviceMemoryProperties memory = VkPhysicalDeviceMemoryProperties.malloc();
	private final VkPhysicalDeviceFeatures features = VkPhysicalDeviceFeatures.malloc();

	/**
	 * All capability queue including present support
	 */
	private final VkQueue allQueue;

	/**
	 * Transfer capability queue for asynchronous transfers
	 */
	private final VkQueue transferQueue;


	private boolean enableAsyncTransfer;
	private int familyQueue;
	private int indexQueue;
	private int familyTransfer;
	private int indexTransfer;

	private static final float PriorityAllQueue = 1.0F;
	private static final float PriorityFakeAsyncQueue = 0.0F;
	private static final float PriorityTrueAsyncQueue = 0.0F;


	public VulkanDevice(VkInstance instance) {
		physicalDevice = chooseDevice(instance);
		if(physicalDevice == null || !isDeviceValid(physicalDevice)) {
			VulkanUtility.LogSevere("Failed to Find Valid Device");
			throw new RuntimeException("Failed to Find Valid Vulkan Device");
		}
		logicalDevice = createLogicalDevice(instance,physicalDevice);
		allQueue = getQueue(false);
		transferQueue = getQueue(true);
		loadDeviceInfo();
		updateMetadata();
	}

	@Override
	public void close() {
		vkDestroyDevice(logicalDevice,null);
		memory.free();
		properties.free();
		features.free();
	}

	//////////////////////
	/// Interface Code ///
	//////////////////////

	void printDetailedDeviceInfo() {
		VulkanUtility.LogInfo("Device Metadata:");
		VulkanUtility.LogInfo(" - Device Features:");
		if(features.geometryShader()) VulkanUtility.LogInfo("    - Geometry Shader");
		if(features.tessellationShader()) VulkanUtility.LogInfo("    - Tessellation Shader");
		//TODO: add more features
		VulkanUtility.LogInfo(" - Device Properties:");
		VulkanUtility.LogInfo("    Vendor = " + VulkanUtility.getVendorAsString(properties.vendorID()));
		VulkanUtility.LogInfo("    Device = " + properties.deviceNameString());
		//TODO: limits
		VulkanUtility.LogInfo(" - Device Queue Family Info:");
		if(enableAsyncTransfer) {
			if(familyTransfer == familyQueue) {
				VulkanUtility.LogInfo("    2x All Property Queue");
			}else{
				VulkanUtility.LogInfo("    1x All Property Queue");
				VulkanUtility.LogInfo("    1x Transfer Only Queue");
			}
		}else{
			VulkanUtility.LogInfo("    1x All Property Queue");
		}
		VulkanUtility.LogInfo(" - Device Memory:");
		for(int i = 0; i < memory.memoryTypeCount(); i++) {
			int propFlags = memory.memoryTypes(i).propertyFlags();
			int heapIndex = memory.memoryTypes(i).heapIndex();
			int memFlags = memory.memoryHeaps(heapIndex).flags();
			List<String> propArray = new ArrayList<>();
			if((propFlags & VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT) != 0) propArray.add("Device-Local");
			if((propFlags & VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT) != 0) propArray.add("Host-Visible");
			if((propFlags & VK_MEMORY_PROPERTY_HOST_COHERENT_BIT) != 0) propArray.add("Host-Coherent");
			if((propFlags & VK_MEMORY_PROPERTY_HOST_CACHED_BIT) != 0) propArray.add("Host-Cached");
			if((propFlags & VK_MEMORY_PROPERTY_LAZILY_ALLOCATED_BIT) != 0) propArray.add("Lazy-Alloc");
			if(memFlags == VK_MEMORY_HEAP_DEVICE_LOCAL_BIT) {
				VulkanUtility.LogInfo("    " + i + ": Device || " + String.join(":",propArray));
			}else {
				VulkanUtility.LogInfo("    " + i + ": Host   || " + String.join(":", propArray));
			}
		}
	}

	/////////////////////
	/// Metadata Code ///
	/////////////////////

	private void loadDeviceInfo() {
		vkGetPhysicalDeviceMemoryProperties(physicalDevice,memory);
		vkGetPhysicalDeviceFeatures(physicalDevice,features);
		vkGetPhysicalDeviceProperties(physicalDevice,properties);
	}

	private void updateMetadata() {
		ScreenDebugInfo.RendererType = "Vulkan " +
                       VulkanUtility.getVersionAsString(properties.apiVersion()) + " ";
		ScreenDebugInfo.RendererVendor = VulkanUtility.getVendorAsString(properties.vendorID()) + " ";
		String driverVersion = properties.vendorID() == 0x10DE ?
				                       VulkanUtility.getNvidiaDriverVersionString(properties.driverVersion())
									   : VulkanUtility.getVersionAsString(properties.driverVersion());
		ScreenDebugInfo.RendererDriver = properties.deviceNameString() + " " + driverVersion + " ";
	}


	///////////////////////////
	/// Initialization Code ///
	///////////////////////////


	private boolean isDeviceValid(@NotNull VkPhysicalDevice device) {
		try(MemoryStack stack = stackPush()) {
			VkPhysicalDeviceFeatures tmpFeatures = VkPhysicalDeviceFeatures.mallocStack(stack);
			vkGetPhysicalDeviceFeatures(device, tmpFeatures);
			//TODO: replace with better validity check
			return tmpFeatures.geometryShader();
		}
	}

	private VkPhysicalDevice chooseDevice(VkInstance instance) {
		try(MemoryStack stack = stackPush()) {
			IntBuffer sizeRef = stack.mallocInt(1);
			final String err = "Failed to enumerate physical devices";
			VulkanUtility.ValidateSuccess(err,vkEnumeratePhysicalDevices(instance,sizeRef,null));
			PointerBuffer physicalDeviceList = stack.mallocPointer(sizeRef.get(0));
			VulkanUtility.ValidateSuccess(err,vkEnumeratePhysicalDevices(instance,sizeRef,physicalDeviceList));
			VkPhysicalDevice bestDevice = null;
			double bestScore = Double.MIN_VALUE;
			for(int i = 0; i < sizeRef.get(0); i++) {
				VkPhysicalDevice device = new VkPhysicalDevice(physicalDeviceList.get(i),instance);
				double score = scoreDevice(stack,device);
				if(score > bestScore) {
					bestScore = score;
					bestDevice = device;
				}
			}
			return bestDevice;
		}
	}

	private static double scoreDevice(@NotNull MemoryStack supStack,@NotNull VkPhysicalDevice device) {
		try(MemoryStack stack = supStack.push()) {
			VkPhysicalDeviceFeatures tmpFeatures = VkPhysicalDeviceFeatures.mallocStack(stack);
			vkGetPhysicalDeviceFeatures(device, tmpFeatures);
			VkPhysicalDeviceProperties tmpProps = VkPhysicalDeviceProperties.mallocStack(stack);
			vkGetPhysicalDeviceProperties(device,tmpProps);
			VkPhysicalDeviceMemoryProperties tmpMems = VkPhysicalDeviceMemoryProperties.mallocStack(stack);
			vkGetPhysicalDeviceMemoryProperties(device,tmpMems);
			double score = 0;
			if(tmpProps.deviceType() == VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU) {
				score += 1000;
			}else if(tmpProps.deviceType() == VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU) {
				score += 100;
			}
			if(tmpFeatures.geometryShader()) {
				score += 100;
			}
			if(tmpFeatures.tessellationShader()) {
				score += 100;
			}
			double memAmount = 0;
			for(int i = 0; i < tmpMems.memoryHeapCount(); i++) {
				int flags = tmpMems.memoryHeaps(i).flags();
				if((flags & VK_MEMORY_HEAP_DEVICE_LOCAL_BIT) != 0) {
					memAmount += tmpMems.memoryHeaps(i).size();
				}
			}
			return score * memAmount;
		}
	}

	@NotNull
	private VkQueue getQueue(boolean isTransfer) {
		try(MemoryStack stack = stackPush()) {
			PointerBuffer pointer = stack.mallocPointer(1);
			int family = isTransfer ? familyTransfer : familyQueue;
			int index  = isTransfer ? indexTransfer  : indexQueue;
			vkGetDeviceQueue(logicalDevice,family,index,pointer);
			return new VkQueue(pointer.get(0),logicalDevice);
		}
	}

	@NotNull
	private VkDevice createLogicalDevice(VkInstance instance, VkPhysicalDevice device) {
		try(MemoryStack stack = stackPush()) {
			PointerBuffer pointer = stack.callocPointer(1);
			VkDeviceCreateInfo createInfo = VkDeviceCreateInfo.callocStack(stack);
			VkPhysicalDeviceFeatures deviceFeatures = VkPhysicalDeviceFeatures.mallocStack(stack);
			vkGetPhysicalDeviceFeatures(device,deviceFeatures);
			createInfo.sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO);
			createInfo.pNext(VK_NULL_HANDLE);
			createInfo.flags(0);
			createInfo.ppEnabledLayerNames(chooseEnabledLayers(stack));
			createInfo.ppEnabledExtensionNames(chooseEnabledExtensions(stack));
			createInfo.pQueueCreateInfos(chooseQueues(instance,stack));
			createInfo.pEnabledFeatures(deviceFeatures);
			VulkanUtility.ValidateSuccess("Failed to create device",
					vkCreateDevice(physicalDevice,createInfo,null,pointer));
			VulkanUtility.LogInfo("Successfully Created Device");
			return new VkDevice(pointer.get(0),physicalDevice,createInfo);
		}
	}

	private VkDeviceQueueCreateInfo.Buffer chooseQueues(@NotNull VkInstance instance, @NotNull MemoryStack stack) {
		int allQueue = -1;
		int allQueueLim = 0;
		int transferOnlyQueue = -1;
		IntBuffer sizeRef = stack.mallocInt(1);
		vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice,sizeRef,null);
		VkQueueFamilyProperties.Buffer queueProperties = VkQueueFamilyProperties.mallocStack(sizeRef.get(0),stack);
		vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice,sizeRef,queueProperties);

		for(int i = 0; i < sizeRef.get(0); i++) {
			queueProperties.position(i);
			boolean presentSupport = GLFWVulkan.glfwGetPhysicalDevicePresentationSupport(instance,physicalDevice,i);
			boolean allType = queueProperties.queueFlags() ==
					                  (VK_QUEUE_GRAPHICS_BIT | VK_QUEUE_TRANSFER_BIT | VK_QUEUE_COMPUTE_BIT | VK_QUEUE_SPARSE_BINDING_BIT);
			boolean hasTransfer = (queueProperties.queueFlags() & VK_QUEUE_TRANSFER_BIT) != 0;
			boolean onlyTransfer = queueProperties.queueFlags() == VK_QUEUE_TRANSFER_BIT;
			if(allType && presentSupport) {
				allQueue = i;
				allQueueLim = queueProperties.queueCount();
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
		enableAsyncTransfer = !(transferOnlyQueue == allQueue && allQueueLim <= 1);
		VulkanUtility.LogInfo("Async Transfer Queue : " + (enableAsyncTransfer ? "Enabled" : "Disabled"));
		familyQueue = allQueue;
		indexQueue = 0;
		boolean dualQueue = transferOnlyQueue == allQueue;
		if(enableAsyncTransfer) {
			familyTransfer = transferOnlyQueue;
			indexTransfer = dualQueue ? 1 : 0;
		}else{
			familyTransfer = 0;
			indexTransfer = 0;
		}
		//Create//
		VkDeviceQueueCreateInfo.Buffer queueInfo = VkDeviceQueueCreateInfo.callocStack(dualQueue ? 1 : 2, stack);
		{
			queueInfo.position(0);
			queueInfo.sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO);
			queueInfo.pNext(VK_NULL_HANDLE);
			queueInfo.flags(0);
			queueInfo.queueFamilyIndex(familyQueue);
			if(dualQueue && enableAsyncTransfer) {
				queueInfo.pQueuePriorities(stack.floats(PriorityAllQueue, PriorityFakeAsyncQueue));
			}else{
				queueInfo.pQueuePriorities(stack.floats(PriorityAllQueue));
			}
		}
		if(!dualQueue) {
			queueInfo.position(1);
			queueInfo.sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO);
			queueInfo.pNext(VK_NULL_HANDLE);
			queueInfo.flags(0);
			queueInfo.queueFamilyIndex(familyTransfer);
			queueInfo.pQueuePriorities(stack.floats(PriorityTrueAsyncQueue));
		}
		queueInfo.position(0);
		return queueInfo;
	}

	private PointerBuffer chooseEnabledLayers(@NotNull MemoryStack stack) {
		IntBuffer sizeRef = stack.callocInt(1);
		final String err = "Error Enumerating Device Layers";
		VulkanUtility.ValidateSuccess(err,vkEnumerateDeviceLayerProperties(physicalDevice,sizeRef,null));
		VkLayerProperties.Buffer layerList = VkLayerProperties.callocStack(sizeRef.get(0),stack);
		VulkanUtility.ValidateSuccess(err,vkEnumerateDeviceLayerProperties(physicalDevice,sizeRef,layerList));
		List<ByteBuffer> enabledLayers = new ArrayList<>();
		for(int i = 0; i < sizeRef.get(0); i++) {
			layerList.position(i);
			if(VulkanState.flag_vulkanDebug) {
				VulkanUtility.LogDebug("Device Layer: " + layerList.layerNameString());
			}
			if(VulkanState.flag_vulkanDebug) {
				if(layerList.layerNameString().equals("VK_LAYER_LUNARG_standard_validation")) {
					enabledLayers.add(layerList.layerName());
					VulkanUtility.LogInfo("Enabled Device Layer: Standard Validation");
					continue;
				}
			}
			if(VulkanState.flag_vulkanRenderDoc) {
				if(layerList.layerNameString().equals("VK_LAYER_RENDERDOC_Capture")) {
					enabledLayers.add(layerList.layerName());
					VulkanUtility.LogInfo("Enabled Device Layer: RenderDoc");
					continue;
				}
			}
			if(OpenVoxel.getLaunchParameters().hasFlag("-VKLayer:"+layerList.layerNameString())) {
				enabledLayers.add(layerList.layerName());
				VulkanUtility.LogInfo("Enabled Custom Layer: " + layerList.layerNameString());
			}
		}
		return VulkanUtility.toPointerBuffer(stack,enabledLayers);
	}

	private PointerBuffer chooseEnabledExtensions(@NotNull MemoryStack stack) {
		IntBuffer sizeRef = stack.callocInt(1);
		final String err = "Error Enumerating Device Extensions";
		VulkanUtility.ValidateSuccess(err,vkEnumerateDeviceExtensionProperties(physicalDevice,(ByteBuffer)null,sizeRef,null));
		VkExtensionProperties.Buffer extensionList = VkExtensionProperties.callocStack(sizeRef.get(0),stack);
		VulkanUtility.ValidateSuccess(err,vkEnumerateDeviceExtensionProperties(physicalDevice,(ByteBuffer)null,sizeRef,extensionList));
		List<ByteBuffer> enabledExtensions = new ArrayList<>();
		enabledExtensions.add(stack.UTF8("VK_KHR_swapchain"));
		for(int i = 0; i < sizeRef.get(0); i++) {
			extensionList.position(i);
			if(VulkanState.flag_vulkanDebug) {
				VulkanUtility.LogDebug("Device Extension: " + extensionList.extensionNameString());
			}
			if(VulkanState.flag_vulkanDebug) {
				if(extensionList.extensionNameString().equals("VK_EXT_debug_marker")) {
					enabledExtensions.add(extensionList.extensionName());
					VulkanUtility.LogInfo("Enabled Ext: Debug Marker");
				}
			}
			//Enable Standard Useful Extensions
			String extName = extensionList.extensionNameString();
			if(extName.equals("VK_KHR_maintenance1") || extName.equals("VK_KHR_maintenance2")) {
				enabledExtensions.add(extensionList.extensionName());
				VulkanUtility.LogInfo("Enabled Ext: KHR Maintenance Extension: " + extName);
			}
			if(extName.equals("VK_NV_geometry_shader_passthrough")) {
				enabledExtensions.add(extensionList.extensionName());
				VulkanUtility.LogInfo("Enabled Ext: NV Geometry Shader Passthrough");
			}
			if(extName.equals("VK_EXT_blend_operation_advanced")) {
				enabledExtensions.add(extensionList.extensionName());
				VulkanUtility.LogInfo("Enabled Ext: EXT Advanced Blend Operations");
			}
		}
		return VulkanUtility.toPointerBuffer(stack,enabledExtensions);
	}

}