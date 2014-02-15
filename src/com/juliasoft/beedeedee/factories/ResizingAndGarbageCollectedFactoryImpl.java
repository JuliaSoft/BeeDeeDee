package com.juliasoft.beedeedee.factories;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.juliasoft.beedeedee.bdd.Assignment;
import com.juliasoft.beedeedee.bdd.BDD;
import com.juliasoft.beedeedee.bdd.ReplacementWithExistingVarException;
import com.juliasoft.beedeedee.bdd.UnsatException;
import com.juliasoft.utils.concurrent.Executors;

class ResizingAndGarbageCollectedFactoryImpl extends ResizingAndGarbageCollectedFactory {
	private final static int FIRST_NODE_NUM = 2;
	private final static int NUMBER_OF_PREALLOCATED_VARS = 1000;
	private final static int NUM_OF_PREALLOCATED_NODES = FIRST_NODE_NUM + 2 * NUMBER_OF_PREALLOCATED_VARS;
	protected final ResizingAndGarbageCollectedUniqueTable ut;
	private final List<BDDImpl> allBDDsCreatedSoFar = new ArrayList<BDDImpl>();
	private final int ZERO;
	private final int ONE;
	private final int[] vars = new int[NUMBER_OF_PREALLOCATED_VARS];
	private final int[] notVars = new int[NUMBER_OF_PREALLOCATED_VARS];
	private int maxVar;
	
	ResizingAndGarbageCollectedFactoryImpl(int utSize, int cacheSize) {
		utSize = Math.max(utSize, NUM_OF_PREALLOCATED_NODES);
		ut = new ResizingAndGarbageCollectedUniqueTable(utSize, cacheSize, this);

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
		if (var > maxVar)  // track maximum variable index (for satCount)
			synchronized (this) {
				if (var > maxVar)
					maxVar = var;
			}

		return low == high ? low : ut.get(var, low, high);
	}

	private int MKSimple(int var, int low, int high) {
		return low == high ? low : ut.get(var, low, high);
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
			return MKSimple(var, low, high);
		
		if (varLow == varHigh)
			return MKSimple(varLow, MKInOrder(var, ut.low(low), ut.low(high)), MKInOrder(var, ut.high(low), ut.high(high)));
		if (varLow < varHigh)
			return MKSimple(varLow, MKInOrder(var, ut.low(low), high), MKInOrder(var, ut.high(low), high));
		/*
		 * since var cannot appear in low and high
 		 * we have: varHigh < varLow &&  varHigh < var) 
		 */
		return MKSimple(varHigh, MKInOrder(var, low, ut.low(high)), MKInOrder(var, low, ut.high(high)));
	}
	
	@Override
	public int nodesCount() {
		synchronized (ut.getGCLock()) {
			return ut.nodesCount();
		}
	}

	@Override
	public void printStatistics() {
		synchronized (ut.getGCLock()) {
			ut.printStatistics();
		}
	}

	@Override
	public BDDImpl makeVar(int i) {
		synchronized (ut.getGCLock()) {
			if (i >= NUMBER_OF_PREALLOCATED_VARS)
				return new BDDImpl(MK(i, ZERO, ONE));
			else
				return new BDDImpl(vars[i]);
		}
	}

	@Override
	public BDDImpl makeNotVar(int i) {
		synchronized (ut.getGCLock()) {
			if (i >= NUMBER_OF_PREALLOCATED_VARS)
				return new BDDImpl(MK(i, ONE, ZERO));
			else
				return new BDDImpl(notVars[i]);
		}
	}

	private AtomicInteger freedBDDs = new AtomicInteger();
	
	private class BDDImpl implements BDD {

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
			if (id >= NUM_OF_PREALLOCATED_NODES) {
				id = -1;
				ut.scheduleGC();
				ut.gcIfAlmostFull();
			}
			else
				id = -1;

			// when there are enough free bdds, shrink the list
			if (freedBDDs.incrementAndGet() > 100000)
				synchronized (ut.getGCLock()) {
					synchronized (allBDDsCreatedSoFar) {
						if (freedBDDs.get() > 100000) {
							List<BDDImpl> copy = new ArrayList<BDDImpl>(allBDDsCreatedSoFar);
							allBDDsCreatedSoFar.clear();

							for (BDDImpl bdd : copy)
								if (bdd.id >= NUM_OF_PREALLOCATED_NODES) {
									allBDDsCreatedSoFar.add(bdd);
								}
							
							freedBDDs.set(0);
						}
					}
				}
		}

		@Override
		public String toString() {
			if (id < 0)
				return "[freed zombie BDD]";

			synchronized (ut.getGCLock()) {
				return id + ": " + ut.var(id) + ">" + ut.low(id) + ">" + ut.high(id);
			}
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
						result = MKSimple(v1, applyAND(ut.low(bdd1), ut.low(bdd2)), applyAND(ut.high(bdd1), ut.high(bdd2))));
				else if (v1 < v2)
					ut.putIntoCache(Operator.AND, bdd1, bdd2, result = MKSimple(v1, applyAND(ut.low(bdd1), bdd2), applyAND(ut.high(bdd1), bdd2)));
				else
					ut.putIntoCache(Operator.AND, bdd1, bdd2, result = MKSimple(v2, applyAND(bdd1, ut.low(bdd2)), applyAND(bdd1, ut.high(bdd2))));
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
						result = MKSimple(v1, applyOR(ut.low(bdd1), ut.low(bdd2)), applyOR(ut.high(bdd1), ut.high(bdd2))));
				else if (v1 < v2)
					ut.putIntoCache(Operator.OR, bdd1, bdd2, result = MKSimple(v1, applyOR(ut.low(bdd1), bdd2), applyOR(ut.high(bdd1), bdd2)));
				else
					ut.putIntoCache(Operator.OR, bdd1, bdd2, result = MKSimple(v2, applyOR(bdd1, ut.low(bdd2)), applyOR(bdd1, ut.high(bdd2))));
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
						result = MKSimple(v1, applyBIIMP(ut.low(bdd1), ut.low(bdd2)), applyBIIMP(ut.high(bdd1), ut.high(bdd2))));
				else if (v1 < v2)
					ut.putIntoCache(Operator.BIIMP, bdd1, bdd2, result = MKSimple(v1, applyBIIMP(ut.low(bdd1), bdd2), applyBIIMP(ut.high(bdd1), bdd2)));
				else
					ut.putIntoCache(Operator.BIIMP, bdd1, bdd2, result = MKSimple(v2, applyBIIMP(bdd1, ut.low(bdd2)), applyBIIMP(bdd1, ut.high(bdd2))));
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
						result = MKSimple(v1, applyXOR(ut.low(bdd1), ut.low(bdd2)), applyXOR(ut.high(bdd1), ut.high(bdd2))));
				else if (v1 < v2)
					ut.putIntoCache(Operator.XOR, bdd1, bdd2, result = MKSimple(v1, applyXOR(ut.low(bdd1), bdd2), applyXOR(ut.high(bdd1), bdd2)));
				else
					ut.putIntoCache(Operator.XOR, bdd1, bdd2, result = MKSimple(v2, applyXOR(bdd1, ut.low(bdd2)), applyXOR(bdd1, ut.high(bdd2))));
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
						result = MKSimple(v1, applyIMP(ut.low(bdd1), ut.low(bdd2)), applyIMP(ut.high(bdd1), ut.high(bdd2))));
				else if (v1 < v2)
					ut.putIntoCache(Operator.IMP, bdd1, bdd2, result = MKSimple(v1, applyIMP(ut.low(bdd1), bdd2), applyIMP(ut.high(bdd1), bdd2)));
				else
					ut.putIntoCache(Operator.IMP, bdd1, bdd2, result = MKSimple(v2, applyIMP(bdd1, ut.low(bdd2)), applyIMP(bdd1, ut.high(bdd2))));
			}

			return result;
		}

		@Override
		public BDD or(BDD other) {
			ut.gcIfAlmostFull();

			synchronized (ut.getGCLock()) {
				return new BDDImpl(applyOR(id, ((BDDImpl) other).id));
			}
		}

		@Override
		public BDD orWith(BDD other) {
			synchronized (ut.getGCLock()) {
				setId(applyOR(id, ((BDDImpl) other).id));
			}

			other.free();

			return this;
		}

		@Override
		public BDD and(BDD other) {
			ut.gcIfAlmostFull();

			synchronized (ut.getGCLock()) {
				return new BDDImpl(applyAND(id, ((BDDImpl) other).id));
			}
		}

		@Override
		public BDD andWith(BDD other) {
			synchronized (ut.getGCLock()) {
				setId(applyAND(id, ((BDDImpl) other).id));
			}

			other.free();

			return this;
		}

		@Override
		public BDD xor(BDD other) {
			ut.gcIfAlmostFull();

			synchronized (ut.getGCLock()) {
				return new BDDImpl(applyXOR(id, ((BDDImpl) other).id));
			}
		}

		@Override
		public BDD xorWith(BDD other) {
			synchronized (ut.getGCLock()) {
				setId(applyXOR(id, ((BDDImpl) other).id));
			}

			other.free();

			return this;
		}

		@Override
		public BDD nand(BDD other) {
			ut.gcIfAlmostFull();

			synchronized (ut.getGCLock()) {
				return new BDDImpl(applyIMP(applyAND(id, ((BDDImpl) other).id), ZERO));
			}
		}

		@Override
		public BDD nandWith(BDD other) {
			synchronized (ut.getGCLock()) {
				setId(applyIMP(applyAND(id, ((BDDImpl) other).id), ZERO));
			}

			other.free();

			return this;
		}

		@Override
		public BDD not() {
			ut.gcIfAlmostFull();

			synchronized (ut.getGCLock()) {
				return new BDDImpl(applyIMP(id, ZERO));
			}
		}

		@Override
		public BDD notWith() {
			synchronized (ut.getGCLock()) {
				setId(applyIMP(id, ZERO));
			}
			
			return this;
		}

		@Override
		public BDD imp(BDD other) {
			ut.gcIfAlmostFull();

			synchronized (ut.getGCLock()) {
				return new BDDImpl(applyIMP(id, ((BDDImpl) other).id));
			}
		}

		@Override
		public BDD impWith(BDD other) {
			synchronized (ut.getGCLock()) {
				setId(applyIMP(id, ((BDDImpl) other).id));
			}

			other.free();

			return this;
		}

		@Override
		public BDD biimp(BDD other) {
			ut.gcIfAlmostFull();

			synchronized (ut.getGCLock()) {
				return new BDDImpl(applyBIIMP(id, ((BDDImpl) other).id));
			}
		}

		@Override
		public BDD biimpWith(BDD other) {
			synchronized (ut.getGCLock()) {
				setId(applyBIIMP(id, ((BDDImpl) other).id));
			}

			other.free();

			return this;
		}

		@Override
		public BDD copy() {
			ut.gcIfAlmostFull();

			synchronized (ut.getGCLock()) {
				return new BDDImpl(id);
			}
		}

		@Override
		public Assignment anySat() throws UnsatException {
			AssignmentImpl assignment = new AssignmentImpl();

			synchronized (ut.getGCLock()) {
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
			synchronized (ut.getGCLock()) {
				return allSat(id);
			}
		}

		private List<Assignment> allSat(int bdd) {
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
			synchronized (ut.getGCLock()) {
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
			synchronized (ut.getGCLock()) {
				return pathCount(id);
			}
		}
		
		private long pathCount(int id) {
			if (id < FIRST_NODE_NUM) // terminal node
				return id;
			
			return pathCount(ut.low(id)) + pathCount(ut.high(id));
		}

		@Override
		public BDD restrict(BDD var) {
			synchronized (ut.getGCLock()) {
				int res = id;
				for (int varId = ((BDDImpl)var).id; varId >= FIRST_NODE_NUM; varId = ut.high(varId)) {
					if (ut.low(varId) == ZERO)
						res = restrict(res, ut.var(varId), true);
					if (ut.low(varId) == ONE)
						res = restrict(res, ut.var(varId), false);
				}
	
				return new BDDImpl(res);
			}
		}

		@Override
		public BDD restrictWith(BDD var) {
			synchronized (ut.getGCLock()) {
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

		@Override
		public BDD restrict(int var, boolean value) {
			synchronized (ut.getGCLock()) {
				return new BDDImpl(restrict(id, var, value));
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
				result = MKSimple(ut.var(u), restrict(ut.low(u), var, value), restrict(ut.high(u), var, value));
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
			return quant(var, false);
		}
		
		private BDD quant(BDD var, boolean exist) {
			ArrayList<Integer> vars = new ArrayList<Integer>();
			int pos = 0;

			synchronized (ut.getGCLock()) {
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
					result = MKSimple(var, a, b);
			}

			ut.getQuantCache().put(exist, bdd, vars, hashCodeOfVars, result);
			
			return result;
		}

		@Override
		public BDD simplify(BDD d) {
			synchronized (ut.getGCLock()) {
				return new BDDImpl(simplify(((BDDImpl) d).id, id));
			}
		}
		
		private int simplify(int d, int u) {
			if (d == ZERO || u == ZERO)
				return ZERO;

			if (u == ONE)
				return ONE;

			int vu = ut.var(u), vd = ut.var(d);

			if (d == ONE)
				return MKSimple(vu, simplify(d, ut.low(u)), simplify(d, ut.high(u)));
			
			if (vd == vu) {
				if (ut.low(d) == ZERO)
					return simplify(ut.high(d), ut.high(u));

				if (ut.high(d) == ZERO)
					return simplify(ut.low(d), ut.low(u));

				return MKSimple(vu, simplify(ut.low(d), ut.low(u)), simplify(ut.high(d), ut.high(u)));
			}
			
			if (vd < vu)
				return MKSimple(vd, simplify(ut.low(d), u), simplify(ut.high(d), u));

			return MKSimple(vu, simplify(d, ut.low(u)), simplify(d, ut.high(u)));
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

			synchronized (ut.getGCLock()) {
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

			synchronized (ut.getGCLock()) {
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

		@Override
		public BDD replace(Map<Integer, Integer> renaming) {
			if (id == ZERO)
				return makeZero();
			if (id == ONE)
				return makeOne();

			int hash = renaming.hashCode();

			synchronized (ut.getGCLock()) {
				return new BDDImpl(replace(id, renaming, hash));
			}
		}

		@Override
		public BDD replaceWith(Map<Integer, Integer> renaming) {
			if (id < FIRST_NODE_NUM) // terminal node
				return this;

			int hashCodeOfRenaming = renaming.hashCode();

			synchronized (ut.getGCLock()) {
				setId(replace(id, renaming, hashCodeOfRenaming));
			}

			return this;
		}

		private int replace(int bdd, Map<Integer, Integer> renaming, int hashOfRenaming) {
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
			synchronized (ut.getGCLock()) {
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
					return MKSimple(vf, ite(ut.low(f), ut.low(g), ut.low(h)), ite(ut.high(f), ut.high(g), ut.high(h)));
				else if (vf < vh)
					return MKSimple(vf, ite(ut.low(f), ut.low(g), h), ite(ut.high(f), ut.high(g), h));
				else
					return MKSimple(vh, ite(f, g, ut.low(h)), ite(f, g, ut.high(h)));
			else if (vf < vg)
				if (vf == vh)
					return MKSimple(vf, ite(ut.low(f), g, ut.low(h)), ite(ut.high(f), g, ut.high(h)));
				else if (vf < vh)
					return MKSimple(vf, ite(ut.low(f), g, h), ite(ut.high(f), g, h));
				else
					return MKSimple(vh, ite(f, g, ut.low(h)), ite(f, g, ut.high(h)));
			else
				if (vg == vh)
					return MKSimple(vg, ite(f, ut.low(g), ut.low(h)), ite(f, ut.high(g), ut.high(h)));
				else if (vg < vh)
					return MKSimple(vg, ite(f, ut.low(g), h), ite(f, ut.high(g), h));
				else
					return MKSimple(vh, ite(f, g, ut.low(h)), ite(f, g, ut.high(h)));
		}

		@Override
		public BDD relProd(BDD other, BDD var) {
			// TODO this implementation is correct, but not efficient
			return and(other).exist(var);
		}
		
		@Override
		public BDD compose(BDD other, int var) {
			synchronized (ut.getGCLock()) {
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
					return MKSimple(v1, compose(ut.low(id1), ut.low(id2), var), compose(ut.high(id1), ut.high(id2), var));
				else if (v1 < v2)
					return MKSimple(v1, compose(ut.low(id1), id2, var), compose(ut.high(id1), id2, var));
				else
					return MKSimple(v2, compose(id1, ut.low(id2), var), compose(id1, ut.high(id2), var));
			else
				return ite(id2, ut.high(id1), ut.low(id1));
		}

		@Override
		public boolean equalsAux(BDD other) {
			if (this == other)
				return true;

			BDDImpl otherImpl = (BDDImpl) other;

			synchronized (ut.getGCLock()) {
				return id == otherImpl.id;
			}
		}

		@Override
		public int hashCodeAux() {
			return hashCode;
		}

		@Override
		public int var() {
			synchronized (ut.getGCLock()) {
				return ut.var(id);
			}
		}

		@Override
		public BDD high() {
			synchronized (ut.getGCLock()) {
				return new BDDImpl(ut.high(id));
			}
		}

		@Override
		public BDD low() {
			synchronized (ut.getGCLock()) {
				return new BDDImpl(ut.low(id));
			}
		}
	}
	
	private class AssignmentImpl implements Assignment {

		private final Map<Integer, Boolean> truthTable;

		private AssignmentImpl() {
			this.truthTable = new TreeMap<Integer, Boolean>();
		}

		private void put(int var, boolean value) {
			truthTable.put(var, value);
		}

		@Override
		public boolean holds(BDD var) throws IndexOutOfBoundsException {
			int varNum;

			synchronized (ut.getGCLock()) {
				varNum = ut.var(((BDDImpl) var).id);
			}

			Boolean result = truthTable.get(varNum);
			if (result != null)
				return result;

			throw new IndexOutOfBoundsException("unknown variable " + varNum);
		}

		@Override
		public BDD toBDD() {
			BDD res = makeOne();
			Set<Integer> vars = truthTable.keySet();

			synchronized (ut.getGCLock()) {
				for (int v : vars) {
					BDD var = new BDDImpl(MK(v, ZERO, ONE));
					if (!truthTable.get(v))
						var.notWith();
				
					res.andWith(var);
				}
			}

			return res;
		}
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder("<");
			
			for (int v : truthTable.keySet())
				sb.append(v).append(":").append(truthTable.get(v) ? 1 : 0).append(", ");
			
			return sb.substring(0, sb.length() - 2).concat(">").toString();
		}
	}

	@Override
	public void printNodeTable() {
		synchronized (ut.getGCLock()) {
			System.out.println(ut);
		}
	}

	@Override
	public BDD makeZero() {
		synchronized (ut.getGCLock()) {
			return new BDDImpl(ZERO);
		}
	}

	@Override
	public BDD makeOne() {
		synchronized (ut.getGCLock()) {
			return new BDDImpl(ONE);
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
	 * by the remapping.
	 *
	 * @param remapping a map from old to new index
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

		for (BDDImpl bdd: copy)
			if (bdd.id >= NUM_OF_PREALLOCATED_NODES) {
				markAsAlive(bdd.id, aliveNodes);
				allBDDsCreatedSoFar.add(bdd);
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
					if (id >= NUM_OF_PREALLOCATED_NODES && id % total == num) {
						markAsAlive(id, aliveNodes);
						alive.add(bdd);
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
		int count = 0;
		Set<Integer> seen = new HashSet<Integer>();
		
		for (BDD bdd : bdds) {
			BDDImpl bddi = (BDDImpl) bdd;

			synchronized (ut.getGCLock()) {
				count += bddi.nodeCount(bddi.id, seen);
			}
		}
		
		return count;
	}
}