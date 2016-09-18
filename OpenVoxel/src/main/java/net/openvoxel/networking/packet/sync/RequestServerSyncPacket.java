package net.openvoxel.networking.packet.sync;

import net.openvoxel.networking.protocol.AbstractPacket;
import net.openvoxel.networking.protocol.ReadOnlyBuffer;
import net.openvoxel.networking.protocol.WriteOnlyBuffer;

/**
 * Created by James on 11/09/2016.
 *
 * Request synchronisation
 */
public class RequestServerSyncPacket extends AbstractPacket{

	public int syncID;

	@Override
	public void storeData(WriteOnlyBuffer buffer) {
		buffer.writeInt(syncID);
	}

	@Override
	public void loadData(ReadOnlyBuffer buffer) {
		syncID = buffer.readInt();
	}
}
