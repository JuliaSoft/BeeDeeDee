package com.juliasoft.beedeedee.ger;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.juliasoft.beedeedee.bdd.BDD;
import com.juliasoft.beedeedee.factories.Factory;

public class BDDGerTest {

	private Factory factory;
	private BDD bddX1biX2;
	private BDD bddX3;

	@Before
	public void setUp() {
		factory = Factory.mkResizingAndGarbageCollected(10, 10);
		// x1 <-> x2
		bddX1biX2 = factory.makeVar(1);
		bddX1biX2.biimpWith(factory.makeVar(2));
		// x3
		bddX3 = factory.makeVar(3);
	}

	@Test
	public void testBasicMethods() {
		BDD bdd = factory.makeVar(2);
		BDD bddGer = new BDDGer(bdd);
		assertEquals(2, bddGer.var());
		assertTrue(bddGer.low().isZero());
		assertTrue(bddGer.high().isOne());
	}

	@Test
	public void testZero() {
		BDD bdd = factory.makeZero();
		BDD bddGer = new BDDGer(bdd);
		assertTrue(bddGer.isZero());
		// TODO can l be non-empty?
	}

	@Test
	public void testOne1() {
		BDD bdd = factory.makeOne();
		BDD bddGer = new BDDGer(bdd);
		assertTrue(bddGer.isOne());
	}

	@Test
	public void testOne2() {
		BDD bddGer = new BDDGer(bddX1biX2);
		// the normalized BDD is ONE, but not the original one
		assertFalse(bddGer.isOne());
	}

	@Test
	public void testOr() {
		BDD bddGer1 = new BDDGer(bddX1biX2);
		BDD bddGer2 = new BDDGer(bddX3);

		BDD or = bddGer1.or(bddGer2);
		BDD originalOr = bddX1biX2.or(bddX3);

		assertTrue(or.isEquivalentTo(originalOr));
	}

	@Test
	public void testAnd() {
		BDD bddGer1 = new BDDGer(bddX1biX2);
		BDD bddGer2 = new BDDGer(bddX3);

		BDD and = bddGer1.and(bddGer2);
		BDD originalAnd = bddX1biX2.and(bddX3);

		assertTrue(and.isEquivalentTo(originalAnd));
	}

	@Test
	public void testNot() {
		// (x1 <-> x2) | x3
		BDD bdd = bddX1biX2.or(bddX3);
		BDD bddGer = new BDDGer(bdd);

		BDD not = bddGer.not();
		BDD originalNot = bdd.not();

		assertTrue(not.isEquivalentTo(originalNot));
	}
}
