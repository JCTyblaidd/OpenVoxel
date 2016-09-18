package net.openvoxel.networking.packet.sync;

import net.openvoxel.api.util.Version;
import net.openvoxel.loader.mods.ModHandle;
import net.openvoxel.loader.mods.ModLoader;
import net.openvoxel.networking.protocol.AbstractPacket;
import net.openvoxel.networking.protocol.ReadOnlyBuffer;
import net.openvoxel.networking.protocol.WriteOnlyBuffer;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by James on 04/09/2016.
 */
public class SyncDetailedModInformationPacket extends AbstractPacket{

	public static class SyncModInfo {
		public final String modID;
		public final Version modVersion;
		public final boolean needsClient;
		public final boolean needsServer;

		public SyncModInfo(String modID, Version modVersion, boolean needsClient, boolean needsServer) {
			this.modID = modID;
			this.modVersion = modVersion;
			this.needsClient = needsClient;
			this.needsServer = needsServer;
		}
		public SyncModInfo(ModHandle handle) {
			this(handle.getInformation().id(),
					Version.parseVersion(handle.getInformation().version()),
					handle.getInformation().requiresClient(),
					handle.getInformation().requiresServer());
		}
	}

	public List<SyncModInfo> SYNC ;

	public SyncDetailedModInformationPacket() {
		SYNC = new ArrayList<>();
	}

	public void load() {
		SYNC.clear();
		ModLoader.getInstance().streamMods().map(SyncModInfo::new).forEach(SYNC::add);
	}

	@Override
	public void storeData(WriteOnlyBuffer buffer) {
		buffer.writeIterable(SYNC,o -> {
			buffer.writeString(o.modID);
			buffer.writeInt(o.modVersion.toVersionInteger());
			buffer.writeByte((byte)(o.needsServer ? o.needsClient ? 3 : 2 : o.needsClient ? 1 : 0));
		});
	}

	@Override
	public void loadData(ReadOnlyBuffer buffer) {
		SYNC.clear();
		buffer.readIterable(()->{
			String id = buffer.readString();
			Version ver = new Version(buffer.readInt());
			byte flags = buffer.readByte();
			boolean needsServer = flags >= 2;
			boolean needsClient = flags % 2 == 1;
			return new SyncModInfo(id,ver,needsClient,needsServer);
		},SYNC);
	}
}
