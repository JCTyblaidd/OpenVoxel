package net.openvoxel.world.generation.util;

import org.lwjgl.stb.STBPerlin;

import java.util.Random;

/**
 * Created by James on 10/04/2017.
 *
 * Random Noise Generation
 */
public class SeededPerlinNoise {

	private float dX, dY, dZ;

	public SeededPerlinNoise(long seed,int seedDiff) {
		Random random = new Random(seed);
		random.nextBytes(new byte[seedDiff]);
		dX = random.nextFloat();
		dY = random.nextFloat();
		dZ = random.nextFloat();
	}

	public float getNoiseAt(float x, float y, float z) {
		return STBPerlin.stb_perlin_noise3(x+dX,y+dY,z+dZ,0,0,0);
	}
}
