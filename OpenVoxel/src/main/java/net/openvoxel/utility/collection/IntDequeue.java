package net.openvoxel.utility.collection;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

public class IntDequeue {

	private TIntList from;
	private TIntList to;

	public IntDequeue() {
		from = new TIntArrayList();
		to = new TIntArrayList();
	}


	public void add(int val) {
		from.add(val);
	}

	public int remove() {
		if(to.isEmpty()) {
			forwardQueue();
		}
		return to.removeAt(to.size()-1);
	}


	public boolean isEmpty() {
		return from.isEmpty() && to.isEmpty();
	}

	private void forwardQueue() {
		for(int i = from.size()-1; i >= 0; i--) {
			to.add(from.get(i));
		}
		from.clear();
	}

}
