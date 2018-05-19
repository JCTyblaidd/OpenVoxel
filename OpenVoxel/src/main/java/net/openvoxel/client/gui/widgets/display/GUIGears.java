package net.openvoxel.client.gui.widgets.display;

import gnu.trove.list.TFloatList;
import gnu.trove.list.array.TFloatArrayList;
import net.openvoxel.client.gui.widgets.GUIObjectSizable;
import net.openvoxel.client.renderer.common.IGuiRenderer;
import org.joml.Vector2f;

public class GUIGears extends GUIObjectSizable {

	private int col;
	private int numTeeth;
	private float innerRadius;
	private float outerRadius;
	private float toothDepth;

	private float cos_angle = 1.0F;
	private float sin_angle = 0.0F;
	private float r_cos_angle;
	private float r_sin_angle;

	private long lastUpdate;

	private TFloatList gearList;

	public GUIGears(int col, int numTeeth, float innerRadius, float toothDepth, float rotationSpeed) {
		this.col = col;
		this.numTeeth = numTeeth;
		this.innerRadius = innerRadius;
		this.outerRadius = 1.0F;
		this.toothDepth = toothDepth;
		r_cos_angle = (float)Math.cos(Math.toRadians(rotationSpeed));
		r_sin_angle = (float)Math.sin(Math.toRadians(rotationSpeed));
		lastUpdate = System.currentTimeMillis();
		gearList = new TFloatArrayList();
		generate();
	}

	private void updateAngle() {
		float new_cos = (cos_angle * r_cos_angle) - (sin_angle * r_sin_angle);
		float new_sin = (cos_angle * r_sin_angle) + (sin_angle * r_cos_angle);
		cos_angle = new_cos;
		sin_angle = new_sin;
	}


	private void tick() {
		long newTime = System.currentTimeMillis();
		int delta = (int)(newTime - lastUpdate);
		lastUpdate = newTime;
		for(int i = 0; i < delta; i++) {
			updateAngle();
		}
	}

	private void generateFace(Vector2f a, Vector2f b, Vector2f c) {
		gearList.add(c.x / 2.F);
		gearList.add(c.y / 2.F);

		gearList.add(b.x / 2.F);
		gearList.add(b.y / 2.F);

		gearList.add(a.x / 2.F);
		gearList.add(a.y / 2.F);
	}

	private void generate() {
		Vector2f ix0 = new Vector2f();
		Vector2f ix1 = new Vector2f();
		Vector2f ix2 = new Vector2f();
		Vector2f ix3 = new Vector2f();
		Vector2f ix4 = new Vector2f();
		Vector2f ix5 = new Vector2f();

		float r0 = innerRadius;
		float r1 = outerRadius - toothDepth;
		float r2 = outerRadius;
		float da = 2.0f * (float)Math.PI / numTeeth / 4.0f;
		for(int i = 0; i < numTeeth; i++) {
			float ta = i * 2.0f * (float)Math.PI / (float)numTeeth;

			float cos_ta     = (float)Math.cos(ta);
			float cos_ta_1da = (float)Math.cos(ta + da);
			float cos_ta_2da = (float)Math.cos(ta + 2.0f * da);
			float cos_ta_3da = (float)Math.cos(ta + 3.0f * da);
			float cos_ta_4da = (float)Math.cos(ta + 4.0f * da);
			float sin_ta     = (float)Math.sin(ta);
			float sin_ta_1da = (float)Math.sin(ta + da);
			float sin_ta_2da = (float)Math.sin(ta + 2.0f * da);
			float sin_ta_3da = (float)Math.sin(ta + 3.0f * da);
			float sin_ta_4da = (float)Math.sin(ta + 4.0f * da);

			ix0.set(r0 * cos_ta, r0 * sin_ta);
			ix1.set(r1 * cos_ta, r1 * sin_ta);
			ix2.set(r0 * cos_ta, r0 * sin_ta);
			ix3.set(r1 * cos_ta_3da, r1 * sin_ta_3da);
			ix4.set(r0 * cos_ta_4da, r0 * sin_ta_4da);
			ix5.set(r1 * cos_ta_4da, r1 * sin_ta_4da);
			generateFace(ix0, ix1, ix2);
			generateFace(ix1, ix3, ix2);
			generateFace(ix2, ix3, ix4);
			generateFace(ix3, ix5, ix4);

			// front sides of teeth
			ix0.set(r1 * cos_ta, r1 * sin_ta);
			ix1.set(r2 * cos_ta_1da, r2 * sin_ta_1da);
			ix2.set(r1 * cos_ta_3da, r1 * sin_ta_3da);
			ix3.set(r2 * cos_ta_2da, r2 * sin_ta_2da);
			generateFace(ix0, ix1, ix2);
			generateFace(ix1, ix3, ix2);
		}
	}

	@Override
	public void Draw(IGuiRenderer drawHandle) {
		tick();
		float X = getPosX(drawHandle.getScreenWidth());
		float Y = getPosY(drawHandle.getScreenHeight());
		float W = getWidth(drawHandle.getScreenWidth());
		float H = getHeight(drawHandle.getScreenHeight());
		float X_START = X + W / 2.F;
		float Y_START = Y + H / 2.F;
		drawHandle.Begin(null);
		for(int i = 0; i < gearList.size(); i += 2) {
			float X_RAW = gearList.get(i);
			float Y_RAW = gearList.get(i+1);
			float XV = (X_RAW * cos_angle) - (Y_RAW * sin_angle);
			float YV = (X_RAW * sin_angle) + (Y_RAW * cos_angle);
			drawHandle.VertexWithCol(X_START + XV * W,Y_START + YV * H,col);
		}
	}

	@Override
	public boolean isDrawDirty() {
		return r_sin_angle != 0.0F;
	}
}
