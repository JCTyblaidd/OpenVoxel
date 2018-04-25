package net.openvoxel.api.util;

import org.jetbrains.annotations.NotNull;

/**
 * Created by James on 31/07/2016.
 *
 * Version Information
 */
public class Version implements Comparable<Version>{

	private byte major;
	private byte minor;
	private byte patch;

	public Version(byte maj, byte min, byte ptch) {
		major = maj;
		minor = min;
		patch = ptch;
	}

	public Version(int maj, int min, int ptch) {
		this((byte)maj,(byte)min,(byte)ptch);
	}

	public Version(int ID) {
		patch = (byte)(ID & 0xFF);
		minor = (byte)((ID >> 12) & 0xFF);
		major = (byte)((ID >> 12) & 0xFF);
	}

	public int getMajor() {
		return major;
	}

	public int getMinor() {
		return minor;
	}

	public int getPatch() {
		return patch;
	}

	/****
	 * Parse Version:
	 *  valid:  x.y.z
	 *          x.y.z-TEXT
	 *          x.y
	 *          x.y-TEXT
	 *          x
	 *          x-TEXT
	 * @param str the version as a string
	 * @return a version
	 */
	public static Version parseVersion(String str) {
		String actual_data = str.split("-")[0];
		String[] params = actual_data.replace(".","@").split("@");//TODO: better fix, this is just a shoddy hack
		int major = (byte)Integer.parseInt(params[0]);
		int minor = 0;
		int patch = 0;
		if(params.length > 1) {
			minor = (byte)Integer.parseInt(params[1]);
		}
		if(params.length > 2) {
			patch = (byte)Integer.parseInt(params[2]);
		}
		return new Version(major,minor,patch);
	}

	public int toVersionInteger() {
		int int_maj = major;
		int int_min = minor;
		int int_pch = patch;
		return (int_maj << 24) | (int_min << 12) | int_pch;
	}

	@Override
	public int compareTo(@NotNull Version o) {
		int v1 = toVersionInteger();
		int v2 = o.toVersionInteger();
		if(v1 == v2) return 0;
		return v1 > v2 ? 1 : -1;
	}

	@Override
	public int hashCode() {
		return toVersionInteger();
	}

	public String getValString() {
		return Integer.toString(major)+"."+Integer.toString(minor)+"."+Integer.toString(patch);
	}

	@Override
	public String toString() {
		return "Version["+getValString()+"]";
	}
}
