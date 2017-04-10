package net.openvoxel.client.renderer.gl3.font;

import net.openvoxel.client.renderer.gl3.util.OGL3BasicShader;
import org.joml.Matrix4f;


import static org.lwjgl.opengl.GL20.*;

/**
 * Created by James on 04/09/2016.
 */
public class OGL3FontShader extends OGL3BasicShader{

	private int colMain_Uniform;
	private int shadowPower_Uniform;
	private int colOutline_Uniform;
	private int texID;
	private int matrix_uniform;

	public OGL3FontShader(String shaderSource) {
		super(shaderSource, "fontShader");
		colMain_Uniform = glGetUniformLocation(program_ID,"colour");
		shadowPower_Uniform = glGetUniformLocation(program_ID,"shadow");
		colOutline_Uniform = glGetUniformLocation(program_ID,"outline_colour");
		texID = glGetUniformLocation(program_ID,"texture");
		matrix_uniform = glGetUniformLocation(program_ID,"mat");
	}

	private float[] getCol(int ID) {
		int b1 = (ID & 0xFF);
		int b2 = ((ID >> 8) & 0xFF);
		int b3 = ((ID >> 16) & 0xFF);
		int b4 =((ID >> 24) & 0xFF);
		return new float[]{b1 / 255.0F,b2/255.0F,b3/255.0F,b4/255.0F};
	}

	public void setColour(int col) {
		//glUniform1i(colMain_Uniform,col);
		float[] v = getCol(col);
		//System.out.println(Arrays.toString(v));
		glUniform4f(colMain_Uniform,v[0],v[1],v[2],v[3]);
	}


	public void setOutlineColour(int col) {
		//glUniform1i(colOutline_Uniform,col);
		float[] v = getCol(col);
		//System.out.println(Arrays.toString(v));
		glUniform4f(colOutline_Uniform,v[0],v[1],v[2],v[3]);
	}

	public void setShadow(float power) {
		glUniform1f(shadowPower_Uniform,power);
	}

	public void setTexture(int id) {
		glUniform1i(texID,id);
	}

	public void setMatrix(Matrix4f mat) {
		setUniformMat4(matrix_uniform,mat);
	}
}
