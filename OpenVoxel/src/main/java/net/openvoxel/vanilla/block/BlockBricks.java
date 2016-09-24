package net.openvoxel.vanilla.block;

import net.openvoxel.client.textureatlas.Icon;
import net.openvoxel.client.textureatlas.IconAtlas;
import net.openvoxel.common.block.Block;
import net.openvoxel.common.block.IBlockAccess;
import net.openvoxel.common.util.BlockFace;

/**
 * Created by James on 24/09/2016.
 *
 * Brick Block
 */
public class BlockBricks extends Block{

	private Icon icon;

	@Override
	public void loadTextureAtlasData(IconAtlas texAtlas) {
		icon = texAtlas.register("block/brick.diff.png","block/brick.normals.png","block/brick.pbr.png");
	}

	@Override
	public Icon getIconAtSide(IBlockAccess blockAccess, BlockFace face) {
		return icon;
	}
}
