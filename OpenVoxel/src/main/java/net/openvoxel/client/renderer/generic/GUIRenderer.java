package net.openvoxel.client.renderer.generic;

import net.openvoxel.client.gui_framework.Screen;
import net.openvoxel.common.resources.ResourceHandle;
import org.joml.Matrix4f;


/**
 * Created by James on 25/08/2016.
 *
 * GUI Rendering Handler
 */
public interface GUIRenderer {

	void DisplayScreen(Screen screen);

	void beginDraw();

	/**
	 * Abstract GUI Tessellator For Drawing A GUI
	 */
	interface GUITessellator {
		void Begin();
		void Draw();
		void SetTexture(ResourceHandle handle);
		void EnableTexture(boolean enabled);//Reset On Begin() to true
		void SetMatrix(Matrix4f mat);//Reset On Begin() to identity
		void EnableColour(boolean enabled);//Reset On Begin() to false
		void SetZ(float zPos);
		void Vertex(float x, float y);
		void Vertex(float x, float y, float z);
		void VertexWithUV(float x, float y ,float u, float v);
		void VertexWithUV(float x, float y, float z, float u, float v);
		void VertexWithCol(float x, float y, int RGB);
		void VertexWithCol(float x, float y, float z, int RGB);
		void VertexWithColUV(float x, float y, float u, float v, int RGB);
		void VertexWithColUV(float x, float y, float z, float u, float v, int RGB);

		//Important GUI Hooks: Separate Draw Calls - Do Not Call While Drawing//
		void DrawText(float x, float y, float height,String text);
		void DrawText(float x, float y, float height,String text, int col, int colOutline);
		void DrawItem(float x, float y, float width, float height);
		float GetTextWidthRatio(String text);

		float getScreenWidth();
		float getScreenHeight();

		void resetScissor();
		void scissor(int x, int y, int w, int h);
	}
}
