package net.openvoxel.client.renderer.gl3;

import net.openvoxel.api.logger.Logger;
import net.openvoxel.client.ClientInput;
import net.openvoxel.client.gui_framework.Screen;
import net.openvoxel.client.renderer.generic.GUIRenderer;
import net.openvoxel.client.renderer.generic.GUIRenderer.GUITessellator;
import net.openvoxel.client.renderer.gl3.font.OGL3FontRenderer;
import net.openvoxel.client.renderer.gl3.util.OGL3ReloadableShader;
import net.openvoxel.client.renderer.gl3.util.OGL3Texture;
import net.openvoxel.client.renderer.gl3.util.shader.OGL3GUIShader;
import net.openvoxel.common.resources.ResourceHandle;
import net.openvoxel.common.resources.ResourceManager;
import net.openvoxel.common.resources.ResourceType;
import org.joml.Matrix4f;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

/**
 * Created by James on 25/08/2016.
 * Draw on-top of the rendered world
 */
public class OGL3GUIRenderer implements GUIRenderer, GUITessellator{

	@Override
	public void DisplayScreen(Screen screen) {
		screen.DrawScreen(this);
	}

	private static final int ALLOC_SIZE = 128 * 3;

	@Override
	public void beginDraw() {
		//Set State//
		glDisable(GL_DEPTH_TEST);
		glEnable(GL_BLEND);
		glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
		guiShader.use();//Save Switching//
	}

	@Override
	public void finishDraw() {
		//NO OP//
	}

	/**************************** TESSELLATOR CLASS ******************************************************/

	private OGL3ReloadableShader<OGL3GUIShader> guiShader;

	public OGL3GUIRenderer() {
		guiShader = new OGL3ReloadableShader<>(ResourceManager.getResource(ResourceType.SHADER, "gui/guiShader")) {
			@Override
			public OGL3GUIShader newShader(String src) {
				return new OGL3GUIShader(src);
			}
		};
		PositionArray = new float[ALLOC_SIZE * 3];
		UVArray = new float[ALLOC_SIZE * 3];
		ColArray = new int[ALLOC_SIZE];
		PositionBuffer = glGenBuffers();
		UVBuffer = glGenBuffers();
		ColBuffer = glGenBuffers();
		VAO = glGenVertexArrays();
		glBindVertexArray(VAO);
		glBindBuffer(GL_ARRAY_BUFFER,PositionBuffer);
		glBufferData(GL_ARRAY_BUFFER,PositionArray,GL_STREAM_DRAW);
		glVertexAttribPointer(0,3,GL_FLOAT,false,0,0);
		glEnableVertexAttribArray(0);
		glBindBuffer(GL_ARRAY_BUFFER,UVBuffer);
		glBufferData(GL_ARRAY_BUFFER,UVArray,GL_STREAM_DRAW);
		glVertexAttribPointer(1,2,GL_FLOAT,false,0,0);
		glEnableVertexAttribArray(1);
		glBindBuffer(GL_ARRAY_BUFFER,ColBuffer);
		glBufferData(GL_ARRAY_BUFFER,UVArray,GL_STREAM_DRAW);
		glVertexAttribPointer(2,4,GL_UNSIGNED_BYTE,true,0,0);
		glEnableVertexAttribArray(2);
		glBindVertexArray(0);
		glDisableVertexAttribArray(0);
		glDisableVertexAttribArray(1);
		glDisableVertexAttribArray(2);
	}

	private float[] PositionArray;
	private int PositionBuffer;
	private float[] UVArray;
	private int UVBuffer;
	private int[] ColArray;
	private int ColBuffer;
	private int VAO;

	private int DrawIndex;
	private boolean Drawing;

	private boolean Enable_Texture;
	private boolean Enable_Colour;

	private static final Matrix4f identity_mat;
	static {
		identity_mat = new Matrix4f();
	}

	private float Z_Position = 0;
	private Matrix4f mat = identity_mat;
	private OGL3Texture tex;



	@Override
	public void Begin() {
		if(Drawing) throw new RuntimeException("GUI Renderer Already Drawing");
		DrawIndex = 0;
		Enable_Colour = true;
		Enable_Texture = true;
		Drawing = true;
		mat = identity_mat;
	}

	@Override
	public void Draw() {
		if(!Drawing) throw new RuntimeException("Cannot Draw State That Hasn't Been Drawn");
		if(DrawIndex >= ALLOC_SIZE) {
			throw new RuntimeException("GUI Area - Too Large to Draw");
		}
		Drawing = false;
		OGL3GUIShader shader = guiShader.getShader();
		shader.Use();
		shader.setColEnable(Enable_Colour);
		shader.setTexEnable(Enable_Texture);
		shader.setMatrix(mat);
		shader.setZOffset(Z_Position);
		shader.setTexture(0);
		if(tex != null) {
			tex.bind(0);
		}
		//Draw//
		glBindBuffer(GL_ARRAY_BUFFER,PositionBuffer);
		glBufferSubData(GL_ARRAY_BUFFER,0,PositionArray);
		glBindBuffer(GL_ARRAY_BUFFER,UVBuffer);
		glBufferSubData(GL_ARRAY_BUFFER,0,UVArray);
		glBindBuffer(GL_ARRAY_BUFFER,ColBuffer);
		glBufferSubData(GL_ARRAY_BUFFER,0,ColArray);
		glBindVertexArray(VAO);
		glDrawArrays(GL_TRIANGLES,0,DrawIndex);
	}

	@Override
	public void SetTexture(ResourceHandle handle) {
		Enable_Texture = true;
		tex = OGL3Texture.getTexture(handle);
	}

	@Override
	public void EnableTexture(boolean enabled) {
		Enable_Texture = enabled;
	}

/**
	@Override
	public void EnableColour(boolean enabled) {
		Enable_Colour = enabled;
	}
	**/
/**
	@Override
	public void SetZ(float zPos) {
		Z_Position = zPos;
	}
**/
	@Override
	public void SetMatrix(Matrix4f mat) {
		this.mat = mat;
	}

	@Override
	public void Vertex(float x, float y) {
		VertexWithColUV(x,y,0,0,0,0xFFFFFFFF);
	}
/**
	@Override
	public void Vertex(float x, float y, float z) {
		VertexWithColUV(x,y,z,0,0,0xFFFFFFFF);
	}
**/
	@Override
	public void VertexWithUV(float x, float y, float u, float v) {
		VertexWithColUV(x,y,0,u,v,0xFFFFFFFF);
	}
/**
	@Override
	public void VertexWithUV(float x, float y, float z, float u, float v) {
		VertexWithColUV(x,y,z,u,v,0xFFFFFFFF);
	}
**/
	@Override
	public void VertexWithCol(float x, float y, int RGB) {
		VertexWithColUV(x,y,0,0,0,RGB);
	}
/**
	@Override
	public void VertexWithCol(float x, float y, float z, int RGB) {
		VertexWithColUV(x,y,z,0,0,RGB);
	}
**/
	@Override
	public void VertexWithColUV(float x, float y, float u, float v, int RGB) {
		VertexWithColUV(x,y,0,u,v,RGB);
	}

	//@Override
	public void VertexWithColUV(float x, float y, float z, float u, float v, int RGB) {
		final int i = DrawIndex * 3;
		PositionArray[i] = x*2 - 1;
		PositionArray[i+1] = -(y*2 - 1);//y*2 - 1
		PositionArray[i+2] = z;
		final int j = DrawIndex * 2;
		UVArray[j] = u;
		UVArray[j+1] = v;
		ColArray[DrawIndex] = RGB;
		DrawIndex++;
	}

	@Override
	public float GetTextWidthRatio(String text) {
		return OGL3FontRenderer.inst.getWidth(text);
	}

	@Override
	public void DrawText(float x, float y, float height, String text) {
		OGL3FontRenderer.inst.DrawText(x,y,Z_Position+0.1F,height,text);//TODO: check
	}

	@Override
	public void DrawText(float x, float y, float height, String text, int col, int colOutline) {
		OGL3FontRenderer.inst.DrawText(x,y,Z_Position+0.05F,height,text,col,colOutline);
	}

	@Override
	public void DrawItem(float x, float y, float width, float height) {
		Logger.getLogger("GUI Renderer : OGL3").Warning("Draw Item Not Implemented");
	}

	@Override
	public float getScreenWidth() {
		return ClientInput.currentWindowWidth.get();
	}

	@Override
	public float getScreenHeight() {
		return ClientInput.currentWindowHeight.get();
	}

	@Override
	public void resetScissor() {
		glScissor(0,0,ClientInput.currentWindowWidth.get(),ClientInput.currentWindowHeight.get());
		glDisable(GL_SCISSOR_TEST);
	}

	@Override
	public void scissor(int x, int y,int w,int h) {
		glEnable(GL_SCISSOR_TEST);
		glScissor(x,y,w,h);
	}
}
