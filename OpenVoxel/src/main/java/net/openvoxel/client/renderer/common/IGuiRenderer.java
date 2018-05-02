package net.openvoxel.client.renderer.common;

import net.openvoxel.common.item.ItemStack;
import net.openvoxel.common.resources.ResourceHandle;
import org.joml.Matrix4f;

public abstract class IGuiRenderer {
	//Abstract Methods
	public abstract void Begin(ResourceHandle texture);
	public abstract void SetTexture(ResourceHandle handle);
	public abstract void EnableTexture(boolean enabled);//Reset On Begin() to true
	public abstract void SetMatrix(Matrix4f mat);//Reset On Begin() to identity

	public abstract void VertexWithColUV(float x, float y, float u, float v, int RGB);

	public abstract void DrawText(float x, float y, float height,String text, int col);
	public abstract float GetTextWidthRatio(String text);

	//TODO: DRAW ITEM & BLOCKS IN GUI
	public void DrawItem(ItemStack stack, float x, float y, float z, float scale) {}

	public abstract float getScreenWidth();
	public abstract float getScreenHeight();

	public abstract void pushScissor(int x, int y, int w, int h);
	public abstract void popScissor();

	///////////////////
	// Alias Methods //
	///////////////////

	public final void Vertex(float x, float y) {
		VertexWithColUV(x,y,0,0,0xFFFFFFFF);
	}
	public final void VertexWithUV(float x, float y ,float u, float v) {
		VertexWithColUV(x,y,u,v,0xFFFFFFFF);
	}
	public final void VertexWithCol(float x, float y, int RGB) {
		VertexWithColUV(x,y,0,0,RGB);
	}

	public final void DrawText(float x, float y, float height,String text) {
		DrawText(x,y,height,text,0xFFFFFFFF);
	}
}
