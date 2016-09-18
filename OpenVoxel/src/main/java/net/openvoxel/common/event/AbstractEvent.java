package net.openvoxel.common.event;

/**
 * Created by James on 31/07/2016.
 */
public abstract class AbstractEvent {

	private boolean isCancelledV = false;

	public void setCancelled(boolean flag) {
		isCancelledV = flag;
	}

	public boolean isCancelled() {
		return isCancelledV;
	}

}
