package net.openvoxel.networking.protocol;

/**
 * Created by James on 01/09/2016.
 *
 * Base Class For The AbstractPacket
 */
public abstract class AbstractPacket {
	/**
	 * Serialize Information About This Packet
	 * @param buffer write only ByteBuf Reference
	 */
	public abstract void storeData(WriteOnlyBuffer buffer);
	/**
	 * Deserialize Information About This Packet
	 * @param buffer read only ByteBuf Reference
	 */
	public abstract void loadData(ReadOnlyBuffer buffer);
}
