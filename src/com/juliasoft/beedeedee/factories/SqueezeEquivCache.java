package com.juliasoft.beedeedee.factories;

import java.util.Arrays;

import com.juliasoft.beedeedee.er.EquivalenceRelation;

public class SqueezeEquivCache {
	private final int[] bdds;
	private EquivalenceRelation[] ers;
	private final int[] results;

	SqueezeEquivCache(int size) {
		this.bdds = new int[size];
		this.ers = new EquivalenceRelation[size];
		this.results = new int[size];
		clear();
	}

	public void clear() {
		Arrays.fill(bdds, -1);
	}

	public int get(int bdd, EquivalenceRelation er) {
		int pos = hash(bdd);

		return bdds[pos] == bdd && ers[pos].equals(er) ? results[pos] : -1;
	}

	public void put(int bdd, EquivalenceRelation er, int res) {
		int pos = hash(bdd);

		ers[pos] = er;
		results[pos] = res;
		bdds[pos] = bdd;
	}

	private int hash(int bdd) {
		return Math.abs(bdd % bdds.length);
	}
}