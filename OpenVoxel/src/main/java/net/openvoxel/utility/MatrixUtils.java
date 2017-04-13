package net.openvoxel.utility;


import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

/**
 * Created by James on 10/04/2017.
 *
 * Matrix Data Generation Utilities
 */
public class MatrixUtils {


	private static final Matrix4f projectionMatrix = new Matrix4f();
	public static Matrix4f genProjectionMatrix(float FoV, float aspectRatio, Vector2f zLimits) {
		projectionMatrix.identity().perspective(FoV,aspectRatio,zLimits.x,zLimits.y);
		return projectionMatrix;
	}

	private static final Matrix4f cameraMatrix = new Matrix4f();
	public static Matrix4f genCameraMatrix(Vector3f position, Matrix3f caNormMatrix) {
		return cameraMatrix.identity().set(caNormMatrix).translate(position.negate());
	}

	private static final Matrix3f cameraNormalMatrix = new Matrix3f();
	public static Matrix3f genCameraNormalMatrix(float pitch, float yaw) {
		return cameraNormalMatrix.identity().rotateX(pitch).rotateY(yaw);
	}

	private static final Matrix4f chunkPositionMatrix = new Matrix4f();
	public static Matrix4f genChunkPositionMatrix(float X, float Y, float Z) {
		return chunkPositionMatrix.identity().translate(X,Y,Z);
	}


	private static final Matrix4f projectionViewMatrix = new Matrix4f();
	public static Matrix4f getLastProjectionViewMatrix() {
		//projectionMatrix.set(cameraMatrix).mul(projectionMatrix);
		projectionViewMatrix.set(projectionMatrix).mul(cameraMatrix);
		return projectionViewMatrix;
	}
}
