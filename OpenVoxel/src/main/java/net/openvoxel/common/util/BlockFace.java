package net.openvoxel.common.util;

import net.openvoxel.common.block.Block;
import org.jetbrains.annotations.NotNull;

/**
 * Created by James on 28/08/2016.
 *
 * Block Face Displacement Utility
 */
public enum BlockFace {
	UP   ( 0, 1, 0, 0),//Y+
	DOWN ( 0,-1, 0, 1),//Y-
	WEST ( 0, 0, 1, 2),//Z+
	EAST ( 0, 0,-1, 3),//Z-
	NORTH( 1, 0, 0, 4),//X+
	SOUTH(-1 ,0, 0, 5);//X-

	public final int xOffset;
	public final int yOffset;
	public final int zOffset;
	public final int faceID;

	@NotNull
	public static BlockFace getOppositeFace(@NotNull BlockFace blockFace) {
		switch (blockFace) {
			case DOWN:
				return UP;
			case UP:
				return DOWN;
			case EAST:
				return WEST;
			case WEST:
				return EAST;
			case NORTH:
				return SOUTH;
			case SOUTH:
				return NORTH;
			default:
				throw new NullPointerException();
		}
	}

	//Quick value getters for using indeces
	public static final int[] array_xOffsets;
	public static final int[] array_yOffsets;
	public static final int[] array_zOffsets;
	public static final int[] array_opposite;
	public static final int face_count = 6;
	static {
		array_xOffsets = new int[6];
		array_yOffsets = new int[6];
		array_zOffsets = new int[6];
		array_opposite = new int[6];
		for(BlockFace face : BlockFace.values()) {
			int id = face.faceID;
			array_xOffsets[id] = face.xOffset;
			array_yOffsets[id] = face.yOffset;
			array_zOffsets[id] = face.zOffset;
			array_opposite[id] = getOppositeFace(face).faceID;
		}
	}

	BlockFace(int x, int y, int z,int ID){
		xOffset=x;
		yOffset=y;
		zOffset=z;
		faceID =ID;
	}

}
