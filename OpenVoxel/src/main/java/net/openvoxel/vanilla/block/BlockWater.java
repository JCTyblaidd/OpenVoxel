package net.openvoxel.vanilla.block;

import net.openvoxel.client.textureatlas.Icon;
import net.openvoxel.client.textureatlas.IconAtlas;
import net.openvoxel.common.block.Block;
import net.openvoxel.common.block.IBlockAccess;
import net.openvoxel.common.util.BlockFace;

public class BlockWater extends Block {

	private Icon blockTex;

	@Override
	public void loadTextureAtlasData(IconAtlas texAtlas) {
		blockTex = texAtlas.register("block/water/water.diff","block/water/water.normals","block/water/water.pbr");
	}

	@Override
	public Icon getIconAtSide(IBlockAccess blockAccess, BlockFace face) {
		return blockTex;
	}

	@Override
	public boolean isOpaque(BlockFace face) {
		return false;
	}

	@Override
	public boolean isCompleteOpaque() {
		return false;
	}

}
