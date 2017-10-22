package net.openvoxel.client.renderer.vk;

/**
 * Constants that determine how parts of the vulkan renderer is implemented
 */
class VkImplFlags {

	/**
	 * Use coherent memory (mapped and unmapped every draw)
	 * Recommended: False
	 */
	static boolean gui_use_coherent_memory() {
		return false;
	}

	/**
	 * Remove the need for copying data while using non-coherent memory
	 * instead writing it to permanent mapped memory
	 * Recommended: True
	 */
	static boolean gui_direct_to_non_coherent_memory() {
		return true;
	}

	/**
	 * Allow re-using draw calls with already existing data if no state has changed
	 * Recommended: True
	 */
	static boolean gui_allow_draw_caching() {
		return true;
	}
}
