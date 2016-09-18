package net.openvoxel.loader.mods;

import net.openvoxel.api.util.Version;

/**
 * Created by James on 03/09/2016.
 */
public class ModDependency {

	public final String ID;
	public final Version minVersion;
	public final Version maxVersion;

	public ModDependency(String id, Version minVersion, Version maxVersion) {
		ID = id;
		this.minVersion = minVersion;
		this.maxVersion = maxVersion;
	}

	public static ModDependency parseDependency(String value) {
		String[] dat = value.split("&");
		String ID = dat[0];
		Version Min = null;
		Version Max = null;
		if(dat.length >= 2) {
			String[] strip = dat[1].split("->");
			if(strip.length >= 1) {
				Min = Version.parseVersion(strip[0]);
			}
			if(strip.length >= 2){
				Max = Version.parseVersion(strip[1]);
			}
		}
		return new ModDependency(ID,Min,Max);
	}

	public void checkExistsAndVersion() throws ModLoader.ModLoadingException{
		if(!ModLoader.isModLoaded(ID)) {
			throw new ModLoader.ModLoadingException("Missing Dependency: " + ID);
		}
		Version ver = ModLoader.getModVersion(ID);
		if(minVersion != null && minVersion.compareTo(ver) == 1) {
			throw new ModLoader.ModLoadingException("Wrong Dependency Version: " + ver.getValString() + ", is not >= "+minVersion.getValString());
		}
		if(maxVersion != null && maxVersion.compareTo(ver) == -1) {
			throw new ModLoader.ModLoadingException("Wrong Dependency Version: " + ver.getValString() + ", is not <= "+maxVersion.getValString());
		}
	}
}
