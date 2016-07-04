package com.juliasoft.beedeedee.er;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.juliasoft.beedeedee.bdd.BDD;
import com.juliasoft.beedeedee.bdd.ReplacementWithExistingVarException;
import com.juliasoft.beedeedee.er.ER;
import com.juliasoft.beedeedee.er.EquivalenceRelation;
import com.juliasoft.beedeedee.er.Pair;
import com.juliasoft.beedeedee.factories.Factory;
import com.juliasoft.beedeedee.factories.ResizingAndGarbageCollectedFactory;

public class ERTest {

	private ResizingAndGarbageCollectedFactory factory;

	@Before
	public void setUp() throws Exception {
		factory = Factory.mkResizingAndGarbageCollected(10, 10, 0);
	}

	@Test
	public void testSqueezeAll() {
		// bdd for x1 <-> x2
		BDD bdd = factory.makeVar(1);
		bdd.biimpWith(factory.makeVar(2));

		// construct a set of equivalence classes
		EquivalenceRelation e = new EquivalenceRelation();
		e.addClass(1, 2);

		ER ger = new ER(bdd, e);
		BDD n = ger.getSqueezedBDD();

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

		EquivalenceRelation e = new EquivalenceRelation();
		e.addClass(1, 2);

		ER ger = new ER(bdd, e);
		BDD n = ger.getSqueezedBDD();

		BDD expected = factory.makeVar(3);
		assertTrue(n.isEquivalentTo(expected));

//		assertEquals(3, factory.bddCount());
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

		ER ger1 = new ER(bdd1);

		ER ger2 = new ER(bdd2);

		ER and = ger1.and(ger2);

		BDD n = and.getN();
		BDD expectedN = factory.makeVar(4);
		assertTrue(n.isEquivalentTo(expectedN));

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

		ER ger1 = new ER(bdd1);

		ER ger2 = new ER(bdd2);

		ER and = ger1.and(ger2);

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

		EquivalenceRelation e1 = new EquivalenceRelation();
		e1.addClass(1, 2);
		ER ger1 = new ER(bdd1, e1);

		EquivalenceRelation e2 = new EquivalenceRelation();
		e2.addClass(1, 2, 3);
		ER ger2 = new ER(bdd2, e2);

		ER and = ger1.and(ger2);

		EquivalenceRelation equiv = and.getEquiv();
		// {{1, 2, 3}, {6, 7}}
		assertEquals(2, equiv.size());
		// FIXME order dependent
		Iterator<BitSet> it = equiv.iterator();
		BitSet next = it.next();
		BitSet expected = new BitSet();
		expected.set(1, 4);
		assertEquals(expected, next);
		next = it.next();
		expected.clear();
		expected.set(6, 8);
		assertEquals(expected, next);

		BDD n = and.getN();
		BDD expectedN = factory.makeVar(8);
		assertTrue(n.isEquivalentTo(expectedN));

//		assertEquals(4, factory.bddCount());
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

		EquivalenceRelation e1 = new EquivalenceRelation();
		e1.addClass(1, 2);
		ER ger1 = new ER(bdd1, e1);

		EquivalenceRelation e2 = new EquivalenceRelation();
		e2.addClass(1, 3);
		ER ger2 = new ER(bdd2, e2);

		ER or = ger1.or(ger2);

		BDD n = or.getN();
		BDD expectedN = bdd1.or(bdd2);
		assertTrue(n.isEquivalentTo(expectedN));

		EquivalenceRelation equiv = or.getEquiv();
		assertTrue(equiv.isEmpty());

//		assertEquals(4, factory.bddCount());
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

		EquivalenceRelation e1 = new EquivalenceRelation();
		e1.addClass(1, 2);
		ER ger1 = new ER(bdd1, e1);

		EquivalenceRelation e2 = new EquivalenceRelation();
		e2.addClass(1, 2, 3);
		ER ger2 = new ER(bdd2, e2);

		ER or = ger1.or(ger2);

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

		EquivalenceRelation e1 = new EquivalenceRelation();
		e1.addClass(1, 2);
		ER ger1 = new ER(bdd1, e1);

		EquivalenceRelation e2 = new EquivalenceRelation();
		e2.addClass(1, 2, 3);
		ER ger2 = new ER(bdd2, e2);

		ER or = ger1.or(ger2);

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
	public void testXor() {
		// bdd for x1 <-> x2
		BDD bdd1 = factory.makeVar(1);
		bdd1.biimpWith(factory.makeVar(2));
		// bdd for (x1 <-> x3) & x4
		BDD bdd2 = factory.makeVar(1);
		bdd2.biimpWith(factory.makeVar(3));
		bdd2.andWith(factory.makeVar(4));

		EquivalenceRelation e1 = new EquivalenceRelation();
		e1.addClass(1, 2);
		ER ger1 = new ER(bdd1, e1);

		EquivalenceRelation e2 = new EquivalenceRelation();
		e2.addClass(1, 3);
		ER ger2 = new ER(bdd2, e2);

		ER xor = ger1.xor(ger2);

		BDD n = xor.getN();
		BDD expectedN = bdd1.xor(bdd2);
		assertTrue(n.isEquivalentTo(expectedN));

		EquivalenceRelation equiv = xor.getEquiv();
		assertTrue(equiv.isEmpty());

//		assertEquals(4, factory.bddCount());
	}

	@Test
	public void testNot() {
		// bdd for (x1 <-> x2) & x8
		BDD bdd = factory.makeVar(1);
		bdd.biimpWith(factory.makeVar(2));
		bdd.andWith(factory.makeVar(8));
		ER ger = new ER(bdd).normalize();
		ER notGer = ger.not();

		BDD full = notGer.getFullBDD();
		BDD expected = bdd.not();
		assertTrue(expected.isEquivalentTo(full));

//		assertEquals(5, factory.bddCount());
	}

	@Test
	public void testMaxVar1() {
		// bdd for (x1 <-> x2) & x3
		BDD bdd = factory.makeVar(1);
		bdd.biimpWith(factory.makeVar(2));
		bdd.andWith(factory.makeVar(3));

		ER ger = new ER(bdd);
		ger = ger.normalize();

		assertEquals(3, ger.maxVar());
	}

	@Test
	public void testMaxVar2() {
		// bdd for (x1 <-> x6) & x3
		BDD bdd = factory.makeVar(1);
		bdd.biimpWith(factory.makeVar(6));
		bdd.andWith(factory.makeVar(3));

		ER ger = new ER(bdd);
		ger = ger.normalize();

		assertEquals(6, ger.maxVar());
	}

//	@Test
//	public void testGeneratePairs() {
//		List<Pair> pairs = fakeGer.generatePairs(3);
//		assertEquals(6, pairs.size());
//		assertTrue(pairs.contains(new Pair(0, 1)));
//		assertTrue(pairs.contains(new Pair(0, 2)));
//		assertTrue(pairs.contains(new Pair(0, 3)));
//		assertTrue(pairs.contains(new Pair(1, 2)));
//		assertTrue(pairs.contains(new Pair(1, 3)));
//		assertTrue(pairs.contains(new Pair(2, 3)));
//	}

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

		ER ger = new ER(bdd);
		ER normalized = ger.normalize();

		List<Pair> pairs = normalized.getEquiv().pairs();
		assertEquals(1, pairs.size());
		assertEquals(new Pair(1, 2), pairs.get(0));

		BDD n = normalized.getN();
		BDD expected = factory.makeOne();
		assertTrue(n.isEquivalentTo(expected));

		assertEquals(3, factory.bddCount());
	}

	@Test
	public void testNormalize2() {
		// bdd for (x1 <-> x2) & x3
		BDD bdd = factory.makeVar(1);
		bdd.biimpWith(factory.makeVar(2));
		bdd.andWith(factory.makeVar(3));

		ER ger = new ER(bdd);
		ER normalized = ger.normalize();

		List<Pair> pairs = normalized.getEquiv().pairs();
		assertEquals(1, pairs.size());
		assertEquals(new Pair(1, 2), pairs.get(0));

		BDD n = normalized.getN();
		BDD expected = factory.makeVar(3);
		assertTrue(n.isEquivalentTo(expected));

		assertEquals(3, factory.bddCount());
	}

	@Test
	public void testRenameWithLeader1() {
		// bdd for x2 & x3
		BDD bdd = factory.makeVar(2);
		bdd.andWith(factory.makeVar(3));

		EquivalenceRelation l = new EquivalenceRelation();
		l.addClass(1, 2);
		BDD n = bdd.renameWithLeader(l);

		BDD expected = factory.makeVar(1);
		expected.andWith(factory.makeVar(3));
		assertTrue(n.isEquivalentTo(expected));

//		assertEquals(3, factory.bddCount());
	}

	@Test
	public void testRenameWithLeader2() {
		BDD bdd = factory.makeVar(2);

		EquivalenceRelation l = new EquivalenceRelation();
		l.addClass(1, 2);
		BDD n = bdd.renameWithLeader(l);

		BDD expected = factory.makeVar(1);
		assertTrue(n.isEquivalentTo(expected));

//		assertEquals(3, factory.bddCount());
	}

	@Test
	public void testRenameWithLeader3() {
		BDD bdd = factory.makeVar(1);

		EquivalenceRelation l = new EquivalenceRelation();
		l.addClass(2, 3);
		BDD n = bdd.renameWithLeader(l);

		BDD expected = factory.makeVar(1);
		assertTrue(n.isEquivalentTo(expected));

//		assertEquals(3, factory.bddCount());
	}

	@Test
	public void testRenameWithLeader4() {
		BDD bdd = factory.makeVar(1);

		EquivalenceRelation l = new EquivalenceRelation();
		l.addClass(2, 3);
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

		EquivalenceRelation l = new EquivalenceRelation();
		l.addClass(1, 2);
		l.addClass(6, 7);
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
		ER ger = new ER(bdd).normalize();
		BDD full = ger.getFullBDD();
		assertTrue(full.isEquivalentTo(bdd));

//		assertEquals(3, factory.bddCount());
	}

	@Test
	public void testSatCount1() {
		// x1 XOR x2 - satCount = 2
		BDD bdd = factory.makeVar(0).xorWith(factory.makeVar(1));
		EquivalenceRelation l = new EquivalenceRelation();
		l.addClass(1, 2); // 1 value - bound to leader's value in bdd (var 1)
		ER ger = new ER(bdd, l);

		assertEquals(2, ger.satCount());
	}

	@Test
	public void testSatCount2() {
		// x1 XOR x2 - satCount = 2
		BDD bdd = factory.makeVar(0).xorWith(factory.makeVar(1));
		EquivalenceRelation l = new EquivalenceRelation();
		l.addClass(1, 2);
		l.addClass(3, 4); // 2 values for this - no constraints
		ER ger = new ER(bdd, l);

		assertEquals(4, ger.satCount());
	}

	@Test
	public void testSatCount3() {
		// x1 XOR x2 - satCount = 2
		BDD bdd = factory.makeVar(0).xorWith(factory.makeVar(1));
		EquivalenceRelation l = new EquivalenceRelation();
		l.addClass(2, 3); // 2 values for this...
		l.addClass(4, 5, 6, 7); // times 2 values for this
		ER ger = new ER(bdd, l);

		assertEquals(8, ger.satCount());
	}

	@Test
	public void testVars() {
		// bdd for (x1 <-> x2) & x8
		BDD bdd = factory.makeVar(1);
		bdd.biimpWith(factory.makeVar(2));
		bdd.andWith(factory.makeVar(8));
		ER ger = new ER(bdd).normalize();

		BitSet vars = ger.vars();
		assertEquals(3, vars.cardinality());
		assertTrue(vars.get(1));
		assertTrue(vars.get(2));
		assertTrue(vars.get(8));
	}

	@Test
	public void testBiimp() {
		BDD x1 = factory.makeVar(1);
		BDD x2 = factory.makeVar(2);

		ER ger1 = new ER(x1);
		ER ger2 = new ER(x2);

		ER biimp = ger1.biimp(ger2);

		BDD n = biimp.getN();
		EquivalenceRelation equiv = biimp.getEquiv();

		assertTrue(n.isEquivalentTo(factory.makeOne()));
		EquivalenceRelation expectedEquiv = new EquivalenceRelation();
		expectedEquiv.addClass(1, 2);
		assertEquals(expectedEquiv, equiv);

		// assertEquals(4, factory.bddCount());
	}

	@Test
	public void testImp() {
		BDD x1 = factory.makeVar(1);
		BDD x2 = factory.makeVar(2);

		ER ger1 = new ER(x1);
		ER ger2 = new ER(x2);

		ER imp = ger1.imp(ger2);

		BDD fullBDD = imp.getFullBDD();
		BDD expected = x1.imp(x2);

		assertTrue(fullBDD.isEquivalentTo(expected));

		// assertEquals(4, factory.bddCount());
	}

	@Test
	public void testReplace1() {
		BDD x1 = factory.makeVar(1);
		BDD x2 = factory.makeVar(2);

		ER ger = new ER(x1).normalize();
		ER expected = new ER(x2).normalize();

		Map<Integer, Integer> renaming = new HashMap<>();
		renaming.put(1, 2);
		ER replace = ger.replace(renaming);

		assertEquals(expected, replace);
	}

	@Test(expected = ReplacementWithExistingVarException.class)
	public void testReplace2() {
		EquivalenceRelation l = new EquivalenceRelation();
		l.addClass(1, 2);
		BDD n = factory.makeVar(3);

		ER ger = new ER(n, l);

		Map<Integer, Integer> renaming = new HashMap<>();
		renaming.put(1, 2);
		ger.replace(renaming);
	}

	@Test
	public void testReplace3() {
		EquivalenceRelation l = new EquivalenceRelation();
		l.addClass(1, 2);
		BDD n = factory.makeVar(3);

		ER ger = new ER(n, l);

		EquivalenceRelation l2 = new EquivalenceRelation();
		l2.addClass(2, 4);
		ER expected = new ER(n.copy(), l2);

		Map<Integer, Integer> renaming = new HashMap<>();
		renaming.put(1, 4);
		ER replace = ger.replace(renaming);

		assertEquals(expected, replace);
	}

	@Test
	public void testReplace4() {
		EquivalenceRelation l = new EquivalenceRelation();
		l.addClass(1, 2, 3);
		BDD n = factory.makeVar(1);

		ER ger = new ER(n, l);

		EquivalenceRelation l2 = new EquivalenceRelation();
		l2.addClass(2, 3, 4);
		ER expected = new ER(factory.makeVar(2), l2);

		Map<Integer, Integer> renaming = new HashMap<>();
		renaming.put(1, 4);
		ER replace = ger.replace(renaming);

		assertEquals(expected, replace);
	}

	@Test
	public void testReplace5() {
		EquivalenceRelation l = new EquivalenceRelation();
		l.addClass(6, 9);
		BDD n = factory.makeVar(8);

		ER ger = new ER(n, l);

		EquivalenceRelation l2 = new EquivalenceRelation();
		l2.addClass(3, 9);
		ER expected = new ER(factory.makeVar(8), l2);

		Map<Integer, Integer> renaming = new HashMap<>();
		renaming.put(9, 3);	// this should be performed first
		renaming.put(6, 9);
		ER replace = ger.replace(renaming);

		assertEquals(expected, replace);
	}
}
