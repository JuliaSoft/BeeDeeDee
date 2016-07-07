package com.juliasoft.beedeedee.factories;

import java.util.Arrays;

import com.juliasoft.beedeedee.er.EquivalenceRelation;

// FIXME not thread-safe
public class RenameWithLeaderCache {

	private int size;
	private int[] cache;
	private EquivalenceRelation[] ers;
	private int[] results;

	RenameWithLeaderCache(int size) {
		this.size = size;
		cache = new int[size];
		ers = new EquivalenceRelation[size];
		Arrays.fill(cache, -1);
		results = new int[size];
	}

	void clear() {
		Arrays.fill(cache, -1);
	}

	public int get(int bdd, EquivalenceRelation er) {
		int pos = hash(bdd, er);

		if (cache[pos] == bdd && ers[pos].equals(er)) {
			return results[pos];
		}
		return -1;
	}

	public void put(int bdd, EquivalenceRelation er, int res) {
		int pos = hash(bdd, er);

		cache[pos] = bdd;
		ers[pos] = er.copy();
		results[pos] = res;
	}

	private int hash(int bdd, EquivalenceRelation er) {
		return Math.abs((bdd ^ er.hashCode()) % size);
	}

}
