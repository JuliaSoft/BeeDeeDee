package com.juliasoft.beedeedee.ger;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.juliasoft.beedeedee.bdd.Assignment;
import com.juliasoft.beedeedee.bdd.BDD;
import com.juliasoft.beedeedee.factories.Factory;

public class BDDGerTest {

	private Factory factory;
	private BDD bddX1biX2;
	private BDD bddX3;

	@Before
	public void setUp() {
		factory = Factory.mkResizingAndGarbageCollected(10, 10, 0);
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
		BDD bddGer = new BDDGer(bdd);
		assertEquals(2, bddGer.var());
		assertTrue(bddGer.low().isZero());
		assertTrue(bddGer.high().isOne());

//		assertEquals(5, factory.bddCount()); FIXME uncomment all
	}

	@Test
	public void testZero() {
		BDD bdd = factory.makeZero();
		BDD bddGer = new BDDGer(bdd);
		assertTrue(bddGer.isZero());
		// TODO can l be non-empty?

//		assertEquals(3, factory.bddCount());
	}

	@Test
	public void testOne1() {
		BDD bdd = factory.makeOne();
		BDD bddGer = new BDDGer(bdd);
		assertTrue(bddGer.isOne());

//		assertEquals(3, factory.bddCount());
	}

	@Test
	public void testOne2() {
		BDD bddGer = new BDDGer(bddX1biX2);
		// the normalized BDD is ONE, but not the original one
		assertFalse(bddGer.isOne());

//		assertEquals(2, factory.bddCount());
	}

	@Test
	public void testOr() {
		BDD bddGer1 = new BDDGer(bddX1biX2.copy());
		BDD bddGer2 = new BDDGer(bddX3.copy());

		BDD or = bddGer1.or(bddGer2);
		BDD originalOr = bddX1biX2.or(bddX3);

		assertTrue(or.isEquivalentTo(originalOr));

//		assertEquals(5, factory.bddCount());
	}

	@Test
	public void testAnd() {
		BDD bddGer1 = new BDDGer(bddX1biX2.copy());
		BDD bddGer2 = new BDDGer(bddX3.copy());

		BDD and = bddGer1.and(bddGer2);
		BDD originalAnd = bddX1biX2.and(bddX3);

		assertTrue(and.isEquivalentTo(originalAnd));

//		assertEquals(5, factory.bddCount());
	}

	@Test
	public void testXor() {
		BDD bddGer1 = new BDDGer(bddX1biX2.copy());
		BDD bddGer2 = new BDDGer(bddX3.copy());

		BDD xor = bddGer1.xor(bddGer2);
		BDD originalXor = bddX1biX2.xor(bddX3);

		assertTrue(xor.isEquivalentTo(originalXor));

//		assertEquals(5, factory.bddCount());
	}

	@Test
	public void testNand() {
		BDD bddGer1 = new BDDGer(bddX1biX2.copy());
		BDD bddGer2 = new BDDGer(bddX3.copy());

		BDD nand = bddGer1.nand(bddGer2);
		BDD originalNand = bddX1biX2.nand(bddX3);

		assertTrue(nand.isEquivalentTo(originalNand));

//		assertEquals(5, factory.bddCount());
	}

	@Test
	public void testNot() {
		// (x1 <-> x2) | x3
		BDD bdd = bddX1biX2.or(bddX3);
		BDD bddGer = new BDDGer(bdd.copy());

		BDD not = bddGer.not();
		BDD originalNot = bdd.not();

		assertTrue(not.isEquivalentTo(originalNot));

//		assertEquals(5, factory.bddCount());
	}

	@Test
	public void testNodeCount1() {
		// (x1 <-> x2) & x3
		BDD bdd = bddX1biX2.and(bddX3);
		BDD bddGer = new BDDGer(bdd);

		assertEquals(1, bddGer.nodeCount());

//		assertEquals(3, factory.bddCount());
	}

	@Test
	public void testNodeCount2() {
		// (x1 <-> x2) | x3
		BDD bdd = bddX1biX2.or(bddX3);
		BDD bddGer = new BDDGer(bdd.copy());

		// the bdd is already normalized, same node count
		assertEquals(bdd.nodeCount(), bddGer.nodeCount());

//		assertEquals(4, factory.bddCount());
	}

	@Test
	public void testAnySat1() {
		// (x1 <-> x2) & x3
		BDD bdd = bddX1biX2.and(bddX3);
		BDD bddGer = new BDDGer(bdd);

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
		BDD bddGer = new BDDGer(bdd);

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
		BDD bddGer = new BDDGer(bdd);

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
		BDD bddGer = new BDDGer(bdd.copy());

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
		BDD bddGer = new BDDGer(bdd.copy());

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
		BDD bddGer = new BDDGer(bdd.copy());

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
		BDD bddGer = new BDDGer(bdd.copy());

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
		BDD bddGer = new BDDGer(bdd.copy());

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
		BDD bddGer = new BDDGer(bdd.copy());

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
		BDD bddGer = new BDDGer(bdd.copy());

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
		BDD bddGer = new BDDGer(bdd.copy());

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
		BDD bddGer = new BDDGer(bdd.copy());

		BDD minterm = factory.makeVar(1).andWith(factory.makeVar(2));
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
		BDD bddGer = new BDDGer(bdd.copy());

		// [], x2 OR x4
		BDD minterm = factory.makeVar(1).andWith(factory.makeVar(2));
		BDD exist = bddGer.exist(minterm);
		BDD originalExist = bdd.exist(minterm);

		assertTrue(exist.isEquivalentTo(originalExist));
	}


}
