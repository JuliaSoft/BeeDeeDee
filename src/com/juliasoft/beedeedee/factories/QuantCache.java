package com.juliasoft.beedeedee.factories;

import java.util.Arrays;

class QuantCache {
	private final static int ENTRY_SIZE = 2;
	private final int[] cache;
	private final int[][] varss;
	private final int size;
	private final Object[] locks = new Object[100];
	
	QuantCache(int size) {
		this.size = size;
		int arraySize = size * ENTRY_SIZE;
		this.cache = new int[arraySize];
		for (int i = 0; i < arraySize; i += ENTRY_SIZE)
			cache[i] = -1;
		this.varss = new int[arraySize][];
		for (int pos = 0; pos < locks.length; pos++)
			locks[pos] = new Object();
	}

	void clear() {
		int arraySize = size * ENTRY_SIZE;
		for (int i = 0; i < arraySize; i += ENTRY_SIZE)
			cache[i] = -1;
	}

	int get(boolean exist, int bdd, int[] vs, int hashCodeOfVs) {
		if (exist)
			bdd = -bdd;

		int pos = hash(bdd, hashCodeOfVs);

		// avoid double call to expensive equals on arrays!
		int[] oldVarss = varss[pos];
		if (cache[pos] == bdd && (oldVarss == vs || Arrays.equals(oldVarss, vs)))
			synchronized (locks[pos % locks.length]) {
				return cache[pos] == bdd && oldVarss == varss[pos++] ? cache[pos] : -1;
			}

		return -1;
	}

	private int hash(int bdd, int hashCodeOfVs) {
		return ENTRY_SIZE * (Math.abs(bdd ^ hashCodeOfVs) % size);
	}

	void put(boolean exist, int bdd, int[] vs, int hashCodeOfVs, int result) {
		if (exist)
			bdd = -bdd;

		int pos = hash(bdd, hashCodeOfVs);

		synchronized (locks[pos % locks.length]) {
			varss[pos] = vs;
			cache[pos++] = bdd;
			cache[pos] = result;
		}
	}
}