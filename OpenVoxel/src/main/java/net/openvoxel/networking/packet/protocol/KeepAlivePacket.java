package net.openvoxel.networking.packet.protocol;

import net.openvoxel.networking.protocol.AbstractPacket;
import net.openvoxel.networking.protocol.ReadOnlyBuffer;
import net.openvoxel.networking.protocol.WriteOnlyBuffer;

/**
 * Created by James on 01/09/2016.
 */
public class KeepAlivePacket extends AbstractPacket{
	@Override
	public void storeData(WriteOnlyBuffer buffer) {
		//NO DATA//
	}

	@Override
	public void loadData(ReadOnlyBuffer buffer) {
		//NO DATA//
	}
}
