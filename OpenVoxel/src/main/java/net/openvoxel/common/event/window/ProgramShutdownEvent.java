package net.openvoxel.common.event.window;

import net.openvoxel.common.event.AbstractEvent;

/**
 * Created by James on 28/08/2016.
 */
public class ProgramShutdownEvent extends AbstractEvent{

	public final boolean isCrash;
	public ProgramShutdownEvent(boolean isCrash) {
		this.isCrash = isCrash;
	}

}
