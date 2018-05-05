package net.openvoxel.utility;

public class MathUtilities {

	public static int roundUpToNearestPowerOf2(int x) {
		x = x - 1;
		x |= x >> 1;
		x |= x >> 2;
		x |= x >> 4;
		x |= x >> 8;
		x |= x >> 16;
		return x + 1;
	}

}