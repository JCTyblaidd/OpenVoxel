package net.openvoxel.client.renderer;

import net.openvoxel.client.renderer.generic.WorldRenderer;
import net.openvoxel.common.block.IBlockAccess;

/**
 * Created by James on 28/08/2016.
 *
 * Block Render Data Writer
 */
public interface IBlockRenderHandler {

	/**
	 * Write Block Data [Updates Will Be Called if: 1.Chunk Updated, 2.Nearby Chunk Updated next to a block in this chunk that has the update flag enabled todo: add flag
	 * @param renderer World Renderer Reference
	 * @param stateAccess Block Information Accessor
	 */
	void storeBlockData(WorldRenderer.WorldBlockRenderer renderer, IBlockAccess stateAccess);

}
