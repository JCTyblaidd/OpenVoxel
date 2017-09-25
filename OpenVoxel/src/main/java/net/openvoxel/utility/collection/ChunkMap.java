package net.openvoxel.utility.collection;

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
		return res | ( (long)z & 0xffffffffL);
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

	public interface ChunkFunctor<T>{
		void call(int x, int z, T t);
	}

	public void forEachChunkCoord(ChunkFunctor<T> functor) {
		dataMap.forEachEntry((k,v) -> {
			int x = (int)(k >> 32);
			int z = (int)(k & 0xffffffffL);
			functor.call(x,z,v);
			return true;
		});
	}

	public void forEachEntry(TLongObjectProcedure<T> procedure) {
		dataMap.forEachEntry(procedure);
	}

	public void emptyAll() {
		dataMap.clear();
	}

}
