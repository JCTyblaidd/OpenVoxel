package net.openvoxel.client.gui.widgets.display;

import net.openvoxel.api.PublicAPI;
import net.openvoxel.client.gui.widgets.GUIObjectSizable;
import net.openvoxel.client.renderer.common.IGuiRenderer;

/**
 * Created by James on 11/09/2016.
 */
public class GUIColour extends GUIObjectSizable {

	private int Col_ll;
	private int Col_lm;
	private int Col_ml;
	private int Col_mm;

	@PublicAPI
	public GUIColour(int col) {
		this(col,col,col,col);
	}

	@PublicAPI
	public GUIColour(int min_min, int min_max, int max_min, int max_max) {
		Col_ll = min_min;
		Col_lm = min_max;
		Col_ml = max_min;
		Col_mm = max_max;
	}

	@PublicAPI
	public GUIColour(int min, int max,boolean isHorizontal) {
		this(min,isHorizontal ? min : max,isHorizontal ? max : min,max);
	}

	@Override
	public void Draw(IGuiRenderer drawHandle) {
		DrawSquareColoured(drawHandle,null,Col_mm,Col_lm,Col_ll,Col_ml);
	}
}
