package net.openvoxel.client.renderer.vk.util;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * Created by James on 14/04/2017.
 *
 * Manages Device Memory
 */
public class VkDeviceMemory {

	private VkDeviceState state;
	private VkPhysicalDeviceMemoryProperties properties;
	//private List<Object> bufferPages;
	//private List<Object> imagePages;


	private void allocImagePage() {
		try(MemoryStack stack = MemoryStack.stackPush()){

		}
	}

	private void allocBufferPage() {

	}
}
