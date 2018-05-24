package net.openvoxel.utility;

import org.joml.Math;
import org.joml.Matrix4fc;
import org.joml.Vector4f;

/**
 * Frustum Intersection Algorithm for Chunk Sections
 *
 *  Mostly for trying performance testing
 *
 *  Based on {@link org.joml.FrustumIntersection}
 */
public class FrustumTest {

	private float nxX, nxY, nxZ, nxW;
	private float pxX, pxY, pxZ, pxW;
	private float nyX, nyY, nyZ, nyW;
	private float pyX, pyY, pyZ, pyW;
	private float nzX, nzY, nzZ, nzW;
	private float pzX, pzY, pzZ, pzW;

	private final Vector4f[] planes = new Vector4f[6];
	{
		for (int i = 0; i < 6; i++) {
			planes[i] = new Vector4f();
		}
	}

	public FrustumTest set(Matrix4fc m) {
		return set(m, true);
	}

	public FrustumTest set(Matrix4fc m, boolean allowTestSpheres) {
		float invl;
		nxX = m.m03() + m.m00(); nxY = m.m13() + m.m10(); nxZ = m.m23() + m.m20(); nxW = m.m33() + m.m30();
		if (allowTestSpheres) {
			invl = (float) (1.0 / Math.sqrt(nxX * nxX + nxY * nxY + nxZ * nxZ));
			nxX *= invl; nxY *= invl; nxZ *= invl; nxW *= invl;
		}
		planes[0].set(nxX, nxY, nxZ, nxW);
		pxX = m.m03() - m.m00(); pxY = m.m13() - m.m10(); pxZ = m.m23() - m.m20(); pxW = m.m33() - m.m30();
		if (allowTestSpheres) {
			invl = (float) (1.0 / Math.sqrt(pxX * pxX + pxY * pxY + pxZ * pxZ));
			pxX *= invl; pxY *= invl; pxZ *= invl; pxW *= invl;
		}
		planes[1].set(pxX, pxY, pxZ, pxW);
		nyX = m.m03() + m.m01(); nyY = m.m13() + m.m11(); nyZ = m.m23() + m.m21(); nyW = m.m33() + m.m31();
		if (allowTestSpheres) {
			invl = (float) (1.0 / Math.sqrt(nyX * nyX + nyY * nyY + nyZ * nyZ));
			nyX *= invl; nyY *= invl; nyZ *= invl; nyW *= invl;
		}
		planes[2].set(nyX, nyY, nyZ, nyW);
		pyX = m.m03() - m.m01(); pyY = m.m13() - m.m11(); pyZ = m.m23() - m.m21(); pyW = m.m33() - m.m31();
		if (allowTestSpheres) {
			invl = (float) (1.0 / Math.sqrt(pyX * pyX + pyY * pyY + pyZ * pyZ));
			pyX *= invl; pyY *= invl; pyZ *= invl; pyW *= invl;
		}
		planes[3].set(pyX, pyY, pyZ, pyW);
		nzX = m.m03() + m.m02(); nzY = m.m13() + m.m12(); nzZ = m.m23() + m.m22(); nzW = m.m33() + m.m32();
		if (allowTestSpheres) {
			invl = (float) (1.0 / Math.sqrt(nzX * nzX + nzY * nzY + nzZ * nzZ));
			nzX *= invl; nzY *= invl; nzZ *= invl; nzW *= invl;
		}
		planes[4].set(nzX, nzY, nzZ, nzW);
		pzX = m.m03() - m.m02(); pzY = m.m13() - m.m12(); pzZ = m.m23() - m.m22(); pzW = m.m33() - m.m32();
		if (allowTestSpheres) {
			invl = (float) (1.0 / Math.sqrt(pzX * pzX + pzY * pzY + pzZ * pzZ));
			pzX *= invl; pzY *= invl; pzZ *= invl; pzW *= invl;
		}
		planes[5].set(pzX, pzY, pzZ, pzW);
		return this;
	}


	public boolean testAabBase(int minX, int  minY, int minZ, int maxX, int maxY, int maxZ) {
		return nxX * (nxX < 0 ? minX : maxX) + nxY * (nxY < 0 ? minY : maxY) + nxZ * (nxZ < 0 ? minZ : maxZ) >= -nxW &&
				       pxX * (pxX < 0 ? minX : maxX) + pxY * (pxY < 0 ? minY : maxY) + pxZ * (pxZ < 0 ? minZ : maxZ) >= -pxW &&
				       nyX * (nyX < 0 ? minX : maxX) + nyY * (nyY < 0 ? minY : maxY) + nyZ * (nyZ < 0 ? minZ : maxZ) >= -nyW &&
				       pyX * (pyX < 0 ? minX : maxX) + pyY * (pyY < 0 ? minY : maxY) + pyZ * (pyZ < 0 ? minZ : maxZ) >= -pyW &&
				       nzX * (nzX < 0 ? minX : maxX) + nzY * (nzY < 0 ? minY : maxY) + nzZ * (nzZ < 0 ? minZ : maxZ) >= -nzW &&
				       pzX * (pzX < 0 ? minX : maxX) + pzY * (pzY < 0 ? minY : maxY) + pzZ * (pzZ < 0 ? minZ : maxZ) >= -pzW;
	}


	public boolean testAabNoBranch(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {

		/*
		 * This is an implementation of the "2.4 Basic intersection test" of the mentioned site.
		 * It does not distinguish between partially inside and fully inside, though, so the test with the 'p' vertex is omitted.
		 */
		return nxX * (nxX < 0 ? minX : maxX) + nxY * (nxY < 0 ? minY : maxY) + nxZ * (nxZ < 0 ? minZ : maxZ) >= -nxW &
				       pxX * (pxX < 0 ? minX : maxX) + pxY * (pxY < 0 ? minY : maxY) + pxZ * (pxZ < 0 ? minZ : maxZ) >= -pxW &
				       nyX * (nyX < 0 ? minX : maxX) + nyY * (nyY < 0 ? minY : maxY) + nyZ * (nyZ < 0 ? minZ : maxZ) >= -nyW &
				       pyX * (pyX < 0 ? minX : maxX) + pyY * (pyY < 0 ? minY : maxY) + pyZ * (pyZ < 0 ? minZ : maxZ) >= -pyW &
				       nzX * (nzX < 0 ? minX : maxX) + nzY * (nzY < 0 ? minY : maxY) + nzZ * (nzZ < 0 ? minZ : maxZ) >= -nzW &
				       pzX * (pzX < 0 ? minX : maxX) + pzY * (pzY < 0 ? minY : maxY) + pzZ * (pzZ < 0 ? minZ : maxZ) >= -pzW;
	}

	public boolean testSphereBase(float x, float y, float z, float r) {
		return nxX * x + nxY * y + nxZ * z + nxW >= -r &&
				       pxX * x + pxY * y + pxZ * z + pxW >= -r &&
				       nyX * x + nyY * y + nyZ * z + nyW >= -r &&
				       pyX * x + pyY * y + pyZ * z + pyW >= -r &&
				       nzX * x + nzY * y + nzZ * z + nzW >= -r &&
				       pzX * x + pzY * y + pzZ * z + pzW >= -r;
	}

	public boolean testSphereNoBranch(float x, float y, float z, float r) {
		return nxX * x + nxY * y + nxZ * z + nxW >= -r &&
				       pxX * x + pxY * y + pxZ * z + pxW >= -r &
				       nyX * x + nyY * y + nyZ * z + nyW >= -r &
				       pyX * x + pyY * y + pyZ * z + pyW >= -r &
				       nzX * x + nzY * y + nzZ * z + nzW >= -r &
				       pzX * x + pzY * y + pzZ * z + pzW >= -r;
	}

}
