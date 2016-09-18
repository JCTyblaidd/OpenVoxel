package net.openvoxel.networking.packet.protocol;

import net.openvoxel.networking.protocol.AbstractPacket;
import net.openvoxel.networking.protocol.ReadOnlyBuffer;
import net.openvoxel.networking.protocol.WriteOnlyBuffer;

import java.util.UUID;

/**
 * Created by James on 11/09/2016.
 *
 * TODO: encrypt UUID + userName
 */
public class JoinGamePacket extends AbstractPacket{

	public String userName;
	public UUID uuid;

	@Override
	public void storeData(WriteOnlyBuffer buffer) {
		buffer.writeString(userName);
		buffer.writeUUID(uuid);
	}

	@Override
	public void loadData(ReadOnlyBuffer buffer) {
		userName = buffer.readString();
		uuid = buffer.readUUID();
	}
}
