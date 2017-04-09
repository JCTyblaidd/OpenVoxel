package net.openvoxel.collection;

import gnu.trove.impl.sync.TSynchronizedLongObjectMap;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.procedure.TLongObjectProcedure;
import gnu.trove.procedure.TObjectProcedure;

/**
 * Created by James on 09/04/2017.
 *
 * Chunk with Grid Red Reference
 */
public class ChunkMap<T> {

	private TLongObjectMap<T> dataMap = new TSynchronizedLongObjectMap<>(new TLongObjectHashMap<>());

	private long val(int x,int z) {
		long res = (long)x << 32;
		return res | z;
	}

	public T get(int x, int z) {
		return dataMap.get(val(x,z));
	}

	public void set(int x, int z, T v) {
		dataMap.put(val(x,z),v);
	}

	public void remove(int x, int z) {
		dataMap.remove(val(x,z));
	}

	public void forEachChunk(TObjectProcedure<T> procedure) {
		dataMap.forEachValue(procedure);
	}

	public void forEachEntry(TLongObjectProcedure<T> procedure) {
		dataMap.forEachEntry(procedure);
	}
}
