package net.openvoxel.utility.collection.trove_extended;

import gnu.trove.impl.Constants;
import gnu.trove.impl.HashFunctions;
import gnu.trove.impl.hash.TPrimitiveHash;

import java.util.Arrays;

public abstract class TVec2LHash extends TPrimitiveHash {
	static final long serialVersionUID = 1L;

	/** the set of longs */
	public transient long[] _set_xy;

	/**
	 * value that represents null
	 *
	 * NOTE: should not be modified after the Hash is created, but is
	 *       not final because of Externalization
	 *
	 */
	protected long no_entry_value;

	protected boolean consumeFreeSlot;

	private int hashVec2(long x, long y) {
		final int prime = 31;
		int result = 1;
		result = prime * result + HashFunctions.hash(x);
		result = prime * result + HashFunctions.hash(y);
		return result;
	}


	/**
	 * Creates a new <code>TLongHash</code> instance with the default
	 * capacity and load factor.
	 */
	public TVec2LHash() {
		super();
		no_entry_value = Constants.DEFAULT_LONG_NO_ENTRY_VALUE;
		//noinspection RedundantCast
		if ( no_entry_value != ( long ) 0 ) {
			Arrays.fill( _set_xy, no_entry_value );
		}
	}


	/**
	 * Creates a new <code>TLongHash</code> instance whose capacity
	 * is the next highest prime above <tt>initialCapacity + 1</tt>
	 * unless that value is already prime.
	 *
	 * @param initialCapacity an <code>int</code> value
	 */
	public TVec2LHash( int initialCapacity ) {
		super( initialCapacity );
		no_entry_value = Constants.DEFAULT_LONG_NO_ENTRY_VALUE;
		//noinspection RedundantCast
		if ( no_entry_value != ( long ) 0 ) {
			Arrays.fill( _set_xy, no_entry_value );
		}
	}


	/**
	 * Creates a new <code>TLongHash</code> instance with a prime
	 * value at or near the specified capacity and load factor.
	 *
	 * @param initialCapacity used to find a prime capacity for the table.
	 * @param loadFactor used to calculate the threshold over which
	 * rehashing takes place.
	 */
	public TVec2LHash( int initialCapacity, float loadFactor ) {
		super(initialCapacity, loadFactor);
		no_entry_value = Constants.DEFAULT_LONG_NO_ENTRY_VALUE;
		//noinspection RedundantCast
		if ( no_entry_value != ( long ) 0 ) {
			Arrays.fill( _set_xy, no_entry_value );
		}
	}


	/**
	 * Creates a new <code>TLongHash</code> instance with a prime
	 * value at or near the specified capacity and load factor.
	 *
	 * @param initialCapacity used to find a prime capacity for the table.
	 * @param loadFactor used to calculate the threshold over which
	 * rehashing takes place.
	 * @param no_entry_value value that represents null
	 */
	public TVec2LHash( int initialCapacity, float loadFactor, long no_entry_value ) {
		super(initialCapacity, loadFactor);
		this.no_entry_value = no_entry_value;
		//noinspection RedundantCast
		if ( no_entry_value != ( long ) 0 ) {
			Arrays.fill( _set_xy, no_entry_value );
		}
	}


	/**
	 * Returns the value that is used to represent null. The default
	 * value is generally zero, but can be changed during construction
	 * of the collection.
	 *
	 * @return the value that represents null
	 */
	public long getNoEntryValue() {
		return no_entry_value;
	}


	/**
	 * initializes the hashtable to a prime capacity which is at least
	 * <tt>initialCapacity + 1</tt>.
	 *
	 * @param initialCapacity an <code>int</code> value
	 * @return the actual capacity chosen
	 */
	protected int setUp( int initialCapacity ) {
		int capacity;

		capacity = super.setUp( initialCapacity );
		_set_xy = new long[capacity * 2];
		return capacity;
	}


	/**
	 * Searches the set for <tt>val</tt>
	 *
	 * @return a <code>boolean</code> value
	 */
	public boolean contains( long val_x, long val_y) {
		return index(val_x,val_y) >= 0;
	}


	/**
	 * Executes <tt>procedure</tt> for each element in the set.
	 *
	 * @param procedure a <code>TObjectProcedure</code> value
	 * @return false if the loop over the set terminated because
	 * the procedure returned false for some value.
	 */
	/*
	public boolean forEach( TLongProcedure procedure ) {
		byte[] states = _states;
		long[] set = _set;
		for ( int i = set.length; i-- > 0; ) {
			if ( states[i] == FULL && ! procedure.execute( set[i] ) ) {
				return false;
			}
		}
		return true;
	}*/


	/**
	 * Releases the element currently stored at <tt>index</tt>.
	 *
	 * @param index an <code>int</code> value
	 */
	protected void removeAt( int index ) {
		int new_index = index*2;
		_set_xy[new_index] = no_entry_value;
		_set_xy[new_index+1] = no_entry_value;
		super.removeAt( index );
	}


	/**
	 * Locates the index of <tt>val</tt>.
	 *
	 * @return the index of <tt>val</tt> or -1 if it isn't in the set.
	 */
	protected int index( long val_x, long val_y ) {
		int hash, probe, index, length;

		final byte[] states = _states;
		final long[] set = _set_xy;
		length = states.length;
		hash = hashVec2(val_x, val_y) & 0x7fffffff;
		index = hash % length;
		byte state = states[index];

		if (state == FREE) {
			return -1;
		}

		int new_index = index * 2;
		if (state == FULL && set[new_index] == val_x && set[new_index+1] == val_y) {
			return index;
		}

		return indexRehashed(val_x, val_y, index, hash, state);
	}

	int indexRehashed(long key_x, long key_y, int index, int hash, byte state) {
		// see Knuth, p. 529
		int length = _set_xy.length / 2;
		int probe = 1 + (hash % (length - 2));
		final int loopIndex = index;
		int mul_index;

		do {
			index -= probe;
			if (index < 0) {
				index += length;
			}
			state = _states[index];
			//
			if (state == FREE) {
				return -1;
			}

			//
			mul_index = index * 2;
			if (key_x == _set_xy[mul_index] && key_y == _set_xy[mul_index+1] && state != REMOVED) {
				return index;
			}
		} while (index != loopIndex);

		return -1;
	}

	/**
	 * Locates the index at which <tt>val</tt> can be inserted.  if
	 * there is already a value equal()ing <tt>val</tt> in the set,
	 * returns that value as a negative integer.
	 *
	 * @return an <code>int</code> value
	 */
	protected int insertKey( long val_x, long val_y ) {
		int hash, index;

		hash = hashVec2(val_x, val_y) & 0x7fffffff;
		index = hash % _states.length;
		byte state = _states[index];

		consumeFreeSlot = false;

		if (state == FREE) {
			consumeFreeSlot = true;
			insertKeyAt(index, val_x, val_y);

			return index;       // empty, all done
		}

		int new_index = index * 2;
		if (state == FULL && _set_xy[new_index] == val_x && _set_xy[new_index+1] == val_y) {
			return -index - 1;   // already stored
		}

		// already FULL or REMOVED, must probe
		return insertKeyRehash(val_x, val_y, index, hash, state);
	}

	int insertKeyRehash(long val_x, long val_y, int index, int hash, byte state) {
		// compute the double hash
		final int length = _set_xy.length / 2;
		int probe = 1 + (hash % (length - 2));
		final int loopIndex = index;
		int firstRemoved = -1;
		int mul_index;
		/*
		 * Look until FREE slot or we start to loop
		 */
		do {
			// Identify first removed slot
			if (state == REMOVED && firstRemoved == -1)
				firstRemoved = index;

			index -= probe;
			if (index < 0) {
				index += length;
			}
			state = _states[index];

			// A FREE slot stops the search
			if (state == FREE) {
				if (firstRemoved != -1) {
					insertKeyAt(firstRemoved, val_x, val_y);
					return firstRemoved;
				} else {
					consumeFreeSlot = true;
					insertKeyAt(index, val_x, val_y);
					return index;
				}
			}

			mul_index = index * 2;
			if (state == FULL && _set_xy[mul_index] == val_x && _set_xy[mul_index+1] == val_y) {
				return -index - 1;
			}

			// Detect loop
		} while (index != loopIndex);

		// We inspected all reachable slots and did not find a FREE one
		// If we found a REMOVED slot we return the first one found
		if (firstRemoved != -1) {
			insertKeyAt(firstRemoved, val_x, val_y);
			return firstRemoved;
		}

		// Can a resizing strategy be found that resizes the set?
		throw new IllegalStateException("No free or removed slots available. Key set full?!!");
	}

	void insertKeyAt(int index, long val_x, long val_y) {
		int new_index = index * 2;
		_set_xy[new_index] = val_x; // insert value
		_set_xy[new_index+1] = val_y;
		_states[index] = FULL;
	}
}
