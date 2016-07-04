package com.juliasoft.beedeedee.er;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;

import com.juliasoft.beedeedee.bdd.Assignment;
import com.juliasoft.beedeedee.er.EquivalenceRelation;
import com.juliasoft.beedeedee.er.Pair;

public class ETest {

	@Test
	public void testIntersect1() {
		// {{1, 2}}
		EquivalenceRelation e1 = new EquivalenceRelation();
		e1.addClass(1, 2);

		// {{1, 3}}
		EquivalenceRelation e2 = new EquivalenceRelation();
		e2.addClass(1, 3);

		EquivalenceRelation intersection = e1.intersect(e2);
		// no common pair
		assertTrue(intersection.isEmpty());
	}

	@Test
	public void testIntersect2() {
		// {{1, 2}}
		EquivalenceRelation e1 = new EquivalenceRelation();
		e1.addClass(1, 2);

		// {{1, 2, 3}}
		EquivalenceRelation e2 = new EquivalenceRelation();
		e2.addClass(1, 2, 3);

		EquivalenceRelation intersection = e1.intersect(e2);

		// one common pair, (1, 2)
		assertEquals(1, intersection.size());
		BitSet next = intersection.iterator().next();
		assertEquals(1, (int) next.nextSetBit(0));
		assertEquals(2, (int) next.nextSetBit(2));
	}

	// FIXME fails with Java 8
	@Test
	public void testIntersect3() {
		// {{1, 2}, {3, 4, 6}}
		EquivalenceRelation e1 = new EquivalenceRelation();
		e1.addClass(1, 2);
		e1.addClass(3, 4, 6);

		// {{1, 2, 5}, {4, 6}}
		EquivalenceRelation e2 = new EquivalenceRelation();
		e2.addClass(1, 2, 5);
		e2.addClass(4, 6);

		EquivalenceRelation intersection = e1.intersect(e2);

		// two common pairs, (1, 2), (4, 6)
		assertEquals(2, intersection.size());
		Iterator<BitSet> it = intersection.iterator();
		BitSet class1 = it.next();
		BitSet class2 = it.next();
		assertEquals(1, (int) class1.nextSetBit(0));
		assertEquals(2, (int) class1.nextSetBit(2));
		assertEquals(4, (int) class2.nextSetBit(0));
		assertEquals(6, (int) class2.nextSetBit(5));
	}

	@Test
	public void testPairGeneration1() {
		EquivalenceRelation e = new EquivalenceRelation();
		e.addClass(1, 2);

		List<Pair> pairs = e.pairs();
		assertEquals(1, pairs.size());
		Pair pair = pairs.get(0);
		assertEquals(1, pair.first);
		assertEquals(2, pair.second);
	}

	@Test
	public void testPairGeneration2() {
		EquivalenceRelation e = new EquivalenceRelation();
		e.addClass(1, 2);
		e.addClass(3, 5, 8);

		List<Pair> pairs = e.pairs();
		assertEquals(4, pairs.size());
		assertTrue(pairs.contains(new Pair(1, 2)));
		assertTrue(pairs.contains(new Pair(3, 5)));
		assertTrue(pairs.contains(new Pair(3, 8)));
		assertTrue(pairs.contains(new Pair(5, 8)));
	}

	@Test
	public void testSubtract1() {
		// {(1, 2)}
		EquivalenceRelation e1 = new EquivalenceRelation();
		e1.addClass(1, 2);

		// {(1, 2), (2, 3), (1, 3)}
		EquivalenceRelation e2 = new EquivalenceRelation();
		e2.addClass(1, 2, 3);

		// expected result is the empty list
		List<Pair> pairs = e1.subtract(e2);
		assertTrue(pairs.isEmpty());
	}

	@Test
	public void testSubtract2() {
		// {(1, 2), (2, 3), (1, 3)}
		EquivalenceRelation e1 = new EquivalenceRelation();
		e1.addClass(1, 2, 3);

		// {(1, 2)}
		EquivalenceRelation e2 = new EquivalenceRelation();
		e2.addClass(1, 2);

		// expected result is {(1, 3)} (removes pairs containing non leaders)
		List<Pair> pairs = e1.subtract(e2);
		assertEquals(1, pairs.size());
		assertTrue(pairs.contains(new Pair(1, 3)));
	}

	@Test
	public void testAddPair1() {
		// {(1, 2)}, {(3, 4)}
		EquivalenceRelation e = new EquivalenceRelation();
		e.addClass(1, 2);
		e.addClass(3, 4);

		e.addPair(new Pair(5, 6));

		// new class {(5, 6)} was created
		assertEquals(3, e.size());
	}

	@Test
	public void testAddPair2() {
		// {(1, 2)}, {(3, 4)}
		EquivalenceRelation e = new EquivalenceRelation();
		e.addClass(1, 2);
		e.addClass(3, 4);

		e.addPair(new Pair(4, 5));

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
		// {(1, 2)}, {(3, 4)}
		EquivalenceRelation e = new EquivalenceRelation();
		e.addClass(1, 2);
		e.addClass(3, 4);

		e.addPair(new Pair(2, 3));

		// eq. classes were joined
		assertEquals(1, e.size());
		BitSet expected = new BitSet();
		expected.set(1, 5);
		assertEquals(expected, e.iterator().next());
	}

	// FIXME red if equivalenceClasses is a HashSet, green if it is an ArrayList
	// (is HashSet.remove() working?)
	@Test
	public void testAddPair4() {
		// bug exhibited in Julia analyzer
		// [{0, 2, 9, 11}]
		EquivalenceRelation e = new EquivalenceRelation();
		e.addClass(0, 2, 9, 11);

		// [(1, 2), (7, 10), (7, 11), (10, 11)]
		e.addPairs(Arrays.asList(new Pair(1, 2), new Pair(7, 10), new Pair(7, 11), new Pair(10, 11)));

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
		// {(1, 2)}, {(3, 4)}
		EquivalenceRelation e = new EquivalenceRelation();
		e.addClass(1, 2);
		e.addClass(3, 4);

		assertEquals(4, e.maxVar());
	}

	@Test
	public void testContainsVar() {
		// {(1, 2)}, {(3, 4)}
		EquivalenceRelation e = new EquivalenceRelation();
		e.addClass(1, 2);
		e.addClass(3, 4);

		assertTrue(e.containsVar(3));
		assertFalse(e.containsVar(7));
	}

	@Test
	public void testUpdateAssignment() {
		Assignment a = mock(Assignment.class);
		when(a.holds(2)).thenReturn(true);

		EquivalenceRelation e = new EquivalenceRelation();
		e.addClass(1, 2);
		e.addClass(3, 4);
		e.updateAssignment(a);

		verify(a).put(1, true);
	}

	@Test
	public void testRemoveVar1() {
		// {(1, 2)}
		EquivalenceRelation e = new EquivalenceRelation();
		e.addClass(1, 2);
		e.removeVar(1);
		assertTrue(e.isEmpty()); // a class must contain at least 2 elements
	}

	@Test
	public void testRemoveVar2() {
		// {(1, 2, 3)}
		EquivalenceRelation e = new EquivalenceRelation();
		e.addClass(1, 2, 3);
		e.removeVar(1);
		List<Pair> pairs = e.pairs();
		assertEquals(1, pairs.size());
		assertEquals(pairs.get(0), new Pair(2, 3));
	}

	@Test
	public void testRemoveVar3() {
		// {(1, 2, 3)}
		EquivalenceRelation e = new EquivalenceRelation();
		e.addClass(1, 2, 3);
		EquivalenceRelation copy = e.copy();
		e.removeVar(4);
		assertEquals(copy, e);
	}

	@Test
	public void testNextLeader1() {
		// {(1, 2, 3)}
		EquivalenceRelation e = new EquivalenceRelation();
		e.addClass(1, 2, 3);
		BitSet excludedVars = new BitSet();
		excludedVars.set(1, 4);
		assertEquals(-1, e.nextLeader(1, excludedVars));
	}

	@Test
	public void testNextLeader2() {
		// {(1, 2, 3)}
		EquivalenceRelation e = new EquivalenceRelation();
		e.addClass(1, 2, 3);
		BitSet excludedVars = new BitSet();
		excludedVars.set(1, 3);
		assertEquals(3, e.nextLeader(1, excludedVars));
	}

	@Test
	public void testNextLeader3() {
		// {(1, 2),(3,4,5)}
		EquivalenceRelation e = new EquivalenceRelation();
		e.addClass(1, 2);
		e.addClass(3, 4, 5);
		BitSet excludedVars = new BitSet();
		excludedVars.set(1);
		excludedVars.set(2);
		excludedVars.set(4);
		assertEquals(-1, e.nextLeader(1, excludedVars));
		assertEquals(5, e.nextLeader(3, excludedVars));
		assertEquals(3, e.nextLeader(4, excludedVars));
	}
}
