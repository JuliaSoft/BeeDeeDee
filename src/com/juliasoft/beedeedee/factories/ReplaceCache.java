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