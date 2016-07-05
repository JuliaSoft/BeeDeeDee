package com.juliasoft.beedeedee.factories;

import java.util.Arrays;

public class SqueezeEquivCache {

	private int size;
	private int[] cache;
	private int[] results;

	SqueezeEquivCache(int size) {
		this.size = size;
		cache = new int[size];
		Arrays.fill(cache, -1);
		results = new int[size];
	}

	void clear() {
		Arrays.fill(cache, -1);
	}

	public int get(int bdd) {
		int pos = hash(bdd);

		if (cache[pos] == bdd) {
			return results[pos];
		}
		return -1;
	}

	public void put(int bdd, int res) {
		int pos = hash(bdd);

		cache[pos] = bdd;
		results[pos] = res;
	}

	private int hash(int bdd) {
		return Math.abs(bdd % size);
	}

}
