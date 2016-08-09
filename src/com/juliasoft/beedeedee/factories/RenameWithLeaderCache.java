package com.juliasoft.beedeedee.factories;

import java.util.Arrays;

import com.juliasoft.beedeedee.er.EquivalenceRelation;

// FIXME not thread-safe
public class RenameWithLeaderCache {
	private int[] bdds;
	private EquivalenceRelation[] ers;
	private int[] results;

	RenameWithLeaderCache(int size) {
		bdds = new int[size];
		ers = new EquivalenceRelation[size];
		results = new int[size];
		clear();
	}

	void clear() {
		Arrays.fill(bdds, -1);
	}

	public int get(int bdd, EquivalenceRelation er) {
		int pos = hash(bdd, er);

		return (bdds[pos] == bdd && ers[pos].equals(er)) ? results[pos] : -1;
	}

	public void put(int bdd, EquivalenceRelation er, int res) {
		int pos = hash(bdd, er);

		bdds[pos] = bdd;
		ers[pos] = er;
		results[pos] = res;
	}

	private int hash(int bdd, EquivalenceRelation er) {
		return Math.abs((bdd ^ er.hashCode()) % bdds.length);
	}
}