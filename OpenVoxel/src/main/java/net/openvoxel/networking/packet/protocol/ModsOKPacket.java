package net.openvoxel.networking.packet.protocol;

import net.openvoxel.networking.protocol.AbstractPacket;
import net.openvoxel.networking.protocol.ReadOnlyBuffer;
import net.openvoxel.networking.protocol.WriteOnlyBuffer;

/**
 * Created by James on 11/09/2016.
 *
 * Notify the other connection that the mods connected are valid
 */
public class ModsOKPacket extends AbstractPacket{
	@Override
	public void storeData(WriteOnlyBuffer buffer) {

	}

	@Override
	public void loadData(ReadOnlyBuffer buffer) {

	}
}
