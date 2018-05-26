package net.openvoxel.client.renderer.vk.core;

import net.openvoxel.OpenVoxel;
import net.openvoxel.client.gui.util.ScreenDebugInfo;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.EXTShaderViewportIndexLayer.VK_EXT_SHADER_VIEWPORT_INDEX_LAYER_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRMaintenance1.VK_KHR_MAINTENANCE1_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRMaintenance2.VK_KHR_MAINTENANCE2_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR;
import static org.lwjgl.vulkan.NVGLSLShader.VK_NV_GLSL_SHADER_EXTENSION_NAME;
import static org.lwjgl.vulkan.NVGeometryShaderPassthrough.VK_NV_GEOMETRY_SHADER_PASSTHROUGH_EXTENSION_NAME;
import static org.lwjgl.vulkan.VK10.*;

//////////////////////////////////////
/// Wrapper around a vulkan device ///
//////////////////////////////////////
public final class VulkanDevice {

	//State
	final VkPhysicalDevice physicalDevice;
	public final VkDevice logicalDevice;

	///Enabled Extensions
	public boolean enabled_KHR_maintenance1 = false;
	public boolean enabled_KHR_maintenance2 = false;
	//public boolean enabled_KHR_dedicated_allocation = false;
	//public boolean enabled_KHR_get_memory_requirements2 = false;
	//public boolean enabled_KHR_push_descriptor = false;
	public boolean enabled_EXT_shader_viewport_index_layer = false;
	public boolean enabled_NV_geometry_passthrough = false;
	public boolean enabled_NV_glsl_shader = false;

	//Properties & Features
	public final VkPhysicalDeviceProperties properties = VkPhysicalDeviceProperties.malloc();
	//private final VkPhysicalDeviceBlendOperationAdvancedPropertiesEXT advBlendProperties;
	//private final VkPhysicalDevicePointClippingPropertiesKHR pointClippingProperties;
	//private final VkPhysicalDevicePushDescriptorPropertiesKHR pushDescriptorProperties;
	//private final VkPhysicalDeviceDiscardRectanglePropertiesEXT discardRectangleProperties;
	public final VkPhysicalDeviceMemoryProperties memory = VkPhysicalDeviceMemoryProperties.malloc();
	public final VkPhysicalDeviceFeatures features = VkPhysicalDeviceFeatures.malloc();
	//private final VkPhysicalDevice16BitStorageFeaturesKHR memory16BitProperties;
	//private final VkPhysicalDeviceSamplerYcbcrConversionFeaturesKHR ycbrConversionFeatures;
	//private final VkPhysicalDeviceVariablePointerFeaturesKHR pointerFeatures;
	//private final VkPhysicalDeviceBlendOperationAdvancedFeaturesEXT advBlendFeatures;

	/*
	 * All capability queue including present support
	 */
	public final VkQueue allQueue;

	/*
	 * Transfer capability queue for asynchronous transfers
	 */
	public final VkQueue transferQueue;

	//Queue Properties
	public final VkExtent3D transferQueueImageGranularity;
	public int allQueueTimestampValidBits;

	private boolean enableAsyncTransfer;
	public int familyQueue;
	public int familyTransfer;
	private int indexQueue;
	private int indexTransfer;

	private static final float PriorityAllQueue = 1.0F;
	private static final float PriorityFakeAsyncQueue = 0.0F;
	private static final float PriorityTrueAsyncQueue = 0.0F;


	VulkanDevice(VkInstance instance,long surface) {
		transferQueueImageGranularity = VkExtent3D.calloc();
		allQueueTimestampValidBits = 0;
		physicalDevice = chooseDevice(instance,surface);
		if(physicalDevice == null || !isDeviceValid(physicalDevice)) {
			VulkanUtility.LogSevere("Failed to Find Valid Device");
			throw new RuntimeException("Failed to Find Valid Vulkan Device");
		}
		logicalDevice = createLogicalDevice(instance,physicalDevice);
		allQueue = getQueue(false);
		if(!isDeviceSurfaceValid(physicalDevice,surface)) {
			//TODO: IMPLEMENT VALIDITY CHECKING BETTER
			VulkanUtility.LogSevere("Failed to Find Valid Vulkan Device: Invalid Surface Support");
			throw new RuntimeException("Failed to Find Valid Vulkan Device Surface Support");
		}
		transferQueue = getQueue(true);
		loadDeviceInfo();
		updateMetadata();
	}

	public void close() {
		vkDestroyDevice(logicalDevice,null);
		memory.free();
		properties.free();
		features.free();
		transferQueueImageGranularity.free();
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
				VulkanUtility.LogInfo("      - Image Granularity = (1,1,1)");
			}else{
				VulkanUtility.LogInfo("    1x All Property Queue");
				VulkanUtility.LogInfo("      - Image Granularity = (1,1,1)");
				VulkanUtility.LogInfo("    1x Transfer Only Queue");
				VulkanUtility.LogInfo("      - Image Granularity = (" +
						                      transferQueueImageGranularity.width() + "," +
						                      transferQueueImageGranularity.height() + "," +
						                      transferQueueImageGranularity.depth() + ")");
			}
		}else{
			VulkanUtility.LogInfo("    1x All Property Queue");
			VulkanUtility.LogInfo("      - Image Granularity = (1,1,1)");
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

	////////////////////
	/// Utility Code ///
	////////////////////

	private boolean supportsVertexBufferType(@NotNull VkPhysicalDevice device, int format) {
		try(MemoryStack stack = stackPush()) {
			VkFormatProperties props = VkFormatProperties.mallocStack(stack);
			vkGetPhysicalDeviceFormatProperties(device,format,props);
			return (props.bufferFeatures() & VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT) == VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT;
		}
	}

	///////////////////////////
	/// Initialization Code ///
	///////////////////////////

	private boolean isDeviceSurfaceValid(@NotNull VkPhysicalDevice device, long surface) {
		try(MemoryStack stack = stackPush()) {
			//Check surface Validity
			IntBuffer isSupported = stack.mallocInt(1);
			int res = vkGetPhysicalDeviceSurfaceSupportKHR(device, familyQueue, surface, isSupported);
			if (res != VK_SUCCESS || isSupported.get(0) == 0) {
				VulkanUtility.LogSevere("Chosen device lacks surface support!");
				return false;
			}else{
				return true;
			}
		}
	}

	private boolean isDeviceValid(@NotNull VkPhysicalDevice device) {
		try(MemoryStack stack = stackPush()) {
			VkPhysicalDeviceFeatures tmpFeatures = VkPhysicalDeviceFeatures.mallocStack(stack);
			vkGetPhysicalDeviceFeatures(device, tmpFeatures);
			//TODO: replace with better validity check

			//Float Buffer Formats [1,2,3,4]
			if(!supportsVertexBufferType(device,VK_FORMAT_R32G32B32A32_SFLOAT)) return false;
			if(!supportsVertexBufferType(device,VK_FORMAT_R32G32B32_SFLOAT)) return false;
			if(!supportsVertexBufferType(device,VK_FORMAT_R32G32_SFLOAT)) return false;
			if(!supportsVertexBufferType(device,VK_FORMAT_R32_SFLOAT)) return false;

			//Normalised Short Formats [1,2,4]
			if(!supportsVertexBufferType(device,VK_FORMAT_R16G16B16A16_UNORM)) return false;
			if(!supportsVertexBufferType(device,VK_FORMAT_R16G16_UNORM)) return false;
			if(!supportsVertexBufferType(device,VK_FORMAT_R16_UNORM)) return false;

			//Normalised Byte Formats [1,2,4]
			if(!supportsVertexBufferType(device,VK_FORMAT_R8G8B8A8_UNORM)) return false;
			if(!supportsVertexBufferType(device,VK_FORMAT_R8G8_UNORM)) return false;
			if(!supportsVertexBufferType(device,VK_FORMAT_R8_UNORM)) return false;

			return tmpFeatures.geometryShader();
		}
	}

	private VkPhysicalDevice chooseDevice(VkInstance instance, long surface) {
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
				double score = scoreDevice(stack,device,surface);
				if(score > bestScore && isDeviceValid(device)) {
					bestScore = score;
					bestDevice = device;
				}
			}
			return bestDevice;
		}
	}

	private double scoreDevice(@NotNull MemoryStack supStack,@NotNull VkPhysicalDevice device, long surface) {
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
			//TODO: CHECK VALID vkGetPhysicalDeviceSurfaceSupportKHR
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
			PointerBuffer pointer = stack.mallocPointer(1);
			VkDeviceCreateInfo createInfo = VkDeviceCreateInfo.mallocStack(stack);
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
				allQueueTimestampValidBits = queueProperties.timestampValidBits();
			}
			if(hasTransfer && transferOnlyQueue == -1) {
				transferOnlyQueue = i;
				transferQueueImageGranularity.set(queueProperties.minImageTransferGranularity());
			}
			if(onlyTransfer) {
				transferOnlyQueue = i;
				transferQueueImageGranularity.set(queueProperties.minImageTransferGranularity());
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
		VkDeviceQueueCreateInfo.Buffer queueInfo = VkDeviceQueueCreateInfo.mallocStack(dualQueue ? 1 : 2, stack);
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
		IntBuffer sizeRef = stack.mallocInt(1);
		final String err = "Error Enumerating Device Layers";
		VulkanUtility.ValidateSuccess(err,vkEnumerateDeviceLayerProperties(physicalDevice,sizeRef,null));
		VkLayerProperties.Buffer layerList = VkLayerProperties.mallocStack(sizeRef.get(0),stack);
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
		IntBuffer sizeRef = stack.mallocInt(1);
		final String err = "Error Enumerating Device Extensions";
		VulkanUtility.ValidateSuccess(err,vkEnumerateDeviceExtensionProperties(physicalDevice,(ByteBuffer)null,sizeRef,null));
		VkExtensionProperties.Buffer extensionList = VkExtensionProperties.mallocStack(sizeRef.get(0),stack);
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
			if(extName.equals(VK_KHR_MAINTENANCE1_EXTENSION_NAME) || extName.equals(VK_KHR_MAINTENANCE2_EXTENSION_NAME)) {
				enabledExtensions.add(extensionList.extensionName());
				VulkanUtility.LogInfo("Enabled Ext: KHR Maintenance Extension: " + extName);
				if(extName.equals(VK_KHR_MAINTENANCE1_EXTENSION_NAME)) {
					enabled_KHR_maintenance1 = true;
				}else{
					enabled_KHR_maintenance2 = true;
				}
			}
			if(extName.equals(VK_NV_GEOMETRY_SHADER_PASSTHROUGH_EXTENSION_NAME)) {
				enabledExtensions.add(extensionList.extensionName());
				VulkanUtility.LogInfo("Enabled Ext: NV Geometry Shader PassThrough");
				enabled_NV_geometry_passthrough = true;
			}
			if(extName.equals(VK_NV_GLSL_SHADER_EXTENSION_NAME)) {
				enabledExtensions.add(extensionList.extensionName());
				VulkanUtility.LogInfo("Enabled Ext: NV GLSL Shader");
				enabled_NV_glsl_shader = true;
			}
			if(extName.equals(VK_EXT_SHADER_VIEWPORT_INDEX_LAYER_EXTENSION_NAME)) {
				enabledExtensions.add(extensionList.extensionName());
				VulkanUtility.LogInfo("Enabled Ext: EXT Shader Viewport Index Layer");
				enabled_EXT_shader_viewport_index_layer = true;
			}
			//if(extName.equals("VK_EXT_blend_operation_advanced")) {
			//	enabledExtensions.add(extensionList.extensionName());
			//	VulkanUtility.LogInfo("Enabled Ext: EXT Advanced Blend Operations");
			//}
		}
		return VulkanUtility.toPointerBuffer(stack,enabledExtensions);
	}

}
