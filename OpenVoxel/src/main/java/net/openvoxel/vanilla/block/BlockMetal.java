package net.openvoxel.vanilla.block;

import net.openvoxel.client.textureatlas.Icon;
import net.openvoxel.client.textureatlas.IconAtlas;
import net.openvoxel.common.block.Block;
import net.openvoxel.common.block.IBlockAccess;
import net.openvoxel.common.util.BlockFace;

/**
 * Created by James on 01/09/2016.
 */
public class BlockMetal extends Block{
	@Override
	public void loadTextureAtlasData(IconAtlas texAtlas) {

	}

	@Override
	public Icon getIconAtSide(IBlockAccess blockAccess, BlockFace face) {
		return null;
	}
}
