package net.openvoxel.client.renderer.common;

import net.openvoxel.api.PublicAPI;
import net.openvoxel.common.item.ItemStack;
import net.openvoxel.common.resources.ResourceHandle;
import org.joml.Matrix4f;

public abstract class IGuiRenderer {
	//Abstract Methods
	@PublicAPI
	public abstract void Begin(ResourceHandle texture);

	@PublicAPI
	public abstract void SetTexture(ResourceHandle handle);

	@PublicAPI
	public abstract void EnableTexture(boolean enabled);//Reset On Begin() to true

	@PublicAPI
	public abstract void SetMatrix(Matrix4f mat);//Reset On Begin() to identity

	@PublicAPI
	public abstract void VertexWithColUV(float x, float y, float u, float v, int RGB);

	@PublicAPI
	public abstract void DrawText(float x, float y, float height,CharSequence text, int col);

	@PublicAPI
	public abstract float GetTextWidthRatio(CharSequence text);

	//TODO: DRAW ITEM & BLOCKS IN GUI
	public void DrawItem(ItemStack stack, float x, float y, float z, float scale) {}

	@PublicAPI
	public abstract float getScreenWidth();
	@PublicAPI
	public abstract float getScreenHeight();

	@PublicAPI
	public abstract void pushScissor(int x, int y, int w, int h);
	@PublicAPI
	public abstract void popScissor();

	///////////////////
	// Alias Methods //
	///////////////////

	@PublicAPI
	public final void Vertex(float x, float y) {
		VertexWithColUV(x,y,0,0,0xFFFFFFFF);
	}
	@PublicAPI
	public final void VertexWithUV(float x, float y ,float u, float v) {
		VertexWithColUV(x,y,u,v,0xFFFFFFFF);
	}
	@PublicAPI
	public final void VertexWithCol(float x, float y, int RGB) {
		VertexWithColUV(x,y,0,0,RGB);
	}

	@PublicAPI
	public final void VertexRect(
			float X1, float X2,
			float Y1, float Y2,
			int  COL){
		VertexRect(
				X1, X2,
				Y1, Y2,
				0,1,
				0,1,
				COL, COL,
				COL, COL
		);
	}
	@PublicAPI
	public final void VertexRect(
			float X1, float X2,
			float Y1, float Y2,
			int  C00, int  C01,
			int  C10, int  C11) {
		VertexRect(
				X1, X2,
				Y1, Y2,
				0,1,
				0,1,
				C00, C01,
				C10, C11
		);
	}
	@PublicAPI
	public final void VertexRect(
			float X1, float X2,
			float Y1, float Y2,
			float U0, float U1,
			float V0, float V1,
			int  COL){
		VertexRect(
				X1, X2,
				Y1, Y2,
				U0, U1,
				V0, V1,
				COL, COL,
				COL, COL
		);
	}
	@PublicAPI
	public final void VertexRect(
			float X1, float X2,
			float Y1, float Y2,
			float U0, float U1,
			float V0, float V1,
			int  C00, int  C01,
			int  C10, int  C11) {
		VertexWithColUV(X1,Y1,U0,V0,C11);
		VertexWithColUV(X1,Y2,U0,V1,C10);
		VertexWithColUV(X2,Y2,U1,V1,C00);

		VertexWithColUV(X1,Y1,U0,V0,C11);
		VertexWithColUV(X2,Y2,U1,V1,C00);
		VertexWithColUV(X2,Y1,U1,V0,C01);
	}

	@PublicAPI
	public final void DrawText(float x, float y, float height,CharSequence text) {
		DrawText(x,y,height,text,0xFFFFFFFF);
	}
}
