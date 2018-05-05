package net.openvoxel.vanilla.block;

import net.openvoxel.client.textureatlas.Icon;
import net.openvoxel.client.textureatlas.IconAtlas;
import net.openvoxel.common.block.Block;
import net.openvoxel.common.block.IBlockAccess;
import net.openvoxel.common.util.BlockFace;

/**
 * Created by James on 22/09/2016.
 */
public class BlockGrass extends Block {

	private Icon blockTex;

	@Override
	public void loadTextureAtlasData(IconAtlas texAtlas) {
		blockTex = texAtlas.register("block/grass/grass.diff","block/grass/grass.normals","block/grass/grass.pbr");
	}

	@Override
	public Icon getIconAtSide(IBlockAccess blockAccess, BlockFace face) {
		return blockTex;
	}
}
