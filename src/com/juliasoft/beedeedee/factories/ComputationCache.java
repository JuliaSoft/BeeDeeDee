package com.juliasoft.beedeedee.factories;

class ComputationCache {
	private final static int ENTRY_SIZE = 4;
	private final int[] cache;
	private final int size;

	ComputationCache(int size) {
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

	private final Object[] locks = new Object[1000];

	int get(Operator op, int bdd1, int bdd2) {
		if (op != Operator.IMP)
			// we ensure that symmetrical operations are kept in a normal form
			if (bdd1 > bdd2) {
				int temp = bdd1;
				bdd1 = bdd2;
				bdd2 = temp;
			}

		int ordinal = op.ordinal();
		int pos = hash(ordinal, bdd1, bdd2), pos1 = pos + 1, pos2 = pos + 2;
		int[] cache = this.cache;

		if (cache[pos1] == bdd1 && cache[pos2] == bdd2 && cache[pos] == ordinal)
			synchronized (locks[pos % locks.length]) {
				return (cache[pos1] == bdd1 && cache[pos2] == bdd2 && cache[pos] == ordinal) ? cache[pos + 3] : -1;
			}

		return -1;
	}

	void put(Operator op, int bdd1, int bdd2, int result) {
		if (op != Operator.IMP)
			// we ensure that symmetrical operations are kept in a normal form
			if (bdd1 > bdd2) {
				int temp = bdd1;
				bdd1 = bdd2;
				bdd2 = temp;
			}

		int ordinal = op.ordinal();
		int pos = hash(ordinal, bdd1, bdd2), pos1 = pos + 1, pos2 = pos + 2;
		int[] cache = this.cache;

		if (cache[pos1] != bdd1 || cache[pos2] != bdd2 || cache[pos] != ordinal)
			synchronized (locks[pos % locks.length]) {
				cache[pos] = ordinal;
				cache[pos1] = bdd1;
				cache[pos2] = bdd2;
				cache[pos + 3] = result;
			}
	}

	int getSize() {
		return size;
	}

	private int hash(int op, int bdd1, int bdd2) {
		return ENTRY_SIZE * (Math.abs(op ^ bdd1 ^ (bdd2 << 2)) % size);
	}
}