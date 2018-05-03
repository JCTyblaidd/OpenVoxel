package net.openvoxel.client.renderer.vk.world;

import net.openvoxel.client.renderer.vk.core.VulkanDevice;
import net.openvoxel.client.renderer.vk.core.VulkanMemory;

/**
 * Manages Memory for World Chunks
 *  With Each stored block having a state:
 *   UP_TO_DATE
 *   OUT_OF_DATE [int countdown]
 *   FREE
 *
 *  Page Types [HOST,DEVICE]
 *  Transfer Between (Bidirectional...)
 */
public class VulkanWorldMemoryManager {

	private VulkanWorldMemoryPage pageDeviceLocal;
	private VulkanWorldMemoryPage pageHostVisible;
	private VulkanMemory memory;

	public VulkanWorldMemoryManager(VulkanDevice device, VulkanMemory memory) {
		this.memory = memory;
		pageDeviceLocal = new VulkanWorldMemoryPage(device,memory,true);
		pageHostVisible = new VulkanWorldMemoryPage(device,memory,false);
	}
	



}
