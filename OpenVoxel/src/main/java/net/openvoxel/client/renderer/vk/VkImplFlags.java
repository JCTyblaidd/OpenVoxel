package net.openvoxel.client.renderer.vk;

import net.openvoxel.OpenVoxel;

/**
 * Constants that determine how parts of the vulkan renderer is implemented
 */
public class VkImplFlags {


	/**
	 * Wait on fences when needed, instead of using queueWaitIdle after every draw call
	 * Recommended: True {Can cause issues with validation layers due to bug?}
	 */
	public static boolean renderer_use_delayed_fence_waiting() {
		//TODO: enable after validation layer is fixed
		return false;
	}

	/**
	 * Use coherent memory (mapped and unmapped every draw)
	 * Recommended: False
	 */
	static boolean gui_use_coherent_memory() {
		return OpenVoxel.getLaunchParameters().hasFlag("vkGuiUseCoherentMemory");
	}

	/**
	 * Remove the need for copying data while using non-coherent memory
	 * instead writing it to permanent mapped memory
	 * Recommended: True
	 */
	static boolean gui_direct_to_non_coherent_memory() {
		return !OpenVoxel.getLaunchParameters().hasFlag("vkGuiUseIndirectMemoryWrite");
	}

	/**
	 * Allow re-using draw calls with already existing data if no state has changed
	 * Recommended: True
	 */
	static boolean gui_allow_draw_caching() {
		return !OpenVoxel.getLaunchParameters().hasFlag("vkGuiDisableDrawCaching");
	}
}
