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

import java.io.Closeable;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
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
import com.juliasoft.beedeedee.er.ERFactory;
import com.juliasoft.julia.checkers.nullness.Inner0NonNull;
import com.juliasoft.utils.concurrent.Executors;

/**
 * A factory for Binary Decision Diagrams with automatic resizing and garbage
 * collection.
 */
public class Factory {

	/**
	 * Constructs a factory with automatic resizing and garbage collection.
	 * 
	 * @param utSize the initial size of the node table
	 * @param cacheSize the size of the caches
	 * @return an instance of the factory
	 */
	public static Factory mk(int utSize, int cacheSize) {
		return new Factory(utSize, cacheSize);
	}

	/**
	 * Constructs a factory with automatic resizing and garbage collection.
	 * 
	 * @param utSize the initial size of the node table
	 * @param cacheSize the size of the caches
	 * @param numberOfPreallocatedVars the number of single variable BDDs to preallocate
	 * @return an instance of the factory
	 */
	public static Factory mk(int utSize, int cacheSize, int numberOfPreallocatedVars) {
		return new Factory(utSize, cacheSize, numberOfPreallocatedVars);
	}

	/**
	 * Constructs a factory with automatic resizing and garbage collection, and
	 * using the ER representation, that separates information on equivalent
	 * variables from the bdd.
	 * 
	 * @param utSize
	 *            the initial size of the node table
	 * @param cacheSize
	 *            the size of the caches
	 * @return an instance of the factory
	 */
	public static Factory mkER(int utSize, int cacheSize) {
		return new ERFactory(utSize, cacheSize);
	}

	public static interface GarbageCollectionListener {

		/**
		 * Called when a garbage collection operation is about to start.
		 * 
		 * @param num the progressive number of the garbage collection operation
		 * @param size the number of nodes in the garbage collected table
		 * @param free the number of free nodes after the operation
		 * @param totalTime the cumulative garbage collection time up to now
		 */

		public void onStart(int num, int size, int free, long totalTime);

		/**
		 * Called when a garbage collection operation has been performed.
		 * 
		 * @param num the progressive number of the garbage collection operation
		 * @param size the number of nodes in the garbage collected table
		 * @param free the number of free nodes after the operation
		 * @param time the time required for the garbage collection
		 * @param totalTime the cumulative garbage collection time up to now
		 */

		public void onStop(int num, int size, int free, long time, long totalTime);
	}

	public static interface ResizeListener {

		/**
		 * Called when a resize operation is about to start.
		 * 
		 * @param num the progressive number of the resize operation
		 * @param oldSize the old size of the table
		 * @param newSize the new size of the table
		 * @param totalTime the cumulative resize time up to now
		 */

		public void onStart(int num, int oldSize, int newSize, long totalTime);

		/**
		 * Called when a resize operation has been performed.
		 * 
		 * @param num the progressive number of the resize operation
		 * @param oldSize the old size of the table
		 * @param newSize the new size of the table
		 * @param time the time required for the resize
		 * @param totalTime the cumulative resize time up to now
		 */

		public void onStop(int num, int oldSize, int newSize, long time, long totalTime);
	}

	protected final static int FIRST_NODE_NUM = 2;
	protected final int NUMBER_OF_PREALLOCATED_VARS;
	protected final static int DEFAULT_NUMBER_OF_PREALLOCATED_VARS = 1000;
	protected final int NUM_OF_PREALLOCATED_NODES;
	protected ResizingAndGarbageCollectedUniqueTable ut;
	private final ArrayList<BDDImpl> allBDDsCreatedSoFar = new ArrayList<BDDImpl>();
	protected int ZERO;
	protected int ONE;
	protected final int[] vars;
	protected final int[] notVars;
	private int maxVar;

	protected class GCLock implements Closeable {
		private final ReentrantLock lock;
	
		public GCLock() {
			this.lock = ut.getGCLock();
			this.lock.lock();
		}
	
		@Override
		public void close() {
			lock.unlock();
		}
	}

	protected Factory(int utSize, int cacheSize) {
		this(utSize, cacheSize, DEFAULT_NUMBER_OF_PREALLOCATED_VARS);
	}

	Factory(int utSize, int cacheSize, int numberOfPreallocatedVars) {
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

	/**
	 * Call this method when the factory is no longer needed.
	 */
	public void done() {
		Executors.shutdown();
	}

	protected final int MK(int var, int low, int high) {
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

	/**
	 * @return the current number of nodes in the factory
	 */
	public int nodesCount() {
		try (GCLock lock = new GCLock()) {
			return ut.nodesCount();
		}
	}

	public void printStatistics() {
		try (GCLock lock = new GCLock()) {
			ut.printStatistics();
		}
	}

	/**
	 * Constructs a BDD representing a single variable.
	 * 
	 * @param v the number of the variable
	 * @return the variable as a BDD object 
	 */
	public BDD makeVar(int v) {
		try (GCLock lock = new GCLock()) {
			return mk(innerMakeVar(v));
		}
	}

	protected int innerMakeVar(int v) {
		updateMaxVar(v);

		if (v >= NUMBER_OF_PREALLOCATED_VARS)
			return MK(v, ZERO, ONE);
		else
			return vars[v];
	}

	protected BDDImpl mk(int id) {
		return new BDDImpl(id);
	}

	/**
	 * Constructs a BDD representing the negation of a single variable.
	 * 
	 * @param i the number of the variable
	 * @return the negation of the variable as a BDD object 
	 */
	public BDD makeNotVar(int i) {
		try (GCLock lock = new GCLock()) {
			return mkOptmizedNot(i);
		}
	}

	private BDDImpl mkOptmizedNot(int v) {
		updateMaxVar(v);

		if (v >= NUMBER_OF_PREALLOCATED_VARS)
			return mk(MK(v, ONE, ZERO));
		else
			return mk(notVars[v]);
	}

	private int freedBDDsCounter;

	public class BDDImpl implements BDD {

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

		protected BDDImpl(int id) {
			setId(id);

			synchronized (allBDDsCreatedSoFar) {
				allBDDsCreatedSoFar.add(this);
			}
		}

		protected final BDDImpl mkBDDImpl(int id) {
			return new BDDImpl(id);
		}

		protected void setId(int id) {
			this.id = id;
			this.hashCode = ut.hashCodeAux(id);
			this.nodeCount = -1;
		}

		public int getId() {
			return id;
		}

		@Override
		public void free() {
			if (id >= NUM_OF_PREALLOCATED_NODES) {
				id = -1;
				ut.scheduleGC();
				ut.gcIfAlmostFull();
			}
			else if (id >= 0) // already freed?
				id = -1;

			shrinkTheListOfAllBDDsIfTooLarge();
		}

		private void shrinkTheListOfAllBDDsIfTooLarge() {
			if (++freedBDDsCounter > 100000)
				try (GCLock lock = new GCLock()) {
					synchronized (allBDDsCreatedSoFar) {
						if (freedBDDsCounter > 100000) {
							@SuppressWarnings("unchecked")
							List<BDDImpl> copy = (List<BDDImpl>) allBDDsCreatedSoFar.clone();
							allBDDsCreatedSoFar.clear();

							for (BDDImpl bdd: copy)
								if (bdd.id >= 0)
									allBDDsCreatedSoFar.add(bdd);									

							freedBDDsCounter = 0;
						}
					}
				}
		}

		@Override
		public String toString() {
			if (id < 0)
				return "[freed zombie BDD]";

			try (GCLock lock = new GCLock()) {
				return "digraph G {\n" + toDot(id) + "}\n";
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

		protected final int innerAnd(int bdd1, int bdd2) {
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
							result = MK(v1, innerAnd(ut.low(bdd1), ut.low(bdd2)), innerAnd(ut.high(bdd1), ut.high(bdd2))));
				else if (v1 < v2)
					ut.putIntoCache(Operator.AND, bdd1, bdd2, result = MK(v1, innerAnd(ut.low(bdd1), bdd2), innerAnd(ut.high(bdd1), bdd2)));
				else
					ut.putIntoCache(Operator.AND, bdd1, bdd2, result = MK(v2, innerAnd(bdd1, ut.low(bdd2)), innerAnd(bdd1, ut.high(bdd2))));
			}

			return result;
		}

		protected final int innerOr(int bdd1, int bdd2) {
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
							result = MK(v1, innerOr(ut.low(bdd1), ut.low(bdd2)), innerOr(ut.high(bdd1), ut.high(bdd2))));
				else if (v1 < v2)
					ut.putIntoCache(Operator.OR, bdd1, bdd2, result = MK(v1, innerOr(ut.low(bdd1), bdd2), innerOr(ut.high(bdd1), bdd2)));
				else
					ut.putIntoCache(Operator.OR, bdd1, bdd2, result = MK(v2, innerOr(bdd1, ut.low(bdd2)), innerOr(bdd1, ut.high(bdd2))));
			}

			return result;
		}

		protected final int innerBiimp(int bdd1, int bdd2) {
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
							result = MK(v1, innerBiimp(ut.low(bdd1), ut.low(bdd2)), innerBiimp(ut.high(bdd1), ut.high(bdd2))));
				else if (v1 < v2)
					ut.putIntoCache(Operator.BIIMP, bdd1, bdd2, result = MK(v1, innerBiimp(ut.low(bdd1), bdd2), innerBiimp(ut.high(bdd1), bdd2)));
				else
					ut.putIntoCache(Operator.BIIMP, bdd1, bdd2, result = MK(v2, innerBiimp(bdd1, ut.low(bdd2)), innerBiimp(bdd1, ut.high(bdd2))));
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

			try (GCLock lock = new GCLock()) {
				return new BDDImpl(innerOr(id, ((BDDImpl) other).id));
			}
		}

		@Override
		public BDD orWith(BDD other) {
			assertNonNull(other);
			
			try (GCLock lock = new GCLock()) {
				setId(innerOr(id, ((BDDImpl) other).id));
			}

			other.free();

			return this;
		}

		@Override
		public BDD and(BDD other) {
			assertNonNull(other);
			ut.gcIfAlmostFull();

			try (GCLock lock = new GCLock()) {
				return new BDDImpl(innerAnd(id, ((BDDImpl) other).id));
			}
		}

		@Override
		public BDD andWith(BDD other) {
			assertNonNull(other);

			try (GCLock lock = new GCLock()) {
				setId(innerAnd(id, ((BDDImpl) other).id));
			}

			other.free();

			return this;
		}

		@Override
		public BDD xor(BDD other) {
			assertNonNull(other);
			ut.gcIfAlmostFull();

			try (GCLock lock = new GCLock()) {
				return new BDDImpl(applyXOR(id, ((BDDImpl) other).id));
			}
		}

		@Override
		public BDD xorWith(BDD other) {
			assertNonNull(other);

			try (GCLock lock = new GCLock()) {
				setId(applyXOR(id, ((BDDImpl) other).id));
			}

			other.free();

			return this;
		}

		@Override
		public BDD nand(BDD other) {
			assertNonNull(other);
			ut.gcIfAlmostFull();

			try (GCLock lock = new GCLock()) {
				return new BDDImpl(applyIMP(innerAnd(id, ((BDDImpl) other).id), ZERO));
			}
		}

		@Override
		public BDD nandWith(BDD other) {
			assertNonNull(other);

			try (GCLock lock = new GCLock()) {
				setId(applyIMP(innerAnd(id, ((BDDImpl) other).id), ZERO));
			}

			other.free();

			return this;
		}

		@Override
		public BDD not() {
			ut.gcIfAlmostFull();

			try (GCLock lock = new GCLock()) {
				return new BDDImpl(innerNot(id));
			}
		}

		@Override
		public BDD notWith() {
			try (GCLock lock = new GCLock()) {
				setId(innerNot(id));
			}

			return this;
		}

		protected final int innerNot(int id) {
			return applyIMP(id, ZERO);
		}

		@Override
		public BDD imp(BDD other) {
			assertNonNull(other);
			ut.gcIfAlmostFull();

			try (GCLock lock = new GCLock()) {
				return new BDDImpl(applyIMP(id, ((BDDImpl) other).id));
			}
		}

		@Override
		public BDD impWith(BDD other) {
			assertNonNull(other);

			try (GCLock lock = new GCLock()) {
				setId(applyIMP(id, ((BDDImpl) other).id));
			}

			other.free();

			return this;
		}

		@Override
		public BDD biimp(BDD other) {
			assertNonNull(other);
			ut.gcIfAlmostFull();

			try (GCLock lock = new GCLock()) {
				return new BDDImpl(innerBiimp(id, ((BDDImpl) other).id));
			}
		}

		@Override
		public BDD biimpWith(BDD other) {
			assertNonNull(other);

			try (GCLock lock = new GCLock()) {
				setId(innerBiimp(id, ((BDDImpl) other).id));
			}

			other.free();

			return this;
		}

		@Override
		public BDD copy() {
			ut.gcIfAlmostFull();

			try (GCLock lock = new GCLock()) {
				return new BDDImpl(id);
			}
		}

		@Override
		public Assignment anySat() throws UnsatException {
			AssignmentImpl assignment = new AssignmentImpl();

			try (GCLock lock = new GCLock()) {
				anySat(id, assignment);
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
			try (GCLock lock = new GCLock()) {
				return allSat(id);
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
					for (Assignment assignment: lowList)
						((AssignmentImpl) assignment).put(var, false);

					List<Assignment> highList = allSat(ut.high(bdd));
					for (Assignment assignment: highList)
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
			try (GCLock lock = new GCLock()) {
				return (long) (Math.pow(2, ut.var(id)) * count(id, maxVar));
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
			try (GCLock lock = new GCLock()) {
				return pathCount(id);
			}
		}

		private long pathCount(int id) {
			if (id < FIRST_NODE_NUM)
				return id;
			else
				return pathCount(ut.low(id)) + pathCount(ut.high(id));
		}

		@Override
		public BDD restrict(BDD var) {
			assertNonNull(var);

			try (GCLock lock = new GCLock()) {
				int res = id;
				for (int varId = ((BDDImpl)var).id; varId >= FIRST_NODE_NUM; varId = ut.high(varId))
					if (ut.low(varId) == ZERO)
						res = restrict(res, ut.var(varId), true);
					else if (ut.low(varId) == ONE)
						res = restrict(res, ut.var(varId), false);

				return new BDDImpl(res);
			}
		}

		@Override
		public BDD restrictWith(BDD var) {
			assertNonNull(var);

			try (GCLock lock = new GCLock()) {
				int res = id;
				for (int varId = ((BDDImpl)var).id; varId >= FIRST_NODE_NUM; varId = ut.high(varId)) {
					if (ut.low(varId) == ZERO)
						res = restrict(res, ut.var(varId), true);
					if (ut.low(varId) == ONE)
						res = restrict(res, ut.var(varId), false);
				}

				setId(res);
			}

			return this;
		}

		protected int innerRestrict(int var, boolean value) {
			return restrict(id, var, value);
		}

		@Override
		public BDD restrict(int var, boolean value) {
			try (GCLock lock = new GCLock()) {
				return new BDDImpl(innerRestrict(var, value));
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

		protected int innerExist(int var) {
			int falseId = innerRestrict(var, false);
			int trueId = innerRestrict(var, true);

			return innerOr(falseId, trueId);
		}

		@Override
		public BDD exist(int var) {
			try (GCLock lock = new GCLock()) {
				return new BDDImpl(innerExist(var));
			}
		}

		@Override
		public BDD exist(BDD vars) {
			assertNonNull(vars);
			return quantify(vars.vars(), true);
		}

		@Override
		public BDD exist(BitSet vars) {
			assertNonNull(vars);
			return quantify(vars, true);
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
			return quantify(var.vars(), false);
		}

		private BDD quantify(BitSet vars, boolean exist) {
			assertNonNull(vars);
			int hashCodeVars = vars.hashCode();

			try (GCLock lock = new GCLock()) {
				return new BDDImpl(innerQuantify(id, vars, exist, hashCodeVars));
			}
		}

		protected final int innerQuantify(int bdd, BitSet vars, boolean exist, int hashCodeOfVars) {
			if (bdd < FIRST_NODE_NUM) // terminal node
				return bdd;

			int result = ut.getQuantCache().get(exist, bdd, vars, hashCodeOfVars);
			if (result >= 0)
				return result;

			int oldA = ut.low(bdd), oldB = ut.high(bdd);
			int a = innerQuantify(oldA, vars, exist, hashCodeOfVars);
			int b = innerQuantify(oldB, vars, exist, hashCodeOfVars);

			int var = ut.var(bdd);

			if (vars.get(var))
				if (exist)
					result = innerOr(a, b);
				else
					result = innerAnd(a, b);
			else
				if (a == oldA && b == oldB)
					result = bdd;
				else
					result = MK(var, a, b);

			ut.getQuantCache().put(exist, bdd, vars, hashCodeOfVars, result);

			return result;
		}

		@Override
		public BDD simplify(BDD d) {
			assertNonNull(d);
			
			try (GCLock lock = new GCLock()) {
				return new BDDImpl(simplify(((BDDImpl) d).id, id));
			}
		}

		private int simplify(int d, int u) {
			if (d == ZERO || u == ZERO)
				return ZERO;
			else if (u == ONE)
				return ONE;

			int vu = ut.var(u), vd = ut.var(d);

			if (d == ONE)
				return MK(vu, simplify(d, ut.low(u)), simplify(d, ut.high(u)));
			else if (vd == vu)
				if (ut.low(d) == ZERO)
					return simplify(ut.high(d), ut.high(u));
				else if (ut.high(d) == ZERO)
					return simplify(ut.low(d), ut.low(u));
				else
					return MK(vu, simplify(ut.low(d), ut.low(u)), simplify(ut.high(d), ut.high(u)));
			else if (vd < vu)
				return MK(vd, simplify(ut.low(d), u), simplify(ut.high(d), u));
			else
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

			try (GCLock lock = new GCLock()) {
				varProfile(id, varp, new HashSet<Integer>());
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

			try (GCLock lock = new GCLock()) {
				return nodeCount = nodeCount(id, new HashSet<Integer>());
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

		protected final int innerReplace(Map<Integer, Integer> renaming, int hashOfRenaming) {
			return innerReplace(id, renaming, hashOfRenaming);
		}

		@Override
		public BDD replace(Map<Integer, Integer> renaming) {
			assertNonNull(renaming);
			if (id == ZERO)
				return makeZero();
			else if (id == ONE)
				return makeOne();

			int hash = renaming.hashCode();

			try (GCLock lock = new GCLock()) {
				return new BDDImpl(innerReplace(renaming, hash));
			}
		}

		@Override
		public BDD replaceWith(Map<Integer, Integer> renaming) {
			assertNonNull(renaming);
			if (id < FIRST_NODE_NUM) // terminal node
				return this;

			int hashCodeOfRenaming = renaming.hashCode();

			try (GCLock lock = new GCLock()) {
				setId(innerReplace(renaming, hashCodeOfRenaming));
			}

			return this;
		}

		private int innerReplace(int bdd, Map<Integer, Integer> renaming, int hashOfRenaming) {
			assertNonNull(renaming);
			if (bdd < FIRST_NODE_NUM) // terminal node
				return bdd;

			int result = ut.getReplaceCache().get(bdd, renaming, hashOfRenaming);
			if (result >= 0)
				return result;

			int oldLow = ut.low(bdd), oldHigh = ut.high(bdd);
			int lowRenamed = innerReplace(oldLow, renaming, hashOfRenaming);
			int highRenamed = innerReplace(oldHigh, renaming, hashOfRenaming);
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

			try (GCLock lock = new GCLock()) {
				return new BDDImpl(ite(id, ((BDDImpl) thenBDD).id, ((BDDImpl) elseBDD).id));
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

			try (GCLock lock = new GCLock()) {
				return new BDDImpl(compose(id, ((BDDImpl) other).id, var));
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
		public boolean isEquivalentTo(BDD other) {
			assertNonNull(other);
			if (this == other)
				return true;

			BDDImpl otherImpl = (BDDImpl) other;

			try (GCLock lock = new GCLock()) {
				return id == otherImpl.id;
			}
		}

		@Override
		public int hashCodeAux() {
			return hashCode;
		}

		@Override
		public int var() {
			try (GCLock lock = new GCLock()) {
				return ut.var(id);
			}
		}

		@Override
		public BDDImpl high() {
			try (GCLock lock = new GCLock()) {
				return new BDDImpl(ut.high(id));
			}
		}

		@Override
		public BDDImpl low() {
			try (GCLock lock = new GCLock()) {
				return new BDDImpl(ut.low(id));
			}
		}

		@Override
		public Factory getFactory() {
			return Factory.this;
		}

		@Override
		public BitSet vars() {
			BitSet vars = new BitSet();

			try (GCLock lock = new GCLock()) {
				updateVars(id, vars);
			}

			return vars;
		}

		private void updateVars(int id, BitSet vars) {
			if (id >= FIRST_NODE_NUM) {
				vars.set(ut.var(id));
				updateVars(ut.low(id), vars);
				updateVars(ut.high(id), vars);
			}
		}

		@Override
		public int maxVar() {
			try (GCLock lock = new GCLock()) {
				return maxVar(id);
			}
		}

		private int maxVar(int bdd) {
			if (bdd < FIRST_NODE_NUM)
				return -1;

			int low = ut.low(bdd);
			int maxVar = Math.max(ut.var(bdd), maxVar(low));
			int high = ut.high(bdd);
			maxVar = Math.max(maxVar, maxVar(high));
			return maxVar;
		}
	}

	/**
	 * @return the maximum variable index used so far
	 */
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
			Boolean result = truthTable.get(i);
			if (result != null)
				return result;

			throw new IndexOutOfBoundsException("unknown variable " + i);
		}

		@Override
		public BDD toBDD() {
			BDD res = makeOne();
			for (int v: truthTable.keySet())
				res.andWith(truthTable.get(v) == Boolean.FALSE ? makeNotVar(v) :  makeVar(v));

			return res;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder("<");

			for (int v: truthTable.keySet())
				sb.append(v).append(":").append(truthTable.get(v) == Boolean.TRUE ? 1 : 0).append(", ");

			return sb.substring(0, sb.length() - 2).concat(">");
		}
	}

	public void printNodeTable() {
		try (GCLock lock = new GCLock()) {
			System.out.println(ut);
		}
	}

	/**
	 * @return a BDD object representing the constant zero
	 */
	public BDD makeZero() {
		try (GCLock lock = new GCLock()) {
			return mk(ZERO);
		}
	}

	/**
	 * @return a BDD object representing the constant one
	 */
	public BDD makeOne() {
		try (GCLock lock = new GCLock()) {
			return mk(ONE);
		}
	}

	/**
	 * Runs the garbage collection.
	 */
	public void gc() {
		ut.gc();
	}

	/**
	 * Sets the maximal increase for the number nodes in the table of nodes
	 * of this factory.
	 *
	 * @param maxIncrease the maximal increase
	 * @return the old maximal increase
	 */
	public int setMaxIncrease(int maxIncrease) {
		return ut.setMaxIncrease(maxIncrease);
	}

	public double setIncreaseFactor(double increaseFactor) {
		return ut.setIncreaseFactor(increaseFactor);
	}

	/**
	 * Sets the cache ratio for the operator caches. When the node table grows,
	 * operator caches will also grow to maintain the ratio.
	 *
	 * @param cacheRatio the cache ratio
	 * @return the old cache ratio
	 */
	public double setCacheRatio(double cacheRatio) {
		return ut.setCacheRatio(cacheRatio);
	}

	/**
	 * Sets the minimum percentage of nodes to be reclaimed after a garbage collection.
	 * If this percentage is not reclaimed, the node table will be grown.
	 * The range of x is 0..1. The default is .20.
	 *
	 * @param minFreeNodes the percentage
	 * @return the old percentage
	 */
	public double setMinFreeNodes(double minFreeNodes) {
		return ut.setMinFreeNodes(minFreeNodes);
	}

	/**
	 * Sets the listener of garbage collection operations.
	 *
	 * @param listener the listener
	 */

	public void setGarbageCollectionListener(GarbageCollectionListener listener) {
		ut.setGarbageCollectionListener(listener);
	}

	/**
	 * Sets the listener of resize operations.
	 *
	 * @param listener the listener
	 */
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

		@SuppressWarnings("unchecked")
		List<BDDImpl> copy = (ArrayList<BDDImpl>) allBDDsCreatedSoFar.clone();
		allBDDsCreatedSoFar.clear();

		for (BDDImpl bdd: copy) {
			allBDDsCreatedSoFar.add(bdd);

			if (bdd.id >= NUM_OF_PREALLOCATED_NODES)
				markAsAlive(bdd.id, aliveNodes);
		}
	}

	private void parallelMarkAliveNodes(final boolean[] aliveNodes) {
		final int total = Runtime.getRuntime().availableProcessors();
		@SuppressWarnings("unchecked")
		final List<BDDImpl> copy = (ArrayList<BDDImpl>) allBDDsCreatedSoFar.clone();

		class AliveNodesMarker implements Runnable {
			private final List<BDDImpl> alive = new ArrayList<>();
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

	/**
	 * Counts the nodes in a collection of BDDs.
	 * Shared nodes are counted only once.
	 * 
	 * @param bdds the collection of BDDs
	 * @return the total number of nodes
	 */
	public int nodeCount(Collection<BDD> bdds) {
		assertNonNull(bdds, "the collection of BBDs cannot be null here");

		int count = 0;
		Set<Integer> seen = new HashSet<>();

		for (BDD bdd: bdds) {
			BDDImpl bddi = (BDDImpl) bdd;
			if (bddi != null)
				try (GCLock lock = new GCLock()) {
					count += bddi.nodeCount(bddi.id, seen);
				}
		}

		return count;
	}

	ArrayList<BDDImpl> getAllBDDsCreatedSoFarCopy() {
		return new ArrayList<>(allBDDsCreatedSoFar);
	}

	/**
	 * @return the number of non-freed BDD instances created so far
	 */
	public int bddCount() {
		synchronized (allBDDsCreatedSoFar) {
			return allBDDsCreatedSoFar.size() - freedBDDsCounter;
		}
	}
}