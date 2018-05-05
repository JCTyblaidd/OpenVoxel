package net.openvoxel.common.block;

import net.openvoxel.common.util.BlockFace;

/**
 * Created by James on 28/08/2016.
 *
 * Block Access Interface
 */
public interface IBlockAccess {

	int getBlockID();
	byte getBlockMetaData();
	Block getBlock();

	boolean blockLoaded();

	long getX();
	long getY();
	long getZ();

	IBlockAccess getOffsetBlockData(BlockFace face);

}