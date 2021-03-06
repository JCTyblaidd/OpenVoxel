package net.openvoxel.server;

import net.openvoxel.client.ClientInput;
import net.openvoxel.client.control.Renderer;
import net.openvoxel.client.renderer.gl3.OGL3DisplayHandle;
import net.openvoxel.client.renderer.gl3.OGL3Renderer;
import net.openvoxel.common.entity.living.player.EntityPlayerSP;
import net.openvoxel.networking.ClientNetworkHandler;
import net.openvoxel.world.World;
import net.openvoxel.world.client.ClientWorld;
import net.openvoxel.world.generation.DebugWorldGenerator;
import org.lwjgl.glfw.GLFW;

/**
 * Created by James on 10/04/2017.
 *
 * Screen Background Client Server : automatically rotates the player
 */
public class BackgroundClientServer extends ClientServer{

	public BackgroundClientServer() {
		thePlayer = null;
		serverConnection = new ClientNetworkHandler();
		//DEBUG CODE TODO: remove and update
		thePlayer = new EntityPlayerSP();
		World theWorld = new ClientWorld(new DebugWorldGenerator());
		thePlayer.currentWorld = theWorld;
		thePlayer.xPos = 100;
		thePlayer.yPos = 105;
		thePlayer.zPos = 100;
		thePlayer.setPitch(0);
		thePlayer.setYaw(0);
		dimensionMap.put(0,theWorld);
		//END OF DEBUG CODE
	}

	@Override
	public void run() {
		super.run();
		//debug_tick();
	}
	private void debug_tick() {
		//thePlayer.setYaw(thePlayer.getYaw() + 0.004F);
		//thePlayer.xPos += 0.004F;
		float cos = (float)Math.cos(thePlayer.getYaw());
		float sin = (float)Math.sin(thePlayer.getYaw());
		float cosP = (float)Math.cos(thePlayer.getPitch());
		float sinP = (float)Math.cos(thePlayer.getPitch());
		//Terrible//
		//if(!ClientInput.isKeyDown(GLFW.GLFW_KEY_ESCAPE)) {
			OGL3Renderer r = (OGL3Renderer)Renderer.renderer;
			OGL3DisplayHandle h = (OGL3DisplayHandle)r.getDisplayHandle();
			long w = h.getWindow();
			GLFW.glfwSetInputMode(w,GLFW.GLFW_CURSOR,GLFW.GLFW_CURSOR_DISABLED);
		//}
		if(EntityPlayerSP.keyForward.isDownRaw()) {
			thePlayer.xPos += cosP * sin * 1.0F;
			thePlayer.zPos += cosP * cos * 1.0F;
			thePlayer.yPos += sinP * 1.0F;
		}
		if(EntityPlayerSP.keyBackward.isDownRaw()) {
			thePlayer.xPos += cosP * sin * -1.0F;
			thePlayer.zPos += cosP * cos * -1.0F;
			thePlayer.yPos += sinP * -1.0F;
		}
		if(EntityPlayerSP.keyLeft.isDownRaw()) {
			thePlayer.xPos += cos * 1.0F;
			thePlayer.zPos += sin * 1.0F;
		}
		if(EntityPlayerSP.keyRight.isDownRaw()) {
			thePlayer.xPos += cos * -1.0F;
			thePlayer.zPos += sin * -1.0F;
		}
		if(EntityPlayerSP.keyJump.isDownRaw()) {
			thePlayer.yPos += 0.5F;
		}
		if(EntityPlayerSP.keyCrouch.isDownRaw()) {
			thePlayer.yPos -= 0.5F;
		}
		float a = (float)ClientInput.unhandledMouseDelta.x;
		float b = (float)ClientInput.unhandledMouseDelta.y;
		ClientInput.unhandledMouseDelta.x = 0;
		ClientInput.unhandledMouseDelta.y = 0;
		a /= 300;
		b /= 300;
		thePlayer.setYaw(thePlayer.getYaw() + a);
		thePlayer.setPitch(thePlayer.getPitch() + b);
	}

}
