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
 * The cache for compose operations.
 */
class ComposeCache {
	private final static int ENTRY_SIZE = 4;
	private final int[] cache;
	private final int size;
	private final Object[] locks = new Object[100];
	
	/**
	 * Constructs a ComposeCache of the given size.
	 * 
	 * @param size the size of the cache
	 */
	ComposeCache(int size) {
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

	/**
	 * Gets an entry from this cache.
	 * 
	 * @param bdd1 the first operand bdd index
	 * @param bdd2 the second operand bdd index
	 * @param var the variable to compose
	 * @return the index of the result, or -1 if not found
	 */
	int get(int bdd1, int bdd2, int var) {
		int pos = hash(bdd1, bdd2, var);
		int[] cache = this.cache;

		if (cache[pos++] == bdd1 && cache[pos++] == bdd2 && cache[pos] == var) {
			pos -= 2;

			synchronized (locks[pos % locks.length]) {
				return (cache[pos++] == bdd1 && cache[pos++] == bdd2 && cache[pos++] == var) ? cache[pos] : -1;
			}
		}

		return -1;
	}

	private static int TRIPLE(int a, int b, int c) {
		int sum = a + b + c;
		return ((sum * (sum + 1)) >> 1) + a;
	}

	private int hash(int bdd1, int bdd2, int var) {
		return ENTRY_SIZE * (Math.abs(TRIPLE(bdd1, bdd2, var)) % size);
	}

	/**
	 * Puts an entry into this cache.
	 * 
	 * @param bdd1 the first operand bdd index
	 * @param bdd2 the second operand bdd index
	 * @param var the variable to compose
	 * @param result the computation result
	 */
	void put(int bdd1, int bdd2, int var, int result) {
		int pos = hash(bdd1, bdd2, var);
		int[] cache = this.cache;

		synchronized (locks[pos % locks.length]) {
			cache[pos++] = bdd1;
			cache[pos++] = bdd2;
			cache[pos++] = var;
			cache[pos] = result;
		}
	}
}