package com.juliasoft.beedeedee.factories;

 import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.juliasoft.beedeedee.bdd.BDD;
import com.juliasoft.beedeedee.factories.ResizingAndGarbageCollectedUniqueTable;

// TODO broken
public class UniqueTableTest extends ResizingAndGarbageCollectedUniqueTable {

	private static final int INITIAL_NODE_NUM = 10;
	private static final int ZERO = indexForNode(0);
	private static final int ONE = indexForNode(1);

	private static int N2 = indexForNode(2);
	private static int N3 = indexForNode(3);
	private static int N4 = indexForNode(4);
	private static int N5 = indexForNode(5);
	private static FactoryStub factoryStub = new FactoryStub(10, 10);

	private static class FactoryStub extends ResizingAndGarbageCollectedFactoryImpl {

		private int[] handles = new int[10];
		
		private FactoryStub(int utSize, int cacheSize) {
			super(utSize, cacheSize);
		}

		@Override
		public int nodesCount() {
			return 0;
		}

		@Override
		public void printStatistics() {
		}

//		@Override
//		public BDD makeVar(int i) {
//			return null;
//		}

		@Override
		public void printNodeTable() {
		}

		@Override
		public BDD makeZero() {
			return null;
		}

		@Override
		public BDD makeOne() {
			return null;
		}

		@Override
		public void gc() {
		}

		void insertHandles(int... handles) {
			this.handles = handles;
		}
	}

	public UniqueTableTest() {
		super(INITIAL_NODE_NUM, 1000, factoryStub);
	}

	@Before
	public void resetHandles() {
		factoryStub.insertHandles();
	}
	
	private static int indexForNode(int node) {
		return node;
	}

	private void insertSomeVarNodes(int n) {
		get(100, -1, -1);
		get(101, -1, -1);
		
		for (int i = 0; i < n; i++)
			get(i, ZERO, ONE);
	}
	
	/*
	 * GC
	 */

	@Test
	public void testGC1_CompactTable() {
		insertSomeVarNodes(3);
		get(3, N2, N3); // node 5: L2 H3

		factoryStub.insertHandles(N5);
		// 4 is dead now
		
		assertEquals(6, nodesCount());

		// compact table
		gc();
		assertEquals(5, nodesCount());

		assertEquals(3, var(N4));
	}

	@Test
	public void testGC2_UpdateChildren() {
		insertSomeVarNodes(3);

		get(3, N2, N4); // 5: L2 H4

		factoryStub.insertHandles(N5);
		
		assertEquals(6, nodesCount());

		// 3 is dead
		// compact table
		gc();
		assertEquals(5, nodesCount());

		assertEquals(2, var(N3)); // 4 -> 3
		assertEquals(3, var(N4)); // 5 -> 4
		assertEquals(N2, low(N4));
		// high updated
		assertEquals(N3, high(N4));
	}

}