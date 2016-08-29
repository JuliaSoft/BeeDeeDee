package com.juliasoft.beedeedee.factories;

import java.util.Arrays;

public class RenameWithLeaderCache {
	private int[] bdds;
	private EquivalenceRelation[] ers;
	private int[] results;
	private final Object[] locks = new Object[100];

	RenameWithLeaderCache(int size) {
		bdds = new int[size];
		ers = new EquivalenceRelation[size];
		results = new int[size];
		clear();
		for (int i = 0; i < locks.length; i++)
			locks[i] = new Object();
	}

	void clear() {
		Arrays.fill(bdds, -1);
	}

	public int get(int bdd, EquivalenceRelation er) {
		int pos = hash(bdd, er);

		synchronized (locks[pos % locks.length]) {
			return bdds[pos] == bdd && ers[pos] == er ? results[pos] : -1;
		}
	}

	public void put(int bdd, EquivalenceRelation er, int res) {
		int pos = hash(bdd, er);

		synchronized (locks[pos % locks.length]) {
			bdds[pos] = bdd;
			ers[pos] = er;
			results[pos] = res;
		}
	}

	private int hash(int bdd, EquivalenceRelation er) {
		return Math.abs((bdd ^ er.hashCode()) % bdds.length);
	}
}