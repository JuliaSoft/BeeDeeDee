package com.juliasoft.beedeedee.factories;

import java.util.Arrays;

import com.juliasoft.beedeedee.factories.ERFactory.EquivResult;

public class EquivCache {
	private final int[] bdds;
	private final EquivResult[] results;

	EquivCache(int size) {
		this.bdds = new int[size];
		this.results = new EquivResult[size];
		clear();
	}

	void clear() {
		Arrays.fill(bdds, -1);
	}

	public EquivResult get(int bdd) {
		int pos = hash(bdd);

		return bdds[pos] == bdd ? results[pos] : null;
	}

	public void put(int bdd, EquivResult result) {
		int pos = hash(bdd);

		bdds[pos] = bdd;
		results[pos] = result;
	}

	private int hash(int bdd) {
		return Math.abs(bdd % bdds.length);
	}
}