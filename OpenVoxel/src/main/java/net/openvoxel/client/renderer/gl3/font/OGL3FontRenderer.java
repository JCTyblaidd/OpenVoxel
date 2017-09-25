package net.openvoxel.client.renderer.gl3.font;

import net.openvoxel.client.ClientInput;
import net.openvoxel.client.renderer.gl3.util.OGL3ReloadableShader;
import net.openvoxel.client.renderer.gl3.util.OGL3Texture;
import net.openvoxel.common.resources.ResourceManager;
import net.openvoxel.common.resources.ResourceType;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glDisableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

/**
 * Created by James on 04/09/2016.
 *
 * Font Renderer
 */
public class OGL3FontRenderer {

	public static OGL3FontRenderer inst;

	private int bufferPos;
	private float[] posData;
	private int bufferUV;
	private float[] uvData;

	private int VAO;
	private int triCount;

	private static final int ALLOC_SIZE = 2048;

	private static final float[] CharSizes = new float[]{0,13,13,13,13,13,13,13,13,13,13,13,13,0,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,
			6,8,10,12,13,18,17,6,8,8,12,12,6,8,6,10,13,13,13,13,13,13,13,13,13,13,7,7,12,12,12,12,22,14,14,13,15,12,11,16,16,6,8,13,11,21,
			16,17,13,17,14,11,12,16,14,22,13,12,12,8,10,8,12,12,7,12,13,11,13,12,8,12,13,6,6,11,6,20,13,13,13,13,9,10,8,13,11,18,11,11,10,
			8,12,8,12,13,13,13,6,8,10,17,12,12,10,26,11,8,22,13,12,13,13,6,6,10,10,12,12,23,11,18,10,8,21,13,10,12,6,8,12,13,12,13,12,12,
			10,21,10,13,12,8,13,10,8,12,8,8,7,14,15,6,8,6,11,13,16,17,17,12,14,14,14,14,14,14,19,13,12,12,12,12,6,6,6,6,16,16,17,17,17,17,
			17,12,17,16,16,16,16,12,13,13,12,12,12,12,12,12,19,11,12,12,12,12,6,6,6,6,13,13,13,13,13,13,13,12,13,13,13,13,13,11,13,11};

	private static final int SHEET_WIDTH = 16;
	private static final int SHEET_HEIGHT = 16;
	private static final float CELL_WIDTH = 1.0F / (SHEET_WIDTH);
	private static final float CELL_HEIGHT = 1.0F / (SHEET_HEIGHT);
	private static final float WIDTH_SCALE = 32;

	public static void Init() {
		inst = new OGL3FontRenderer();
	}

	private OGL3Texture fontTexture = OGL3Texture.getTexture(ResourceManager.getImage("font/font"));
	private OGL3ReloadableShader<OGL3FontShader> fontShader = new OGL3ReloadableShader<>(ResourceManager.getResource(ResourceType.SHADER, "font/fontShader")) {
		@Override
		public OGL3FontShader newShader(String src) {
			return new OGL3FontShader(src);
		}
	};

	private OGL3FontRenderer() {
		fontTexture.makeLinear();
		posData = new float[3 * ALLOC_SIZE];
		uvData = new float[2 * ALLOC_SIZE];
		bufferPos = glGenBuffers();
		bufferUV = glGenBuffers();
		VAO = glGenVertexArrays();
		glBindVertexArray(VAO);

		glBindBuffer(GL_ARRAY_BUFFER,bufferPos);
		glBufferData(GL_ARRAY_BUFFER,posData,GL_STREAM_DRAW);
		glVertexAttribPointer(0,3,GL_FLOAT,false,0,0);
		glEnableVertexAttribArray(0);
		//
		glBindBuffer(GL_ARRAY_BUFFER,bufferUV);
		glBufferData(GL_ARRAY_BUFFER,uvData,GL_STREAM_DRAW);
		glVertexAttribPointer(1,2,GL_FLOAT,false,0,0);
		glEnableVertexAttribArray(1);

		glBindBuffer(GL_ARRAY_BUFFER,0);
		glBindVertexArray(0);
		glDisableVertexAttribArray(0);
		glDisableVertexAttribArray(1);
	}

	public void DrawText(float X, float Y, float Z, float Height,String text,boolean reversed) {
		DrawText(X,Y,Z,Height,text,reversed,0xFFFFFFFF,0xFFFFFFFF,0.0F);
	}

	public void DrawText(float X, float Y, float Z, float Height, String text) {
		DrawText(X,Y,Z,Height,text,false,0xFFFFFFFF,0xFFFFFFFF,0.0F);
	}

	public void DrawText(float X, float Y, float Z, float Height, String text,int col) {
		DrawText(X,Y,Z,Height,text,false,col,0xFFFFFFFF,0.0F);
	}
	public void DrawText(float X, float Y, float Z, float Height,String text,boolean reversed,int col) {
		DrawText(X,Y,Z,Height,text,reversed,col,0xFFFFFFFF,0.0F);
	}

	public void DrawText(float X, float Y, float Z, float Height, String text,int col,int outlineCol) {
		DrawText(X,Y,Z,Height,text,false,col,outlineCol,0.0F);
	}

	public void DrawText(float X, float Y, float Z, float Height, String text,int col, int outlineCol, float shadowPower) {
		DrawText(X,Y,Z,Height,text,false,col,outlineCol,shadowPower);
	}

	private float DrawChar(float X, float Y, float Z, float Height,char c,boolean Reversed,float aspectRatio) {
		int charID = ((int)c)-32;//1

		int YCell = charID / SHEET_HEIGHT;//0
		int XCell = charID - (YCell * SHEET_HEIGHT);//1

		float minU = XCell * CELL_WIDTH;
		float Width = (CharSizes[charID+32] / WIDTH_SCALE);
		float maxU = minU + (Width * CELL_WIDTH);
		float maxV = YCell * CELL_HEIGHT;
		float minV = maxV + CELL_HEIGHT;

		float realWidth = Height * Width * aspectRatio;
		float maxY = Y + Height;
		float maxX = X + realWidth;

		if(Reversed) {
			Vertex(maxX,maxY,Z,maxU,maxV);
			Vertex(X,maxY,Z,minU,maxV);
			Vertex(X,Y,Z,minU,minV);
			//
			Vertex(maxX,Y,Z,maxU,minV);
			Vertex(maxX,maxY,Z,maxU,maxV);
			Vertex(X,Y,Z,minU,minV);
		}else{
			Vertex(X,Y,Z,minU,minV);
			Vertex(X,maxY,Z,minU,maxV);
			Vertex(maxX,maxY,Z,maxU,maxV);
			//
			Vertex(X,Y,Z,minU,minV);
			Vertex(maxX,maxY,Z,maxU,maxV);
			Vertex(maxX,Y,Z,maxU,minV);
		}
		return realWidth;
	}

	private void Vertex(float X, float Y, float Z, float U, float V) {
		final int POS = triCount*3;
		posData[POS] = X;
		posData[POS+1] = Y;
		posData[POS+2] = Z;

		final int UV = triCount*2;
		uvData[UV] = U;
		uvData[UV+1] = V;

		triCount++;
	}

	public float DrawText(float X, float Y, float Z, float Height,String text,boolean Reversed,int col, int outlineCol, float shadowPower ) {
		final int SIZE = text.length();
		triCount = 0;
		float runningOffset = 0;
		final float aspect = (float)ClientInput.currentWindowHeight.get() / ClientInput.currentWindowWidth.get();
		for(int i = 0; i < SIZE; i++){
			char c = text.charAt(i);
			runningOffset += DrawChar(X+runningOffset,Y,Z,Height,c,Reversed,aspect);
		}
		glBindBuffer(GL_ARRAY_BUFFER,bufferPos);
		glBufferSubData(GL_ARRAY_BUFFER,0,posData);
		glBindBuffer(GL_ARRAY_BUFFER,bufferUV);
		glBufferSubData(GL_ARRAY_BUFFER,0,uvData);
		glBindBuffer(GL_ARRAY_BUFFER,0);
		OGL3FontShader shader =  fontShader.getShader();
		fontTexture.bind(0);
		shader.Use();
		shader.setTexture(0);
		shader.setColour(col);
		shader.setOutlineColour(outlineCol);
		shader.setShadow(shadowPower);
		glBindVertexArray(VAO);
		glDrawArrays(GL_TRIANGLES,0,triCount);
		glBindVertexArray(0);
		return runningOffset;
	}


	public float getWidth(String text) {
		final int SIZE = text.length();
		final float aspect = (float)ClientInput.currentWindowHeight.get() / ClientInput.currentWindowWidth.get();
		float runningOffset = 0;
		for(int i = 0; i < SIZE; i++){
			char c = text.charAt(i);
			runningOffset += (CharSizes[(int)c] / WIDTH_SCALE) * aspect;
		}
		return runningOffset;
	}


}
