package net.openvoxel.client.renderer.vk.world.draw.snips;

import gnu.trove.list.TLongList;
import org.lwjgl.vulkan.VkCommandBuffer;

import java.io.Closeable;
import java.util.List;

/**
 * Snip of code for drawing a shadow map
 */
public class DrawShadowMap implements Closeable {

	private TLongList asyncShadowOpaqueCommandPool;
	private TLongList asyncShadowTransparentCommandPool;

	private List<VkCommandBuffer> asyncShadowOpaqueCommandBuffers;
	private List<VkCommandBuffer> asyncShadowTransparentCommandBuffers;

	public DrawShadowMap(int cascadeCount,int asyncCount) {

	}

	@Override
	public void close() {

	}
}
