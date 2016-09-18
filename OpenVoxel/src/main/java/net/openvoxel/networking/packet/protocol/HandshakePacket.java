package net.openvoxel.networking.packet.protocol;

import net.openvoxel.networking.protocol.AbstractPacket;
import net.openvoxel.networking.protocol.ReadOnlyBuffer;
import net.openvoxel.networking.protocol.WriteOnlyBuffer;

/**
 * Created by James on 11/09/2016.
 *
 * Client -> Server: HI
 * Server -> Client: HI
 * Client -> Server: RequestPacketSync
 * Server -> Client: [If Not LocalHost] Send PacketSync
 * Client -> Server: RequestSync(0)
 * Server -> Client: Sync_Mods
 * Client -> Server: Sync_Mods
 * Server -> Client: Mods OK
 * Client -> Server: RequestSync(1)
 * Server -> Client: Block+Item Sync
 * Client -> Server: Join Game
 * #Normal Protocol Begins
 */
public class HandshakePacket extends AbstractPacket{

	public boolean directedToServer;

	@Override
	public void storeData(WriteOnlyBuffer buffer) {
		buffer.writeBoolean(directedToServer);
	}

	@Override
	public void loadData(ReadOnlyBuffer buffer) {
		directedToServer = buffer.readBoolean();
	}
}
