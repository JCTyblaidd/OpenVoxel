package net.openvoxel.client.renderer.common;

import net.openvoxel.common.block.Block;
import net.openvoxel.common.block.IBlockAccess;
import net.openvoxel.common.util.AABB;

public class DefaultTransparentBlockRenderer extends DefaultBlockRenderer {

	@Override
	public void storeBlockData(IBlockRenderer renderer, IBlockAccess stateAccess, boolean opaqueDraw) {
		Block block = stateAccess.getBlock();

		//Skip Draw if not applicable//
		if(opaqueDraw) return;

		AABB bounds = block.getBlockBounds();
		renderUp(block,renderer,stateAccess,bounds);
		renderDown(block,renderer,stateAccess,bounds);
		renderWest(block,renderer,stateAccess,bounds);
		renderEast(block,renderer,stateAccess,bounds);
		renderNorth(block,renderer,stateAccess,bounds);
		renderSouth(block,renderer,stateAccess,bounds);
	}
}
