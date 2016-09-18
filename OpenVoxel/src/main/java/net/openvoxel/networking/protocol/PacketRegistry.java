package net.openvoxel.networking.protocol;

import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import net.openvoxel.api.logger.Logger;
import net.openvoxel.networking.packet.protocol.*;
import net.openvoxel.networking.packet.sync.RequestServerSyncPacket;
import net.openvoxel.networking.packet.sync.SyncDetailedModInformationPacket;
import net.openvoxel.networking.packet.sync.SyncLoadedModsHashPacket;
import net.openvoxel.networking.packet.sync.SyncRegistryPacket;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Created by James on 01/09/2016.
 */
public class PacketRegistry {

	private TObjectIntHashMap<Class<? extends AbstractPacket>> classToIntMap;
	private TIntObjectHashMap<Class<? extends AbstractPacket>> intToClassMap;
	private HashMap<String,Class<? extends AbstractPacket>> registeredPackets;
	private TIntObjectHashMap<Class<? extends AbstractPacket>> defaultPackets;

	public static PacketRegistry CreateWithDefaults() {
		PacketRegistry registry = new PacketRegistry();
		registry.registerDefaultPacket(0, KeepAlivePacket.class);
		registry.registerDefaultPacket(1, HandshakePacket.class);
		registry.registerDefaultPacket(2, RequestPacketSync.class);
		registry.registerDefaultPacket(3, LoadPacketRegistryPacket.class);
		registry.registerDefaultPacket(4, SyncDetailedModInformationPacket.class);
		registry.registerDefaultPacket(5, SyncLoadedModsHashPacket.class);
		registry.registerDefaultPacket(6, RequestServerSyncPacket.class);
		registry.registerDefaultPacket(7, SyncRegistryPacket.class);
		registry.registerDefaultPacket(8, ModsOKPacket.class);
		registry.registerDefaultPacket(9, JoinGamePacket.class);
		return registry;
	}

	public LoadPacketRegistryPacket createLoadPacketRegistryPacket(boolean auto_resolve) {
		if(auto_resolve) generateDefaultMappings();
		LoadPacketRegistryPacket packet = new LoadPacketRegistryPacket();
		registeredPackets.forEach((name,clazz) -> {
			int val = classToIntMap.get(clazz);
			packet.packetTypes.put(val,name);
		});
		return packet;
	}

	public void loadFromPacketRegistrypacket(LoadPacketRegistryPacket packet) {
		classToIntMap.clear();
		intToClassMap.clear();
		defaultPackets.forEachEntry((v,pkt) -> {
			classToIntMap.put(pkt,v);
			intToClassMap.put(v,pkt);
			return true;
		});
		packet.packetTypes.forEachEntry((id,name) -> {
			Class<? extends AbstractPacket> clazz = registeredPackets.get(name);
			if(clazz != null) {
				classToIntMap.put(clazz,id);
				intToClassMap.put(id,clazz);
			}else {
				Logger.getLogger("Packet Registry").Warning("Packet Mapping for: " + name + ", received but no class found!");
			}
			return true;
		});
	}

	public PacketRegistry() {
		classToIntMap = new TObjectIntHashMap<>();
		intToClassMap = new TIntObjectHashMap<>();
		registeredPackets = new HashMap<>();
		defaultPackets = new TIntObjectHashMap<>();
	}

	public void registerPacket(String packetID,Class<? extends AbstractPacket> pkt) {
		registeredPackets.put(packetID,pkt);
	}
	public void registerDefaultPacket(int value,Class<? extends AbstractPacket> pkt) {
		defaultPackets.put(value,pkt);
		classToIntMap.put(pkt,value);
		intToClassMap.put(value,pkt);
	}

	public Class<? extends AbstractPacket> getPacket(int ID) {
		return intToClassMap.get(ID);
	}
	public AbstractPacket getNewObject(int ID) {
		try {
			return getPacket(ID).newInstance();
		}catch (Exception e) {
			return null;
		}
	}

	public int getIDFromClass(Class<? extends AbstractPacket> clazz) {
		return classToIntMap.get(clazz);
	}
	public int getIDFromObject(AbstractPacket packet) {
		return getIDFromClass(packet.getClass());
	}

	public void generateDefaultMappings() {
		classToIntMap.clear();
		intToClassMap.clear();
		defaultPackets.forEachEntry((v,pkt) -> {
			classToIntMap.put(pkt,v);
			intToClassMap.put(v,pkt);
			return true;
		});
		Set<Map.Entry<String,Class<? extends AbstractPacket>>> entrySet = registeredPackets.entrySet();
		Iterator<Map.Entry<String,Class<? extends AbstractPacket>>> iterator = entrySet.iterator();
		int currentIndex = 0;
		while(iterator.hasNext()) {
			Map.Entry<String,Class<? extends AbstractPacket>> entry = iterator.next();
			while(intToClassMap.containsKey(currentIndex)) {
				currentIndex++;
			}
			classToIntMap.put(entry.getValue(),currentIndex);
			intToClassMap.put(currentIndex,entry.getValue());
			currentIndex++;
		}
	}

}
