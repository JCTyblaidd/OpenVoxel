package net.openvoxel.client.gui.widgets;

import net.openvoxel.client.renderer.common.IGuiRenderer;
import net.openvoxel.common.resources.ResourceHandle;
import net.openvoxel.common.resources.ResourceManager;
import org.joml.Matrix4f;


/**
 * Created by James on 14/09/2016.
 */
@Deprecated
public class GUILoadingCircle extends GUIObjectSizable {

	public static ResourceHandle loadCircle = ResourceManager.getImage("gui/load_circle");

	public float x_v = 1;//Real
	public float y_v = 0;//Imaginary
	public float r_x_v;
	public float r_y_v;
	public int colour;

	public GUILoadingCircle() {
		this((float)(Math.PI / 100));
	}

	public GUILoadingCircle(float rotation_speed) {
		this(rotation_speed,0xFFFFFFFF);
	}

	public GUILoadingCircle(float rotation_speed,int colour) {
		setRotationSpeed(rotation_speed);
		this.colour = colour;
	}

	public void setRotationSpeed(float speed) {
		r_x_v = (float)Math.cos(speed);//Real
		r_y_v = (float)Math.sin(speed);//Imaginary
	}

	private void _rotate() {
		float n_x = (r_x_v * x_v) - (r_y_v * y_v);
		y_v = (r_x_v * y_v) + (r_y_v * x_v);
		x_v = n_x;
	}

	@Override
	public void Draw(IGuiRenderer drawHandle) {
		_rotate();
		DrawSquare(drawHandle,loadCircle,colour);
	}

	@Override
	public void DrawSquare(IGuiRenderer drawHandle, ResourceHandle Image, int col) {
		final float screenWidth = drawHandle.getScreenWidth();
		final float screenHeight = drawHandle.getScreenHeight();
		final float X1 = getPosX(screenWidth);
		final float Y1 = getPosY(screenHeight);
		final float X2 = X1 + getWidth(screenWidth);
		final float Y2 = Y1 + getHeight(screenHeight);
		drawHandle.Begin(null);
		Matrix4f mat = new Matrix4f();
		mat.m00(x_v);
		mat.m11(x_v);
		mat.m01(-y_v);
		mat.m10(y_v);
		//TODO: fix
		System.out.println("S:" + Math.sqrt((x_v *x_v)+(y_v * y_v)));
		drawHandle.SetMatrix(mat);
		drawHandle.SetTexture(Image);
		drawHandle.VertexRect(X1,X2,Y1,Y2,col);
	}
}
