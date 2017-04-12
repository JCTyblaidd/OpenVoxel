package net.openvoxel.vanilla.block;

import net.openvoxel.client.textureatlas.Icon;
import net.openvoxel.client.textureatlas.IconAtlas;
import net.openvoxel.common.block.Block;
import net.openvoxel.common.block.IBlockAccess;
import net.openvoxel.common.util.BlockFace;

/**
 * Created by James on 12/04/2017.
 *
 */
public class BlockStone extends Block {

	private Icon icon;

	@Override
	public void loadTextureAtlasData(IconAtlas texAtlas) {

	}

	@Override
	public Icon getIconAtSide(IBlockAccess blockAccess, BlockFace face) {
		return null;
	}
}
