package net.openvoxel.common.block;

import net.openvoxel.common.util.BlockFace;

/**
 * Created by James on 28/08/2016.
 */
public interface IBlockAccess {

	int getBlockID();
	byte getBlockMetaData();
	Block getBlock();

	boolean blockLoaded();

	int getX();
	int getY();
	int getZ();

	IBlockAccess getOffsetBlockData(BlockFace face);

}