package com.juliasoft.beedeedee.factories;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.juliasoft.beedeedee.er.Pair;

@SuppressWarnings("rawtypes")
class EquivCache {

	private int size;
	private int[] cache;
	private Set[] results;

	EquivCache(int size) {
		this.size = size;
		cache = new int[size];
		Arrays.fill(cache, -1);
		results = new Set[size];
	}

	void clear() {
		Arrays.fill(cache, -1);
	}

	@SuppressWarnings("unchecked")
	public Set<Pair> get(int bdd) {
		int pos = hash(bdd);

		if (cache[pos] == bdd) {
			return results[pos];
		}
		return null;
	}

	public void put(int bdd, Set<Pair> result) {
		int pos = hash(bdd);

		cache[pos] = bdd;
		results[pos] = new HashSet<>(result);
	}

	private int hash(int bdd) {
		return Math.abs(bdd % size);
	}
}
