package net.openvoxel.client.utility;

import org.joml.FrustumIntersection;
import org.joml.Matrix4f;

/**
 * Created by James on 10/04/2017.
 *
 * Utility Class For Projection View Frustum Culling,
 *
 * Includes Gradual Reduction Culling
 */
public class FrustumCuller {

	private FrustumIntersection intersection;
	private Matrix4f projectionViewMatrix;
	public FrustumCuller() {
		intersection = new FrustumIntersection();
		projectionViewMatrix = new Matrix4f();
	}

	public void updateFrustum(Matrix4f projectionViewMatrix) {
		this.projectionViewMatrix.set(projectionViewMatrix);
		intersection.set(this.projectionViewMatrix);
	}

	public boolean chunkSectionCollides(int X, int Y, int Z) {
		float xReal = X * 16.0F;
		float yReal = Y * 16.0F;
		float zReal = Z * 16.0F;
		return intersection.testAab(xReal,yReal,zReal,xReal+16.0F,yReal+16.0F,zReal+16.0F);
	}

	public boolean chunkHalfSectionCollides(int X, int Ys, int Z) {
		float xReal = X * 16.0F;
		float zReal = Z * 16.0F;
		return intersection.testAab(xReal,Ys,zReal,xReal+16.0F,Ys+128.0F,zReal+16.0F);
	}

	public boolean chunkCollides(int X, int Z) {
		float xReal = X * 16.0F;
		float zReal = Z * 16.0F;
		return intersection.testAab(xReal,0.F,zReal,xReal+16.0F,256.0F,zReal+16.0F);
	}

	public boolean chunk2x2Collides(int X, int Z) {
		float xReal = X * 16.0F;
		float zReal = Z * 16.0F;
		return intersection.testAab(xReal,0.F,zReal,xReal+32.0F,256.0F,zReal+32.0F);
	}

	public boolean chunk4x4Collides(int X, int Z) {
		float xReal = X * 16.0F;
		float zReal = Z * 16.0F;
		return intersection.testAab(xReal,0.F,zReal,xReal+64.0F,256.0F,zReal+64.0F);
	}

	public boolean chunk8x8Collides(int X, int Z) {
		float xReal = X * 16.0F;
		float zReal = Z * 16.0F;
		return intersection.testAab(xReal,0.F,zReal,xReal+128.0F,256.0F,zReal+128.0F);
	}
}
