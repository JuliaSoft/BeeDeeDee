package com.juliasoft.beedeedee.ger;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.Test;

public class ETest {

	@Test
	public void testIntersect1() {
		// {{1, 2}}
		E e1 = new E();
		e1.addClass(1, 2);

		// {{1, 3}}
		E e2 = new E();
		e2.addClass(1, 3);

		E intersection = e1.intersect(e2);
		// no common pair
		assertTrue(intersection.isEmpty());
	}

	@Test
	public void testIntersect2() {
		// {{1, 2}}
		E e1 = new E();
		e1.addClass(1, 2);

		// {{1, 2, 3}}
		E e2 = new E();
		e2.addClass(1, 2, 3);

		E intersection = e1.intersect(e2);

		// one common pair, (1, 2)
		assertEquals(1, intersection.size());
		SortedSet<Integer> next = intersection.iterator().next();
		assertEquals(1, (int) next.first());
		assertEquals(2, (int) next.last());
	}

	@Test
	public void testIntersect3() {
		// {{1, 2}, {3, 4, 6}}
		E e1 = new E();
		e1.addClass(1, 2);
		e1.addClass(3, 4, 6);

		// {{1, 2, 5}, {4, 6}}
		E e2 = new E();
		e2.addClass(1, 2, 5);
		e2.addClass(4, 6);

		E intersection = e1.intersect(e2);

		// two common pairs, (1, 2), (4, 6)
		assertEquals(2, intersection.size());
		Iterator<SortedSet<Integer>> it = intersection.iterator();
		SortedSet<Integer> class1 = it.next();
		SortedSet<Integer> class2 = it.next();
		assertEquals(1, (int) class1.first());
		assertEquals(2, (int) class1.last());
		assertEquals(4, (int) class2.first());
		assertEquals(6, (int) class2.last());
	}

	@Test
	public void testPairGeneration1() {
		E e = new E();
		e.addClass(1, 2);

		List<Pair> pairs = e.pairs();
		assertEquals(1, pairs.size());
		Pair pair = pairs.get(0);
		assertEquals(1, pair.first);
		assertEquals(2, pair.second);
	}

	@Test
	public void testPairGeneration2() {
		E e = new E();
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
		E e1 = new E();
		e1.addClass(1, 2);

		// {(1, 2), (2, 3), (1, 3)}
		E e2 = new E();
		e2.addClass(1, 2, 3);

		// expected result is the empty list
		List<Pair> pairs = e1.subtract(e2);
		assertTrue(pairs.isEmpty());
	}

	@Test
	public void testSubtract2() {
		// {(1, 2), (2, 3), (1, 3)}
		E e1 = new E();
		e1.addClass(1, 2, 3);

		// {(1, 2)}
		E e2 = new E();
		e2.addClass(1, 2);

		// expected result is {(2, 3), (1, 3)}
		List<Pair> pairs = e1.subtract(e2);
		assertEquals(2, pairs.size());
		assertTrue(pairs.contains(new Pair(2, 3)));
		assertTrue(pairs.contains(new Pair(1, 3)));
	}

	@Test
	public void testAddPair1() {
		// {(1, 2)}, {(3, 4)}
		E e = new E();
		e.addClass(1, 2);
		e.addClass(3, 4);

		e.addPair(new Pair(5, 6));

		// new class {(5, 6)} was created
		assertEquals(3, e.size());
	}

	@Test
	public void testAddPair2() {
		// {(1, 2)}, {(3, 4)}
		E e = new E();
		e.addClass(1, 2);
		e.addClass(3, 4);

		e.addPair(new Pair(4, 5));

		// 5 was added to 4's class
		assertEquals(2, e.size());
		for (SortedSet<Integer> eqClass : e) {
			assertTrue(eqClass.equals(new TreeSet<>(Arrays.asList(1, 2)))
					|| eqClass.equals(new TreeSet<>(Arrays.asList(3, 4, 5))));
		}
	}

	@Test
	public void testAddPair3() {
		// {(1, 2)}, {(3, 4)}
		E e = new E();
		e.addClass(1, 2);
		e.addClass(3, 4);

		e.addPair(new Pair(2, 3));

		// eq. classes were joined
		assertEquals(1, e.size());
		assertEquals(new TreeSet<>(Arrays.asList(1, 2, 3, 4)), e.iterator().next());
	}
}
