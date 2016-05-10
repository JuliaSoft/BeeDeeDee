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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;

import com.juliasoft.beedeedee.bdd.Assignment;
import com.juliasoft.beedeedee.bdd.BDD;
import com.juliasoft.beedeedee.bdd.ReplacementWithExistingVarException;
import com.juliasoft.beedeedee.bdd.UnsatException;
import com.juliasoft.beedeedee.ger.E;
import com.juliasoft.beedeedee.ger.LeaderFunction;
import com.juliasoft.beedeedee.ger.Pair;
import com.juliasoft.julia.checkers.nullness.Inner0NonNull;
import com.juliasoft.utils.concurrent.Executors;

class ResizingAndGarbageCollectedFactoryImpl extends ResizingAndGarbageCollectedFactory {
	private final static int FIRST_NODE_NUM = 2;
	protected final int NUMBER_OF_PREALLOCATED_VARS;
	protected final static int DEFAULT_NUMBER_OF_PREALLOCATED_VARS = 1000;
	private final int NUM_OF_PREALLOCATED_NODES;
	protected ResizingAndGarbageCollectedUniqueTable ut;
	private final List<BDDImpl> allBDDsCreatedSoFar = new ArrayList<BDDImpl>();
	protected int ZERO;
	protected int ONE;
	protected final int[] vars;
	protected final int[] notVars;
	private int maxVar;
	
	ResizingAndGarbageCollectedFactoryImpl(int utSize, int cacheSize) {
		this(utSize, cacheSize, DEFAULT_NUMBER_OF_PREALLOCATED_VARS);
	}

	ResizingAndGarbageCollectedFactoryImpl(int utSize, int cacheSize, int numberOfPreallocatedVars) {
		NUMBER_OF_PREALLOCATED_VARS = numberOfPreallocatedVars;
		NUM_OF_PREALLOCATED_NODES = FIRST_NODE_NUM + 2 * NUMBER_OF_PREALLOCATED_VARS;
		vars = new int[NUMBER_OF_PREALLOCATED_VARS];
		notVars = new int[NUMBER_OF_PREALLOCATED_VARS];

		utSize = Math.max(utSize, NUM_OF_PREALLOCATED_NODES);
		setUT(new ResizingAndGarbageCollectedUniqueTable(utSize, cacheSize, this));
	}

	protected void setUT(ResizingAndGarbageCollectedUniqueTable uniqueTable) {
		ut = uniqueTable;
		// insert 0 and 1
		ZERO = ut.get(Integer.MAX_VALUE - 1, -1, -1);
		ONE = ut.get(Integer.MAX_VALUE, -1, -1);

		// insert lower variables
		for (int var = 0; var < NUMBER_OF_PREALLOCATED_VARS; var++)
			vars[var] = ut.get(var, ZERO, ONE);

		// and their negation
		for (int var = 0; var < NUMBER_OF_PREALLOCATED_VARS; var++)
			notVars[var] = ut.get(var, ONE, ZERO);
	}

	@Override
	public void done() {
		Executors.shutdown();
	}

	private int MK(int var, int low, int high) {
		return low == high ? low : ut.get(var, low, high);
	}

	private void updateMaxVar(int var) {
		if (var > maxVar)  // track maximum variable index (for satCount)
			synchronized (this) {
				if (var > maxVar)
					maxVar = var;
			}
	}

	/* 
	 * used only by replace()
	 * Precondition: 
	 * low and high are Ordered BDD,
	 * var does not appear in low.
	 */

	private int MKInOrder(int var, int low, int high) {
		int varLow = ut.var(low);
		int varHigh = ut.var(high);
		
		if (var == varLow || var == varHigh)
			throw new ReplacementWithExistingVarException(var);
		
		if (var < varLow && var < varHigh)
			return MK(var, low, high);
		
		if (varLow == varHigh)
			return MK(varLow, MKInOrder(var, ut.low(low), ut.low(high)), MKInOrder(var, ut.high(low), ut.high(high)));
		if (varLow < varHigh)
			return MK(varLow, MKInOrder(var, ut.low(low), high), MKInOrder(var, ut.high(low), high));
		/*
		 * since var cannot appear in low and high
 		 * we have: varHigh < varLow &&  varHigh < var) 
		 */
		return MK(varHigh, MKInOrder(var, low, ut.low(high)), MKInOrder(var, low, ut.high(high)));
	}
	
	@Override
	public int nodesCount() {
		ReentrantLock lock = ut.getGCLock();
		lock.lock();
		try {
			return ut.nodesCount();
		}
		finally {
			lock.unlock();
		}
	}

	@Override
	public void printStatistics() {
		ReentrantLock lock = ut.getGCLock();
		lock.lock();
		try {
			ut.printStatistics();
		}
		finally {
			lock.unlock();
		}
	}

	@Override
	public BDDImpl makeVar(int i) {
		ReentrantLock lock = ut.getGCLock();
		lock.lock();
		try {
			return mkOptimized(i);
		}
		finally {
			lock.unlock();
		}
	}

	private BDDImpl mkOptimized(int v) {
		updateMaxVar(v);

		if (v >= NUMBER_OF_PREALLOCATED_VARS)
			return new BDDImpl(MK(v, ZERO, ONE));
		else
			return new BDDImpl(vars[v]);
	}

	@Override
	public BDDImpl makeNotVar(int i) {
		ReentrantLock lock = ut.getGCLock();
		lock.lock();
		try {
			return mkOptmizedNot(i);
		}
		finally {
			lock.unlock();
		}
	}

	private BDDImpl mkOptmizedNot(int v) {
		updateMaxVar(v);

		if (v >= NUMBER_OF_PREALLOCATED_VARS)
			return new BDDImpl(MK(v, ONE, ZERO));
		else
			return new BDDImpl(notVars[v]);
	}

	private int freedBDDsCounter;

	Map<Integer, BitSet> cacheVarsEntailed = new HashMap<>();
	private BitSet varsEntailed(int id) {
		BitSet result = cacheVarsEntailed.get(id);
		if (result != null)
			return result;

		result = new VarsCalculator(true, id).result;
		cacheVarsEntailed.put(id, result);

		return result;
	}

	Map<Integer, BitSet> cacheVarsDisentailed = new HashMap<>();
	private BitSet varsDisentailed(int id) {
		BitSet result = cacheVarsDisentailed.get(id);
		if (result != null)
			return result;

		result = new VarsCalculator(false, id).result;
		cacheVarsDisentailed.put(id, result);

		return result;
	}

	private class VarsCalculator {
		private final BitSet s = new BitSet();
		private BitSet result;

		private VarsCalculator(boolean entailed, int id) {
			if (entailed)
				varsEntailed(id);
			else
				varsDisentailed(id);

			if (result == null)
				result = universe();
		}

		private void varsEntailed(int id) {
			while (id != ZERO && id != ONE) {
				int var = ut.var(id);
				// do we have reached a variable that is already considered false?
				if (result != null && result.previousSetBit(var) < 0)
					return;

				s.set(var);
				varsEntailed(ut.high(id));
				s.clear(var);
				id = ut.low(id);
			}

			if (id == ONE)
				if (result == null)
					result = (BitSet) s.clone();
				else
					result.and(s);
		}

		private void varsDisentailed(int id) {
			while (id != ZERO && id != ONE) {
				int var = ut.var(id);
				// do we have reached a variable that is already considered false?
				if (result != null && result.previousSetBit(var) < 0)
					return;

				s.set(var);
				varsDisentailed(ut.low(id));
				s.clear(var);
				id = ut.high(id);
			}

			if (id == ONE)
				if (result == null)
					result = (BitSet) s.clone();
				else
					result.and(s);
		}

		/**
		 * Computes the set of all variable indexes up to max var index created so far.
		 * 
		 * @param f the BDD from which to compute maxVar
		 * @return the set of all variable indexes
		 */

		private BitSet universe() {
			BitSet u = new BitSet();
			int maxVar = getMaxVar();
			if (maxVar > 0)
				for (int i = 0; i <= maxVar; i++)
					u.set(i);

			return u;
		}
	}

	class BDDImpl implements BDD {

		/**
		 * The position of this BDD inside the table of BDD nodes.
		 */

		protected int id;

		/**
		 * A unique identifier of the node where this BDD starts.
		 */

		private int hashCode;

		/**
		 * A cache for the nodeCount() method. -1 means that it is not valid.
		 */

		private int nodeCount;

		private BDDImpl(int id) {
			setId(id);

			synchronized (allBDDsCreatedSoFar) {
				allBDDsCreatedSoFar.add(this);
			}
		}

		private void setId(int id) {
			this.id = id;
			this.hashCode = ut.hashCodeAux(id);
			this.nodeCount = -1;
		}

		@Override
		public void free() {
			if (id == -1) {
				return;	// already freed, idempotent
			}
			if (id >= NUM_OF_PREALLOCATED_NODES) {
				id = -1;
				ut.scheduleGC();
				ut.gcIfAlmostFull();
			}
			else
				id = -1;

			shrinkTheListOfAllBDDsIfTooLarge();
		}

		private void shrinkTheListOfAllBDDsIfTooLarge() {
			if (++freedBDDsCounter > 100000) {
				ReentrantLock lock = ut.getGCLock();
				lock.lock();
				try {
					synchronized (allBDDsCreatedSoFar) {
						if (freedBDDsCounter > 100000) {
							List<BDDImpl> copy = new ArrayList<BDDImpl>(allBDDsCreatedSoFar);
							allBDDsCreatedSoFar.clear();

							for (BDDImpl bdd : copy) {
								if (bdd.id >= 0) {
									allBDDsCreatedSoFar.add(bdd);									
								}
							}

							freedBDDsCounter = 0;
						}
					}
				}
				finally {
					lock.unlock();
				}
			}
		}

		@Override
		public String toString() {
			if (id < 0)
				return "[freed zombie BDD]";

			ReentrantLock lock = ut.getGCLock();
			lock.lock();
			try {
				return "digraph G {\n" + toDot(id) + "}\n";
			}
			finally {
				lock.unlock();
			}
		}

		private String toDot(int currentId) {
			String s = "";

			boolean terminal = false;

			int var = ut.var(currentId);
			if (currentId < 2) {
				var = currentId == 0 ? 0 : 1;

				terminal = true;
			}

			int low = ut.low(currentId);
			int high = ut.high(currentId);

			s += currentId + " [label=" + var + (terminal ? ",shape=box]\n" : "]\n");
			if (terminal) {
				return s;
			}

			s += currentId + " -> " + low + " [style=dotted];\n";
			s += currentId + " -> " + high + ";\n";

			s += toDot(low);
			s += toDot(high);

			return s;
		}

		/**
		 * Recursive version of the apply() function.
		 */
		
		private int applyAND(int bdd1, int bdd2) {
			if (bdd1 == bdd2)
				return bdd1;

			if (bdd1 == ZERO || bdd2 == ZERO)
				return ZERO;

			if (bdd1 == ONE)
				return bdd2;

			if (bdd2 == ONE)
				return bdd1;
		
			int result;
			if ((result = ut.getFromCache(Operator.AND, bdd1, bdd2)) < 0) {
				int v1 = ut.var(bdd1), v2 = ut.var(bdd2);

				if (v1 == v2)
					ut.putIntoCache(Operator.AND, bdd1, bdd2,
						result = MK(v1, applyAND(ut.low(bdd1), ut.low(bdd2)), applyAND(ut.high(bdd1), ut.high(bdd2))));
				else if (v1 < v2)
					ut.putIntoCache(Operator.AND, bdd1, bdd2, result = MK(v1, applyAND(ut.low(bdd1), bdd2), applyAND(ut.high(bdd1), bdd2)));
				else
					ut.putIntoCache(Operator.AND, bdd1, bdd2, result = MK(v2, applyAND(bdd1, ut.low(bdd2)), applyAND(bdd1, ut.high(bdd2))));
			}

			return result;
		}

		private int applyOR(int bdd1, int bdd2) {
			if (bdd1 == bdd2)
				return bdd1;

			if (bdd1 == ONE || bdd2 == ONE)
				return ONE;

			if (bdd1 == ZERO)
				return bdd2;

			if (bdd2 == ZERO)
				return bdd1;
		
			int result;
			if ((result = ut.getFromCache(Operator.OR, bdd1, bdd2)) < 0) {
				int v1 = ut.var(bdd1), v2 = ut.var(bdd2);

				if (v1 == v2)
					ut.putIntoCache(Operator.OR, bdd1, bdd2,
						result = MK(v1, applyOR(ut.low(bdd1), ut.low(bdd2)), applyOR(ut.high(bdd1), ut.high(bdd2))));
				else if (v1 < v2)
					ut.putIntoCache(Operator.OR, bdd1, bdd2, result = MK(v1, applyOR(ut.low(bdd1), bdd2), applyOR(ut.high(bdd1), bdd2)));
				else
					ut.putIntoCache(Operator.OR, bdd1, bdd2, result = MK(v2, applyOR(bdd1, ut.low(bdd2)), applyOR(bdd1, ut.high(bdd2))));
			}

			return result;
		}

		private int applyBIIMP(int bdd1, int bdd2) {
			if (bdd1 == bdd2)
				return ONE;

			if (bdd1 == ZERO && bdd2 == ONE)
				return ZERO;

			if (bdd1 == ONE && bdd2 == ZERO)
				return ZERO;

			if (bdd1 == ONE)
				return bdd2;

			if (bdd2 == ONE)
				return bdd1;

			int result;
			if ((result = ut.getFromCache(Operator.BIIMP, bdd1, bdd2)) < 0) {
				int v1 = ut.var(bdd1), v2 = ut.var(bdd2);

				if (v1 == v2)
					ut.putIntoCache(Operator.BIIMP, bdd1, bdd2,
						result = MK(v1, applyBIIMP(ut.low(bdd1), ut.low(bdd2)), applyBIIMP(ut.high(bdd1), ut.high(bdd2))));
				else if (v1 < v2)
					ut.putIntoCache(Operator.BIIMP, bdd1, bdd2, result = MK(v1, applyBIIMP(ut.low(bdd1), bdd2), applyBIIMP(ut.high(bdd1), bdd2)));
				else
					ut.putIntoCache(Operator.BIIMP, bdd1, bdd2, result = MK(v2, applyBIIMP(bdd1, ut.low(bdd2)), applyBIIMP(bdd1, ut.high(bdd2))));
			}

			return result;
		}

		private int applyXOR(int bdd1, int bdd2) {
			if (bdd1 == bdd2 || (bdd1 == ONE && bdd2 == ONE) || (bdd1 == ZERO && bdd2 == ZERO))
				return ZERO;

			if ((bdd1 == ONE && bdd2 == ZERO) || (bdd1 == ZERO && bdd2 == ONE))
				return ONE;

			if (bdd1 == ZERO)
				return bdd2;

			if (bdd2 == ZERO)
				return bdd1;
		
			int result;
			if ((result = ut.getFromCache(Operator.XOR, bdd1, bdd2)) < 0) {
				int v1 = ut.var(bdd1), v2 = ut.var(bdd2);

				if (v1 == v2)
					ut.putIntoCache(Operator.XOR, bdd1, bdd2,
						result = MK(v1, applyXOR(ut.low(bdd1), ut.low(bdd2)), applyXOR(ut.high(bdd1), ut.high(bdd2))));
				else if (v1 < v2)
					ut.putIntoCache(Operator.XOR, bdd1, bdd2, result = MK(v1, applyXOR(ut.low(bdd1), bdd2), applyXOR(ut.high(bdd1), bdd2)));
				else
					ut.putIntoCache(Operator.XOR, bdd1, bdd2, result = MK(v2, applyXOR(bdd1, ut.low(bdd2)), applyXOR(bdd1, ut.high(bdd2))));
			}

			return result;
		}

		/**
		 * Recursive version of the apply() function.
		 */
		
		private int applyIMP(int bdd1, int bdd2) {
			if (bdd1 == bdd2 || bdd1 == ZERO)
				return ONE;

			if (bdd1 == ONE)
				return bdd2;
		
			int result;
			if ((result = ut.getFromCache(Operator.IMP, bdd1, bdd2)) < 0) {
				int v1 = ut.var(bdd1), v2 = ut.var(bdd2);

				if (v1 == v2)
					ut.putIntoCache(Operator.IMP, bdd1, bdd2,
						result = MK(v1, applyIMP(ut.low(bdd1), ut.low(bdd2)), applyIMP(ut.high(bdd1), ut.high(bdd2))));
				else if (v1 < v2)
					ut.putIntoCache(Operator.IMP, bdd1, bdd2, result = MK(v1, applyIMP(ut.low(bdd1), bdd2), applyIMP(ut.high(bdd1), bdd2)));
				else
					ut.putIntoCache(Operator.IMP, bdd1, bdd2, result = MK(v2, applyIMP(bdd1, ut.low(bdd2)), applyIMP(bdd1, ut.high(bdd2))));
			}

			return result;
		}

		@Override
		public BDD or(BDD other) {
			assertNonNull(other);
			ut.gcIfAlmostFull();

			ReentrantLock lock = ut.getGCLock();
			lock.lock();
			try {
				return new BDDImpl(applyOR(id, ((BDDImpl) other).id));
			}
			finally {
				lock.unlock();
			}
		}

		@Override
		public BDD orWith(BDD other) {
			assertNonNull(other);
			ReentrantLock lock = ut.getGCLock();
			lock.lock();
			try {
				setId(applyOR(id, ((BDDImpl) other).id));
			}
			finally {
				lock.unlock();
			}

			other.free();

			return this;
		}

		@Override
		public BDD and(BDD other) {
			assertNonNull(other);
			ut.gcIfAlmostFull();

			ReentrantLock lock = ut.getGCLock();
			lock.lock();
			try {
				return new BDDImpl(applyAND(id, ((BDDImpl) other).id));
			}
			finally {
				lock.unlock();
			}
		}

		@Override
		public BDD andWith(BDD other) {
			assertNonNull(other);
			ReentrantLock lock = ut.getGCLock();
			lock.lock();
			try {
				setId(applyAND(id, ((BDDImpl) other).id));
			}
			finally {
				lock.unlock();
			}

			other.free();

			return this;
		}

		@Override
		public BDD xor(BDD other) {
			assertNonNull(other);
			ut.gcIfAlmostFull();

			ReentrantLock lock = ut.getGCLock();
			lock.lock();
			try {
				return new BDDImpl(applyXOR(id, ((BDDImpl) other).id));
			}
			finally {
				lock.unlock();
			}
		}

		@Override
		public BDD xorWith(BDD other) {
			assertNonNull(other);
			ReentrantLock lock = ut.getGCLock();
			lock.lock();
			try {
				setId(applyXOR(id, ((BDDImpl) other).id));
			}
			finally {
				lock.unlock();
			}

			other.free();

			return this;
		}

		@Override
		public BDD nand(BDD other) {
			assertNonNull(other);
			ut.gcIfAlmostFull();

			ReentrantLock lock = ut.getGCLock();
			lock.lock();
			try {
				return new BDDImpl(applyIMP(applyAND(id, ((BDDImpl) other).id), ZERO));
			}
			finally {
				lock.unlock();
			}
		}

		@Override
		public BDD nandWith(BDD other) {
			assertNonNull(other);
			ReentrantLock lock = ut.getGCLock();
			lock.lock();
			try {
				setId(applyIMP(applyAND(id, ((BDDImpl) other).id), ZERO));
			}
			finally {
				lock.unlock();
			}

			other.free();

			return this;
		}

		@Override
		public BDD not() {
			ut.gcIfAlmostFull();

			ReentrantLock lock = ut.getGCLock();
			lock.lock();
			try {
				return new BDDImpl(applyIMP(id, ZERO));
			}
			finally {
				lock.unlock();
			}
		}

		@Override
		public BDD notWith() {
			ReentrantLock lock = ut.getGCLock();
			lock.lock();
			try {
				setId(applyIMP(id, ZERO));
			}
			finally {
				lock.unlock();
			}
			
			return this;
		}

		@Override
		public BDD imp(BDD other) {
			assertNonNull(other);
			ut.gcIfAlmostFull();

			ReentrantLock lock = ut.getGCLock();
			lock.lock();
			try {
				return new BDDImpl(applyIMP(id, ((BDDImpl) other).id));
			}
			finally {
				lock.unlock();
			}
		}

		@Override
		public BDD impWith(BDD other) {
			assertNonNull(other);
			ReentrantLock lock = ut.getGCLock();
			lock.lock();
			try {
				setId(applyIMP(id, ((BDDImpl) other).id));
			}
			finally {
				lock.unlock();
			}

			other.free();

			return this;
		}

		@Override
		public BDD biimp(BDD other) {
			assertNonNull(other);
			ut.gcIfAlmostFull();

			ReentrantLock lock = ut.getGCLock();
			lock.lock();
			try {
				return new BDDImpl(applyBIIMP(id, ((BDDImpl) other).id));
			}
			finally {
				lock.unlock();
			}
		}

		@Override
		public BDD biimpWith(BDD other) {
			assertNonNull(other);
			ReentrantLock lock = ut.getGCLock();
			lock.lock();
			try {
				setId(applyBIIMP(id, ((BDDImpl) other).id));
			}
			finally {
				lock.unlock();
			}

			other.free();

			return this;
		}

		@Override
		public BDD copy() {
			ut.gcIfAlmostFull();

			ReentrantLock lock = ut.getGCLock();
			lock.lock();
			try {
				return new BDDImpl(id);
			}
			finally {
				lock.unlock();
			}
		}

		@Override
		public Assignment anySat() throws UnsatException {
			AssignmentImpl assignment = new AssignmentImpl();

			ReentrantLock lock = ut.getGCLock();
			lock.lock();
			try {
				anySat(id, assignment);
			}
			finally {
				lock.unlock();
			}

			return assignment;
		}

		private void anySat(int bdd, AssignmentImpl assignment) throws UnsatException {
			if (bdd == ZERO)
				throw new UnsatException();
			else if (bdd != ONE) {
				if (ut.low(bdd) == ZERO) {
					assignment.put(ut.var(bdd), true);
					anySat(ut.high(bdd), assignment);
				}
				else {
					assignment.put(ut.var(bdd), false);
					anySat(ut.low(bdd), assignment);
				}
			}
		}

		@Override
		public List<Assignment> allSat() {
			ReentrantLock lock = ut.getGCLock();
			lock.lock();
			try {
				return allSat(id);
			}
			finally {
				lock.unlock();
			}
		}

		private @Inner0NonNull List<Assignment> allSat(int bdd) {
			List<Assignment> list = new ArrayList<Assignment>();

			if (bdd != ZERO)
				if (bdd == ONE)
					list.add(new AssignmentImpl());
				else {
					int var = ut.var(bdd);

					List<Assignment> lowList = allSat(ut.low(bdd));
					for (Assignment assignment : lowList)
						((AssignmentImpl) assignment).put(var, false);

					List<Assignment> highList = allSat(ut.high(bdd));
					for (Assignment assignment : highList)
						((AssignmentImpl) assignment).put(var, true);

					// join lists
					list.addAll(lowList);
					list.addAll(highList);
				}

			return list;
		}

		//TODO this should probably return a BigInteger
		@Override
		public long satCount() {
			return satCount(maxVar);
		}
		
		//TODO this should probably return a BigInteger
		@Override
		public long satCount(int maxVar) {
			ReentrantLock lock = ut.getGCLock();
			lock.lock();
			try {
				return (long) (Math.pow(2, ut.var(id)) * count(id, maxVar));
			}
			finally {
				lock.unlock();
			}
		}

		private double count(int bdd, int maxVar) {
			if (bdd < FIRST_NODE_NUM) // terminal node
				return bdd;

			int low = ut.low(bdd);
			int high = ut.high(bdd);
			int varLow = low < FIRST_NODE_NUM ? maxVar + 1 : ut.var(low);
			int varHigh = high < FIRST_NODE_NUM ? maxVar + 1 : ut.var(high);
			return Math.pow(2, varLow - ut.var(bdd) - 1) * count(low, maxVar) + Math.pow(2, varHigh - ut.var(bdd) - 1) * count(high, maxVar);
		}

		@Override
		public long pathCount() {
			ReentrantLock lock = ut.getGCLock();
			lock.lock();
			try {
				return pathCount(id);
			}
			finally {
				lock.unlock();
			}
		}
		
		private long pathCount(int id) {
			if (id < FIRST_NODE_NUM) // terminal node
				return id;
			
			return pathCount(ut.low(id)) + pathCount(ut.high(id));
		}

		@Override
		public BDD restrict(BDD var) {
			assertNonNull(var);
			ReentrantLock lock = ut.getGCLock();
			lock.lock();
			try {
				int res = id;
				for (int varId = ((BDDImpl)var).id; varId >= FIRST_NODE_NUM; varId = ut.high(varId)) {
					if (ut.low(varId) == ZERO)
						res = restrict(res, ut.var(varId), true);
					if (ut.low(varId) == ONE)
						res = restrict(res, ut.var(varId), false);
				}
	
				return new BDDImpl(res);
			}
			finally {
				lock.unlock();
			}
		}

		@Override
		public BDD restrictWith(BDD var) {
			assertNonNull(var);
			ReentrantLock lock = ut.getGCLock();
			lock.lock();
			try {
				int res = id;
				for (int varId = ((BDDImpl)var).id; varId >= FIRST_NODE_NUM; varId = ut.high(varId)) {
					if (ut.low(varId) == ZERO)
						res = restrict(res, ut.var(varId), true);
					if (ut.low(varId) == ONE)
						res = restrict(res, ut.var(varId), false);
				}
	
				setId(res);
			}
			finally {
				lock.unlock();
			}

			return this;
		}

		@Override
		public BDD restrict(int var, boolean value) {
			ReentrantLock lock = ut.getGCLock();
			lock.lock();
			try {
				return new BDDImpl(restrict(id, var, value));
			}
			finally {
				lock.unlock();
			}
		}

		private int restrict(int u, int var, boolean value) {
			int result;
			result = ut.getRestrictCache().get(u, var, value);
			if (result >= 0)
				return result;
			
			int diff = ut.var(u) - var;
			if (diff > 0)
				return u;
			else if (diff < 0) {
				result = MK(ut.var(u), restrict(ut.low(u), var, value), restrict(ut.high(u), var, value));
				ut.getRestrictCache().put(u, var, value, result);
				return result;
			}
			else if (value)
				return restrict(ut.high(u), var, value);
			else
				return restrict(ut.low(u), var, value);
		}

		@Override
		public BDD exist(int var) {
			BDD falseOp = restrict(var, false);
			BDD trueOp = restrict(var, true);

			return falseOp.orWith(trueOp);
		}

		@Override
		public BDD exist(BDD var) {
			assertNonNull(var);
			return quant(var, true);
		}

		@Override
		public BDD forAll(int var) {
			BDD falseOp = restrict(var, false);
			BDD trueOp = restrict(var, true);

			return falseOp.andWith(trueOp);
		}

		@Override
		public BDD forAll(BDD var) {
			assertNonNull(var);
			return quant(var, false);
		}
		
		private BDD quant(BDD var, boolean exist) {
			assertNonNull(var);
			ArrayList<Integer> vars = new ArrayList<Integer>();
			int pos = 0;

			ReentrantLock lock = ut.getGCLock();
			lock.lock();
			try {
				int varId = ((BDDImpl) var).id;

				while (varId >= FIRST_NODE_NUM) {
					vars.add(ut.var(varId));
					varId = ut.high(varId);
				}
	
				int[] removedVars = new int[vars.size()];
				for (Integer i: vars)
					removedVars[pos++] = i;
	
				return new BDDImpl(quant_rec(id, removedVars, exist, Arrays.hashCode(removedVars)));
			}
			finally {
				lock.unlock();
			}
		}

		private int quant_rec(int bdd, int[] vars, boolean exist, int hashCodeOfVars) {
			if (bdd < FIRST_NODE_NUM) // terminal node
				return bdd;

			int result = ut.getQuantCache().get(exist, bdd, vars, hashCodeOfVars);
			if (result >= 0)
				return result;

			int oldA = ut.low(bdd), oldB = ut.high(bdd);
			int a = quant_rec(oldA, vars, exist, hashCodeOfVars);
			int b = quant_rec(oldB, vars, exist, hashCodeOfVars);

			int var = ut.var(bdd);

			if (Arrays.binarySearch(vars, var) >= 0)
				if (exist)
					result = applyOR(a, b);
				else
					result = applyAND(a, b);
			else {
				if (a == oldA && b == oldB)
					result = bdd;
				else
					result = MK(var, a, b);
			}

			ut.getQuantCache().put(exist, bdd, vars, hashCodeOfVars, result);
			
			return result;
		}

		@Override
		public BDD simplify(BDD d) {
			assertNonNull(d);
			ReentrantLock lock = ut.getGCLock();
			lock.lock();
			try {
				return new BDDImpl(simplify(((BDDImpl) d).id, id));
			}
			finally {
				lock.unlock();
			}
		}
		
		private int simplify(int d, int u) {
			if (d == ZERO || u == ZERO)
				return ZERO;

			if (u == ONE)
				return ONE;

			int vu = ut.var(u), vd = ut.var(d);

			if (d == ONE)
				return MK(vu, simplify(d, ut.low(u)), simplify(d, ut.high(u)));
			
			if (vd == vu) {
				if (ut.low(d) == ZERO)
					return simplify(ut.high(d), ut.high(u));

				if (ut.high(d) == ZERO)
					return simplify(ut.low(d), ut.low(u));

				return MK(vu, simplify(ut.low(d), ut.low(u)), simplify(ut.high(d), ut.high(u)));
			}
			
			if (vd < vu)
				return MK(vd, simplify(ut.low(d), u), simplify(ut.high(d), u));

			return MK(vu, simplify(d, ut.low(u)), simplify(d, ut.high(u)));
		}

		@Override
		public boolean isZero() {
			return id == ZERO;
		}

		@Override
		public boolean isOne() {
			return id == ONE;
		}

		@Override
		public int[] varProfile() {
			int[] varp = new int[maxVar + 1];

			ReentrantLock lock = ut.getGCLock();
			lock.lock();
			try {
				varProfile(id, varp, new HashSet<Integer>());
			}
			finally {
				lock.unlock();
			}
			
			return varp;
		}

		private void varProfile(int bdd, int[] varp, Set<Integer> seen) {
			// terminal node or already seen
			if (bdd < FIRST_NODE_NUM || !seen.add(bdd))
				return;

			varp[ut.var(bdd)]++;
			varProfile(ut.low(bdd), varp, seen);
			varProfile(ut.high(bdd), varp, seen);
		}

		@Override
		public int nodeCount() {
			// we check in the cache first
			if (nodeCount >= 0)
				return nodeCount;

			ReentrantLock lock = ut.getGCLock();
			lock.lock();
			try {
				return nodeCount = nodeCount(id, new HashSet<Integer>());
			}
			finally {
				lock.unlock();
			}
		}
		
		private int nodeCount(int bdd, Set<Integer> seen) {
			// terminal node or already seen
			if (bdd < FIRST_NODE_NUM || !seen.add(bdd))
				return 0;

			// variables or their negation
			if (bdd < NUM_OF_PREALLOCATED_NODES)
				return 1;

			return 1 + nodeCount(ut.low(bdd), seen) + nodeCount(ut.high(bdd), seen);
		}

		@Override
		public BDD replace(Map<Integer, Integer> renaming) {
			assertNonNull(renaming);
			if (id == ZERO)
				return makeZero();
			if (id == ONE)
				return makeOne();

			int hash = renaming.hashCode();

			ReentrantLock lock = ut.getGCLock();
			lock.lock();
			try {
				return new BDDImpl(replace(id, renaming, hash));
			}
			finally {
				lock.unlock();
			}
		}

		@Override
		public BDD replaceWith(Map<Integer, Integer> renaming) {
			assertNonNull(renaming);
			if (id < FIRST_NODE_NUM) // terminal node
				return this;

			int hashCodeOfRenaming = renaming.hashCode();

			ReentrantLock lock = ut.getGCLock();
			lock.lock();
			try {
				setId(replace(id, renaming, hashCodeOfRenaming));
			}
			finally {
				lock.unlock();
			}

			return this;
		}

		private int replace(int bdd, Map<Integer, Integer> renaming, int hashOfRenaming) {
			assertNonNull(renaming);
			if (bdd < FIRST_NODE_NUM) // terminal node
				return bdd;

			int result = ut.getReplaceCache().get(bdd, renaming, hashOfRenaming);
			if (result >= 0)
				return result;

			int oldLow = ut.low(bdd), oldHigh = ut.high(bdd);
			int lowRenamed = replace(oldLow, renaming, hashOfRenaming);
			int highRenamed = replace(oldHigh, renaming, hashOfRenaming);
			int var = ut.var(bdd);
			Integer newVar = renaming.get(var);
			if (newVar == null)
				newVar = var;

			if (var == newVar && lowRenamed == oldLow && highRenamed == oldHigh)
				result = bdd;
			else
				result = MKInOrder(newVar, lowRenamed, highRenamed);

			ut.getReplaceCache().put(bdd, renaming, result, hashOfRenaming);

			return result;
		}
		
		@Override
		public BDD ite(BDD thenBDD, BDD elseBDD) {
			assertNonNull(thenBDD);
			assertNonNull(elseBDD);
			ReentrantLock lock = ut.getGCLock();
			lock.lock();
			try {
				return new BDDImpl(ite(id, ((BDDImpl) thenBDD).id, ((BDDImpl) elseBDD).id));
			}
			finally {
				lock.unlock();
			}
		}

		private int ite(int f, int g, int h) {
			if (f == ONE)
				return g;
			if (f == ZERO)
				return h;
			if (g == h)
				return g;
			if (g == ONE && h == ZERO)
				return f;
			if (g == ZERO && h == ONE)
				return applyIMP(f, ZERO);

			int vf = ut.var(f);
			int vg = ut.var(g);
			int vh = ut.var(h);

			if (vf == vg)
				if (vf == vh)
					return MK(vf, ite(ut.low(f), ut.low(g), ut.low(h)), ite(ut.high(f), ut.high(g), ut.high(h)));
				else if (vf < vh)
					return MK(vf, ite(ut.low(f), ut.low(g), h), ite(ut.high(f), ut.high(g), h));
				else
					return MK(vh, ite(f, g, ut.low(h)), ite(f, g, ut.high(h)));
			else if (vf < vg)
				if (vf == vh)
					return MK(vf, ite(ut.low(f), g, ut.low(h)), ite(ut.high(f), g, ut.high(h)));
				else if (vf < vh)
					return MK(vf, ite(ut.low(f), g, h), ite(ut.high(f), g, h));
				else
					return MK(vh, ite(f, g, ut.low(h)), ite(f, g, ut.high(h)));
			else
				if (vg == vh)
					return MK(vg, ite(f, ut.low(g), ut.low(h)), ite(f, ut.high(g), ut.high(h)));
				else if (vg < vh)
					return MK(vg, ite(f, ut.low(g), h), ite(f, ut.high(g), h));
				else
					return MK(vh, ite(f, g, ut.low(h)), ite(f, g, ut.high(h)));
		}

		@Override
		public BDD relProd(BDD other, BDD var) {
			assertNonNull(other);
			assertNonNull(var);
			// TODO this implementation is correct, but not efficient
			return and(other).exist(var);
		}
		
		@Override
		public BDD compose(BDD other, int var) {
			assertNonNull(other);
			ReentrantLock lock = ut.getGCLock();
			lock.lock();
			try {
				return new BDDImpl(compose(id, ((BDDImpl) other).id, var));
			}
			finally {
				lock.unlock();
			}
		}
		
		private int compose(int id1, int id2, int var) {
			int v1 = ut.var(id1);
			int v2 = ut.var(id2);
			
			if (v1 > var)
				return id1;
			
			if (v1 < var)
				if (v1 == v2)
					return MK(v1, compose(ut.low(id1), ut.low(id2), var), compose(ut.high(id1), ut.high(id2), var));
				else if (v1 < v2)
					return MK(v1, compose(ut.low(id1), id2, var), compose(ut.high(id1), id2, var));
				else
					return MK(v2, compose(id1, ut.low(id2), var), compose(id1, ut.high(id2), var));
			else
				return ite(id2, ut.high(id1), ut.low(id1));
		}

		@Override
		public BDD squeezeEquiv(LeaderFunction leaderFunction) {
			ReentrantLock lock = ut.getGCLock();
			lock.lock();
			try {
				return new BDDImpl(squeezeEquiv(id, leaderFunction));
			}
			finally {
				lock.unlock();
			}
		}

		@Override
		public BDD squeezeEquivWith(LeaderFunction leaderFunction) {
			ReentrantLock lock = ut.getGCLock();
			lock.lock();
			try {
				setId(squeezeEquiv(id, leaderFunction));
			}
			finally {
				lock.unlock();
			}
			
			return this;
		}

		private int squeezeEquiv(int bdd, LeaderFunction leaderFunction) {
			if (bdd < FIRST_NODE_NUM) {
				return bdd;
			}
			int var = ut.var(bdd);
			if (leaderFunction.get(var) == var) {
				return MK(var, squeezeEquiv(ut.low(bdd), leaderFunction), squeezeEquiv(ut.high(bdd), leaderFunction));
			}
			if (ut.high(bdd) == 0) {
				return squeezeEquiv(ut.low(bdd), leaderFunction);
			}
			return squeezeEquiv(ut.high(bdd), leaderFunction);
		}

		@Override
		public boolean isEquivalentTo(BDD other) {
			assertNonNull(other);
			if (this == other)
				return true;

			BDDImpl otherImpl = (BDDImpl) other;

			ReentrantLock lock = ut.getGCLock();
			lock.lock();
			try {
				return id == otherImpl.id;
			}
			finally {
				lock.unlock();
			}
		}

		@Override
		public int hashCodeAux() {
			return hashCode;
		}

		@Override
		public int var() {
			ReentrantLock lock = ut.getGCLock();
			lock.lock();
			try {
				return ut.var(id);
			}
			finally {
				lock.unlock();
			}
		}

		@Override
		public BDDImpl high() {
			ReentrantLock lock = ut.getGCLock();
			lock.lock();
			try {
				return new BDDImpl(ut.high(id));
			}
			finally {
				lock.unlock();
			}
		}

		@Override
		public BDDImpl low() {
			ReentrantLock lock = ut.getGCLock();
			lock.lock();
			try {
				return new BDDImpl(ut.low(id));
			}
			finally {
				lock.unlock();
			}
		}

		@Override
		public Factory getFactory() {
			return ResizingAndGarbageCollectedFactoryImpl.this;
		}

		@Override
		public BitSet vars() {
			BitSet vars = new BitSet();
			ReentrantLock lock = ut.getGCLock();
			lock.lock();
			try {
				updateVars(id, vars);
			}
			finally {
				lock.unlock();
			}
			return vars;
		}

		private void updateVars(int id, BitSet vars) {
			if (id < FIRST_NODE_NUM) {
				return;
			}
			vars.set(ut.var(id));
			updateVars(ut.low(id), vars);
			updateVars(ut.high(id), vars);
		}

		@Override
		public int maxVar() {
			ReentrantLock lock = ut.getGCLock();
			lock.lock();
			try {
				return maxVar(id);
			}
			finally {
				lock.unlock();
			}
		}

		private int maxVar(int bdd) {
			if (bdd < FIRST_NODE_NUM) {
				return -1;
			}
			int low = ut.low(bdd);
			int maxVar = Math.max(ut.var(bdd), maxVar(low));
			int high = ut.high(bdd);
			maxVar = Math.max(maxVar, maxVar(high));
			return maxVar;
		}

		@Override
		public BDD renameWithLeader(E r) {
			ReentrantLock lock = ut.getGCLock();
			lock.lock();
			try {
				return new BDDImpl(renameWithLeader(id, r, 1, new BitSet()));
			}
			finally {
				lock.unlock();
			}
		}

		private int renameWithLeader(int f, E r, int c, BitSet t) {
			int var = ut.var(f);
			LeaderFunction lf = new LeaderFunction(r);
			int maxVar = r.maxVar();
			BitSet leaders = lf.getLeaders();
			if (maxVar < var || f < FIRST_NODE_NUM) {
				return f;
			}
			BitSet augmented = new BitSet();
			augmented.or(t);
			int minLeader = leaders.nextSetBit(c);
			if (minLeader > 0 && minLeader < var) {
				augmented.set(minLeader);
				c = minLeader;
				return MK(minLeader, renameWithLeader(f, r, c + 1, t), renameWithLeader(f, r, c + 1, augmented));
			}
			c = var;
			if (!r.containsVar(var)) {
				return MK(var, renameWithLeader(ut.low(f), r, c + 1, t), renameWithLeader(ut.high(f), r, c + 1, t));
			}
			int l = lf.get(var);
			if (l == var) {
				augmented.set(c);
				return MK(var, renameWithLeader(ut.low(f), r, c + 1, t), renameWithLeader(ut.high(f), r, c + 1, augmented));
			}
			if (t.get(l)) {
				return renameWithLeader(ut.high(f), r, c + 1, t);
			}
			return renameWithLeader(ut.low(f), r, c + 1, t);
		}

		@Override
		public Set<Pair> equivVars() {
			Set<Pair> result = equivVars(id);
			if (result == null)
				return generateAllPairs();
			else
				return result;
		}

		/**
		 * Generates all equivalent pairs for the BDD with the given id
		 * 
		 * @param id the id
		 * @return the equivalent pairs. If null, it means all pairs
		 */
		private Set<Pair> equivVars(int id) {
			if (id == ZERO)
				return null;
			else if (id == ONE)
				return new HashSet<>();

			EquivCache equivCache = ut.getEquivCache();
			Set<Pair> cached = equivCache.get(id);
			if (cached != null) {
				return cached;
			}
			Set<Pair> equivVars = equivVars(ut.high(id));
			if (equivVars == null)
				equivVars = equivVars(ut.low(id));
			else {
				Set<Pair> lowEquivVars = equivVars(ut.low(id));
				if (lowEquivVars != null)
					equivVars.retainAll(lowEquivVars);
			}

			if (equivVars == null)
				return null;
			else {
				BitSet vars = varsEntailed(ut.high(id));
				vars.and(varsDisentailed(ut.low(id)));
				Set<Pair> pairs = new HashSet<>();
				for (int i = vars.nextSetBit(0), var = ut.var(id); i >= 0; i = vars.nextSetBit(i + 1))
					pairs.add(new Pair(var, i));

				pairs.addAll(equivVars);
				equivCache.put(id, pairs);
				return pairs;
			}
		}

		/**
		 * Generates all ordered pairs of variables up to the given maxVar.
		 * 
		 * @return the list of generated pairs
		 */
		private Set<Pair> generateAllPairs() {
			int maxVar = maxVar();
			Set<Pair> pairs = new HashSet<>();
			for (int i = 0; i < maxVar; i++)
				for (int j = i + 1; j <= maxVar; j++)
					pairs.add(new Pair(i, j));

			return pairs;
		}
	}

	@Override
	public int getMaxVar() {
		return maxVar;
	}

	private class AssignmentImpl implements Assignment {

		private final Map<Integer, Boolean> truthTable;

		private AssignmentImpl() {
			this.truthTable = new TreeMap<Integer, Boolean>();
		}

		@Override
		public void put(int var, boolean value) {
			truthTable.put(var, value);
		}

		@Override
		public boolean holds(BDD var) throws IndexOutOfBoundsException {
			assertNonNull(var);
			return holds(var.var());
		}

		@Override
		public boolean holds(int i) {
			// TODO Auto-generated method stub
			Boolean result = truthTable.get(i);
			if (result != null)
				return result;
			
			throw new IndexOutOfBoundsException("unknown variable " + i);
		}

		@Override
		public BDD toBDD() {
			BDD res = makeOne();
			Set<Integer> vars = truthTable.keySet();

			ReentrantLock lock = ut.getGCLock();
			lock.lock();
			try {
				for (int v : vars) {
					BDD var = ResizingAndGarbageCollectedFactoryImpl.this.mkOptimized(v);
					if (truthTable.get(v) == Boolean.FALSE)
						var.notWith();
				
					res.andWith(var);
				}
			}
			finally {
				lock.unlock();
			}

			return res;
		}
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder("<");
			
			for (int v : truthTable.keySet())
				sb.append(v).append(":").append(truthTable.get(v) == Boolean.TRUE ? 1 : 0).append(", ");
			
			return sb.substring(0, sb.length() - 2).concat(">");
		}
	}

	@Override
	public void printNodeTable() {
		ReentrantLock lock = ut.getGCLock();
		lock.lock();
		try {
			System.out.println(ut);
		}
		finally {
			lock.unlock();
		}
	}

	@Override
	public BDD makeZero() {
		ReentrantLock lock = ut.getGCLock();
		lock.lock();
		try {
			return new BDDImpl(ZERO);
		}
		finally {
			lock.unlock();
		}
	}

	@Override
	public BDD makeOne() {
		ReentrantLock lock = ut.getGCLock();
		lock.lock();
		try {
			return new BDDImpl(ONE);
		}
		finally {
			lock.unlock();
		}
	}

	@Override
	public void gc() {
		ut.gc();
	}

	@Override
	public int setMaxIncrease(int maxIncrease) {
		return ut.setMaxIncrease(maxIncrease);
	}

	@Override
	public double setIncreaseFactor(double increaseFactor) {
		return ut.setIncreaseFactor(increaseFactor);
	}

	@Override
	public double setCacheRatio(double cacheRatio) {
		return ut.setCacheRatio(cacheRatio);
	}

	@Override
	public double setMinFreeNodes(double minFreeNodes) {
		return ut.setMinFreeNodes(minFreeNodes);
	}

	@Override
	public void setGarbageCollectionListener(GarbageCollectionListener listener) {
		ut.setGarbageCollectionListener(listener);
	}

	@Override
	public void setResizeListener(ResizeListener listener) {
		ut.setResizeListener(listener);
	}

	/**
	 * Replaces the index of each BDD created so far into the new id provided
	 * by the given map.
	 *
	 * @param newPositions a map from old to new index
	 */

	protected void updateIndicesOfAllBDDsCreatedSoFar(int[] newPositions) {
		for (BDDImpl bdd: allBDDsCreatedSoFar)
			if (bdd.id >= NUM_OF_PREALLOCATED_NODES)
				bdd.id = newPositions[bdd.id];
	}

	/**
	 * Marks in the given array the positions of the indices of the alive bdds.
	 *
	 * @param aliveNodes the array, that gets modified
	 */

	protected void markAliveNodes(boolean[] aliveNodes) {
		if (ut.size > 900000) {
			parallelMarkAliveNodes(aliveNodes);
			return;
		}

		for (int pos = 0; pos < NUM_OF_PREALLOCATED_NODES; pos++)
			aliveNodes[pos] = true;

		List<BDDImpl> copy = new ArrayList<BDDImpl>(allBDDsCreatedSoFar);
		allBDDsCreatedSoFar.clear();

		for (BDDImpl bdd: copy) {
			allBDDsCreatedSoFar.add(bdd);

			if (bdd.id >= NUM_OF_PREALLOCATED_NODES)
				markAsAlive(bdd.id, aliveNodes);
		}
	}

	private void parallelMarkAliveNodes(final boolean[] aliveNodes) {
		final int total = Runtime.getRuntime().availableProcessors();
		final List<BDDImpl> copy = new ArrayList<BDDImpl>(allBDDsCreatedSoFar);

		class AliveNodesMarker implements Runnable {
			private final List<BDDImpl> alive = new ArrayList<BDDImpl>();
			private final int num;

			private AliveNodesMarker(int num) {
				this.num = num;
			}

			@Override
			public void run() {
				for (BDDImpl bdd: copy) {
					int id = bdd.id;
					if (id % total == num) {
						alive.add(bdd);
						if (id >= NUM_OF_PREALLOCATED_NODES)
							markAsAlive(id, aliveNodes);
					}
				}
			}
		}

		AliveNodesMarker[] slaves = new AliveNodesMarker[total];
		for (int num = 0; num < total; num++)
			slaves[num] = new AliveNodesMarker(num);

		allBDDsCreatedSoFar.clear();
		for (int pos = 0; pos < NUM_OF_PREALLOCATED_NODES; pos++)
			aliveNodes[pos] = true;

		Executors.parallelise(slaves);

		for (AliveNodesMarker slave: slaves)
			allBDDsCreatedSoFar.addAll(slave.alive);
	}

	private void markAsAlive(int node, boolean[] aliveNodes) {
		if (node >= NUM_OF_PREALLOCATED_NODES && !aliveNodes[node]) {
			aliveNodes[node] = true;
			markAsAlive(ut.low(node), aliveNodes);
			markAsAlive(ut.high(node), aliveNodes);
		}
	}

	@Override
	public int nodeCount(Collection<BDD> bdds) {
		assertNonNull(bdds, "the collection of BBDs cannot be null here");

		int count = 0;
		Set<Integer> seen = new HashSet<Integer>();
		
		for (BDD bdd : bdds) {
			BDDImpl bddi = (BDDImpl) bdd;
			if (bddi == null)
				continue;

			ReentrantLock lock = ut.getGCLock();
			lock.lock();
			try {
				count += bddi.nodeCount(bddi.id, seen);
			}
			finally {
				lock.unlock();
			}
		}
		
		return count;
	}

	ArrayList<BDDImpl> getAllBDDsCreatedSoFarCopy() {
		return new ArrayList<>(allBDDsCreatedSoFar);
	}

	@Override
	public int bddCount() {
		synchronized (allBDDsCreatedSoFar) {
			return allBDDsCreatedSoFar.size() - freedBDDsCounter;
		}
	}
}
