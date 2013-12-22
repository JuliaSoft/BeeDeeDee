package com.juliasoft.beedeedee.factories;

import java.util.Map;

class ReplaceCache {
	private final static int ENTRY_SIZE = 2;
	private final int[] cache;
	@SuppressWarnings("rawtypes")
	private final Map[] renamings;
	private final int size;
	private final Object[] locks = new Object[100];
	
	public ReplaceCache(int size) {
		this.size = size;
		int arraySize = size * ENTRY_SIZE;
		this.cache = new int[arraySize];
		for (int i = 0; i < arraySize; i += ENTRY_SIZE)
			cache[i] = -1;
		renamings = new Map[arraySize];
		for (int pos = 0; pos < locks.length; pos++)
			locks[pos] = new Object();
	}

	void clear() {
		int arraySize = size * ENTRY_SIZE;
		for (int i = 0; i < arraySize; i += ENTRY_SIZE)
			cache[i] = -1;
	}

	int get(int bdd, Map<Integer, Integer> renaming, int hashOfRenaming) {
		int pos = hash(bdd, hashOfRenaming);

		// avoid double call to expensive equals on maps!
		@SuppressWarnings("rawtypes")
		Map oldRenaming;

		if (cache[pos] == bdd && (oldRenaming = renamings[pos]) != null &&
				(oldRenaming == renaming || oldRenaming.equals(renaming)))
			synchronized (locks[pos % locks.length]) {
				return (cache[pos] == bdd && oldRenaming == renamings[pos++]) ? cache[pos] : -1;
			}

		return -1;
	}

	private int hash(int bdd, int hashOfRenaming) {
		return ENTRY_SIZE * (Math.abs((bdd ^ hashOfRenaming)) % size);
	}

	void put(int bdd, Map<Integer, Integer> renaming, int result, int hashOfRenaming) {
		int pos = hash(bdd, hashOfRenaming);

		synchronized (locks[pos % locks.length]) {
			cache[pos] = bdd;
			renamings[pos++] = renaming;
			cache[pos] = result;
		}
	}
}