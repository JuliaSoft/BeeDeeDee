package com.juliasoft.beedeedee.er;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;

import com.juliasoft.beedeedee.bdd.Assignment;
import com.juliasoft.beedeedee.factories.EquivalenceRelation;
import com.juliasoft.beedeedee.factories.Pair;

public class EquivalenceRelationTest {

	@Test
	public void testIntersect1() {
		EquivalenceRelation e1 = new EquivalenceRelation(new int[][] {{1, 2}});
		EquivalenceRelation e2 = new EquivalenceRelation(new int[][] {{1, 3}});

		EquivalenceRelation intersection = e1.intersection(e2);
		// no common pair
		assertTrue(intersection.isEmpty());
	}

	@Test
	public void testIntersect2() {
		EquivalenceRelation e1 = new EquivalenceRelation(new int[][] {{1, 2}});
		EquivalenceRelation e2 = new EquivalenceRelation(new int[][] {{1, 2, 3}});

		EquivalenceRelation intersection = e1.intersection(e2);

		// one common pair, (1, 2)
		assertEquals(1, intersection.size());
		BitSet next = intersection.iterator().next();
		assertEquals(1, (int) next.nextSetBit(0));
		assertEquals(2, (int) next.nextSetBit(2));
	}

	@Test
	public void testIntersect3() {
		EquivalenceRelation e1 = new EquivalenceRelation(new int[][] {{1, 2}, {3, 4, 6}});
		EquivalenceRelation e2 = new EquivalenceRelation(new int[][] {{1, 2, 5}, {4, 6}});

		EquivalenceRelation intersection = e1.intersection(e2);

		// two common pairs, (1, 2), (4, 6)
		assertEquals(2, intersection.size());
		Iterator<BitSet> it = intersection.iterator();
		BitSet class1 = it.next();
		BitSet class2 = it.next();
		BitSet expected1 = new BitSet();
		BitSet expected2 = new BitSet();
		expected1.set(1, 3);
		expected2.set(4);
		expected2.set(6);
		assertTrue((expected1.equals(class1) && expected2.equals(class2))
				|| (expected1.equals(class2) && expected2.equals(class1)));
	}

	@Test
	public void testPairGeneration1() {
		EquivalenceRelation e = new EquivalenceRelation(new int[][] {{1, 2}});

		List<Pair> pairs = e.pairs();
		assertEquals(1, pairs.size());
		Pair pair = pairs.get(0);
		assertEquals(1, pair.first);
		assertEquals(2, pair.second);
	}

	@Test
	public void testPairGeneration2() {
		EquivalenceRelation e = new EquivalenceRelation(new int[][] {{1, 2}, {3, 5, 8}});

		List<Pair> pairs = e.pairs();
		assertEquals(4, pairs.size());
		assertTrue(pairs.contains(new Pair(1, 2)));
		assertTrue(pairs.contains(new Pair(3, 5)));
		assertTrue(pairs.contains(new Pair(3, 8)));
		assertTrue(pairs.contains(new Pair(5, 8)));
	}

	@Test
	public void testSubtract1() {
		EquivalenceRelation e1 = new EquivalenceRelation(new int[][] {{1, 2}});
		EquivalenceRelation e2 = new EquivalenceRelation(new int[][] {{1, 2, 3}});

		// expected result is the empty list
		List<Pair> pairs = e1.pairsInDifference(e2);
		assertTrue(pairs.isEmpty());
	}

	@Test
	public void testSubtract2() {
		EquivalenceRelation e1 = new EquivalenceRelation(new int[][] {{1, 2, 3}});
		EquivalenceRelation e2 = new EquivalenceRelation(new int[][] {{1, 2}});

		// expected result is {(1, 3)} (removes pairs containing non leaders)
		List<Pair> pairs = e1.pairsInDifference(e2);
		assertEquals(1, pairs.size());
		assertTrue(pairs.contains(new Pair(1, 3)));
	}

	@Test
	public void testAddPair1() {
		EquivalenceRelation e = new EquivalenceRelation(new int[][] {{1, 2}, {3, 4}});

		e = e.addPairs(Arrays.asList(new Pair(5, 6)));

		// new class {(5, 6)} was created
		assertEquals(3, e.size());
	}

	@Test
	public void testAddPair2() {
		EquivalenceRelation e = new EquivalenceRelation(new int[][] {{1, 2}, {3, 4}});

		e = e.addPairs(Arrays.asList(new Pair(4, 5)));

		// 5 was added to 4's class
		assertEquals(2, e.size());
		BitSet expected1 = new BitSet();
		expected1.set(1);
		expected1.set(2);
		BitSet expected2 = new BitSet();
		expected2.set(3);
		expected2.set(4);
		expected2.set(5);
		for (BitSet eqClass : e) {
			assertTrue(eqClass.equals(expected1) || eqClass.equals(expected2));
		}
	}

	@Test
	public void testAddPair3() {
		EquivalenceRelation e = new EquivalenceRelation(new int[][] {{1, 2}, {3, 4}});

		e = e.addPairs(Arrays.asList(new Pair(2, 3)));

		// eq. classes were joined
		assertEquals(1, e.size());
		BitSet expected = new BitSet();
		expected.set(1, 5);
		assertEquals(expected, e.iterator().next());
	}

	@Test
	public void testAddPair4() {
		// bug exhibited in Julia analyzer
		EquivalenceRelation e = new EquivalenceRelation(new int[][] {{0, 2, 9, 11}});

		// [(1, 2), (7, 10), (7, 11), (10, 11)]
		e = e.addPairs(Arrays.asList(new Pair(1, 2), new Pair(7, 10), new Pair(7, 11), new Pair(10, 11)));

		// KO [{0, 1, 2, 9, 11}, {0, 1, 2, 7, 9, 10, 11}]

		// eq. classes were joined
		assertEquals(1, e.size());
		BitSet expected = new BitSet();
		// OK [{0, 1, 2, 7, 9, 10, 11}]
		expected.set(0, 3);
		expected.set(7);
		expected.set(9, 12);
		assertEquals(expected, e.iterator().next());
	}

	@Test
	public void testMaxVar() {
		EquivalenceRelation e = new EquivalenceRelation(new int[][] {{1, 2}, {3, 4}});

		assertEquals(4, e.maxVar());
	}

	@Test
	public void testContainsVar() {
		EquivalenceRelation e = new EquivalenceRelation(new int[][] {{1, 2}, {3, 4}});

		assertTrue(e.containsVar(3));
		assertFalse(e.containsVar(7));
	}

	@Test
	public void testUpdateAssignment() {
		Assignment a = mock(Assignment.class);
		when(a.holds(2)).thenReturn(true);

		EquivalenceRelation e = new EquivalenceRelation(new int[][] {{1, 2}, {3, 4}});
		e.updateAssignment(a);

		verify(a).put(1, true);
	}

	@Test
	public void testRemoveVar1() {
		EquivalenceRelation e = new EquivalenceRelation(new int[][] {{1, 2}});
		e = e.removeVar(1);
		assertTrue(e.isEmpty()); // a class must contain at least 2 elements
	}

	@Test
	public void testRemoveVar2() {
		EquivalenceRelation e = new EquivalenceRelation(new int[][] {{1, 2, 3}});
		e = e.removeVar(1);
		List<Pair> pairs = e.pairs();
		assertEquals(1, pairs.size());
		assertEquals(pairs.get(0), new Pair(2, 3));
	}

	@Test
	public void testRemoveVar3() {
		EquivalenceRelation e = new EquivalenceRelation(new int[][] {{1, 2, 3}});
		EquivalenceRelation copy = e;
		e = e.removeVar(4);
		assertEquals(copy, e);
	}

	@Test
	public void testNextLeader1() {
		EquivalenceRelation e = new EquivalenceRelation(new int[][] {{1, 2, 3}});
		BitSet excludedVars = new BitSet();
		excludedVars.set(1, 4);
		assertEquals(-1, e.nextLeader(1, excludedVars));
	}

	@Test
	public void testNextLeader2() {
		EquivalenceRelation e = new EquivalenceRelation(new int[][] {{1, 2, 3}});
		BitSet excludedVars = new BitSet();
		excludedVars.set(1, 3);
		assertEquals(3, e.nextLeader(1, excludedVars));
	}

	@Test
	public void testNextLeader3() {
		EquivalenceRelation e = new EquivalenceRelation(new int[][] {{1, 2}, {3, 4, 5}});
		BitSet excludedVars = new BitSet();
		excludedVars.set(1);
		excludedVars.set(2);
		excludedVars.set(4);
		assertEquals(-1, e.nextLeader(1, excludedVars));
		assertEquals(5, e.nextLeader(3, excludedVars));
		assertEquals(3, e.nextLeader(4, excludedVars));
	}
}