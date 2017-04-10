package net.openvoxel.client.keybindings;

import net.openvoxel.client.ClientInput;

/**
 * Created by James on 10/09/2016.
 *
 * Reference to a bound Key
 */
public class KeyBinding {

	int KEY;

	KeyBinding(int v) {
		KEY = v;
	}

	public boolean isDown() {
		return !ClientInput.inputTakenByGUI() && ClientInput.isKeyDown(KEY);
	}

	public boolean isDownRaw() {
		return ClientInput.isKeyDown(KEY);
	}

	public int getKey() {
		return KEY;
	}

	/** Change The Bound Key **/
	public void update(int newKey) {
		this.KEY = newKey;
		KeyManager.saveID();
	}
}
