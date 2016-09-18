package net.openvoxel.networking.protocol;

/**
 * Created by James on 01/09/2016.
 *
 * Base Class For The AbstractPacket
 */
public abstract class AbstractPacket {
	public abstract void storeData(WriteOnlyBuffer buffer);
	public abstract void loadData(ReadOnlyBuffer buffer);
}
