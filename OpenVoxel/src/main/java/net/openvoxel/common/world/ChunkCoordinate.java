package net.openvoxel.common.world;

/**
 * Created by James on 25/08/2016.
 *
 * Loaded Chunk Coordinate
 */
public class ChunkCoordinate {

	public final int X;
	public final int Z;

	public ChunkCoordinate(int X, int Z) {
		this.X = X;
		this.Z = Z;
	}


	@Override
	public int hashCode() {
		return 31 * (31 + X) + Z;
	}

	@Override
	public boolean equals(Object obj) {
		if(!obj.getClass().equals(getClass())) return false;
		ChunkCoordinate coord = (ChunkCoordinate)obj;
		return coord.X == X && coord.Z == Z;
	}
}
