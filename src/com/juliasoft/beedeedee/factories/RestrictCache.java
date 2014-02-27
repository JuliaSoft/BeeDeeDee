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
 * The cache for restrict operations.
 */
class RestrictCache {
	private final static int ENTRY_SIZE = 4;
	private final int[] cache;
	private final int size;
	private final Object[] locks = new Object[100];
	
	/**
	 * Constructs a RestrictCache of the given size.
	 * 
	 * @param size the size of the cache
	 */
	RestrictCache(int size) {
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
	 * @param bdd the operand bdd index
	 * @param var the variable to restrict
	 * @param value the value to restrict the variable to
	 * @return the index of the result, or -1 if not found
	 */
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

	/**
	 * Puts an entry into this cache.
	 * 
	 * @param bdd the operand bdd index
	 * @param var the variable to restrict
	 * @param value the value to restrict the variable to
	 * @param result the computation result
	 */
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