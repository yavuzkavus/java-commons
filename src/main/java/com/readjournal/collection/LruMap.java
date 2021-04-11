package com.readjournal.collection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

public class LruMap<K,V> extends LinkedHashMap<K,V> {
	private static final long serialVersionUID = 1L;
	private transient int maxSize = 5;
	private transient Entry<K, V> removedEntry;

	public LruMap(int maxSize) {
		this(maxSize, maxSize);
	}

	public LruMap(int initialCapacity, int maxSize) {
		super(initialCapacity, 0.75f, true);
		this.maxSize = maxSize;
	}

	@Override
	protected boolean removeEldestEntry(Entry<K, V> eldest) {
		boolean remove = size()>maxSize;
		removedEntry = remove ? eldest : null;
		if( remove )
			eldestRemoved(removedEntry);
		return remove;
	}

	public void eldestRemoved(Entry<K, V> removed) {
		//override, if you wish
	}

	public Entry<K, V> getRemovedEntry() {
		return removedEntry;
	}

	public V getRemovedValue() {
		return removedEntry==null ? null : removedEntry.getValue();
	}

	public void setMaxSize(int maxSize) {
		this.maxSize = maxSize;
		if( size()>maxSize )
			removeEntries(0, size()-maxSize);
	}

	public int getMaxSize() {
		return maxSize;
	}

	public void removeEntries(int begin, int end) {
		List<K> list = new ArrayList<K>( this.keySet() );
		end = Math.min(end, size());
		for(int i=begin; i<end; i++) {
			K key = list.get(i);
			this.remove(key);
		}
	}
}