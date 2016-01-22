/* 
  Copyright 2014 Julia s.r.l.
    
  This file is part of BeeDeeDee.

  BeeDeeDee is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  BeeDeeDee is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with BeeDeeDee.  If not, see <http://www.gnu.org/licenses/>.
*/
package com.juliasoft.beedeedee.factories;

/**
 * The cache for APPLY computations.
 */
class ComputationCache {
	private final static int ENTRY_SIZE = 4;
	private final int[] cache;
	private final int size;

	/**
	 * Constructs a ComputationCache of the given size.
	 * 
	 * @param size the size of the cache
	 */
	ComputationCache(int size) {
		this.size = size;
		int arraySize = size * ENTRY_SIZE;
		this.cache = new int[arraySize];
		for (int i = 0; i < arraySize; i += ENTRY_SIZE)
			cache[i] = -1;

		for (int pos = 0; pos < locks.length; pos++)
			locks[pos] = new Object();
	}

	/**
	 * Clears all the entries in this cache.
	 */
	void clear() {
		int arraySize = size * ENTRY_SIZE;
		for (int i = 0; i < arraySize; i += ENTRY_SIZE)
			cache[i] = -1;
	}

	private final Object[] locks = new Object[1000];

	/**
	 * Gets an entry from this cache.
	 * 
	 * @param op the operator
	 * @param bdd1 the first operand bdd index
	 * @param bdd2 the second operand bdd index
	 * @return the index of the result, or -1 if not found
	 */
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

	/**
	 * Puts an entry into this cache.
	 * 
	 * @param op the operator
	 * @param bdd1 the first operand bdd index
	 * @param bdd2 the second operand bdd index
	 * @param result the computation result
	 */
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

	/**
	 * Returns the size of this cache.
	 * 
	 * @return the size
	 */
	int getSize() {
		return size;
	}

	private int hash(int op, int bdd1, int bdd2) {
		return ENTRY_SIZE * (Math.abs(op ^ bdd1 ^ (bdd2 << 2)) % size);
	}
}