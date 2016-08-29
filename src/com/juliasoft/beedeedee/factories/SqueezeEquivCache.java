package com.juliasoft.beedeedee.factories;

import java.util.Arrays;

public class SqueezeEquivCache {
	private final int[] bdds;
	private EquivalenceRelation[] ers;
	private final int[] results;
	private final Object[] locks = new Object[100];

	SqueezeEquivCache(int size) {
		this.bdds = new int[size];
		this.ers = new EquivalenceRelation[size];
		this.results = new int[size];
		clear();
		for (int i = 0; i < locks.length; i++)
			locks[i] = new Object();
	}

	public void clear() {
		Arrays.fill(bdds, -1);
	}

	public int get(int bdd, EquivalenceRelation er) {
		int pos = hash(bdd, er);

		synchronized (locks[pos % locks.length]) {
			return bdds[pos] == bdd && ers[pos].equals(er) ? results[pos] : -1;
		}
	}

	public void put(int bdd, EquivalenceRelation er, int res) {
		int pos = hash(bdd, er);

		synchronized (locks[pos % locks.length]) {
			ers[pos] = er;
			results[pos] = res;
			bdds[pos] = bdd;
		}
	}

	private int hash(int bdd, EquivalenceRelation er) {
		return Math.abs((bdd ^ er.hashCode()) % bdds.length);
	}
}