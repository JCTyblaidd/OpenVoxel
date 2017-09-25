package net.openvoxel.loader.mods;

import net.openvoxel.api.mods.ASMHandler;
import net.openvoxel.api.util.Version;

/**
 * Created by James on 25/08/2016.
 *
 * Mod Data Array, Including ASM Handlers without a referenced mod
 *
 */
public class EnabledModData {

	public String ID;
	public String Name;
	public Version version;

	//May Be Null it is an ASM Mod
	public ModHandle handle;
	//May be Null if not an ASM Mod
	public ASMHandler asmInfo;

	public EnabledModData(ModHandle handle) {
		ID = handle.getInformation().id();
		Name = handle.getInformation().name();
		version = Version.parseVersion(handle.getInformation().version());
		this.handle = handle;
	}
	public EnabledModData(ASMHandler h) {
		asmInfo = h;
		ID = h.id();
		Name = h.name();
		version = Version.parseVersion(h.version());
	}

	public boolean requiresClient() {
		return handle == null || handle.getInformation().requiresClient();
	}

	public boolean requiresServer() {
		return handle == null || handle.getInformation().requiresServer();
	}

	public long longHash() {
		return ID.hashCode() | (long)version.hashCode() << 32;
	}
}
