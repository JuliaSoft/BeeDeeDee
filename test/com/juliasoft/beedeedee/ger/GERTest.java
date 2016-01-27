package com.juliasoft.beedeedee.ger;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import org.junit.Before;
import org.junit.Test;

import com.juliasoft.beedeedee.bdd.BDD;
import com.juliasoft.beedeedee.factories.Factory;
import com.juliasoft.beedeedee.factories.ResizingAndGarbageCollectedFactory;

public class GERTest {

	private ResizingAndGarbageCollectedFactory factory;
	private GER fakeGer;

	@Before
	public void setUp() throws Exception {
		factory = Factory.mkResizingAndGarbageCollected(10, 10, 0);
		fakeGer = new GER(null, null);
	}

	@Test
	public void testSqueezeAll() {
		// bdd for x1 <-> x2
		BDD bdd = factory.makeVar(1);
		bdd.biimpWith(factory.makeVar(2));

		// construct a set of equivalence classes
		E e = new E();
		e.addClass(1, 2);

		GER ger = new GER(bdd, e);
		BDD n = ger.getSqueezedBDD();

		BDD expected = factory.makeOne();
		assertTrue(n.isEquivalentTo(expected));
	}

	@Test
	public void testSqueezeEquiv() {
		// bdd for (x1 <-> x2) & x3
		BDD bdd = factory.makeVar(1);
		bdd.biimpWith(factory.makeVar(2));
		bdd.andWith(factory.makeVar(3));

		E e = new E();
		e.addClass(1, 2);

		GER ger = new GER(bdd, e);
		BDD n = ger.getSqueezedBDD();

		BDD expected = factory.makeVar(3);
		assertTrue(n.isEquivalentTo(expected));
	}

	@Test
	public void testAnd1() {
		// bdd for x1 <-> x2
		BDD bdd1 = factory.makeVar(1);
		bdd1.biimpWith(factory.makeVar(2));
		// bdd for (x1 <-> x3) & x4
		BDD bdd2 = factory.makeVar(1);
		bdd2.biimpWith(factory.makeVar(3));
		bdd2.andWith(factory.makeVar(4));

		GER ger1 = new GER(bdd1);

		GER ger2 = new GER(bdd2);

		GER and = ger1.and(ger2);

		BDD n = and.getN();
		BDD expectedN = factory.makeVar(4);
		assertTrue(n.isEquivalentTo(expectedN));

		E equiv = and.getEquiv();
		// {{1, 2, 3}}
		assertEquals(1, equiv.size());
		SortedSet<Integer> next = equiv.iterator().next();
		assertTrue(next.contains(1));
		assertTrue(next.contains(2));
		assertTrue(next.contains(3));
		assertEquals(3, next.size());
	}

	@Test
	public void testAnd2() {
		// bdd for x1 <-> x4
		BDD bdd1 = factory.makeVar(1);
		bdd1.biimpWith(factory.makeVar(4));
		// bdd for (x1 <-> x2) & (x2 <-> x3)
		BDD bdd2 = factory.makeVar(1);
		bdd2.biimpWith(factory.makeVar(2));
		BDD temp = factory.makeVar(2);
		temp.biimpWith(factory.makeVar(3));
		bdd2.andWith(temp);

		GER ger1 = new GER(bdd1);

		GER ger2 = new GER(bdd2);

		GER and = ger1.and(ger2);

		E equiv = and.getEquiv();
		// {{1, 2, 3, 4}}
		assertEquals(1, equiv.size());
		SortedSet<Integer> next = equiv.iterator().next();
		assertTrue(next.contains(1));
		assertTrue(next.contains(2));
		assertTrue(next.contains(3));
		assertTrue(next.contains(4));
		assertEquals(4, next.size());

		BDD n = and.getN();
		BDD expectedN = factory.makeOne();
		assertTrue(n.isEquivalentTo(expectedN));
	}

	@Test
	public void testAnd3() {
		// bdd for (x1 <-> x2) & x8
		BDD bdd1 = factory.makeVar(1);
		bdd1.biimpWith(factory.makeVar(2));
		bdd1.andWith(factory.makeVar(8));
		// bdd for (x6 <-> x7) & (x2 <-> x3)
		BDD bdd2 = factory.makeVar(6);
		bdd2.biimpWith(factory.makeVar(7));
		BDD temp = factory.makeVar(2);
		temp.biimpWith(factory.makeVar(3));
		bdd2.andWith(temp);

		E e1 = new E();
		e1.addClass(1, 2);
		GER ger1 = new GER(bdd1, e1);

		E e2 = new E();
		e2.addClass(1, 2, 3);
		GER ger2 = new GER(bdd2, e2);

		GER and = ger1.and(ger2);

		E equiv = and.getEquiv();
		// {{1, 2, 3}, {6, 7}}
		assertEquals(2, equiv.size());
		// FIXME order dependent
		Iterator<SortedSet<Integer>> it = equiv.iterator();
		SortedSet<Integer> next = it.next();
		assertTrue(next.contains(1));
		assertTrue(next.contains(2));
		assertTrue(next.contains(3));
		assertEquals(3, next.size());
		next = it.next();
		assertTrue(next.contains(6));
		assertTrue(next.contains(7));
		assertEquals(2, next.size());

		BDD n = and.getN();
		BDD expectedN = factory.makeVar(8);
		assertTrue(n.isEquivalentTo(expectedN));
	}

	@Test
	public void testOr1() {
		// bdd for x1 <-> x2
		BDD bdd1 = factory.makeVar(1);
		bdd1.biimpWith(factory.makeVar(2));
		// bdd for (x1 <-> x3) & x4
		BDD bdd2 = factory.makeVar(1);
		bdd2.biimpWith(factory.makeVar(3));
		bdd2.andWith(factory.makeVar(4));

		E e1 = new E();
		e1.addClass(1, 2);
		GER ger1 = new GER(bdd1, e1);

		E e2 = new E();
		e2.addClass(1, 3);
		GER ger2 = new GER(bdd2, e2);

		GER or = ger1.or(ger2);

		BDD n = or.getN();
		BDD expectedN = bdd1.or(bdd2);
		assertTrue(n.isEquivalentTo(expectedN));

		E equiv = or.getEquiv();
		assertTrue(equiv.isEmpty());
	}

	@Test
	public void testOr2() {
		// bdd for x1 <-> x2
		BDD bdd1 = factory.makeVar(1);
		bdd1.biimpWith(factory.makeVar(2));
		// bdd for (x1 <-> x2) & (x2 <-> x3)
		BDD bdd2 = factory.makeVar(1);
		bdd2.biimpWith(factory.makeVar(2));
		BDD temp = factory.makeVar(2);
		temp.biimpWith(factory.makeVar(3));
		bdd2.andWith(temp);

		E e1 = new E();
		e1.addClass(1, 2);
		GER ger1 = new GER(bdd1, e1);

		E e2 = new E();
		e2.addClass(1, 2, 3);
		GER ger2 = new GER(bdd2, e2);

		GER or = ger1.or(ger2);

		E equiv = or.getEquiv();
		// (1, 2)
		assertEquals(1, equiv.size());
		SortedSet<Integer> next = equiv.iterator().next();
		assertTrue(next.contains(1));
		assertTrue(next.contains(2));
		assertEquals(2, next.size());

		BDD n = or.getN();
		// we expect (1 or (1 & (x2 <-> x3) & (x1 <-> x3))), that is the
		// constant 1
		BDD expectedN = factory.makeOne();
		assertTrue(n.isEquivalentTo(expectedN));
	}

	@Test
	public void testOr3() {
		// bdd for (x1 <-> x2) & x8
		BDD bdd1 = factory.makeVar(1);
		bdd1.biimpWith(factory.makeVar(2));
		bdd1.andWith(factory.makeVar(8));
		// bdd for (x1 <-> x2) & (x2 <-> x3)
		BDD bdd2 = factory.makeVar(1);
		bdd2.biimpWith(factory.makeVar(2));
		BDD temp = factory.makeVar(2);
		temp.biimpWith(factory.makeVar(3));
		bdd2.andWith(temp);

		E e1 = new E();
		e1.addClass(1, 2);
		GER ger1 = new GER(bdd1, e1);

		E e2 = new E();
		e2.addClass(1, 2, 3);
		GER ger2 = new GER(bdd2, e2);

		GER or = ger1.or(ger2);

		E equiv = or.getEquiv();
		// (1, 2)
		assertEquals(1, equiv.size());
		SortedSet<Integer> next = equiv.iterator().next();
		assertTrue(next.contains(1));
		assertTrue(next.contains(2));
		assertEquals(2, next.size());

		BDD n = or.getN();
		// we expect (x8 or (1 & (x2 <-> x3) & (x1 <-> x3)))
		BDD expectedN = factory.makeVar(2);
		expectedN.biimpWith(factory.makeVar(3));
		temp = factory.makeVar(1);
		temp.biimpWith(factory.makeVar(3));
		expectedN.andWith(temp);
		expectedN.orWith(factory.makeVar(8));
		assertTrue(n.isEquivalentTo(expectedN));
	}

	@Test
	public void testXor() {
		// bdd for x1 <-> x2
		BDD bdd1 = factory.makeVar(1);
		bdd1.biimpWith(factory.makeVar(2));
		// bdd for (x1 <-> x3) & x4
		BDD bdd2 = factory.makeVar(1);
		bdd2.biimpWith(factory.makeVar(3));
		bdd2.andWith(factory.makeVar(4));

		E e1 = new E();
		e1.addClass(1, 2);
		GER ger1 = new GER(bdd1, e1);

		E e2 = new E();
		e2.addClass(1, 3);
		GER ger2 = new GER(bdd2, e2);

		GER xor = ger1.xor(ger2);

		BDD n = xor.getN();
		BDD expectedN = bdd1.xor(bdd2);
		assertTrue(n.isEquivalentTo(expectedN));

		E equiv = xor.getEquiv();
		assertTrue(equiv.isEmpty());
	}

	@Test
	public void testNot() {
		// bdd for (x1 <-> x2) & x8
		BDD bdd = factory.makeVar(1);
		bdd.biimpWith(factory.makeVar(2));
		bdd.andWith(factory.makeVar(8));
		GER ger = new GER(bdd).normalize();
		GER notGer = ger.not();

		BDD full = notGer.getFullBDD();
		BDD expected = bdd.not();
		assertTrue(expected.isEquivalentTo(full));
	}

	@Test
	public void testVarsEntailed1() {
		BDD one = factory.makeOne();
		Set<Integer> varsEntailed = fakeGer.varsEntailed(one);
		assertTrue(varsEntailed.isEmpty());
	}

	@Test
	public void testVarsEntailed2() {
		BDD zero = factory.makeZero();
		factory.makeVar(4); // set maxVar to 4 for the current factory
		Set<Integer> varsEntailed = fakeGer.varsEntailed(zero);
		// expect a set containing all possible variables up to maxVar
		Set<Integer> expected = new HashSet<>(Arrays.asList(0, 1, 2, 3, 4));
		assertEquals(expected, varsEntailed);
	}

	@Test
	public void testVarsEntailed3() {
		BDD f = factory.makeVar(3);
		f.andWith(factory.makeVar(4));

		Set<Integer> varsEntailed = fakeGer.varsEntailed(f);
		Set<Integer> expected = new HashSet<>();
		expected.add(3);
		expected.add(4);
		assertEquals(expected, varsEntailed);
	}

	@Test
	public void testVarsDisentailed() {
		BDD f = factory.makeVar(1);
		f.notWith();
		f.andWith(factory.makeVar(2));

		// disentailed variables are those whose negation is entailed
		Set<Integer> varsDisentailed = fakeGer.varsDisentailed(f);
		Set<Integer> expected = new HashSet<>();
		expected.add(1);
		assertEquals(expected, varsDisentailed);
	}

	@Test
	public void testMaxVar() {
		BDD one = factory.makeOne();
		assertEquals(-1, fakeGer.maxVar(one));
		BDD zero = factory.makeZero();
		assertEquals(-1, fakeGer.maxVar(zero));
		// bdd for (x1 <-> x2) & x8
		BDD bdd = factory.makeVar(1);
		bdd.biimpWith(factory.makeVar(2));
		bdd.andWith(factory.makeVar(8));
		assertEquals(8, fakeGer.maxVar(bdd));
	}

	@Test
	public void testGeneratePairs() {
		List<Pair> pairs = fakeGer.generatePairs(3);
		assertEquals(6, pairs.size());
		assertTrue(pairs.contains(new Pair(0, 1)));
		assertTrue(pairs.contains(new Pair(0, 2)));
		assertTrue(pairs.contains(new Pair(0, 3)));
		assertTrue(pairs.contains(new Pair(1, 2)));
		assertTrue(pairs.contains(new Pair(1, 3)));
		assertTrue(pairs.contains(new Pair(2, 3)));
	}

	@Test
	public void testEquivVars1() {
		BDD one = factory.makeOne();
		List<Pair> equivVars = fakeGer.equivVars(one);
		assertTrue(equivVars.isEmpty());
	}

	@Test
	public void testEquivVars2() {
		BDD zero = factory.makeZero();
		List<Pair> equivVars = fakeGer.equivVars(zero);
		assertTrue(equivVars.isEmpty());
	}

	@Test
	public void testEquivVars3() {
		// bdd for (x1 <-> x2) & x8
		BDD bdd = factory.makeVar(1);
		bdd.biimpWith(factory.makeVar(2));
		bdd.andWith(factory.makeVar(8));
		List<Pair> equivVars = fakeGer.equivVars(bdd);
		assertEquals(1, equivVars.size());
		assertTrue(equivVars.contains(new Pair(1, 2)));
	}

	@Test
	public void testEquivVars4() {
		// bdd for (x1 <-> x2) & (x2 <-> x3)
		BDD bdd = factory.makeVar(1);
		bdd.biimpWith(factory.makeVar(2));
		BDD temp = factory.makeVar(2);
		temp.biimpWith(factory.makeVar(3));
		bdd.andWith(temp);
		List<Pair> equivVars = fakeGer.equivVars(bdd);
		assertEquals(3, equivVars.size());
		assertTrue(equivVars.contains(new Pair(1, 2)));
		assertTrue(equivVars.contains(new Pair(1, 3)));
		assertTrue(equivVars.contains(new Pair(2, 3)));
	}

	@Test
	public void testNormalize1() {
		// bdd for x1 <-> x2
		BDD bdd = factory.makeVar(1);
		bdd.biimpWith(factory.makeVar(2));

		GER ger = new GER(bdd);
		GER normalized = ger.normalize();

		List<Pair> pairs = normalized.getEquiv().pairs();
		assertEquals(1, pairs.size());
		assertEquals(new Pair(1, 2), pairs.get(0));

		BDD n = normalized.getN();
		BDD expected = factory.makeOne();
		assertTrue(n.isEquivalentTo(expected));
	}

	@Test
	public void testNormalize2() {
		// bdd for (x1 <-> x2) & x3
		BDD bdd = factory.makeVar(1);
		bdd.biimpWith(factory.makeVar(2));
		bdd.andWith(factory.makeVar(3));

		GER ger = new GER(bdd);
		GER normalized = ger.normalize();

		List<Pair> pairs = normalized.getEquiv().pairs();
		assertEquals(1, pairs.size());
		assertEquals(new Pair(1, 2), pairs.get(0));

		BDD n = normalized.getN();
		BDD expected = factory.makeVar(3);
		assertTrue(n.isEquivalentTo(expected));
	}

	@Test
	public void testFullBDD() {
		// bdd for (x1 <-> x2) & x8
		BDD bdd = factory.makeVar(1);
		bdd.biimpWith(factory.makeVar(2));
		bdd.andWith(factory.makeVar(8));
		GER ger = new GER(bdd).normalize();
		BDD full = ger.getFullBDD();
		assertTrue(full.isEquivalentTo(bdd));
	}
}
