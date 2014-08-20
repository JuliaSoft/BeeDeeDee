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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import checkers.nullness.quals.NonNull;
import static checkers.nullness.support.NullnessAssertions.assertNonNull;

import com.juliasoft.beedeedee.bdd.Assignment;
import com.juliasoft.beedeedee.bdd.ReplacementWithExistingVarException;
import com.juliasoft.beedeedee.factories.ResizingAndGarbageCollectedFactory.GarbageCollectionListener;
import com.juliasoft.beedeedee.factories.ResizingAndGarbageCollectedFactory.ResizeListener;

import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDBitVector;
import net.sf.javabdd.BDDDomain;
import net.sf.javabdd.BDDException;
import net.sf.javabdd.BDDFactory;
import net.sf.javabdd.BDDPairing;

/**
 * An adapter class to use BeeDeeDee through the JavaBDD interface.
 */
public class JavaBDDAdapterFactory extends BDDFactory {

	// this is non-null since it is definitely initialized by the static pseudo-constructor
	private @NonNull ResizingAndGarbageCollectedFactory factory;
	private int bddVarNum;

	private JavaBDDAdapterFactory() {}
	
	public static BDDFactory init(int nodenum, int cachesize) {
        JavaBDDAdapterFactory f = new JavaBDDAdapterFactory();
        f.initialize(nodenum, cachesize);
        return f;
    }

	
	@Override
	public BDD zero() {
		return new JavaBDDAdapterBDD(factory.makeZero());
	}

	@Override
	public BDD one() {
		return new JavaBDDAdapterBDD(factory.makeOne());
	}

	@Override
	protected void initialize(int nodenum, int cachesize) {
		factory = Factory.mkResizingAndGarbageCollected(nodenum, cachesize);

		// we set the default call-backs

		try {
			Method m = JavaBDDAdapterFactory.class.getDeclaredMethod
				("defaultResizeCallback", new Class[] { int.class, int.class });
			registerResizeCallback(this, m);

			m = JavaBDDAdapterFactory.class.getDeclaredMethod
				("defaultGCCallback", new Class[] { int.class, BDDFactory.GCStats.class });
			registerGCCallback(this, m);
		}
		catch (SecurityException e) {}
		catch (NoSuchMethodException e) {}
	}

	@Override
	public boolean isInitialized() {
		return true;
	}

	@Override
	public void done() {
		factory.done();
		factory = null;
	}

	/**
	 * Unsupported operation.
	 */
	
	@Override
	public void setError(int code) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Unsupported operation.
	 */
	
	@Override
	public void clearError() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Unsupported operation.
	 */
	
	@Override
	public int setMaxNodeNum(int size) {
		throw new UnsupportedOperationException();
	}

	@Override
	public double setMinFreeNodes(double x) {
		return factory.setMinFreeNodes(x);
	}

	@Override
	public int setMaxIncrease(int maxIncrease) {
		return factory.setMaxIncrease(maxIncrease);
	}

	@Override
	public double setIncreaseFactor(double increaseFactor) {
		return factory.setIncreaseFactor(increaseFactor);
	}

	@Override
	public double setCacheRatio(double cacheRatio) {
		return factory.setCacheRatio(cacheRatio);
	}

	/**
	 * Register a callback that is called when garbage collection starts or stops.
	 * 
	 * @param o the object whose method is called
	 * @param m the method that gets called
	 */

	@Override
	public void registerGCCallback(final Object o, final Method m) {
		assertNonNull(m, "garbage collection callback cannot be null");

		factory.setGarbageCollectionListener(new GarbageCollectionListener() {

			@Override
			public void onStart(int num, int size, int free, long totalTime) {
				try {
					m.invoke(o, 1, new GCStats(num, size, free, 0L, totalTime));
				} catch (IllegalAccessException e) {
				} catch (IllegalArgumentException e) {
				} catch (InvocationTargetException e) {
				}
			}

			@Override
			public void onStop(int num, int size, int free, long time, long totalTime) {
				try {
					m.invoke(o, 0, new GCStats(num, size, free, time, totalTime));
				} catch (IllegalAccessException e) {
				} catch (IllegalArgumentException e) {
				} catch (InvocationTargetException e) {
				}
			}
		});
	}

	/**
	 * Unregister a callback that is called when garbage collection starts or stops.
	 * 
	 * @param o the object whose method is called
	 * @param m the method that gets called
	 */

	@Override
	public void unregisterGCCallback(Object o, Method m) {
		factory.setGarbageCollectionListener(null);
	}

	/**
	 * This is the default method that is called every time a garbage collection is performed on the
	 * set of binary decision nodes.
	 * 
	 * @param x
	 *            1 if the garbage collection starts, 0 if it completes
	 * @param stats
	 *            statistics about the effect of the last garbage collection
	 */

	void defaultGCCallback(int x, BDDFactory.GCStats stats) {
		// we only print something at the end of the garbage collection
		if (x == 0)
			System.out.println(stats);
	}

	/**
	 * This is the default method that is called every time a resize operation is performed on the
	 * set of binary decision nodes.
	 * 
	 * @param oldSize
	 * 			  the old size of the table of nodes
	 * @param newSize
	 * 			  the new size of the table of nodes
	 */

	void defaultResizeCallback(int oldSize, int newSize) {
		System.out.println("Resizing node table from " + oldSize + " to " + newSize);
	}

	/**
	 * Register a callback that is called when resize has been performed.
	 * 
	 * @param o the object whose method is called
	 * @param m the method that gets called
	 */

	@Override
	public void registerResizeCallback(final Object o, final Method m) {
		assertNonNull(m, "resize callback cannot be null");

		factory.setResizeListener(new ResizeListener() {

			@Override
			public void onStart(int num, int oldSize, int newSize, long totalTime) {
				// nothing
			}

			@Override
			public void onStop(int num, int oldSize, int newSize, long time, long totalTime) {
				try {
					m.invoke(o, oldSize, newSize);
				} catch (IllegalAccessException e) {
				} catch (IllegalArgumentException e) {
				} catch (InvocationTargetException e) {
				}
			}
		});
	}

	/**
	 * Unregister a callback that is called when resize has been performed.
	 * 
	 * @param o the object whose method is called
	 * @param m the method that gets called
	 */

	@Override
	public void unregisterResizeCallback(Object o, Method m) {
		factory.setResizeListener(null);
	}

	/**
	 * Register a callback that is called when reorder has been performed.
	 * 
	 * @param o the object whose method is called
	 * @param m the method that gets called
	 */

	@Override
	public void registerReorderCallback(Object o, Method m) {
		// unused
	}

	/**
	 * Unregister a callback that is called when reorder has been performed.
	 * 
	 * @param o the object whose method is called
	 * @param m the method that gets called
	 */

	@Override
	public void unregisterReorderCallback(Object o, Method m) {
		// unused
	}

	private static class GCStats extends BDDFactory.GCStats {
		private GCStats(int num, int size, int free, long time, long totalTime) {
			super();

			this.num = num;
			this.nodes = size;
			this.freenodes = free;
			this.time = time;
			this.sumtime = totalTime;
		}
	}

	/**
	 * Unsupported operation.
	 */
	
	@Override
	public int setNodeTableSize(int n) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Unsupported operation.
	 */
	
	@Override
	public int setCacheSize(int n) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int varNum() {
		return bddVarNum;
	}

	@Override
	public int setVarNum(int num) {
		int oldBddVarNum = bddVarNum;
		
		if (num < bddVarNum)
			throw new BDDException("Trying to decrease the number of variables. It was " + bddVarNum);

		if (num == bddVarNum)
			return 0;

		bddVarNum = num;

		return oldBddVarNum;
	}

	@Override
	public BDD ithVar(int var) {
		if (var < 0 || var >= bddVarNum)
			throw new BDDException("Unknown variable " + var + " max allowed is " + (bddVarNum - 1));
		
		return new JavaBDDAdapterBDD(factory.makeVar(var));
	}

	@Override
	public BDD nithVar(int var) {
		if (var < 0 || var >= bddVarNum)
			throw new BDDException("Unknown variable " + var + " max allowed is " + (bddVarNum - 1));
		
		return new JavaBDDAdapterBDD(factory.makeNotVar(var));
	}

	/**
	 * Unsupported operation.
	 */
	
	@Override
	public void printAll() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Unsupported operation.
	 */
	
	@Override
	public void printTable(BDD b) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Unsupported operation.
	 */
	
	@Override
	public int level2Var(int level) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int var2Level(int var) {
		// TODO this should differ from var only after reordering - and calling duplicateVar
		return var;
	}

	/**
	 * Unsupported operation.
	 */
	
	@Override
	public void reorder(ReorderMethod m) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Unsupported operation.
	 */
	
	@Override
	public void autoReorder(ReorderMethod method) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Unsupported operation.
	 */
	
	@Override
	public void autoReorder(ReorderMethod method, int max) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Unsupported operation.
	 */
	
	@Override
	public ReorderMethod getReorderMethod() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Unsupported operation.
	 */
	
	@Override
	public int getReorderTimes() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Unsupported operation.
	 */
	
	@Override
	public void disableReorder() {
		throw new UnsupportedOperationException();

	}

	/**
	 * Unsupported operation.
	 */
	
	@Override
	public void enableReorder() {
		throw new UnsupportedOperationException();

	}

	/**
	 * Unsupported operation.
	 */
	
	@Override
	public int reorderVerbose(int v) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Unsupported operation.
	 */
	
	@Override
	public void setVarOrder(int[] neworder) {
		throw new UnsupportedOperationException();
	}

	@Override
	public BDDPairing makePair() {
		return new JavaBDDAdapterBDDPairing();
	}

	/**
	 * Unsupported operation.
	 */
	
	@Override
	public void swapVar(int v1, int v2) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Unsupported operation.
	 */
	
	@Override
	public int duplicateVar(int var) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Unsupported operation.
	 */
	
	@Override
	public void addVarBlock(BDD var, boolean fixed) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Unsupported operation.
	 */
	
	@Override
	public void addVarBlock(int first, int last, boolean fixed) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Unsupported operation.
	 */
	
	@Override
	public void varBlockAll() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Unsupported operation.
	 */
	
	@Override
	public void clearVarBlocks() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Unsupported operation.
	 */
	
	@Override
	public void printOrder() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Unsupported operation.
	 */
	
	@Override
	public String getVersion() {
		throw new UnsupportedOperationException();
	}

	@SuppressWarnings("unchecked")
	@Override
	public int nodeCount(@SuppressWarnings("rawtypes") Collection r) {
		return factory.nodeCount(r);
	}

	@Override
	public int getNodeTableSize() {
		return factory.nodesCount();
	}

	@Override
	public int getNodeNum() {
		return factory.nodesCount();
	}

	/**
	 * Unsupported operation.
	 */
	
	@Override
	public int getCacheSize() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Unsupported operation.
	 */
	
	@Override
	public int reorderGain() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Unsupported operation.
	 */
	
	@Override
	public void printStat() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Unsupported operation.
	 */
	
	@Override
	protected BDDDomain createDomain(int a, BigInteger b) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Unsupported operation.
	 */
	
	@Override
	protected BDDBitVector createBitVector(int a) {
		throw new UnsupportedOperationException();
	}

	private class JavaBDDAdapterBDD extends BDD {

		private final com.juliasoft.beedeedee.bdd.BDD bdd;

		private JavaBDDAdapterBDD(com.juliasoft.beedeedee.bdd.BDD bdd) {
			this.bdd = bdd;
		}

		@Override
		public BDDFactory getFactory() {
			return JavaBDDAdapterFactory.this;
		}

		@Override
		public boolean isZero() {
			return bdd.isZero();
		}

		@Override
		public boolean isOne() {
			return bdd.isOne();
		}

		@Override
		public int var() {
			return bdd.var();
		}

		@Override
		public BDD high() {
			return new JavaBDDAdapterBDD(bdd.high());
		}

		@Override
		public BDD low() {
			return new JavaBDDAdapterBDD(bdd.low());
		}

		@Override
		public BDD id() {
			return new JavaBDDAdapterBDD(bdd.copy());
		}

		@Override
		public BDD not() {
			return new JavaBDDAdapterBDD(bdd.not());
		}
		
		@Override
		public BDD ite(BDD thenBDD, BDD elseBDD) {
			assertNonNull(thenBDD);
			assertNonNull(elseBDD);
			return new JavaBDDAdapterBDD(bdd.ite(((JavaBDDAdapterBDD)thenBDD).bdd, ((JavaBDDAdapterBDD)elseBDD).bdd));
		}

		@Override
		public BDD relprod(BDD that, BDD var) {
			assertNonNull(that);
			assertNonNull(var);
			return new JavaBDDAdapterBDD(bdd.relProd(((JavaBDDAdapterBDD)that).bdd, ((JavaBDDAdapterBDD)var).bdd));
		}

		@Override
		public BDD compose(BDD g, int var) {
			assertNonNull(g);
			if (var < 0 || var >= bddVarNum)
				throw new BDDException("Unknown variable " + var + " max allowed is " + (bddVarNum - 1));
			
			return new JavaBDDAdapterBDD(bdd.compose(((JavaBDDAdapterBDD)g).bdd, var));
		}

		/**
		 * Unsupported operation.
		 */
		
		@Override
		public BDD veccompose(BDDPairing pair) {
			throw new UnsupportedOperationException();
		}

		/**
		 * Unsupported operation.
		 */
		
		@Override
		public BDD constrain(BDD that) {
			throw new UnsupportedOperationException();
		}

		@Override
		public BDD exist(BDD var) {
			assertNonNull(var);
			return new JavaBDDAdapterBDD(bdd.exist(((JavaBDDAdapterBDD)var).bdd));
		}

		@Override
		public BDD forAll(BDD var) {
			assertNonNull(var);
			return new JavaBDDAdapterBDD(bdd.forAll(((JavaBDDAdapterBDD)var).bdd));
		}

		/**
		 * Unsupported operation.
		 */
		
		@Override
		public BDD unique(BDD var) {
			throw new UnsupportedOperationException();
		}

		@Override
		public BDD restrict(BDD var) {
			assertNonNull(var);
			return new JavaBDDAdapterBDD(bdd.restrict(((JavaBDDAdapterBDD)var).bdd));
		}

		@Override
		public BDD restrictWith(BDD var) {
			assertNonNull(var);
			bdd.restrictWith(((JavaBDDAdapterBDD)var).bdd);
			return this;
		}

		@Override
		public BDD simplify(BDD d) {
			assertNonNull(d);
			return new JavaBDDAdapterBDD(bdd.simplify(((JavaBDDAdapterBDD)d).bdd));
		}

		/**
		 * Unsupported operation.
		 */
		
		@Override
		public BDD support() {
			throw new UnsupportedOperationException();
		}

		@Override
		public BDD apply(BDD that, BDDOp opr) {
			assertNonNull(that);

			if (opr == and)
				return and(that);
			if (opr == xor)
				return xor(that);
			if (opr == or)
				return or(that);
			if (opr == nand)
				return new JavaBDDAdapterBDD(bdd.nand(((JavaBDDAdapterBDD)that).bdd));
			if (opr == imp)
				return imp(that);
			if (opr == biimp)
				return biimp(that);
			
			throw new UnsupportedOperationException("Unsupported operator: " + opr);
		}

		@Override
		public BDD applyWith(BDD that, BDDOp opr) {
			BDD temp = apply(that, opr);
			that.free();
			return temp;
		}

		/**
		 * Unsupported operation.
		 */
		
		@Override
		public BDD applyAll(BDD that, BDDOp opr, BDD var) {
			throw new UnsupportedOperationException();
		}

		/**
		 * Unsupported operation.
		 */
		
		@Override
		public BDD applyEx(BDD that, BDDOp opr, BDD var) {
			throw new UnsupportedOperationException();
		}

		/**
		 * Unsupported operation.
		 */
		
		@Override
		public BDD applyUni(BDD that, BDDOp opr, BDD var) {
			throw new UnsupportedOperationException();
		}

		@Override
		public BDD satOne() {
			return new JavaBDDAdapterBDD(bdd.anySat().toBDD());
		}

		/**
		 * Unsupported operation.
		 */
		
		@Override
		public BDD fullSatOne() {
			throw new UnsupportedOperationException();
		}

		/**
		 * Unsupported operation.
		 */
		
		@Override
		public BDD satOne(BDD var, boolean pol) {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<BDD> allsat() {
			ArrayList<BDD> list = new ArrayList<BDD>();
			for (Assignment a : bdd.allSat()) {
				list.add(new JavaBDDAdapterBDD(a.toBDD()));
			}
			
			return list;
		}

		@Override
		public BDD replace(BDDPairing pair) {
			try {
				return new JavaBDDAdapterBDD(bdd.replace(((JavaBDDAdapterBDDPairing)pair).renaming));
			} catch (ReplacementWithExistingVarException e) {
				throw new BDDException("Trying to replace with variable " + e.getVarNum() + " which is already in the bdd");
			}
		}

		@Override
		public BDD replaceWith(BDDPairing pair) {
			try {
				bdd.replaceWith(((JavaBDDAdapterBDDPairing)pair).renaming);
			} catch (ReplacementWithExistingVarException e) {
				throw new BDDException("Trying to replace with variable " + e.getVarNum() + " which is already in the bdd");
			}

			return this;
		}

		@Override
		public int nodeCount() {
			return bdd.nodeCount();
		}

		/**
		 * Unsupported operation.
		 */
		
		@Override
		public double pathCount() {
			return bdd.pathCount();
		}

		@Override
		public double satCount() {
			return bdd.satCount();
		}

		@Override
		public int[] varProfile() {
			return bdd.varProfile();
		}

		@Override
		public boolean equals(BDD that) {
			return that instanceof JavaBDDAdapterBDD &&
					bdd.equalsAux(((JavaBDDAdapterBDD) that).bdd);
		}

		@Override
		public int hashCode() {
			return bdd.hashCodeAux();
		}

		@Override
		public void free() {
			bdd.free();
		}
		
		/*
		 * Non-abstract methods
		 */
		
		@Override
		public BDD and(BDD that) {
			assertNonNull(that);
			return new JavaBDDAdapterBDD(bdd.and(((JavaBDDAdapterBDD)that).bdd));
		}
		
		@Override
		public BDD andWith(BDD that) {
			assertNonNull(that);
			bdd.andWith(((JavaBDDAdapterBDD)that).bdd);
			return this;
		}
		
		@Override
		public BDD or(BDD that) {
			assertNonNull(that);
			return new JavaBDDAdapterBDD(bdd.or(((JavaBDDAdapterBDD)that).bdd));
		}
		
		@Override
		public BDD orWith(BDD that) {
			assertNonNull(that);
			bdd.orWith(((JavaBDDAdapterBDD)that).bdd);
			return this;
		}

		@Override
		public BDD xor(BDD that) {
			assertNonNull(that);
			return new JavaBDDAdapterBDD(bdd.xor(((JavaBDDAdapterBDD)that).bdd));
		}
		
		@Override
		public BDD xorWith(BDD that) {
			assertNonNull(that);
			bdd.xorWith(((JavaBDDAdapterBDD)that).bdd);
			return this;
		}

		@Override
		public BDD imp(BDD that) {
			assertNonNull(that);
			return new JavaBDDAdapterBDD(bdd.imp(((JavaBDDAdapterBDD)that).bdd));
		}
		
		@Override
		public BDD impWith(BDD that) {
			assertNonNull(that);
			bdd.impWith(((JavaBDDAdapterBDD)that).bdd);
			return this;
		}

		@Override
		public BDD biimp(BDD that) {
			assertNonNull(that);
			return new JavaBDDAdapterBDD(bdd.biimp(((JavaBDDAdapterBDD)that).bdd));
		}
		
		@Override
		public BDD biimpWith(BDD that) {
			assertNonNull(that);
			bdd.biimpWith(((JavaBDDAdapterBDD)that).bdd);
			return this;
		}

		@Override
		public void printSet() {
			for (Assignment a : bdd.allSat()) {
				System.out.println(a);
			}
		}
	}
	
	private class JavaBDDAdapterBDDPairing extends BDDPairing {
		
		private final Map<Integer, Integer> renaming = new HashMap<Integer, Integer>();

		@Override
		public void set(int oldvar, int newvar) {
			if (oldvar == newvar)
				return;

			if (oldvar < 0 || oldvar >= bddVarNum)
	        	throw new BDDException("Unknown variable " + oldvar + " max allowed is " + (bddVarNum - 1));
	        if (newvar < 0 || newvar >= bddVarNum)
	        	throw new BDDException("Unknown variable " + newvar + " max allowed is " + (bddVarNum - 1));

	        renaming.put(oldvar, newvar);
		}

		@Override
		public void set(int oldvar, BDD newvar) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String toString() {
			return renaming.toString();
		}

		@Override
		public void reset() {
			renaming.clear();
		}
	}
}
