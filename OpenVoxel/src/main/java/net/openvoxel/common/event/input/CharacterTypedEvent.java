package net.openvoxel.common.event.input;

import net.openvoxel.common.event.AbstractEvent;

/**
 * Created by James on 01/09/2016.
 */
public class CharacterTypedEvent extends AbstractEvent{

	public final char CHAR;

	public CharacterTypedEvent(char c) {
		CHAR = c;
	}

}
