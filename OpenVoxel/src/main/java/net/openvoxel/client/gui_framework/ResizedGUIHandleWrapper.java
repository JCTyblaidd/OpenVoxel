package net.openvoxel.client.gui_framework;

import net.openvoxel.api.PublicAPI;
import net.openvoxel.client.renderer.common.IGuiRenderer;
import net.openvoxel.common.item.ItemStack;
import net.openvoxel.common.resources.ResourceHandle;
import org.joml.Matrix4f;


/**
 * Created by James on 01/09/2016.
 */
public class ResizedGUIHandleWrapper extends IGuiRenderer {

	private IGuiRenderer tessellator;
	private float x_offset = 0;
	private float y_offset = 0;
	private float x_scale = 1;
	private float y_scale = 1;

	@PublicAPI
	public ResizedGUIHandleWrapper(IGuiRenderer wrap) {
		tessellator = wrap;
	}

	public void set(float xOff, float yOff, float xScale, float yScale) {
		x_offset = xOff;
		y_offset = yOff;
		x_scale = xScale;
		y_scale = yScale;
	}

	@Override
	public void Begin() {
		tessellator.Begin();
	}

	@Override
	public void Draw() {
		tessellator.Draw();
	}

	@Override
	public void SetTexture(ResourceHandle handle) {
		tessellator.SetTexture(handle);
	}

	@Override
	public void EnableTexture(boolean enabled) {
		tessellator.EnableTexture(enabled);
	}

	@Override
	public void SetMatrix(Matrix4f mat) {
		tessellator.SetMatrix(mat);
	}


	@Override
	public void VertexWithColUV(float x, float y, float u, float v, int RGB) {
		tessellator.VertexWithColUV(x * x_scale + x_offset,y * y_scale + y_offset,u,v,RGB);
	}

	@Override
	public void DrawText(float x, float y, float height, String text, int col) {
		tessellator.DrawText(x * x_scale + x_offset,y * y_scale + y_offset,height * y_scale,text,col);
	}

	/*
	@Override
	public void DrawItem(ItemStack stack, float x, float y, float width, float height) {
		tessellator.DrawItem(stack,x * x_scale + x_offset,y * y_scale + y_offset,width * x_scale,height * y_scale);
	}*/

	@Override
	public float GetTextWidthRatio(String text) {
		return tessellator.GetTextWidthRatio(text) * y_scale / x_scale;
	}

	@Override
	public float getScreenWidth() {
		return tessellator.getScreenWidth() * x_scale;
	}

	@Override
	public float getScreenHeight() {
		return tessellator.getScreenHeight() * y_scale;
	}

	@Override
	public void popScissor() {
		tessellator.popScissor();
	}

	@Override
	public void pushScissor(int x, int y, int w, int h) {
		tessellator.pushScissor((int)(x * x_scale + x_offset),(int)(y * y_scale + y_offset),(int)(w * x_scale),(int)(h * y_scale));
	}
}
