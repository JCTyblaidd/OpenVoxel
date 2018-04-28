package net.openvoxel.common.block;

import net.openvoxel.client.renderer.common.IBlockRenderHandler;
import net.openvoxel.client.textureatlas.Icon;
import net.openvoxel.client.textureatlas.IconAtlas;
import net.openvoxel.common.util.AABB;
import net.openvoxel.common.util.BlockFace;

/**
 * Created by James on 28/08/2016.
 *
 * The Only Constant Value Block That Exists
 */
public class BlockAir extends Block{

	public static final BlockAir BLOCK_AIR = new BlockAir();

	@Override
	public AABB getBlockBounds() {
		return EMPTY_BLOCK_AABB;
	}

	@Override
	public IBlockRenderHandler getRenderHandler() {
		return emptyRenderHandler;
	}

	@Override
	public void loadTextureAtlasData(IconAtlas texAtlas) {
		//Load Nothing At All//
	}

	@Override
	public Icon getIconAtSide(IBlockAccess blockAccess, BlockFace face) {
		return null;//NO ICON//
	}

	@Override
	public boolean hasTileEntity() {
		return false;
	}

	@Override
	public boolean isCompleteOpaque() {
		return false;
	}

	@Override
	public boolean isOpaque(BlockFace face) {
		return false;
	}
}
