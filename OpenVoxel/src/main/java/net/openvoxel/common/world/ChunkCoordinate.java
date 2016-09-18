package net.openvoxel.common.world;

/**
 * Created by James on 25/08/2016.
 */
public class ChunkCoordinate {

	public int X;
	public int Z;

	public ChunkCoordinate(int X, int Z) {
		this.X = X;
		this.Z = Z;
	}


	@Override//// TODO: 25/08/2016 Check Hash Code Works
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
