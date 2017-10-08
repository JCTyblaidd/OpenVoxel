package net.openvoxel.client.renderer.vk;

import net.openvoxel.client.renderer.generic.WorldRenderer;
import net.openvoxel.common.entity.living.player.EntityPlayerSP;
import net.openvoxel.world.client.ClientChunk;
import net.openvoxel.world.client.ClientWorld;

/**
 * Created by James on 17/04/2017.
 *
 * Vulkan World Renderer
 */
public class VkWorldRenderer implements WorldRenderer{
	@Override
	public void renderWorld(EntityPlayerSP playerSP, ClientWorld worldSP) {

	}

	@Override
	public void onChunkLoaded(ClientChunk chunk) {

	}

	@Override
	public void onChunkDirty(ClientChunk chunk) {

	}

	@Override
	public void onChunkUnloaded(ClientChunk chunk) {

	}
}
