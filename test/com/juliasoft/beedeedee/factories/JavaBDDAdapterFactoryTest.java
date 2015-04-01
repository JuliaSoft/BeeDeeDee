package com.juliasoft.beedeedee.factories;

import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;

import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDFactory;
import net.sf.javabdd.BDDPairing;
import net.sf.javabdd.JFactory;

import org.junit.Before;
import org.junit.Test;

public class JavaBDDAdapterFactoryTest {

	/** wrapper factory */
	private BDDFactory factory;
	/** original JavaBDD java factory */
	private BDDFactory jfactory;

	@Before
	public void setUp() throws Exception {
		factory = JavaBDDAdapterFactory.init(100, 10);
		factory.setVarNum(10);
		jfactory = JFactory.init(100, 10);
		jfactory.setVarNum(10);
	}

	@Test
	public void test() {
		BDD x0 = factory.ithVar(0);
		BDD x1 = factory.ithVar(1);
		
		BDD and = x0.and(x1);
		
		and.satOne().printSet();
		
		BDD xor = x0.xor(x1);
		xor.printSet();
	}

	@Test
	public void testNodeCount() {
		BDD x0 = jfactory.ithVar(0);
		BDD x1 = jfactory.ithVar(1);
		BDD myX0 = factory.ithVar(0);
		BDD myX1 = factory.ithVar(1);

		BDD and = x0.and(x1);
		BDD myAnd = myX0.and(myX1);
		assertEquals(and.nodeCount(), myAnd.nodeCount());
		
		BDD xor = x0.xor(x1);
		BDD myXor = myX0.xor(myX1);
		assertEquals(xor.nodeCount(), myXor.nodeCount());
		
		assertEquals(0, factory.zero().nodeCount());
		assertEquals(0, factory.one().nodeCount());
	}

	//@Test
	private void testReplace() {
		BDD x0 = jfactory.ithVar(0);
		BDD x1 = jfactory.ithVar(1);
		BDD myX0 = factory.ithVar(0);
		BDD myX1 = factory.ithVar(1);

		// adapter
		BDD myAnd = myX0.and(myX1);
		BDDPairing pair = factory.makePair();
		pair.set(0, 2);
		pair.set(1, 4);
		myAnd.replaceWith(pair);

		// JFactory
		BDD and = x0.and(x1);
		pair = jfactory.makePair();
		pair.set(0, 2);
		pair.set(1, 4);
		and.replaceWith(pair);

		assertBDDEquals(myAnd, and);
	}

	/**
	 * Asserts that two bdds are the same.
	 * @param myBdd an adapter's bdd
	 * @param bdd a JFactory bdd
	 */
	private void assertBDDEquals(BDD myBdd, BDD bdd) {
		try {
			StringWriter mySw = new StringWriter();
			BufferedWriter myOut = new BufferedWriter(mySw);
			factory.save(myOut, myBdd);
			myOut.close();
			
			StringWriter sw = new StringWriter();
			BufferedWriter out = new BufferedWriter(sw);
			jfactory.save(out, bdd);
			out.close();
			
			System.out.println(mySw);
			System.out.println(sw);
			assertEquals(mySw.toString(), sw.toString());
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testEquals() {
		BDD x0 = jfactory.ithVar(0);
		BDD x1 = jfactory.ithVar(1);
		BDD myX0 = factory.ithVar(0);
		BDD myX1 = factory.ithVar(1);

		BDD and = x0.and(x1);
		BDD invAnd = x1.and(x0);
		
		assertTrue(and.equals(invAnd));
		
		BDD myAnd = myX0.and(myX1);
		BDD myInvAnd = myX1.and(myX0);
		
		assertTrue(myAnd.equals(myInvAnd));
	}
	
	@Test
	public void testExist() {
		BDD x0 = jfactory.ithVar(0);
		BDD x1 = jfactory.ithVar(1);
		BDD x2 = jfactory.ithVar(2);

		BDD bdd = x0.and(x1).or(x2);
		System.out.println(bdd);
		
		BDD var = x1.and(x2);
		System.out.println(bdd.exist(var));
	}
	
	@Test
	public void testIte() {
		BDD x0 = jfactory.ithVar(0);
		BDD x1 = jfactory.ithVar(1);
		BDD x2 = jfactory.ithVar(2);
		BDD mx0 = factory.ithVar(0);
		BDD mx1 = factory.ithVar(1);
		BDD mx2 = factory.ithVar(2);

		BDD ite = x0.ite(x1, x2);
		assertEquals(ite, x0.and(x1).or(x0.not().and(x2)));
		
		BDD myIte = mx0.ite(mx1, mx2);
		assertEquals(myIte, mx0.and(mx1).or(mx0.not().and(mx2)));
	}
	
	@Test
	public void testRelprod() {
		BDD x0 = jfactory.ithVar(0);
		BDD x1 = jfactory.ithVar(1);
		BDD mx0 = factory.ithVar(0);
		BDD mx1 = factory.ithVar(1);

		BDD rp = x0.relprod(x1, x0);
		assertEquals(rp, x0.and(x1).exist(x0));
		
		BDD mrp = mx0.relprod(mx1, mx0);
		assertEquals(mrp, mx0.and(mx1).exist(mx0));
	}
}
