package net.openvoxel.networking.packet.sync;

import net.openvoxel.loader.mods.ModLoader;
import net.openvoxel.networking.protocol.AbstractPacket;
import net.openvoxel.networking.protocol.ReadOnlyBuffer;
import net.openvoxel.networking.protocol.WriteOnlyBuffer;

/**
 * Created by James on 04/09/2016.
 */
public class SyncLoadedModsHashPacket extends AbstractPacket{

	/**
	 * The Complete Hash From The Other Node
	 */
	public long HashAll;
	/**
	 * The Hash From The Other That Is Required Here
	 */
	public long HashRequired;

	public SyncLoadedModsHashPacket(long all, long sync) {
		HashAll = all;
		HashRequired = sync;
	}

	/**
	 * @return packet to send to client
	 */
	public static SyncLoadedModsHashPacket loadFromServer() {
		return new SyncLoadedModsHashPacket(ModLoader.getInstance().getModHash(),ModLoader.getInstance().getNeedsClientModHash());
	}

	/**
	 * @return packet to send to server
	 */
	public static SyncLoadedModsHashPacket loadFromClient() {
		return new SyncLoadedModsHashPacket(ModLoader.getInstance().getModHash(),ModLoader.getInstance().getNeedsServerModHash());
	}

	@Override
	public void storeData(WriteOnlyBuffer buffer) {
		buffer.writeLong(HashAll);
		buffer.writeLong(HashRequired);
	}

	@Override
	public void loadData(ReadOnlyBuffer buffer) {
		HashAll = buffer.readLong();
		HashRequired = buffer.readLong();
	}
}
