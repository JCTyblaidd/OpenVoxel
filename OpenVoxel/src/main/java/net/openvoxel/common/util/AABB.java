package net.openvoxel.common.util;


import org.joml.Vector3d;

/**
 * Created by James on 25/08/2016.
 *
 * Axis Aligned Bounding Box
 */
public class AABB {

	public final double minX;
	public final double minY;
	public final double minZ;
	public final double maxX;
	public final double maxY;
	public final double maxZ;

	public AABB(double x1, double y1, double z1, double x2, double y2, double z2) {
		//Limit//
		this.minX = Math.min(x1, x2);
		this.minY = Math.min(y1, y2);
		this.minZ = Math.min(z1, z2);
		this.maxX = Math.max(x1, x2);
		this.maxY = Math.max(y1, y2);
		this.maxZ = Math.max(z1, z2);
	}
	//From Vectors//
	public AABB(Vector3d v1, Vector3d v2) {
		this(v1.x,v1.y,v1.z,v2.x,v2.y,v2.z);
	}

	public boolean collidesWith(AABB other) {
		return this.collidesWith(other.minX, other.minY, other.minZ, other.maxX, other.maxY, other.maxZ);
	}

	public boolean collidesWith(double x1, double y1, double z1, double x2, double y2, double z2) {
		return this.minX < x2 && this.maxX > x1 && this.minY < y2 && this.maxY > y1 && this.minZ < z2 && this.maxZ > z1;
	}

	public boolean collidesWith(Vector3d vec) {
		return vec.x > this.minX && vec.x < this.maxX ? (vec.y > this.minY && vec.y < this.maxY ? vec.z > this.minZ && vec.z < this.maxZ : false) : false;
	}

}
