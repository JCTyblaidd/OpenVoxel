package net.openvoxel.client.culling;

import net.openvoxel.common.util.AABB;

import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;

/**
 * Created by James on 04/09/2016.
 *
 * View Frustum, used for culling objects in the renderer
 */
public class Frustum {

	//X,Y,Z For Each Location
	public Vector3f point1,point2,point3,point4,point5,point6,point7,point8;

	//X,Y,Z = W For Each Plane
	public Vector4f planeBack,planeFront,planeTop,planeBottom,planeLeft,planeRight;

	public Frustum() {
		point1 = new Vector3f();
		point2 = new Vector3f();
		point3 = new Vector3f();
		point4 = new Vector3f();
		point5 = new Vector3f();
		point6 = new Vector3f();
		point7 = new Vector3f();
		point8 = new Vector3f();
		planeBack = new Vector4f();
		planeFront = new Vector4f();
		planeTop = new Vector4f();
		planeBottom = new Vector4f();
		planeLeft = new Vector4f();
		planeRight = new Vector4f();
	}
	public void loadFromCamera(float fov, float aspect, float znear, float zfar, float x, float y, float z, float pitch, float yaw) {
		double dirX,dirY,dirZ;//Convert Direction Into Vector
		dirX = Math.cos(pitch) * Math.sin(yaw);
		dirY = Math.sin(pitch);
		dirZ = Math.cos(pitch) * Math.cos(yaw);

		//Start Calculating Points//

	}

	public void loadFromCamera(Matrix4f ProjMatrix, Matrix4f CameraMatrix) {

	}

	/**
	 * Return if AABB is inside the frustum
	 * @param aabb bouding box
	 * @return should be rendered
	 */
	public boolean isObjectInFrustum(AABB aabb) {
		return true;
	}

	/**
	 * Utility function that checks if it is behind the frustrum
	 * @param aabb
	 * @return
	 */
	public boolean isObjectBehindFrustum(AABB aabb) {
		return true;
	}
}
