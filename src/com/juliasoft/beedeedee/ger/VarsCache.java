package com.juliasoft.beedeedee.ger;

import java.util.BitSet;

import com.juliasoft.beedeedee.bdd.BDD;

class VarsCache {

	private int size;
	private int[] cache;
	private BitSet[] ss;
	private BitSet[] is;
	private boolean[] entaileds;
	private BitSet[] results;

	VarsCache(int size) {
		this.size = size;
		cache = new int[size];
		ss = new BitSet[size];
		is = new BitSet[size];
		entaileds = new boolean[size];
		results = new BitSet[size];
	}

	public BitSet get(BDD f, BitSet s, BitSet i, boolean entailed) {
		int hashCodeAux = f.hashCodeAux();
		int pos = hash(hashCodeAux, s, i);

		if (cache[pos] == hashCodeAux && s.equals(ss[pos]) && i.equals(is[pos]) && entaileds[pos] == entailed) {
			return results[pos];
		}
		return null;
	}

	public void put(BDD f, BitSet s, BitSet i, boolean entailed, BitSet result) {
		int hashCodeAux = f.hashCodeAux();
		int pos = hash(hashCodeAux, s, i);

		cache[pos] = hashCodeAux;
		ss[pos] = (BitSet) s.clone();
		is[pos] = (BitSet) i.clone();
		entaileds[pos] = entailed;
		results[pos] = (BitSet) result.clone();
	}

	private int hash(int hashCodeAux, BitSet s, BitSet i) {
		return Math.abs(((hashCodeAux) ^ (s.hashCode() << 8) ^ (i.hashCode() << 16)) % size);
	}

}
