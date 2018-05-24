package net.openvoxel.client.renderer.common;

import org.jetbrains.annotations.TestOnly;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class IBlockRendererTest {

	@Test
	void testQuaternionConstants() {
		Vector3f vectorIn = new Vector3f(0,0,1);
		Vector3f vectorOut = new Vector3f(0,0,0);

		IBlockRenderer.X_POSITIVE.transform(vectorIn,vectorOut);
		assertTrue(nearlyEqual(vectorOut,1,0,0));

		IBlockRenderer.X_NEGATIVE.transform(vectorIn,vectorOut);
		assertTrue(nearlyEqual(vectorOut,-1,0,0));

		IBlockRenderer.Y_POSITIVE.transform(vectorIn,vectorOut);
		assertTrue(nearlyEqual(vectorOut,0,1,0));

		IBlockRenderer.Y_NEGATIVE.transform(vectorIn,vectorOut);
		assertTrue(nearlyEqual(vectorOut,0,-1,0));

		IBlockRenderer.Z_POSITIVE.transform(vectorIn,vectorOut);
		assertTrue(nearlyEqual(vectorOut,0,0,1));

		IBlockRenderer.Z_NEGATIVE.transform(vectorIn,vectorOut);
		assertTrue(nearlyEqual(vectorOut,0,0,-1));
	}

	@TestOnly
	private boolean nearlyEqual(Vector3f vector,float x, float y, float z) {
		float errX = Math.abs(vector.x - x);
		float errY = Math.abs(vector.y - y);
		float errZ = Math.abs(vector.z - z);
		System.out.println(vector);
		return (errX + errY + errZ) < 0.0001F;
	}
}