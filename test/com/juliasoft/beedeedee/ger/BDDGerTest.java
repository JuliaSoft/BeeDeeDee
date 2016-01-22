package com.juliasoft.beedeedee.ger;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.juliasoft.beedeedee.bdd.BDD;
import com.juliasoft.beedeedee.factories.Factory;

public class BDDGerTest {

	private Factory factory;

	@Before
	public void setUp() {
		factory = Factory.mkResizingAndGarbageCollected(10, 10);
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
		// x1 <-> x2
		BDD bdd = factory.makeVar(1);
		bdd.biimpWith(factory.makeVar(2));
		BDD bddGer = new BDDGer(bdd);
		// the normalized BDD is ONE, but not the original one
		assertFalse(bddGer.isOne());
	}

	@Test
	public void testOr() {
		// x1 <-> x2
		BDD bdd1 = factory.makeVar(1);
		bdd1.biimpWith(factory.makeVar(2));
		BDD bddGer1 = new BDDGer(bdd1);
		// x3
		BDD bdd2 = factory.makeVar(3);
		BDD bddGer2 = new BDDGer(bdd2);

		BDD or = bddGer1.or(bddGer2);
		BDD originalOr = bdd1.or(bdd2);

		assertTrue(or.isEquivalentTo(originalOr));
	}

	@Test
	public void testAnd() {
		// x1 <-> x2
		BDD bdd1 = factory.makeVar(1);
		bdd1.biimpWith(factory.makeVar(2));
		BDD bddGer1 = new BDDGer(bdd1);
		// x3
		BDD bdd2 = factory.makeVar(3);
		BDD bddGer2 = new BDDGer(bdd2);

		BDD and = bddGer1.and(bddGer2);
		BDD originalAnd = bdd1.and(bdd2);

		assertTrue(and.isEquivalentTo(originalAnd));
	}

	@Test
	public void testNot() {
		// (x1 <-> x2) | x3
		BDD bdd = factory.makeVar(1);
		bdd.biimpWith(factory.makeVar(2));
		bdd.orWith(factory.makeVar(3));
		BDD bddGer = new BDDGer(bdd);

		BDD not = bddGer.not();
		BDD originalNot = bdd.not();

		assertTrue(not.isEquivalentTo(originalNot));
	}
}
