/* 
  Copyright 2014 Julia s.r.l.
    
  This file is part of BeeDeeDee.

  BeeDeeDee is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  BeeDeeDee is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with BeeDeeDee.  If not, see <http://www.gnu.org/licenses/>.
*/
package com.juliasoft.beedeedee.factories;

import static com.juliasoft.julia.checkers.nullness.assertions.NullnessAssertions.assertNonNull;

import java.util.Arrays;

class SimpleUniqueTable implements UniqueTable {

	protected static final int VAR_OFFSET = 0;
	protected static final int LOW_OFFSET = 1;
	protected static final int HIGH_OFFSET = 2;
	protected static final int NEXT_OFFSET = 3;
	protected static final int HASHCODEAUX_OFFSET = 4;
	protected static final int NODE_SIZE = 5;

	protected int[] ut;
	protected int[] H;
	protected int size;
	protected int nextPos;
	protected ComputationCache computationCache;
	protected RestrictCache restrictCache;
	protected ReplaceCache replaceCache;
	protected QuantCache quantCache;

	private final int[] hitCounters = new int[Operator.values().length];
	private final int[] opCounters = new int[Operator.values().length];
	protected int hashCodeAuxCounter;

	protected SimpleUniqueTable(int size, int cacheSize) {
		this.size = size;
		this.ut = new int[size * getNodeSize()];
		this.H = new int[size];
		this.computationCache = new ComputationCache(cacheSize);
		this.restrictCache = new RestrictCache(Math.max(1, cacheSize / 20));
		this.replaceCache = new ReplaceCache(Math.max(1, cacheSize / 20));
		this.quantCache = new QuantCache(Math.max(1, cacheSize / 20));

		Arrays.fill(H, -1);
	}

	protected int getNodeSize() {
		return NODE_SIZE;
	}

	@Override
	public final int getSize() {
		return size;
	}

	@Override
	public final int getCacheSize() {
		return computationCache.getSize();
	}

	@Override
	public final int nodesCount() {
		return nextPos;
	}

	@Override
	public final void printStatistics() {
		for (int i = 0; i < opCounters.length; i++) {
			System.out.print(" +" + opCounters[i]);
			System.out.print(" *" + hitCounters[i]);
		}
	}

	/*
	 * Node accessor methods
	 */

	protected final boolean isVarLowHigh(int id, int var, int low, int high) {
		int pos = id * getNodeSize() + 2;

		return ut[pos--] == high && ut[pos--] == low && ut[pos] == var;
	}

	@Override
	public int var(int id) {
		return ut[id * getNodeSize() + VAR_OFFSET];
	}

	@Override
	public int low(int id) {
		return ut[id * getNodeSize() + LOW_OFFSET];
	}

	@Override
	public int high(int id) {
		return ut[id * getNodeSize() + HIGH_OFFSET];
	}

	protected final int next(int id) {
		return ut[id * getNodeSize() + NEXT_OFFSET];
	}

	protected final int hashCodeAux(int id) {
		return ut[id * getNodeSize() + HASHCODEAUX_OFFSET];
	}

	/*
	 * Node mutator methods
	 */

	protected int setAtNextPos(int varNumber, int lowNode, int highNode, int next) {
		int nextPos = this.nextPos++;
		int pos = nextPos * getNodeSize();
		int[] table = ut;

		table[pos++] = varNumber;
		table[pos++] = lowNode;
		table[pos++] = highNode;
		table[pos++] = next;
		table[pos] = hashCodeAuxCounter++;

		return nextPos;
	}

	protected final void setNext(int node, int nextNode) {
		ut[node * getNodeSize() + NEXT_OFFSET] = nextNode;
	}
	
	@Override
	public String toString() {
		String s = "";
		int nextPos = this.nextPos * getNodeSize();

		for (int i = 0; i < nextPos; i++) {
			if (i % getNodeSize() == 0) {
				s += i / getNodeSize() + ": \t" + var(i / getNodeSize()) + "\t";
			} else {
				s += ut[i] + "\t";
			}
			if ((i + 1) % getNodeSize() == 0) {
				s += "\n";
			}
		}

		return s;
	}

	@Override
	public String toDot() {
		String s = "digraph G {\n";

		// skip terminals
		for (int i = 0; i < nextPos; i++) {
			s += i + " -> " + low(i) + " [style=dotted];\n";
			s += i + " -> " + high(i) + ";\n";
		}

		s += "}\n";

		return s;
	}

	
	/*
	 * Cache management
	 */
	
	@Override
	public final int getFromCache(Operator op, int bdd1, int bdd2) {
		assertNonNull(op);
		return computationCache.get(op, bdd1, bdd2);
	}

	@Override
	public final void putIntoCache(Operator op, int bdd1, int bdd2, int result) {
		assertNonNull(op);
		computationCache.put(op, bdd1, bdd2, result);
	}

	protected final int hash(int var, int low, int high) {
		return hash(var, low, high, size);
	}

	protected final int hash(int var, int low, int high, int size) {
		int num = low;

		while (low > 0) {
			low >>= 1;
			high <<= 1;
		}

		return (num |= high) >= 0 ? num % size : (-num) % size;
	}

	@Override
	public int get(int var, int low, int high) {
		int bin, pos = hash(var, low, high);

		if ((bin = H[pos]) < 0 || var(bin) > var)
			return H[pos] = setAtNextPos(var, low, high, bin);
		else
			do {
				if (isVarLowHigh(bin, var, low, high))
					return bin;

				int old = bin;
				if ((bin = next(bin)) < 0 || var(bin) > var) {
					setNext(old, nextPos);
					return setAtNextPos(var, low, high, bin);
				}
			}
			while (true);
	}

	public final RestrictCache getRestrictCache() {
		return restrictCache;
	}
	
	public final ReplaceCache getReplaceCache() {
		return replaceCache;
	}
	
	public final QuantCache getQuantCache() {
		return quantCache;
	}
}