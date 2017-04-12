package net.openvoxel.client.renderer.gl3.worldrender;

import net.openvoxel.client.renderer.gl3.OGL3WorldRenderer;
import net.openvoxel.client.renderer.gl3.worldrender.cache.OGL3RenderCache;
import net.openvoxel.client.renderer.gl3.worldrender.shader.OGL3World_ShaderCache;
import net.openvoxel.common.entity.living.player.EntityPlayerSP;
import net.openvoxel.world.client.ClientChunk;
import net.openvoxel.world.client.ClientChunkSection;
import net.openvoxel.world.client.ClientWorld;

import java.util.List;
import java.util.Set;

/**
 * Created by James on 10/04/2017.
 * Simple World Rendering Code
 */
public class OGL3ForwardWorldRenderer {

	private OGL3WorldRenderer worldRenderer;

	public OGL3ForwardWorldRenderer(OGL3WorldRenderer worldRenderer) {
		this.worldRenderer = worldRenderer;
	}

	public void renderWorld(EntityPlayerSP player, ClientWorld world, Set<ClientChunk> toRender) {
		OGL3World_ShaderCache.BLOCK_SIMPLE.use();
		for(ClientChunk chunk : toRender) {
			if(chunk != null) {
				for(int y = 0; y < 16; y++) {
					ClientChunkSection section = chunk.getSectionAt(y);
					if(section.renderCache.get() != null) {
						OGL3RenderCache cache = worldRenderer.cacheManager.loadRenderCache(section);
						if (cache.cacheExists()) {
							//Set Uniform Vertex//
							worldRenderer.setupCacheUniform(chunk,y);
							cache.draw();
						}
					}
				}
			}
		}
	}
}
