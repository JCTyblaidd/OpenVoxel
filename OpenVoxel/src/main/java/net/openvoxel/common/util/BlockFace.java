package net.openvoxel.common.util;

/**
 * Created by James on 28/08/2016.
 */
public enum BlockFace {
	UP(0,1,0),//Y+
	DOWN(0,-1,0),//Y-
	WEST(0,0,1),//Z+
	EAST(0,0,-1),//Z-
	NORTH(1,0,0),//X+
	SOUTH(-1,0,0);//X-

	public final int xOffset;
	public final int yOffset;
	public final int zOffset;

	public static BlockFace getOppositeFace(BlockFace blockFace) {
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
				return null;
		}
	}

	BlockFace(int x, int y, int z){
		xOffset=x;
		yOffset=y;
		zOffset=z;
	}

}
