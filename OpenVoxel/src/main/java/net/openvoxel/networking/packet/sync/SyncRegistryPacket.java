package net.openvoxel.networking.packet.sync;

import gnu.trove.map.hash.TIntObjectHashMap;
import net.openvoxel.networking.protocol.AbstractPacket;
import net.openvoxel.networking.protocol.ReadOnlyBuffer;
import net.openvoxel.networking.protocol.WriteOnlyBuffer;

/**
 * Created by James on 04/09/2016.
 *
 * Synchronize Block, Item Values Between the client and the server
 */
public class SyncRegistryPacket extends AbstractPacket{

	public TIntObjectHashMap<String>    itemMap;
	public TIntObjectHashMap<String>    blockMap;

	public SyncRegistryPacket() {
		itemMap = new TIntObjectHashMap<>();
		blockMap = new TIntObjectHashMap<>();
	}

	@Override
	public void storeData(WriteOnlyBuffer buffer) {
		buffer.writeIntMap(itemMap,buffer::writeString);
		buffer.writeIntMap(blockMap,buffer::writeString);
	}

	@Override
	public void loadData(ReadOnlyBuffer buffer) {
		itemMap.clear(); blockMap.clear();
		buffer.readIntMap(itemMap,buffer::readString);
		buffer.readIntMap(blockMap,buffer::readString);
	}
}
