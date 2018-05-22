package net.openvoxel.common.entity.living.player;

import net.openvoxel.api.side.Side;
import net.openvoxel.api.side.SideOnly;
import net.openvoxel.client.keybindings.KeyBinding;
import net.openvoxel.client.keybindings.KeyManager;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Created by James on 15/09/2016.
 *
 * References THIS CLIENTS Entity Player
 */
public class EntityPlayerSP extends EntityPlayer{

	//KeyBindings//
	public static final KeyBinding keyForward;
	public static final KeyBinding keyBackward;
	public static final KeyBinding keyLeft;
	public static final KeyBinding keyRight;
	public static final KeyBinding keyJump;
	public static final KeyBinding keyCrouch;
	public static final KeyBinding keySprint;

	public static final KeyBinding keyOpenInv;
	public static final KeyBinding keySlot1,keySlot2,keySlot3,keySlot4,keySlot5,keySlot6,keySlot7,keySlot8,keySlot9,keySlot0;
	static {
		keyForward = KeyManager.getBinding("player.movement.forward",GLFW_KEY_W);
		keyBackward = KeyManager.getBinding("player.movement.backward",GLFW_KEY_S);
		keyLeft = KeyManager.getBinding("player.movement.left",GLFW_KEY_A);
		keyRight = KeyManager.getBinding("player.movement.right",GLFW_KEY_D);
		keyJump = KeyManager.getBinding("player.movement.jump",GLFW_KEY_SPACE);
		keyCrouch = KeyManager.getBinding("player.movement.crouch",GLFW_KEY_LEFT_SHIFT);
		keySprint = KeyManager.getBinding("player.movement.sprint",GLFW_KEY_LEFT_CONTROL);

		keyOpenInv = KeyManager.getBinding("player.inventory.open",GLFW_KEY_E);

		keySlot0 = KeyManager.getBinding("player.inventory.slot0",GLFW_KEY_0);
		keySlot1 = KeyManager.getBinding("player.inventory.slot1",GLFW_KEY_1);
		keySlot2 = KeyManager.getBinding("player.inventory.slot2",GLFW_KEY_2);
		keySlot3 = KeyManager.getBinding("player.inventory.slot3",GLFW_KEY_3);
		keySlot4 = KeyManager.getBinding("player.inventory.slot4",GLFW_KEY_4);
		keySlot5 = KeyManager.getBinding("player.inventory.slot5",GLFW_KEY_5);
		keySlot6 = KeyManager.getBinding("player.inventory.slot6",GLFW_KEY_6);
		keySlot7 = KeyManager.getBinding("player.inventory.slot7",GLFW_KEY_7);
		keySlot8 = KeyManager.getBinding("player.inventory.slot8",GLFW_KEY_8);
		keySlot9 = KeyManager.getBinding("player.inventory.slot9",GLFW_KEY_9);
	}

	public EntityPlayerSP() {
		super();
		//clientConnection = null;
	}

	//public EntityPlayerSP(ClientNetworkHandler networkHandler) {
	//	this();
	//	clientConnection = networkHandler;
	//}

	//Network Handler//
	//public ClientNetworkHandler clientConnection;

	@Override
	public void tickClient() {
		super.tickClient();
		tickClientInputs();
	}


	private void selectSlot(int v) {
		//TODO: clientInv::selectSlot
	}

	@SideOnly(side = Side.CLIENT)
	void tickClientInputs() {
		if(keyOpenInv.isDown()) {
			//Send Open Inventory Packet//
		}else{
			if(keySlot0.isDown()) selectSlot(0);
			if(keySlot1.isDown()) selectSlot(1);
			if(keySlot2.isDown()) selectSlot(2);
			if(keySlot3.isDown()) selectSlot(3);
			if(keySlot4.isDown()) selectSlot(4);
			if(keySlot5.isDown()) selectSlot(5);
			if(keySlot6.isDown()) selectSlot(6);
			if(keySlot7.isDown()) selectSlot(7);
			if(keySlot8.isDown()) selectSlot(8);
			if(keySlot9.isDown()) selectSlot(9);
		}
		//TODO: Handle Other Inputs
	}
}
