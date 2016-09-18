package net.openvoxel.client.renderer.vk.util;

import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkInstance;

/**
 * Created by James on 03/09/2016.
 */
public class VkDeviceState {

	public static VkDevice deviceWorld;
	public static VkDevice deviceGUI;
	public static boolean areDifferent;

	public static VkInstance instance;

	public static void loadVulkan() {

	}

}
