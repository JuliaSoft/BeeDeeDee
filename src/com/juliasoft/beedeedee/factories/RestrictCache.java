package com.juliasoft.beedeedee.factories;

class RestrictCache {
	private final static int ENTRY_SIZE = 4;
	private final int[] cache;
	private final int size;
	private final Object[] locks = new Object[100];
	
	RestrictCache(int size) {
		this.size = size;
		int arraySize = size * ENTRY_SIZE;
		this.cache = new int[arraySize];
		for (int i = 0; i < arraySize; i += ENTRY_SIZE)
			cache[i] = -1;
		for (int pos = 0; pos < locks.length; pos++)
			locks[pos] = new Object();
	}

	void clear() {
		int arraySize = size * ENTRY_SIZE;
		for (int i = 0; i < arraySize; i += ENTRY_SIZE)
			cache[i] = -1;
	}

	int get(int bdd, int var, boolean value) {
		int pos = hash(bdd, var);
		int[] cache = this.cache;

		if (cache[pos++] == bdd && cache[pos++] == var && cache[pos] == (value ? 1 : 0)) {
			pos -= 2;

			synchronized (locks[pos % locks.length]) {
				return (cache[pos++] == bdd && cache[pos++] == var && cache[pos++] == (value ? 1 : 0)) ? cache[pos] : -1;
			}
		}

		return -1;
	}

	private static int PAIR(int a, int b) {
		int sum = a + b;
		return ((sum * (sum + 1)) >> 1) + a;
	}

	private int hash(int bdd, int var) {
		return ENTRY_SIZE * (Math.abs(PAIR(bdd, var)) % size);
	}

	void put(int bdd, int var, boolean value, int result) {
		int pos = hash(bdd, var);
		int[] cache = this.cache;

		synchronized (locks[pos % locks.length]) {
			cache[pos++] = bdd;
			cache[pos++] = var;
			cache[pos++] = value ? 1 : 0;
			cache[pos] = result;
		}
	}
}