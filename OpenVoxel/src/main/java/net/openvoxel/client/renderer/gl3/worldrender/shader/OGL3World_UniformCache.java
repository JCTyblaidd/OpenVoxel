package net.openvoxel.client.renderer.gl3.worldrender.shader;

import net.openvoxel.client.renderer.gl3.OGL3Renderer;
import net.openvoxel.client.renderer.gl3.util.shader.STD140Layout;
import net.openvoxel.utility.MatrixUtils;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

import static net.openvoxel.client.renderer.gl3.util.shader.STD140Layout.LayoutType.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.glBindBufferBase;
import static org.lwjgl.opengl.GL31.GL_UNIFORM_BUFFER;

/**
 * Created by James on 28/09/2016.
 *
 * Uniform Buffer Information
 */
public class OGL3World_UniformCache {

	private static void storeMat4(ByteBuffer buffer,int offset,Matrix4f matrix4f) {
		matrix4f.get(offset,buffer);
	}

	private static void storeMat3(ByteBuffer buffer,int offset,Matrix3f matrix3f) {
		buffer.putFloat(offset,matrix3f.m00);
		buffer.putFloat(offset+4,matrix3f.m01);
		buffer.putFloat(offset+8,matrix3f.m02);
		buffer.putFloat(offset+16,matrix3f.m10);
		buffer.putFloat(offset+20,matrix3f.m11);
		buffer.putFloat(offset+24,matrix3f.m12);
		buffer.putFloat(offset+32,matrix3f.m20);
		buffer.putFloat(offset+36,matrix3f.m21);
		buffer.putFloat(offset+40,matrix3f.m22);
	}

	private static void storeVec3(ByteBuffer buffer,int offset,Vector3f vector3f) {
		buffer.putFloat(offset,vector3f.x);
		buffer.putFloat(offset+4,vector3f.y);
		buffer.putFloat(offset+8,vector3f.z);
	}

	private static void storeVec2(ByteBuffer buffer,int offset,Vector2f vector2f) {
		buffer.putFloat(offset,vector2f.x);
		buffer.putFloat(offset+4,vector2f.y);
	}

	//Buffer References//
	private static int UBO_Settings, UBO_FinalFrame, UBO_ChunkConstants, UBO_ShadowInfo, UBO_VoxelInfo;
	//Data Allocation//
	private static ByteBuffer buf_settings, buf_final_frame, buf_chunk_constants, buf_shadow_info, buf_voxel_info;

	//Offset Information//
	private static final int offsetFrame_AnimIndex;
	private static final int offsetFrame_WorldTick;
	private static final int offsetFrame_ProjMatrix ;
	private static final int offsetFrame_InverseProjMatrix;
	private static final int offsetFrame_ZLimits;
	private static final int offsetFrame_CamMatrix;
	private static final int offsetFrame_CamNormMatrix;
	private static final int offsetFrame_InvCamNormMatrix;
	private static final int offsetFrame_PlayerPosition;
	private static final int offsetFrame_DayProgress;
	private static final int offsetFrame_DayProgressMatrix;
	private static final int offsetFrame_SunlightPower;
	private static final int offsetFrame_DirSun;
	private static final int offsetFrame_SkyEnabled;
	private static final int offsetFrame_FogColour;
	private static final int offsetFrame_SkyLightColour;
	private static final int offsetFrame_IsRaining;
	private static final int offsetFrame_IsThunder;
	private static final int offsetFrame_TileSize;
	private static final int offsetFrame_SIZE;

	private static final int offsetChunk_ChunkMatrix;
	private static final int offsetChunk_SIZE;

	private static final int offsetShadow_Mat1;
	private static final int offsetShadow_Mat2;
	private static final int offsetShadow_Mat3;
	private static final int offsetShadow_SIZE;

	private static final int offsetVoxel_MinVoxel;
	private static final int offsetVoxel_SizeVoxel;
	private static final int offsetVoxel_SIZE;

	static {
		STD140Layout layoutFrameData = new STD140Layout(//TODO: find out the issue with the layout
			INT,    //animIndex;
			INT,    //worldTick;
			MAT4,   //projMatrix;
			MAT4,   //invProjMatrix;
			VEC2,   //zLimits;
			MAT4,   //camMatrix;
			MAT3,   //camNormMatrix;
			MAT3,   //invCamNormMatrix;
		    VEC3,   //Player position
			FLOAT,  //dayProgress;
			FLOAT,  //skyLightPower;
			MAT3,   //dayProgressMatrix;
			VEC3,   //dirSun;
			BOOL,   //skyEnabled;
			VEC3,   //fogColour;
			VEC3,   //skyLightColour;
			BOOL,   //isRaining;
			BOOL,   //isThunder;
			VEC2    //tileSize;
		);
		offsetFrame_AnimIndex           = layoutFrameData.getOffset(0);
		offsetFrame_WorldTick           = layoutFrameData.getOffset(1);
		offsetFrame_ProjMatrix          = layoutFrameData.getOffset(2);
		offsetFrame_InverseProjMatrix   = layoutFrameData.getOffset(3);
		offsetFrame_ZLimits             = layoutFrameData.getOffset(4);
		offsetFrame_CamMatrix           = layoutFrameData.getOffset(5);
		offsetFrame_CamNormMatrix       = layoutFrameData.getOffset(6);
		offsetFrame_InvCamNormMatrix    = layoutFrameData.getOffset(7);
		offsetFrame_PlayerPosition      = layoutFrameData.getOffset(8);
		offsetFrame_DayProgress         = layoutFrameData.getOffset(9);
		offsetFrame_SunlightPower       = layoutFrameData.getOffset(10);
		offsetFrame_DayProgressMatrix   = layoutFrameData.getOffset(11);
		offsetFrame_DirSun              = layoutFrameData.getOffset(12);
		offsetFrame_SkyEnabled          = layoutFrameData.getOffset(13);
		offsetFrame_FogColour           = layoutFrameData.getOffset(14);
		offsetFrame_SkyLightColour      = layoutFrameData.getOffset(15);
		offsetFrame_IsRaining           = layoutFrameData.getOffset(16);
		offsetFrame_IsThunder           = layoutFrameData.getOffset(17);
		offsetFrame_TileSize            = layoutFrameData.getOffset(18);
		offsetFrame_SIZE = layoutFrameData.getTotalSize();

		STD140Layout layoutChunkData = new STD140Layout(
			MAT4
		);

		offsetChunk_ChunkMatrix = layoutChunkData.getOffset(0);
		offsetChunk_SIZE = layoutChunkData.getTotalSize();


		STD140Layout layoutShadowData = new STD140Layout(
			MAT4,
			MAT4,
			MAT4
		);

		offsetShadow_Mat1 = layoutShadowData.getOffset(0);
		offsetShadow_Mat2 = layoutShadowData.getOffset(1);
		offsetShadow_Mat3 = layoutShadowData.getOffset(2);
		offsetShadow_SIZE = layoutShadowData.getTotalSize();

		STD140Layout layoutVoxelData = new STD140Layout(
			VEC3,
            VEC3
		);

		offsetVoxel_MinVoxel            = layoutVoxelData.getOffset(0);
		offsetVoxel_SizeVoxel           = layoutVoxelData.getOffset(1);
		offsetVoxel_SIZE                = layoutVoxelData.getTotalSize();
	}

	/**
	 * Generate and update the required uniform information
	 */
	public static void calcAndUpdateFrameInformation(int animCounter,float FoV,Vector2f zLims,float aspectRatio,
	                                                 Vector3f cameraPos, float yaw, float pitch, float dayProgress,
	                                                 float skylightPower,Vector3f skyLightColour, boolean skyEnabled,
	                                                 Vector3f fogColour, boolean isRaining, boolean isThunder,Vector2f tileSize) {
		Matrix4f projMatrix = MatrixUtils.genProjectionMatrix(FoV,aspectRatio,zLims);
		Matrix3f camNormMatrix = MatrixUtils.genCameraNormalMatrix(pitch,yaw);
		Matrix4f camMatrix = MatrixUtils.genCameraMatrix(cameraPos,camNormMatrix);
		cameraPos.negate();//Undo Camera Position Negation
		int worldTick = animCounter % 128;
		Matrix3f dayProgressMatrix = new Matrix3f();
		Vector3f dirSun = new Vector3f(1,0,0);
		updateFrameInformation(animCounter,worldTick,projMatrix,zLims,camMatrix,camNormMatrix,cameraPos,dayProgress,
				skylightPower,dayProgressMatrix,dirSun,skyEnabled,fogColour,skyLightColour,isRaining,isThunder,tileSize);
	}

	/**
	 * Update data
	 * Important Usage Notice all matrix values could be inverted and changed
	 */
	private static void updateFrameInformation(int animIndex, int worldTick, Matrix4f projMatrix,Vector2f zLimits,
	                                          Matrix4f camMatrix, Matrix3f camNormMatrix,Vector3f playerPosition, float dayProgress,
	                                          float skylightPower, Matrix3f dayProgressMatrix,Vector3f dirSun,
	                                          boolean skyEnabled, Vector3f fogColour,Vector3f skyLightColour,
	                                          boolean isRaining,boolean isThunder,Vector2f tileSize) {
		buf_final_frame.putInt(offsetFrame_AnimIndex,animIndex);
		buf_final_frame.putInt(offsetFrame_WorldTick,worldTick);//TODO: fix matrix inverts
		storeMat4(buf_final_frame,offsetFrame_ProjMatrix,projMatrix);// projMatrix.invert();
		storeMat4(buf_final_frame,offsetFrame_InverseProjMatrix,projMatrix);
		storeVec2(buf_final_frame,offsetFrame_ZLimits,zLimits);
		storeMat4(buf_final_frame,offsetFrame_CamMatrix,camMatrix);
		storeMat3(buf_final_frame,offsetFrame_CamNormMatrix,camNormMatrix);// camNormMatrix.invert();
		storeMat3(buf_final_frame,offsetFrame_InvCamNormMatrix,camNormMatrix);
		storeVec3(buf_final_frame,offsetFrame_PlayerPosition,playerPosition);
		buf_final_frame.putFloat(offsetFrame_DayProgress,dayProgress);
		buf_final_frame.putFloat(offsetFrame_SunlightPower,skylightPower);
		storeMat3(buf_final_frame,offsetFrame_DayProgressMatrix,dayProgressMatrix);
		storeVec3(buf_final_frame,offsetFrame_DirSun,dirSun);
		buf_final_frame.put(offsetFrame_SkyEnabled,(byte)(skyEnabled?1:0));
		storeVec3(buf_final_frame,offsetFrame_FogColour,fogColour);
		storeVec3(buf_final_frame,offsetFrame_SkyLightColour,skyLightColour);
		buf_final_frame.put(offsetFrame_IsRaining,(byte)(isRaining?1:0));
		buf_final_frame.put(offsetFrame_IsThunder,(byte)(isThunder?1:0));
		storeVec2(buf_final_frame,offsetFrame_TileSize,tileSize);
		updateFinalFrame();
	}

	public static void Load() {
		int[] arr = new int[5];
		glGenBuffers(arr);
		UBO_Settings = arr[0];
		UBO_FinalFrame = arr[1];
		UBO_ChunkConstants = arr[2];
		UBO_ShadowInfo = arr[3];
		UBO_VoxelInfo = arr[4];
		buf_settings = MemoryUtil.memAlloc(15);
		buf_final_frame = MemoryUtil.memAlloc(offsetFrame_SIZE);
		buf_chunk_constants = MemoryUtil.memAlloc(offsetChunk_SIZE);
		buf_shadow_info = MemoryUtil.memAlloc(offsetShadow_SIZE);
		buf_voxel_info = MemoryUtil.memAlloc(offsetVoxel_SIZE);
		initSettings();
		initFinalFrame();
		initChunkConstants();
		initShadowInfo();
		initVoxelInfo();
	}

	public static void FreeMemory() {
		MemoryUtil.memFree(buf_settings);
		MemoryUtil.memFree(buf_final_frame);
		MemoryUtil.memFree(buf_chunk_constants);
		MemoryUtil.memFree(buf_shadow_info);
		MemoryUtil.memFree(buf_voxel_info);
	}

	private static void initSettings() {
		buf_settings.position(0);
		glBindBuffer(GL_UNIFORM_BUFFER,UBO_Settings);
		glBufferData(GL_UNIFORM_BUFFER,buf_settings,GL_DYNAMIC_DRAW);
		glBindBufferBase(GL_UNIFORM_BUFFER, OGL3Renderer.UniformBlockBinding_Settings,UBO_Settings);
	}

	private static void initFinalFrame() {
		buf_final_frame.position(0);
		glBindBuffer(GL_UNIFORM_BUFFER,UBO_FinalFrame);
		glBufferData(GL_UNIFORM_BUFFER, buf_final_frame,GL_DYNAMIC_DRAW);
		glBindBufferBase(GL_UNIFORM_BUFFER,OGL3Renderer.UniformBlockBinding_FrameInfo,UBO_FinalFrame);
	}

	private static void updateFinalFrame() {
		buf_final_frame.position(0);
		glBindBuffer(GL_UNIFORM_BUFFER,UBO_FinalFrame);
		glBufferSubData(GL_UNIFORM_BUFFER, 0,buf_final_frame);
	}

	private static void initChunkConstants() {
		buf_chunk_constants.position(0);
		glBindBuffer(GL_UNIFORM_BUFFER,UBO_ChunkConstants);
		glBufferData(GL_UNIFORM_BUFFER,buf_chunk_constants,GL_DYNAMIC_DRAW);
		glBindBufferBase(GL_UNIFORM_BUFFER, OGL3Renderer.UniformBlockBinding_ChunkInfo,UBO_ChunkConstants);
	}

	private static void updateChunkConstants() {
		buf_chunk_constants.position(0);
		glBindBuffer(GL_UNIFORM_BUFFER,UBO_ChunkConstants);
		glBufferSubData(GL_UNIFORM_BUFFER,0,buf_chunk_constants);
	}

	private static void initShadowInfo() {
		buf_shadow_info.position(0);
		glBindBuffer(GL_UNIFORM_BUFFER, UBO_ShadowInfo);
		glBufferData(GL_UNIFORM_BUFFER, buf_shadow_info,GL_DYNAMIC_DRAW);
		glBindBufferBase(GL_UNIFORM_BUFFER, OGL3Renderer.UniformBlockBinding_ShadowInfo,UBO_ShadowInfo);
	}

	private static void initVoxelInfo() {
		buf_voxel_info.position(0);
		glBindBuffer(GL_UNIFORM_BUFFER, UBO_VoxelInfo);
		glBufferData(GL_UNIFORM_BUFFER, buf_voxel_info,GL_DYNAMIC_DRAW);
		glBindBufferBase(GL_UNIFORM_BUFFER, OGL3Renderer.UniformBlockBinding_VoxelInfo,UBO_VoxelInfo);
	}

	public static void setChunkUniform(Matrix4f matrix4f) {
		storeMat4(buf_chunk_constants,offsetChunk_ChunkMatrix,matrix4f);
		updateChunkConstants();
	}
}
