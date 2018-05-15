package net.openvoxel.client.gui.widgets.input;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Created by James on 11/09/2016.
 *
 * Toggle Button
 */
public class GUIToggleButton extends GUIButton implements Consumer<GUIButton>{

	private List<String> validValues;
	private BiConsumer<GUIToggleButton,String> action;

	public GUIToggleButton(List<String> vals, String select) {
		super(select);
		setAction(this);
		validValues = vals;
	}

	public void setToggleAction(BiConsumer<GUIToggleButton,String> update) {
		action = update;
	}

	public void setToggleAction(Consumer<String> update) {
		action = (button,str) -> update.accept(str);
	}

	@Override
	public void accept(GUIButton guiButton) {
		int id = validValues.indexOf(str);
		id++;
		int lim = validValues.size();
		if(id >= lim) {
			id = 0;
		}
		str = validValues.get(id);
		if(action != null) {
			action.accept(this,str);
		}
	}
}
