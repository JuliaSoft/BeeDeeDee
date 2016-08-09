package com.juliasoft.beedeedee.factories;

import java.util.Arrays;

public class SqueezeEquivCache {
	private final int[] cacheAndResults;
	private final static int SIZE = 100;

	SqueezeEquivCache() {
		this.cacheAndResults = new int[SIZE * 2];
		Arrays.fill(cacheAndResults, -1);
	}

	public int get(int bdd) {
		int pos = hash(bdd);

		return cacheAndResults[pos] == bdd ? cacheAndResults[SIZE + pos] : -1;
	}

	public void put(int bdd, int res) {
		int pos = hash(bdd);

		cacheAndResults[pos] = bdd;
		cacheAndResults[SIZE + pos] = res;
	}

	private int hash(int bdd) {
		return Math.abs(bdd % SIZE);
	}
}