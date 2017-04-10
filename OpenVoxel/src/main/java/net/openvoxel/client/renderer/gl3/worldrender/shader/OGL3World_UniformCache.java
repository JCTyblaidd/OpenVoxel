package net.openvoxel.client.renderer.gl3.worldrender.shader;

import net.openvoxel.client.renderer.gl3.OGL3Renderer;
import net.openvoxel.client.renderer.gl3.atlas.OGL3TextureAtlas;
import net.openvoxel.client.renderer.gl3.util.shader.STD140Layout;
import net.openvoxel.utility.MatrixUtils;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

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
	private static int UBO_Settings, UBO_FinalFrame, UBO_ChunkConstants;
	//Data Allocation//
	private static ByteBuffer buf_settings, buf_final_frame, buf_chunk_constants;

	//Offset Information//
	private static final int offsetFrame_AnimIndex;
	private static final int offsetFrame_WorldTick;
	private static final int offsetFrame_ProjMatrix ;
	private static final int offsetFrame_InverseProjMatrix;
	private static final int offsetFrame_ZLimits;
	private static final int offsetFrame_CamMatrix;
	private static final int offsetFrame_CamNormMatrix;
	private static final int offsetFrame_InvCamNormMatrix;
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

	private static final int offsetChunk_ChunkMatrix = 0;//Only Value//
	private static final int offsetChunk_SIZE = 64;

	static {
		STD140Layout layout = new STD140Layout(//TODO: find out the issue with the layout
			INT,    //animIndex;
			INT,    //worldTick;
			MAT4,   //projMatrix;
			MAT4,   //invProjMatrix;
			VEC2,   //zLimits;
			MAT4,   //camMatrix;
			MAT3,   //camNormMatrix;
			MAT3,   //invCamNormMatrix;
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
		offsetFrame_AnimIndex           = layout.getOffset(0);
		offsetFrame_WorldTick           = layout.getOffset(1);
		offsetFrame_ProjMatrix          = layout.getOffset(2);
		offsetFrame_InverseProjMatrix   = layout.getOffset(3);
		offsetFrame_ZLimits             = layout.getOffset(4);
		offsetFrame_CamMatrix           = layout.getOffset(5);
		offsetFrame_CamNormMatrix       = layout.getOffset(6);
		offsetFrame_InvCamNormMatrix    = layout.getOffset(7);
		offsetFrame_DayProgress         = layout.getOffset(8);
		offsetFrame_SunlightPower       = layout.getOffset(9);
		offsetFrame_DayProgressMatrix   = layout.getOffset(10);
		offsetFrame_DirSun              = layout.getOffset(11);
		offsetFrame_SkyEnabled          = layout.getOffset(12);
		offsetFrame_FogColour           = layout.getOffset(13);
		offsetFrame_SkyLightColour      = layout.getOffset(14);
		offsetFrame_IsRaining           = layout.getOffset(15);
		offsetFrame_IsThunder           = layout.getOffset(16);
		offsetFrame_TileSize            = layout.getOffset(17);
		offsetFrame_SIZE = layout.getTotalSize();
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
		int worldTick = animCounter % 128;
		Matrix3f dayProgressMatrix = new Matrix3f();
		Vector3f dirSun = new Vector3f(1,0,0);
		updateFrameInformation(animCounter,worldTick,projMatrix,zLims,camMatrix,camNormMatrix,dayProgress,
				skylightPower,dayProgressMatrix,dirSun,skyEnabled,fogColour,skyLightColour,isRaining,isThunder,tileSize);
	}

	/**
	 * Update data
	 * Important Usage Notice all matrix values could be inverted and changed
	 */
	private static void updateFrameInformation(int animIndex, int worldTick, Matrix4f projMatrix,Vector2f zLimits,
	                                          Matrix4f camMatrix, Matrix3f camNormMatrix, float dayProgress,
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
		int[] arr = new int[3];
		glGenBuffers(arr);
		UBO_Settings = arr[0];
		UBO_FinalFrame = arr[1];
		UBO_ChunkConstants = arr[2];
		buf_settings = BufferUtils.createByteBuffer(15);
		buf_final_frame = BufferUtils.createByteBuffer(offsetFrame_SIZE);
		buf_chunk_constants = BufferUtils.createByteBuffer(offsetChunk_SIZE);
		updateSettings();
		updateFinalFrame();
		updateChunkConstants();
	}

	private static void updateSettings() {
		buf_settings.position(0);
		glBindBuffer(GL_UNIFORM_BUFFER,UBO_Settings);
		glBufferData(GL_UNIFORM_BUFFER,buf_settings,GL_DYNAMIC_DRAW);
		glBindBufferBase(GL_UNIFORM_BUFFER, OGL3Renderer.UniformBlockBinding_Settings,UBO_Settings);
	}

	private static void updateFinalFrame() {
		buf_final_frame.position(0);
		glBindBuffer(GL_UNIFORM_BUFFER,UBO_FinalFrame);
		glBufferData(GL_UNIFORM_BUFFER, buf_final_frame,GL_DYNAMIC_DRAW);
		glBindBufferBase(GL_UNIFORM_BUFFER,OGL3Renderer.UniformBlockBinding_FrameInfo,UBO_FinalFrame);
	}

	private static void updateChunkConstants() {
		buf_chunk_constants.position(0);
		glBindBuffer(GL_UNIFORM_BUFFER,UBO_ChunkConstants);
		glBufferData(GL_UNIFORM_BUFFER,buf_chunk_constants,GL_DYNAMIC_DRAW);
		glBindBufferBase(GL_UNIFORM_BUFFER, OGL3Renderer.UniformBlockBinding_ChunkInfo,UBO_ChunkConstants);
	}

	public static void setChunkUniform(Matrix4f matrix4f) {
		storeMat4(buf_chunk_constants,offsetChunk_ChunkMatrix,matrix4f);
		updateChunkConstants();
	}
}
