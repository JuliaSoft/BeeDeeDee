package com.juliasoft.beedeedee.er;

import java.util.Arrays;
import java.util.BitSet;

public class RenameWithLeaderInternalCache {

	private int size;
	private int[] cache;
	private int[] levels;
	private BitSet[] ts;
	private int[] results;

	RenameWithLeaderInternalCache(int size) {
		this.size = size;
		cache = new int[size];
		levels = new int[size];
		ts = new BitSet[size];
		Arrays.fill(cache, -1);
		results = new int[size];
	}

	public int get(int f, int level, BitSet t) {
		int pos = hash(f, level, t);

		if (cache[pos] == f && levels[pos] == level && ts[pos].equals(t)) {
			return results[pos];
		}
		return -1;
	}

	public void put(int f, int level, BitSet t, int res) {
		int pos = hash(f, level, t);

		cache[pos] = f;
		levels[pos] = level;
		ts[pos] = (BitSet) t.clone();
		results[pos] = res;
	}

	private int hash(int f, int level, BitSet t) {
		return Math.abs((f ^ (level << 24) ^ t.hashCode()) % size);
	}

}
