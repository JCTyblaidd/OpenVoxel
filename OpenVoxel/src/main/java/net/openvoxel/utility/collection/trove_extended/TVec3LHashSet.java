package net.openvoxel.utility.collection.trove_extended;

import java.util.Arrays;

public class TVec3LHashSet extends TVec3LHash {
	static final long serialVersionUID = 1L;


	/**
	 * Creates a new <code>TLongHashSet</code> instance with the default
	 * capacity and load factor.
	 */
	public TVec3LHashSet() {
		super();
	}


	/**
	 * Creates a new <code>TLongHashSet</code> instance with a prime
	 * capacity equal to or greater than <tt>initialCapacity</tt> and
	 * with the default load factor.
	 *
	 * @param initialCapacity an <code>int</code> value
	 */
	public TVec3LHashSet( int initialCapacity ) {
		super( initialCapacity );
	}


	/**
	 * Creates a new <code>TIntHash</code> instance with a prime
	 * value at or near the specified capacity and load factor.
	 *
	 * @param initialCapacity used to find a prime capacity for the table.
	 * @param load_factor used to calculate the threshold over which
	 * rehashing takes place.
	 */
	public TVec3LHashSet( int initialCapacity, float load_factor ) {
		super( initialCapacity, load_factor );
	}


	/**
	 * Creates a new <code>TLongHashSet</code> instance with a prime
	 * capacity equal to or greater than <tt>initial_capacity</tt> and
	 * with the specified load factor.
	 *
	 * @param initial_capacity an <code>int</code> value
	 * @param load_factor a <code>float</code> value
	 * @param no_entry_value a <code>long</code> value that represents null.
	 */
	public TVec3LHashSet( int initial_capacity, float load_factor,
	                     long no_entry_value ) {
		super( initial_capacity, load_factor, no_entry_value );
		//noinspection RedundantCast
		if ( no_entry_value != ( long ) 0 ) {
			Arrays.fill( _set_xyz, no_entry_value );
		}
	}


	/**
	 * Creates a new <code>TLongHashSet</code> instance that is a copy
	 * of the existing Collection.
	 *
	 * @param collection a <tt>Collection</tt> that will be duplicated.
	 */
	/*
	public TVec3LHashSet( Collection<? extends Long> collection ) {
		this( Math.max( collection.size(), DEFAULT_CAPACITY ) );
		addAll( collection );
	}*/


	/*
	 * Creates a new <code>TLongHashSet</code> instance that is a copy
	 * of the existing set.
	 *
	 * @param collection a <tt>TLongSet</tt> that will be duplicated.
	 */
	/*
	public TVec3LHashSet( TLongCollection collection ) {
		this( Math.max( collection.size(), DEFAULT_CAPACITY ) );
		if ( collection instanceof TVec3LHashSet) {
			TLongHashSet hashset = ( TLongHashSet ) collection;
			this._loadFactor = hashset._loadFactor;
			this.no_entry_value = hashset.no_entry_value;
			//noinspection RedundantCast
			if ( this.no_entry_value != ( long ) 0 ) {
				Arrays.fill( _set, this.no_entry_value );
			}
			setUp( (int) Math.ceil( DEFAULT_CAPACITY / _loadFactor ) );
		}
		addAll( collection );
	}*/


	/**
	 * Creates a new <code>TLongHashSet</code> instance containing the
	 * elements of <tt>array</tt>.
	 *
	 * @param array an array of <code>long</code> primitives
	 */
	/*
	public TLongHashSet( long[] array ) {
		this( Math.max( array.length, DEFAULT_CAPACITY ) );
		addAll( array );
	}*/


	/** {@inheritDoc} */
	/*
	public TLongIterator iterator() {
		return new TLongHashSet.TLongHashIterator( this );
	}*/


	/** {@inheritDoc} */
	/*
	public long[] toArray() {
		long[] result = new long[ size() ];
		long[] set = _set;
		byte[] states = _states;

		for ( int i = states.length, j = 0; i-- > 0; ) {
			if ( states[i] == FULL ) {
				result[j++] = set[i];
			}
		}
		return result;
	}*/


	/** {@inheritDoc} */
	/*
	public long[] toArray( long[] dest ) {
		long[] set = _set;
		byte[] states = _states;

		for ( int i = states.length, j = 0; i-- > 0; ) {
			if ( states[i] == FULL ) {
				dest[j++] = set[i];
			}
		}

		if ( dest.length > _size ) {
			dest[_size] = no_entry_value;
		}
		return dest;
	}*/


	/** {@inheritDoc} */
	public boolean add( long x, long y, long z ) {
		int index = insertKey(x,y,z);

		if ( index < 0 ) {
			return false;       // already present in set, nothing to add
		}

		postInsertHook( consumeFreeSlot );

		return true;            // yes, we added something
	}


	/** {@inheritDoc} */
	public boolean remove( long x, long y, long z ) {
		int index = index(x,y,z);
		if ( index >= 0 ) {
			removeAt( index );
			return true;
		}
		return false;
	}


	/** {@inheritDoc} */
	/*
	public boolean containsAll( Collection<?> collection ) {
		for ( Object element : collection ) {
			if ( element instanceof Long ) {
				long c = ( ( Long ) element ).longValue();
				if ( ! contains( c ) ) {
					return false;
				}
			} else {
				return false;
			}

		}
		return true;
	}*/


	/** {@inheritDoc} */
	/*
	public boolean containsAll( TLongCollection collection ) {
		TLongIterator iter = collection.iterator();
		while ( iter.hasNext() ) {
			long element = iter.next();
			if ( ! contains( element ) ) {
				return false;
			}
		}
		return true;
	}*/


	/** {@inheritDoc} */
	/*
	public boolean containsAll( long[] array ) {
		for ( int i = array.length; i-- > 0; ) {
			if ( ! contains( array[i] ) ) {
				return false;
			}
		}
		return true;
	}*/


	/** {@inheritDoc} */
	/*
	public boolean addAll( Collection<? extends Long> collection ) {
		boolean changed = false;
		for ( Long element : collection ) {
			long e = element.longValue();
			if ( add( e ) ) {
				changed = true;
			}
		}
		return changed;
	}*/


	/** {@inheritDoc} */
	/*
	public boolean addAll( TLongCollection collection ) {
		boolean changed = false;
		TLongIterator iter = collection.iterator();
		while ( iter.hasNext() ) {
			long element = iter.next();
			if ( add( element ) ) {
				changed = true;
			}
		}
		return changed;
	}*/


	/** {@inheritDoc} */
	/*
	public boolean addAll( long[] array ) {
		boolean changed = false;
		for ( int i = array.length; i-- > 0; ) {
			if ( add( array[i] ) ) {
				changed = true;
			}
		}
		return changed;
	}*/


	/** {@inheritDoc} */
	@SuppressWarnings({"SuspiciousMethodCalls"})
	/*
	public boolean retainAll( Collection<?> collection ) {
		boolean modified = false;
		TLongIterator iter = iterator();
		while ( iter.hasNext() ) {
			if ( ! collection.contains( Long.valueOf ( iter.next() ) ) ) {
				iter.remove();
				modified = true;
			}
		}
		return modified;
	}*/


	/** {@inheritDoc} */
	/*
	public boolean retainAll( TLongCollection collection ) {
		if ( this == collection ) {
			return false;
		}
		boolean modified = false;
		TLongIterator iter = iterator();
		while ( iter.hasNext() ) {
			if ( ! collection.contains( iter.next() ) ) {
				iter.remove();
				modified = true;
			}
		}
		return modified;
	}*/


	/** {@inheritDoc} */
	/*
	public boolean retainAll( long[] array ) {
		boolean changed = false;
		Arrays.sort( array );
		long[] set = _set;
		byte[] states = _states;

		_autoCompactTemporaryDisable = true;
		for ( int i = set.length; i-- > 0; ) {
			if ( states[i] == FULL && ( Arrays.binarySearch( array, set[i] ) < 0) ) {
				removeAt( i );
				changed = true;
			}
		}
		_autoCompactTemporaryDisable = false;

		return changed;
	}*/


	/** {@inheritDoc} */
	/*
	public boolean removeAll( Collection<?> collection ) {
		boolean changed = false;
		for ( Object element : collection ) {
			if ( element instanceof Long ) {
				long c = ( ( Long ) element ).longValue();
				if ( remove( c ) ) {
					changed = true;
				}
			}
		}
		return changed;
	}*/


	/** {@inheritDoc} */
	/*
	public boolean removeAll( TLongCollection collection ) {
		boolean changed = false;
		TLongIterator iter = collection.iterator();
		while ( iter.hasNext() ) {
			long element = iter.next();
			if ( remove( element ) ) {
				changed = true;
			}
		}
		return changed;
	}*/


	/** {@inheritDoc} */
	/*
	public boolean removeAll( long[] array ) {
		boolean changed = false;
		for ( int i = array.length; i-- > 0; ) {
			if ( remove(array[i]) ) {
				changed = true;
			}
		}
		return changed;
	}*/


	/** {@inheritDoc} */
	public void clear() {
		super.clear();
		long[] set = _set_xyz;
		byte[] states = _states;

		for ( int i = (set.length/3); i-- > 0; ) {
			set[i*3] = no_entry_value;
			set[i*3+1] = no_entry_value;
			set[i*3+2] = no_entry_value;
			states[i] = FREE;
		}
	}


	/** {@inheritDoc} */
	protected void rehash( int newCapacity ) {
		int oldCapacity = _set_xyz.length / 3;

		long oldSet[] = _set_xyz;
		byte oldStates[] = _states;

		_set_xyz = new long[newCapacity * 3];
		_states = new byte[newCapacity];

		for ( int i = oldCapacity; i-- > 0; ) {
			if( oldStates[i] == FULL ) {
				int i3 = i*3;
				long o1 = oldSet[i3];
				long o2 = oldSet[i3+1];
				long o3 = oldSet[i3+2];
				int index = insertKey(o1,o2,o3);
			}
		}
	}


	/** {@inheritDoc} */
	/*
	public boolean equals( Object other ) {
		if ( ! ( other instanceof TLongSet) ) {
			return false;
		}
		TLongSet that = ( TLongSet ) other;
		if ( that.size() != this.size() ) {
			return false;
		}
		for ( int i = _states.length; i-- > 0; ) {
			if ( _states[i] == FULL ) {
				if ( ! that.contains( _set[i] ) ) {
					return false;
				}
			}
		}
		return true;
	}*/


	/** {@inheritDoc} */
	/*
	public int hashCode() {
		int hashcode = 0;
		for ( int i = _states.length; i-- > 0; ) {
			if ( _states[i] == FULL ) {
				hashcode += HashFunctions.hash( _set[i] );
			}
		}
		return hashcode;
	}*/


	/** {@inheritDoc} */
	/*
	public String toString() {
		StringBuilder buffy = new StringBuilder( _size * 2 + 2 );
		buffy.append("{");
		for ( int i = _states.length, j = 1; i-- > 0; ) {
			if ( _states[i] == FULL ) {
				buffy.append( _set[i] );
				if ( j++ < _size ) {
					buffy.append( "," );
				}
			}
		}
		buffy.append("}");
		return buffy.toString();
	}*/


//	class TLongHashIterator extends THashPrimitiveIterator implements TLongIterator {
//
//		/** the collection on which the iterator operates */
//		private final TLongHash _hash;
//
//		/** {@inheritDoc} */
//		public TLongHashIterator( TLongHash hash ) {
//			super( hash );
//			this._hash = hash;
//		}
//
//		/** {@inheritDoc} */
//		public long next() {
//			moveToNextIndex();
//			return _hash._set[_index];
//		}
//	}

}
