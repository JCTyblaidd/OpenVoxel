package net.openvoxel.networking.packet.protocol;

import gnu.trove.map.hash.TIntObjectHashMap;
import net.openvoxel.networking.protocol.AbstractPacket;
import net.openvoxel.networking.protocol.ReadOnlyBuffer;
import net.openvoxel.networking.protocol.WriteOnlyBuffer;

/**
 * Created by James on 01/09/2016.
 *
 * Synchronize Packets
 * 
 */
public class LoadPacketRegistryPacket extends AbstractPacket{

	public TIntObjectHashMap<String> packetTypes;

	public LoadPacketRegistryPacket() {
		packetTypes = new TIntObjectHashMap<>();
	}

	@Override
	public void storeData(WriteOnlyBuffer buffer) {
		buffer.writeIntMap(packetTypes,buffer::writeString);
	}

	@Override
	public void loadData(ReadOnlyBuffer buffer) {
		buffer.readIntMap(packetTypes,buffer::readString);
	}
}
