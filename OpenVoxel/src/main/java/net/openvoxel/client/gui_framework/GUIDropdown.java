package net.openvoxel.client.gui_framework;

import net.openvoxel.client.renderer.IGuiRenderer;

import java.util.List;

/**
 * Created by James on 10/09/2016.
 *
 * Drop Down Selection Box
 */
public class GUIDropdown extends GUIObjectSizable{

	private List<String> Options;
	private String selected;
	private boolean state_drop;
	private boolean[] state_hover;

	public GUIDropdown(List<String> values,String select) {
		Options = values;
		if(Options.contains(select)) {
			selected = select;
		}else{
			selected = Options.get(0);
		}
		state_drop = false;
		state_hover = new boolean[values.size()];
	}

	@Override
	public void Draw(IGuiRenderer drawHandle) {
		if(state_drop) {

		}
	}


	private void OnMouseEnter(int Selection) {
		state_hover[Selection] = true;
	}
	private void OnMouseLeave(int Selection) {
		state_hover[Selection] = false;
	}
	private void OnMousePress(int Selection) {
		selected = Options.get(Selection);
		state_drop = false;
		state_hover[Selection] = false;
	}
}
