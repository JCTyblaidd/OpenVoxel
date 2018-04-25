package net.openvoxel.client.renderer.vk.world;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.nio.ByteBuffer;

public class VkWorldDescriptorManager {

	private static final int MAT4_OFFSET_SHADOW_MAPS_1 = 0;
	private static final int MAT4_OFFSET_SHADOW_MAPS_2 = 64;
	private static final int MAT4_OFFSET_SHADOW_MAPS_3 = 128;
	private static final int MAT4_OFFSET_PROJ_MATRIX = 192;
	private static final int MAT4_OFFSET_PROJ_MATRIX_INV = 256;
	private static final int MAT4_OFFSET_PLAYER_POS_MATRIX = 320;
	private static final int MAT3_OFFSET_PLAYER_NORM_MATRIX = 0;
	private static final int MAT3_OFFSET_PLAYER_NORM_INV_MATRIX = 0;
	private static final int VEC3_OFFSET_PLAYER_POS = 0;
	private static final int VEC2_OFFSET_Z_LIMITS = 0;
	private static final int MAT3_OFFSET_DAY_PROGRESS_MATRIX = 0;
	private static final int VEC3_OFFSET_DIR_SUN = 0;
	private static final int VEC3_OFFSET_FOG_COLOUR = 0;
	private static final int VEC3_OFFSET_SKYLIGHT_COLOUR = 0;
	private static final int FLOAT_OFFSET_DAY_PROGRESS = 0;
	private static final int FLOAT_OFFSET_SKY_LIGHT_POWER = 0;
	private static final int BOOL_OFFSET_IS_RAINING = 0;
	private static final int BOOL_OFFSET_IS_THUNDER = 0;
	private static final int BOOL_OFFSET_IS_PLAYER_IN_FLUID = 0;

	public static final int DESCRIPTOR_SET_SIZE = 0;


	private static Matrix4f tempMatrix4 = new Matrix4f();
	private static Matrix3f tempMatrix3 = new Matrix3f();
	public static void storeDescriptorInfo(ByteBuffer target, Matrix4f shadow1, Matrix4f shadow2, Matrix4f shadow3,
	                                       Vector3f playerPos, float playerFoV, float aspectRatio,Vector2f zLimits,
	                                       float playerYaw, float playerPitch, float dayProgress,Vector3f fogColour,
	                                       Vector3f skylightColour, float lightPower,
	                                       boolean isRaining, boolean isThunder, boolean isPlayerInFluid) {
		{
			target.position(MAT4_OFFSET_SHADOW_MAPS_1);
			shadow1.get(target);
			target.position(MAT4_OFFSET_SHADOW_MAPS_2);
			shadow2.get(target);
			target.position(MAT4_OFFSET_SHADOW_MAPS_3);
			shadow3.get(target);
		}
		{
			tempMatrix4.identity();
			tempMatrix4.perspective(playerFoV,aspectRatio,zLimits.x,zLimits.y);
			target.position(MAT4_OFFSET_PROJ_MATRIX);
			tempMatrix4.get(target);
			tempMatrix4.invert();
			target.position(MAT4_OFFSET_PROJ_MATRIX_INV);
			tempMatrix4.get(target);
			target.position(VEC2_OFFSET_Z_LIMITS);
			zLimits.get(target);
		}
		{
			target.position(VEC3_OFFSET_PLAYER_POS);
			playerPos.get(target);
			tempMatrix4.identity();
			tempMatrix3.identity();
			tempMatrix3.rotateX(playerYaw);
			tempMatrix4.rotateX(playerYaw);
			tempMatrix3.rotateY(playerPitch);
			tempMatrix4.rotateY(playerPitch);
			tempMatrix4.translate(playerPos.negate());
			target.position(MAT4_OFFSET_PLAYER_POS_MATRIX);
			tempMatrix4.get(target);
			target.position(MAT3_OFFSET_PLAYER_NORM_MATRIX);
			tempMatrix3.get(target);
			tempMatrix3.invert();
			target.position(MAT3_OFFSET_PLAYER_NORM_INV_MATRIX);
			tempMatrix3.get(target);
		}
		{
			tempMatrix3.identity();

		}
	}

}
