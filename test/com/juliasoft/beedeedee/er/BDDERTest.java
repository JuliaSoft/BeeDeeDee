package com.juliasoft.beedeedee.er;

import static org.junit.Assert.*;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.juliasoft.beedeedee.bdd.Assignment;
import com.juliasoft.beedeedee.bdd.BDD;
import com.juliasoft.beedeedee.bdd.ReplacementWithExistingVarException;
import com.juliasoft.beedeedee.factories.EquivalenceRelation;
import com.juliasoft.beedeedee.factories.Factory;
import com.juliasoft.beedeedee.factories.Pair;
import com.juliasoft.beedeedee.factories.ERFactory.BDDER;

public class BDDERTest {

	private Factory factory;
	private BDD bddX1biX2;
	private BDD bddX3;

	@Before
	public void setUp() {
		factory = Factory.mk(10, 10, 0);
		// x1 <-> x2
		bddX1biX2 = factory.makeVar(1);
		bddX1biX2.biimpWith(factory.makeVar(2));
		// x3
		bddX3 = factory.makeVar(3);
		// here factory.bddCount() is 2
	}

	@Test
	public void testBasicMethods() {
		BDD bdd = factory.makeVar(2);
		BDD bddGer = factory.new BDDER(bdd);
		assertEquals(2, bddGer.var());
		assertTrue(bddGer.low().isZero());
		assertTrue(bddGer.high().isOne());

//		assertEquals(5, factory.bddCount());
	}

	@Test
	public void testZero() {
		BDD bdd = factory.makeZero();
		BDD bddGer = new BDDER(bdd);
		assertTrue(bddGer.isZero());
		// TODO can l be non-empty?

//		assertEquals(3, factory.bddCount());
	}

	@Test
	public void testOne1() {
		BDD bdd = factory.makeOne();
		BDD bddGer = new BDDER(bdd);
		assertTrue(bddGer.isOne());

//		assertEquals(3, factory.bddCount());
	}

	@Test
	public void testOne2() {
		BDD bddGer = new BDDER(bddX1biX2);
		// the normalized BDD is ONE, but not the original one
		assertFalse(bddGer.isOne());

//		assertEquals(2, factory.bddCount());
	}

	@Test
	public void testOr1() {
		BDD bddGer1 = new BDDER(bddX1biX2.copy());
		BDD bddGer2 = new BDDER(bddX3.copy());

		BDDER or = (BDDER) bddGer1.or(bddGer2);
		BDD originalOr = bddX1biX2.or(bddX3);

		assertTrue(or.isNormalized());
		assertTrue(or.isEquivalentTo(originalOr));

//		assertEquals(5, factory.bddCount());
	}

	@Test
	public void testOr2() {
		// testcase from the Julia analyzer
		BDD bdd1 = factory.makeVar(0);
		bdd1.orWith(factory.makeVar(4));
		bdd1.orWith(factory.makeVar(6));
		bdd1.orWith(factory.makeVar(7));
		bdd1.notWith();

		BDD bdd2 = factory.makeNotVar(0);
		BDD b2 = factory.makeNotVar(4);
		b2.andWith(factory.makeVar(6));
		BDD b1 = factory.makeVar(1);
		b1.andWith(b2);
		BDD temp = factory.makeNotVar(1);
		BDD b3 = factory.makeNotVar(4);
		BDD b5 = factory.makeVar(6);
		b5.andWith(factory.makeNotVar(7));
		b3.andWith(b5);
		temp.andWith(b3);
		b1.orWith(temp);
		bdd2.andWith(b1);

		BDDER bddGer1 = new BDDER(bdd1.copy());
		BDDER bddGer2 = new BDDER(bdd2.copy());

		BDDER or = (BDDER) bddGer1.or(bddGer2);
		BDD originalOr = bdd1.or(bdd2);

		assertTrue(or.isNormalized());
		assertTrue(or.isEquivalentTo(originalOr));
	}

	@Test
	public void testOr3() {
		BDD bdd1 = factory.makeVar(0);
		bdd1.orWith(factory.makeVar(4));
		bdd1.orWith(factory.makeVar(6));
		bdd1.notWith();

		BDD bdd2 = factory.makeNotVar(0);
		BDD b2 = factory.makeNotVar(4);
		b2.andWith(factory.makeVar(6));
		BDD b1 = factory.makeVar(1);
		b1.andWith(b2);
		bdd2.andWith(b1);

		BDDER bddGer1 = new BDDER(bdd1.copy());
		BDDER bddGer2 = new BDDER(bdd2.copy());

		BDDER or = (BDDER) bddGer1.or(bddGer2);
		BDD originalOr = bdd1.or(bdd2);

		assertTrue(or.isNormalized());
		assertTrue(or.isEquivalentTo(originalOr));
	}

	@Test
	public void testAnd1() {
		BDD bddGer1 = new BDDER(bddX1biX2.copy());
		BDD bddGer2 = new BDDER(bddX3.copy());

		BDD and = bddGer1.and(bddGer2);
		BDD originalAnd = bddX1biX2.and(bddX3);

		assertTrue(and.isEquivalentTo(originalAnd));

//		assertEquals(5, factory.bddCount());
	}

	@Test
	public void testAnd2() {
		BDD bdd1 = factory.makeVar(0);
		bdd1.orWith(factory.makeVar(2));
		bdd1.notWith();
		BDD bdd2 = factory.makeVar(2);

		BDD bddGer1 = new BDDER(bdd1.copy());
		BDD bddGer2 = new BDDER(bdd2.copy());

		BDD and = bddGer1.and(bddGer2);
		BDD originalAnd = bdd1.and(bdd2);

		assertTrue(and.isEquivalentTo(originalAnd));
	}

	@Test
	public void testXor() {
		BDD bddGer1 = new BDDER(bddX1biX2.copy());
		BDD bddGer2 = new BDDER(bddX3.copy());

		BDD xor = bddGer1.xor(bddGer2);
		BDD originalXor = bddX1biX2.xor(bddX3);

		assertTrue(xor.isEquivalentTo(originalXor));

//		assertEquals(5, factory.bddCount());
	}

	@Test
	public void testNand() {
		BDD bddGer1 = new BDDER(bddX1biX2.copy());
		BDD bddGer2 = new BDDER(bddX3.copy());

		BDD nand = bddGer1.nand(bddGer2);
		BDD originalNand = bddX1biX2.nand(bddX3);

		assertTrue(nand.isEquivalentTo(originalNand));

//		assertEquals(5, factory.bddCount());
	}

	@Test
	public void testNot() {
		// (x1 <-> x2) | x3
		BDD bdd = bddX1biX2.or(bddX3);
		BDD bddGer = new BDDER(bdd.copy());

		BDD not = bddGer.not();
		BDD originalNot = bdd.not();

		assertTrue(not.isEquivalentTo(originalNot));

//		assertEquals(5, factory.bddCount());
	}

	@Test
	public void testNodeCount1() {
		// (x1 <-> x2) & x3
		BDD bdd = bddX1biX2.and(bddX3);
		BDD bddGer = new BDDER(bdd);

		assertEquals(1, bddGer.nodeCount());

//		assertEquals(3, factory.bddCount());
	}

	@Test
	public void testNodeCount2() {
		// (x1 <-> x2) | x3
		BDD bdd = bddX1biX2.or(bddX3);
		BDD bddGer = new BDDER(bdd.copy());

		// the bdd is already normalized, same node count
		assertEquals(bdd.nodeCount(), bddGer.nodeCount());

//		assertEquals(4, factory.bddCount());
	}

	@Test
	public void testAnySat1() {
		// (x1 <-> x2) & x3
		BDD bdd = bddX1biX2.and(bddX3);
		BDD bddGer = new BDDER(bdd);

		Assignment anySat = bddGer.anySat();
		assertTrue(anySat.holds(bddX3));
		assertEquals(anySat.holds(factory.makeVar(1)), anySat.holds(factory.makeVar(2)));

//		assertEquals(5, factory.bddCount());
	}

	@Test
	public void testAnySat2() {
		// (x1 <-> x2) & (x3 <-> x4)
		BDD biimp = bddX3.biimp(factory.makeVar(4));
		BDD bdd = bddX1biX2.andWith(biimp);
		BDD bddGer = new BDDER(bdd);

		Assignment anySat = bddGer.anySat();
		assertEquals(anySat.holds(factory.makeVar(1)), anySat.holds(factory.makeVar(2)));
		assertEquals(anySat.holds(factory.makeVar(3)), anySat.holds(factory.makeVar(4)));

//		assertEquals(5, factory.bddCount());
	}

	@Test
	public void testAnySat3() {
		// (x1 <-> x2) & (x1 & x3)
		BDD and = bddX3.and(factory.makeVar(1));
		BDD bdd = bddX1biX2.andWith(and);
		// {1, 2, 3}, x1
		BDD bddGer = new BDDER(bdd);

		Assignment anySat = bddGer.anySat();
		assertTrue(anySat.holds(bddX3));
		assertEquals(anySat.holds(factory.makeVar(1)), anySat.holds(factory.makeVar(2)));
		assertEquals(anySat.holds(factory.makeVar(1)), anySat.holds(factory.makeVar(3)));

//		assertEquals(5, factory.bddCount());
	}

	@Test
	public void testExist1() {
		// (x1 <-> x2)
		BDD bdd = factory.makeVar(1);
		bdd.biimpWith(factory.makeVar(2));
		// {1, 2}, 1
		BDD bddGer = new BDDER(bdd.copy());

		BDD exist = bddGer.exist(1);
		BDD originalExist = bdd.exist(1);

		assertTrue(exist.isEquivalentTo(originalExist));
	}

	@Test
	public void testExist2() {
		// (x1 <-> x2) & (x3 <-> x4)
		BDD biimp = bddX3.biimp(factory.makeVar(4));
		BDD bdd = bddX1biX2.andWith(biimp);
		// [{1,2},{3,4}], 1
		BDD bddGer = new BDDER(bdd.copy());

		BDD exist = bddGer.exist(1);
		BDD originalExist = bdd.exist(1);

		assertTrue(exist.isEquivalentTo(originalExist));
	}

	@Test
	public void testExist3() {
		// (x1 <-> x2) & (x2 <-> x3)
		BDD biimp = bddX3.biimp(factory.makeVar(2));
		BDD bdd = bddX1biX2.andWith(biimp);
		// [{1,2,3}], 1
		BDD bddGer = new BDDER(bdd.copy());

		BDD exist = bddGer.exist(1);
		BDD originalExist = bdd.exist(1);

		assertTrue(exist.isEquivalentTo(originalExist));
	}

	@Test
	public void testExist4() {
		// (x1 <-> x2) & (x2 <-> x3) & x1
		BDD biimp = bddX3.biimp(factory.makeVar(2));
		BDD bdd = bddX1biX2.andWith(biimp);
		bdd.andWith(factory.makeVar(1));
		// [{1,2,3}], x1
		BDD bddGer = new BDDER(bdd.copy());

		// [{2,3}], x2
		BDD exist = bddGer.exist(1);
		BDD originalExist = bdd.exist(1);

		assertTrue(exist.isEquivalentTo(originalExist));
	}

	@Test
	public void testExist5() {
		// (x1 <-> x2) & (x2 <-> x3) & (x1 OR x4)
		BDD biimp = bddX3.biimp(factory.makeVar(2));
		BDD bdd = bddX1biX2.andWith(biimp);
		BDD temp = factory.makeVar(1);
		temp.orWith(factory.makeVar(4));
		bdd.andWith(temp);
		// [{1,2,3}], x1 OR x4
		BDD bddGer = new BDDER(bdd.copy());

		// [{2,3}], x2 OR x4
		BDD exist = bddGer.exist(1);
		BDD originalExist = bdd.exist(1);

		assertTrue(exist.isEquivalentTo(originalExist));
	}

	@Test
	public void testExist6() {
		// (x1 <-> x2) & (x1 OR x4)
		BDD temp = factory.makeVar(1);
		temp.orWith(factory.makeVar(4));
		BDD bdd = bddX1biX2.andWith(temp);
		// [{1,2}], x1 OR x4
		BDD bddGer = new BDDER(bdd.copy());

		// x2 OR x4
		BDD exist = bddGer.exist(1);
		BDD originalExist = bdd.exist(1);

		assertTrue(exist.isEquivalentTo(originalExist));
	}

	@Test
	public void testExist7() {
		// (x2 <-> x3) & (x1 OR x4)
		BDD temp = factory.makeVar(1);
		temp.orWith(factory.makeVar(4));
		BDD bdd = bddX3.biimp(factory.makeVar(2));
		bdd.andWith(temp);
		// [{2,3}], x1 OR x4
		BDD bddGer = new BDDER(bdd.copy());

		// [{2,3}]
		BDD exist = bddGer.exist(1);
		BDD originalExist = bdd.exist(1);

		assertTrue(exist.isEquivalentTo(originalExist));
	}

	@Test
	public void testExistMulti1() {
		// (x1 <-> x2) & (x3 <-> x4)
		BDD biimp = bddX3.biimp(factory.makeVar(4));
		BDD bdd = bddX1biX2.andWith(biimp);
		// [{1,2},{3,4}], 1
		BDD bddGer = new BDDER(bdd.copy());

		BDD minterm = factory.makeVar(1).andWith(factory.makeVar(3));
		BDD exist = bddGer.exist(minterm);
		BDD originalExist = bdd.exist(minterm);

		assertTrue(exist.isEquivalentTo(originalExist));
	}

	@Test
	public void testExistMulti2() {
		// (x1 <-> x2) & (x2 <-> x3)
		BDD biimp = bddX3.biimp(factory.makeVar(2));
		BDD bdd = bddX1biX2.andWith(biimp);
		// [{1,2,3}], 1
		BDD bddGer = new BDDER(bdd.copy());

		BDD minterm = factory.makeVar(1).andWith(factory.makeVar(2)).andWith(factory.makeVar(3));
		BDD exist = bddGer.exist(minterm);
		BDD originalExist = bdd.exist(minterm);

		assertTrue(exist.isEquivalentTo(originalExist));
	}

	@Test
	public void testExistMulti3() {
		// (x1 <-> x2) & (x2 <-> x3) & (x1 OR x4)
		BDD biimp = bddX3.biimp(factory.makeVar(2));
		BDD bdd = bddX1biX2.andWith(biimp);
		BDD temp = factory.makeVar(1);
		temp.orWith(factory.makeVar(4));
		bdd.andWith(temp);
		// [{1,2,3}], x1 OR x4
		BDD bddGer = new BDDER(bdd.copy());

		// [], x3 OR x4
		BDD minterm = factory.makeVar(1).andWith(factory.makeVar(2));
		BDD exist = bddGer.exist(minterm);
		BDD originalExist = bdd.exist(minterm);

		assertTrue(exist.isEquivalentTo(originalExist));
	}


	/*
	 * low level tests
	 */

	@Test
	public void testSqueezeAll() {
		// bdd for x1 <-> x2
		BDD bdd = factory.makeVar(1);
		bdd.biimpWith(factory.makeVar(2));

		EquivalenceRelation e = new EquivalenceRelation(new int[][] {{1, 2}});

		BDDER er = new BDDER(bdd, e);
		BDD n = er.getSqueezedBDD();

		BDD expected = factory.makeOne();
		assertTrue(n.isEquivalentTo(expected));

//		assertEquals(3, factory.bddCount()); FIXME uncomment all
	}

	@Test
	public void testSqueezeEquiv() {
		// bdd for (x1 <-> x2) & x3
		BDD bdd = factory.makeVar(1);
		bdd.biimpWith(factory.makeVar(2));
		bdd.andWith(factory.makeVar(3));

		EquivalenceRelation e = new EquivalenceRelation(new int[][] {{1, 2}});

		BDDER er = new BDDER(bdd, e);
		BDD n = er.getSqueezedBDD();

		BDD expected = factory.makeVar(3);
		assertTrue(n.isEquivalentTo(expected));

//		assertEquals(3, factory.bddCount());
	}

	@Test
	public void testAnd1b() {
		// bdd for x1 <-> x2
		BDD bdd1 = factory.makeVar(1);
		bdd1.biimpWith(factory.makeVar(2));
		// bdd for (x1 <-> x3) & x4
		BDD bdd2 = factory.makeVar(1);
		bdd2.biimpWith(factory.makeVar(3));
		bdd2.andWith(factory.makeVar(4));

		BDDER er1 = factory.new BDDER(bdd1);

		BDDER er2 = new BDDER(bdd2);

		BDDER and = er1.and_(er2);

		BDD expectedN = factory.makeVar(4);
		assertTrue(and.getN().isEquivalentTo(expectedN));

		EquivalenceRelation equiv = and.getEquiv();
		// {{1, 2, 3}}
		assertEquals(1, equiv.size());
		BitSet next = equiv.iterator().next();
		BitSet expected = new BitSet();
		expected.set(1, 4);
		assertEquals(expected, next);

//		assertEquals(4, factory.bddCount());
	}

	@Test
	public void testAnd2b() {
		// bdd for x1 <-> x4
		BDD bdd1 = factory.makeVar(1);
		bdd1.biimpWith(factory.makeVar(4));
		// bdd for (x1 <-> x2) & (x2 <-> x3)
		BDD bdd2 = factory.makeVar(1);
		bdd2.biimpWith(factory.makeVar(2));
		BDD temp = factory.makeVar(2);
		temp.biimpWith(factory.makeVar(3));
		bdd2.andWith(temp);

		BDDER er1 = new BDDER(bdd1);

		BDDER er2 = new BDDER(bdd2);

		BDDER and = er1.and_(er2);

		EquivalenceRelation equiv = and.getEquiv();
		// {{1, 2, 3, 4}}
		assertEquals(1, equiv.size());
		BitSet next = equiv.iterator().next();
		BitSet expected = new BitSet();
		expected.set(1, 5);
		assertEquals(expected, next);

		BDD n = and.getN();
		BDD expectedN = factory.makeOne();
		assertTrue(n.isEquivalentTo(expectedN));

//		assertEquals(4, factory.bddCount());
	}

	@Test
	public void testAnd3b() {
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

		EquivalenceRelation e1 = new EquivalenceRelation(new int[][] {{1, 2}});
		BDDER er1 = new BDDER(bdd1, e1);

		EquivalenceRelation e2 = new EquivalenceRelation(new int[][] {{1, 2, 3}});
		BDDER er2 = new BDDER(bdd2, e2);

		BDDER and = er1.and_(er2);

		EquivalenceRelation equiv = and.getEquiv();
		// {{1, 2, 3}, {6, 7}}
		assertEquals(2, equiv.size());
		Iterator<BitSet> it = equiv.iterator();
		BitSet class1 = it.next();
		BitSet class2 = it.next();
		BitSet expected1 = new BitSet();
		BitSet expected2 = new BitSet();
		expected1.set(1, 4);
		expected2.set(6, 8);
		assertTrue((expected1.equals(class1) && expected2.equals(class2))
				|| (expected1.equals(class2) && expected2.equals(class1)));

		BDD n = and.getN();
		BDD expectedN = factory.makeVar(8);
		assertTrue(n.isEquivalentTo(expectedN));

//		assertEquals(4, factory.bddCount());
	}

	@Test
	public void testOr1b() {
		// bdd for x1 <-> x2
		BDD bdd1 = factory.makeVar(1);
		bdd1.biimpWith(factory.makeVar(2));
		// bdd for (x1 <-> x3) & x4
		BDD bdd2 = factory.makeVar(1);
		bdd2.biimpWith(factory.makeVar(3));
		bdd2.andWith(factory.makeVar(4));

		EquivalenceRelation e1 = new EquivalenceRelation(new int[][] {{1, 2}});
		BDDER er1 = new BDDER(bdd1, e1);

		EquivalenceRelation e2 = new EquivalenceRelation(new int[][] {{1, 3}});
		BDDER er2 = new BDDER(bdd2, e2);

		BDDER or = er1.or_(er2);

		BDD n = or.getN();
		BDD expectedN = bdd1.or(bdd2);
		assertTrue(n.isEquivalentTo(expectedN));

		EquivalenceRelation equiv = or.getEquiv();
		assertTrue(equiv.isEmpty());

//		assertEquals(4, factory.bddCount());
	}

	@Test
	public void testOr2b() {
		// bdd for x1 <-> x2
		BDD bdd1 = factory.makeVar(1);
		bdd1.biimpWith(factory.makeVar(2));
		// bdd for (x1 <-> x2) & (x2 <-> x3)
		BDD bdd2 = factory.makeVar(1);
		bdd2.biimpWith(factory.makeVar(2));
		BDD temp = factory.makeVar(2);
		temp.biimpWith(factory.makeVar(3));
		bdd2.andWith(temp);

		EquivalenceRelation e1 = new EquivalenceRelation(new int[][] {{1, 2}});
		BDDER er1 = new BDDER(bdd1, e1);

		EquivalenceRelation e2 = new EquivalenceRelation(new int[][] {{1, 2, 3}});
		BDDER er2 = new BDDER(bdd2, e2);

		BDDER or = er1.or_(er2);

		EquivalenceRelation equiv = or.getEquiv();
		// (1, 2)
		assertEquals(1, equiv.size());
		BitSet next = equiv.iterator().next();
		BitSet expected = new BitSet();
		expected.set(1, 3);
		assertEquals(expected, next);

		BDD n = or.getN();
		// we expect (1 or (1 & (x2 <-> x3) & (x1 <-> x3))), that is the
		// constant 1
		BDD expectedN = factory.makeOne();
		assertTrue(n.isEquivalentTo(expectedN));

//		assertEquals(4, factory.bddCount());
	}

	@Test
	public void testOr3b() {
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

		EquivalenceRelation e1 = new EquivalenceRelation(new int[][] {{1, 2}});
		BDDER er1 = new BDDER(bdd1, e1);

		EquivalenceRelation e2 = new EquivalenceRelation(new int[][] {{1, 2, 3}});
		BDDER er2 = new BDDER(bdd2, e2);

		BDDER or = er1.or_(er2);

		EquivalenceRelation equiv = or.getEquiv();
		// (1, 2)
		assertEquals(1, equiv.size());
		BitSet next = equiv.iterator().next();
		BitSet expected = new BitSet();
		expected.set(1, 3);
		assertEquals(expected, next);

		BDD n = or.getN();
		// we expect (x8 or (1 & (x1 <-> x3)))
		BDD expectedN = factory.makeVar(1);
		expectedN.biimpWith(factory.makeVar(3));
		expectedN.orWith(factory.makeVar(8));
		assertTrue(n.isEquivalentTo(expectedN));

//		assertEquals(4, factory.bddCount());
	}

	@Test
	public void testXorb() {
		// bdd for x1 <-> x2
		BDD bdd1 = factory.makeVar(1);
		bdd1.biimpWith(factory.makeVar(2));
		// bdd for (x1 <-> x3) & x4
		BDD bdd2 = factory.makeVar(1);
		bdd2.biimpWith(factory.makeVar(3));
		bdd2.andWith(factory.makeVar(4));

		EquivalenceRelation e1 = new EquivalenceRelation(new int[][] {{1, 2}});
		BDDER er1 = new BDDER(bdd1, e1);

		EquivalenceRelation e2 = new EquivalenceRelation(new int[][] {{1, 3}});
		BDDER er2 = new BDDER(bdd2, e2);

		BDDER xor = er1.xor_(er2);

		BDD n = xor.getN();
		BDD expectedN = bdd1.xor(bdd2);
		assertTrue(n.isEquivalentTo(expectedN));

		EquivalenceRelation equiv = xor.getEquiv();
		assertTrue(equiv.isEmpty());

//		assertEquals(4, factory.bddCount());
	}

	@Test
	public void testNotb() {
		// bdd for (x1 <-> x2) & x8
		BDD bdd = factory.makeVar(1);
		bdd.biimpWith(factory.makeVar(2));
		bdd.andWith(factory.makeVar(8));
		BDD expected = bdd.not();

		BDDER er = new BDDER(bdd);
		BDDER notGer = er.not_();

		BDD full = notGer.getFullBDD();
		assertTrue(expected.isEquivalentTo(full));

//		assertEquals(5, factory.bddCount());
	}

	@Test
	public void testMaxVar1() {
		// bdd for (x1 <-> x2) & x3
		BDD bdd = factory.makeVar(1);
		bdd.biimpWith(factory.makeVar(2));
		bdd.andWith(factory.makeVar(3));

		BDDER er = new BDDER(bdd);

		assertEquals(3, er.maxVar());
	}

	@Test
	public void testMaxVar2() {
		// bdd for (x1 <-> x6) & x3
		BDD bdd = factory.makeVar(1);
		bdd.biimpWith(factory.makeVar(6));
		bdd.andWith(factory.makeVar(3));

		BDDER er = new BDDER(bdd);

		assertEquals(6, er.maxVar());
	}

	@Test
	public void testEquivVars1() {
		BDD one = factory.makeOne();
		Set<Pair> equivVars = one.equivVars();
		assertTrue(equivVars.isEmpty());

//		assertEquals(1, factory.bddCount());
	}

	@Test
	public void testEquivVars2() {
		BDD zero = factory.makeZero();
		Set<Pair> equivVars = zero.equivVars();
		assertTrue(equivVars.isEmpty());

//		assertEquals(1, factory.bddCount());
	}

	@Test
	public void testEquivVars3() {
		// bdd for (x1 <-> x2) & x8
		BDD bdd = factory.makeVar(1);
		bdd.biimpWith(factory.makeVar(2));
		bdd.andWith(factory.makeVar(8));
		Set<Pair> equivVars = bdd.equivVars();
		assertEquals(1, equivVars.size());
		assertTrue(equivVars.contains(new Pair(1, 2)));

//		assertEquals(1, factory.bddCount());
	}

	@Test
	public void testEquivVars4() {
		// bdd for (x1 <-> x2) & (x2 <-> x3)
		BDD bdd = factory.makeVar(1);
		bdd.biimpWith(factory.makeVar(2));
		BDD temp = factory.makeVar(2);
		temp.biimpWith(factory.makeVar(3));
		bdd.andWith(temp);
		Set<Pair> equivVars = bdd.equivVars();
		assertEquals(2, equivVars.size());
		Pair p1 = new Pair(1, 2);
		Pair p2 = new Pair(1, 3);
		Pair p3 = new Pair(2, 3);
		assertTrue(equivVars.contains(p1) && equivVars.contains(p2) || equivVars.contains(p2) && equivVars.contains(p3)
				|| equivVars.contains(p1) && equivVars.contains(p3));

//		assertEquals(1, factory.bddCount());
	}

	@Test
	public void testNormalize1() {
		// bdd for x1 <-> x2
		BDD bdd = factory.makeVar(1);
		bdd.biimpWith(factory.makeVar(2));

		BDDER er = new BDDER(bdd, new EquivalenceRelation());	// non-normalizing constructor
		BDDER normalized = er.normalize();

		List<Pair> pairs = normalized.getEquiv().pairs();
		assertEquals(1, pairs.size());
		assertEquals(new Pair(1, 2), pairs.get(0));

		BDD n = normalized.getN();
		BDD expected = factory.makeOne();
		assertTrue(n.isEquivalentTo(expected));

//		assertEquals(3, factory.bddCount());
	}

	@Test
	public void testNormalize2() {
		// bdd for (x1 <-> x2) & x3
		BDD bdd = factory.makeVar(1);
		bdd.biimpWith(factory.makeVar(2));
		bdd.andWith(factory.makeVar(3));

		BDDER er = new BDDER(bdd, new EquivalenceRelation());	// non-normalizing constructor
		BDDER normalized = er.normalize();

		List<Pair> pairs = normalized.getEquiv().pairs();
		assertEquals(1, pairs.size());
		assertEquals(new Pair(1, 2), pairs.get(0));

		BDD n = normalized.getN();
		BDD expected = factory.makeVar(3);
		assertTrue(n.isEquivalentTo(expected));

//		assertEquals(3, factory.bddCount());
	}

	@Test
	public void testRenameWithLeader1() {
		// bdd for x2 & x3
		BDD bdd = factory.makeVar(2);
		bdd.andWith(factory.makeVar(3));

		EquivalenceRelation l = new EquivalenceRelation(new int[][] {{1, 2}});
		BDD n = bdd.renameWithLeader(l);

		BDD expected = factory.makeVar(1);
		expected.andWith(factory.makeVar(3));
		assertTrue(n.isEquivalentTo(expected));

//		assertEquals(3, factory.bddCount());
	}

	@Test
	public void testRenameWithLeader2() {
		BDD bdd = factory.makeVar(2);

		EquivalenceRelation l = new EquivalenceRelation(new int[][] {{1, 2}});
		BDD n = bdd.renameWithLeader(l);

		BDD expected = factory.makeVar(1);
		assertTrue(n.isEquivalentTo(expected));

//		assertEquals(3, factory.bddCount());
	}

	@Test
	public void testRenameWithLeader3() {
		BDD bdd = factory.makeVar(1);

		EquivalenceRelation l = new EquivalenceRelation(new int[][] {{2, 3}});
		BDD n = bdd.renameWithLeader(l);

		BDD expected = factory.makeVar(1);
		assertTrue(n.isEquivalentTo(expected));

//		assertEquals(3, factory.bddCount());
	}

	@Test
	public void testRenameWithLeader4() {
		BDD bdd = factory.makeVar(1);

		EquivalenceRelation l = new EquivalenceRelation(new int[][] {{2, 3}});
		BDD n = bdd.renameWithLeader(l);

		BDD expected = factory.makeVar(1);
		assertTrue(n.isEquivalentTo(expected));

//		assertEquals(3, factory.bddCount());
	}

	@Test
	public void testRenameWithLeader5() {
		// bdd for (x6 <-> x7) & (x2 <-> x3)
		BDD bdd = factory.makeVar(6);
		bdd.biimpWith(factory.makeVar(7));
		BDD temp = factory.makeVar(2);
		temp.biimpWith(factory.makeVar(3));
		bdd.andWith(temp);

		EquivalenceRelation l = new EquivalenceRelation(new int[][] {{1, 2}, {6, 7}});
		BDD n = bdd.renameWithLeader(l);

		BDD expected = factory.makeVar(1);
		expected.biimpWith(factory.makeVar(3));
		assertTrue(n.isEquivalentTo(expected));

//		assertEquals(3, factory.bddCount());
	}

	@Test
	public void testFullBDD() {
		// bdd for (x1 <-> x2) & x8
		BDD bdd = factory.makeVar(1);
		bdd.biimpWith(factory.makeVar(2));
		bdd.andWith(factory.makeVar(8));
		BDDER er = new BDDER(bdd.copy());
		BDD full = er.getFullBDD();

		assertTrue(er.equivalentBDDs(full, bdd));

//		assertEquals(3, factory.bddCount());
	}

	@Test
	public void testSatCount1() {
		// x1 XOR x2 - satCount = 2
		BDD bdd = factory.makeVar(0).xorWith(factory.makeVar(1));
		EquivalenceRelation l = new EquivalenceRelation(new int[][] {{1, 2}});
		// 1 value - bound to leader's value in bdd (var 1)
		BDDER er = new BDDER(bdd, l);

		assertEquals(2, er.satCount());
	}

	@Test
	public void testSatCount2() {
		// x1 XOR x2 - satCount = 2
		BDD bdd = factory.makeVar(0).xorWith(factory.makeVar(1));
		EquivalenceRelation l = new EquivalenceRelation(new int[][] {{1, 2}, {3, 4}});
		// 2 values for this - no constraints
		BDDER er = new BDDER(bdd, l);

		assertEquals(4, er.satCount());
	}

	@Test
	public void testSatCount3() {
		// x1 XOR x2 - satCount = 2
		BDD bdd = factory.makeVar(0).xorWith(factory.makeVar(1));
		EquivalenceRelation l = new EquivalenceRelation(new int[][] {{2, 3}, {4, 5, 6, 7}});
		// 2 values for this...
		// times 2 values for this
		BDDER er = new BDDER(bdd, l);

		assertEquals(8, er.satCount());
	}

	@Test
	public void testVars() {
		// bdd for (x1 <-> x2) & x8
		BDD bdd = factory.makeVar(1);
		bdd.biimpWith(factory.makeVar(2));
		bdd.andWith(factory.makeVar(8));
		BDDER er = new BDDER(bdd);

		BitSet vars = er.vars();
		assertEquals(3, vars.cardinality());
		assertTrue(vars.get(1));
		assertTrue(vars.get(2));
		assertTrue(vars.get(8));
	}

	@Test
	public void testBiimp() {
		BDD x1 = factory.makeVar(1);
		BDD x2 = factory.makeVar(2);

		BDDER er1 = new BDDER(x1);
		BDDER er2 = new BDDER(x2);

		BDDER biimp = er1.biimp_(er2);

		BDD n = biimp.getN();
		EquivalenceRelation equiv = biimp.getEquiv();

		assertTrue(n.isEquivalentTo(factory.makeOne()));
		EquivalenceRelation expectedEquiv = new EquivalenceRelation(new int[][] {{1, 2}});
		assertEquals(expectedEquiv, equiv);

		// assertEquals(4, factory.bddCount());
	}

	@Test
	public void testImp() {
		BDD x1 = factory.makeVar(1);
		BDD x2 = factory.makeVar(2);

		BDDER er1 = new BDDER(x1.copy());
		BDDER er2 = new BDDER(x2.copy());

		BDDER imp = er1.imp_(er2);

		BDD fullBDD = imp.getFullBDD();
		BDD expected = x1.imp(x2);

		assertTrue(imp.equivalentBDDs(expected, fullBDD));

		// assertEquals(4, factory.bddCount());
	}

	@Test
	public void testReplace1() {
		BDD x1 = factory.makeVar(1);
		BDD x2 = factory.makeVar(2);

		BDDER er = new BDDER(x1);
		BDDER expected = new BDDER(x2);

		Map<Integer, Integer> renaming = new HashMap<>();
		renaming.put(1, 2);
		BDDER replace = er.replace_(renaming);

		assertTrue(replace.equivalentBDDs(expected, replace));
	}

	@Test(expected = ReplacementWithExistingVarException.class)
	public void testReplace2() {
		EquivalenceRelation l = new EquivalenceRelation(new int[][] {{1, 2}});
		BDD n = factory.makeVar(3);

		BDDER er = new BDDER(n, l);

		Map<Integer, Integer> renaming = new HashMap<>();
		renaming.put(1, 2);
		er.replace(renaming);
	}

	@Test
	public void testReplace3() {
		EquivalenceRelation l = new EquivalenceRelation(new int[][] {{1, 2}});
		BDD n = factory.makeVar(3);

		BDDER er = new BDDER(n, l);

		EquivalenceRelation l2 = new EquivalenceRelation(new int[][] {{2, 4}});
		BDDER expected = new BDDER(n.copy(), l2);

		Map<Integer, Integer> renaming = new HashMap<>();
		renaming.put(1, 4);
		BDDER replace = er.replace_(renaming);

		assertTrue(replace.equivalentBDDs(expected, replace));
	}

	@Test
	public void testReplace4() {
		EquivalenceRelation l = new EquivalenceRelation(new int[][] {{1, 2, 3}});
		BDD n = factory.makeVar(1);

		BDDER er = new BDDER(n, l);

		EquivalenceRelation l2 = new EquivalenceRelation(new int[][] {{2, 3, 4}});
		BDDER expected = new BDDER(factory.makeVar(2), l2);

		Map<Integer, Integer> renaming = new HashMap<>();
		renaming.put(1, 4);
		BDDER replace = er.replace_(renaming);

		assertTrue(replace.equivalentBDDs(expected, replace));
	}

	@Test
	public void testReplace5() {
		EquivalenceRelation l = new EquivalenceRelation(new int[][] {{6, 9}});
		BDD n = factory.makeVar(8);

		BDDER er = new BDDER(n, l);

		EquivalenceRelation l2 = new EquivalenceRelation(new int[][] {{3, 9}});
		BDDER expected = new BDDER(factory.makeVar(8), l2);

		Map<Integer, Integer> renaming = new HashMap<>();
		renaming.put(9, 3);	// this should be performed first
		renaming.put(6, 9);
		BDDER replace = er.replace_(renaming);

		assertTrue(replace.equivalentBDDs(expected, replace));
	}

}
