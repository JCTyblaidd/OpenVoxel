package net.openvoxel.client.renderer.gl3.util.shader;

import net.openvoxel.client.renderer.gl3.util.OGL3BasicShader;

import javax.vecmath.Matrix4f;

import static org.lwjgl.opengl.GL20.*;

/**
 * Created by James on 01/09/2016.
 */
public class OGL3GUIShader extends OGL3BasicShader{

	private int uniform_zOffset;
	private int uniform_matrix;
	private int uniform_enableTex;
	private int uniform_enableCol;
	private int uniform_texture;

	public OGL3GUIShader(String shaderSource) {
		super(shaderSource, "guiShader");
		uniform_zOffset = glGetUniformLocation(program_ID,"Z_Offset");
		uniform_matrix = glGetUniformLocation(program_ID,"matrix");
		uniform_enableCol = glGetUniformLocation(program_ID,"Enable_Col");
		uniform_enableTex = glGetUniformLocation(program_ID,"Enable_Tex");
		uniform_texture = glGetUniformLocation(program_ID,"texture");
	}

	public void setZOffset(float Z) {
		glUniform1f(uniform_zOffset,Z);
	}

	public void setMatrix(Matrix4f Mat) {
		setUniformMat4(uniform_matrix,Mat);
	}

	public void setTexEnable(boolean flag) {
		glUniform1i(uniform_enableTex,flag ? 1 : 0);
	}

	public void setColEnable(boolean flag) {
		glUniform1i(uniform_enableCol,flag ? 1 : 0);
	}

	public void setTexture(int VAL) {
		glUniform1i(uniform_texture,VAL);
	}
}
