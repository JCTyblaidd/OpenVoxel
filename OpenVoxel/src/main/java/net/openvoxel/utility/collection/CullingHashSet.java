package net.openvoxel.utility.collection;

import gnu.trove.set.hash.TIntHashSet;
import net.openvoxel.utility.collection.trove_extended.TVec3LHashSet;

public class CullingHashSet {

	private TIntHashSet internal_set;
	private TVec3LHashSet fallback_set;

	public CullingHashSet() {
		internal_set = new TIntHashSet();
		fallback_set = new TVec3LHashSet();
	}

	// -127 <= x,y,z <= 127
	private int pack(int x, int y, int z) {
		byte _x = (byte)x;
		byte _y = (byte)y;
		byte _z = (byte)z;
		return (_x & 0xFF) | ((_y & 0xFF) << 8) | ((_z & 0x0F) << 16);
	}

	private boolean use_internal(int x, int y, int z) {
		return x > -128 && x < 128 && y > -128 && y < 128 && z > -128 && z < 128;
	}

	///
	/// API Methods
	///

	public void add(int x, int y, int z) {
		if(use_internal(x,y,z)) {
			internal_set.add(pack(x,y,z));
		}else{
			fallback_set.add(x,y,z);
		}
	}

	public boolean contains(int x, int y, int z) {
		if(use_internal(x,y,z)) {
			return internal_set.contains(pack(x,y,z));
		}else{
			return fallback_set.contains(x,y,z);
		}
	}

	public void clear() {
		internal_set.clear();
		fallback_set.clear();
	}
}
