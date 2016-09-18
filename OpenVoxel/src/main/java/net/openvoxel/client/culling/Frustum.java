package net.openvoxel.client.culling;

import net.openvoxel.common.util.AABB;

import javax.vecmath.Vector3f;

/**
 * Created by James on 04/09/2016.
 *
 * View Frustum
 */
public class Frustum {

	public Vector3f point1;
	public Vector3f point2;
	public Vector3f point3;
	public Vector3f point4;
	public Vector3f point5;
	public Vector3f point6;
	public Vector3f point7;
	public Vector3f point8;


	public Vector3f planeBack;
	public Vector3f planeFront;
	public Vector3f planeTop;
	public Vector3f planeBottom;
	public Vector3f planeLeft;
	public Vector3f planeRight;

	public Frustum() {

	}
	public void loadFromCamera(float fov, float aspect, float znear, float zfar, float x, float y, float z, float pitch, float yaw) {

	}

	public boolean isObjectInFrustum(AABB aabb) {
		return true;
	}

	public boolean isObjectBehindFrustum(AABB aabb) {
		return true;
	}
}
