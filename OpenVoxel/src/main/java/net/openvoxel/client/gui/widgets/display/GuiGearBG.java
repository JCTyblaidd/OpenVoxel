package net.openvoxel.client.gui.widgets.display;

import net.openvoxel.client.gui.framework.GUIObject;
import net.openvoxel.client.renderer.common.IGuiRenderer;

public class GuiGearBG extends GUIObject {

	private GUIColour background;
	private GUIGears gearLarge;
	private GUIGears gearMedium;
	private GUIGears gearSmall;

	public GuiGearBG(int col) {
		background = new GUIColour(col);
		background.setupFullscreen();
		gearLarge = new GUIGears(0x4A008CFF,12,0.1F,0.15F,0.05F);
		gearMedium = new GUIGears(0xD3459A00, 8, 0.1F, 0.2F, 0.1F);
		gearSmall = new GUIGears(0xC3A456FF,7,0.1F,0.2F,-0.1F);
	}

	@Override
	public void Draw(IGuiRenderer drawHandle) {
		float width = drawHandle.getScreenWidth();
		float height = drawHandle.getScreenHeight();
		float split1 = width / 3.F;
		float split2 = (height - split1/2.F);
		float split3 = width / 2.F;
		float split4 = height / 2.F;

		//Background
		background.Draw(drawHandle);

		//Top Left
		gearLarge.setPosition(0,0,-split1/2.F,-split1/2.F);
		gearLarge.setSize(0,0,split1,split1);
		gearLarge.Draw(drawHandle);

		//Bottom Left
		gearMedium.setPosition(0,1,-split2/2.F,-split2/2.F);
		gearMedium.setSize(0,0,split2,split2);
		gearMedium.Draw(drawHandle);

		//Top Right
		gearSmall.setPosition(1,0,-split3/2.F,-split3/2.F);
		gearSmall.setSize(0,0,split3,split3);
		gearSmall.Draw(drawHandle);

		//Bottom Right
		gearLarge.setPosition(1,1,-split4/2.F,-split4/2.F);
		gearLarge.setSize(0,0,split4,split4);
		gearLarge.Draw(drawHandle);
	}

	@Override
	public boolean isDrawDirty() {
		return true;
	}
}
