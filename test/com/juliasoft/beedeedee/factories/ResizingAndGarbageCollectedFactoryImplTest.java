package com.juliasoft.beedeedee.factories;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.BitSet;

import org.junit.Before;
import org.junit.Test;

import com.juliasoft.beedeedee.bdd.BDD;
import com.juliasoft.beedeedee.factories.ResizingAndGarbageCollectedFactoryImpl.BDDImpl;

public class ResizingAndGarbageCollectedFactoryImplTest {

	private ResizingAndGarbageCollectedFactoryImpl factory;

	@Before
	public void setUp() {
		factory = new ResizingAndGarbageCollectedFactoryImpl(10, 10, 0);
	}

	@Test
	public void testMarkAliveNodes1() {
		boolean[] aliveNodes = new boolean[10];
		factory.markAliveNodes(aliveNodes);

		boolean[] expected = new boolean[10];
		// terminal are always alive
		expected[0] = true;
		expected[1] = true;
		assertArrayEquals(expected, aliveNodes);
	}

	@Test
	public void testMarkAliveNodes2() {
		factory.makeVar(5);

		boolean[] aliveNodes = new boolean[10];
		factory.markAliveNodes(aliveNodes);

		boolean[] expected = new boolean[10];
		// terminal are always alive
		expected[0] = true;
		expected[1] = true;
		// var 5 is alive
		expected[2] = true;
		assertArrayEquals(expected, aliveNodes);
	}

	@Test
	public void testMarkAliveNodes3() {
		BDD x5 = factory.makeVar(5);
		factory.makeVar(7);
		x5.free();

		boolean[] aliveNodes = new boolean[10];
		factory.markAliveNodes(aliveNodes);

		boolean[] expected = new boolean[10];
		// terminal are always alive
		expected[0] = true;
		expected[1] = true;
		// var 7 is alive
		expected[3] = true;
		assertArrayEquals(expected, aliveNodes);
	}

	@Test
	public void testUpdateIndicesOfAllBDDsCreatedSoFar() {
		factory.makeVar(5); // 2
		factory.makeVar(7); // 3

		int[] newPositions = new int[10];
		newPositions[2] = 4;
		newPositions[3] = 6;
		factory.updateIndicesOfAllBDDsCreatedSoFar(newPositions);

		ArrayList<BDDImpl> allBDDsCreatedSoFarCopy = factory.getAllBDDsCreatedSoFarCopy();
		for (BDDImpl bdd : allBDDsCreatedSoFarCopy) {
			assertTrue(bdd.id == 4 || bdd.id == 6);
		}
	}

	@Test
	public void testBddCount() {
		assertEquals(0, factory.bddCount());
		BDD x2 = factory.makeVar(2);
		assertEquals(1, factory.bddCount());
		x2.free();
		assertEquals(0, factory.bddCount());
	}

	@Test
	public void testBddVars1() {
		BDD x2 = factory.makeVar(2);
		BitSet vars = x2.vars();
		assertEquals(1, vars.cardinality());
		assertTrue(vars.get(2));
	}

	@Test
	public void testBddVars2() {
		BDD bdd = factory.makeVar(2);
		bdd.xorWith(factory.makeVar(3));

		BitSet vars = bdd.vars();
		assertEquals(2, vars.cardinality());
		assertTrue(vars.get(2));
		assertTrue(vars.get(3));
	}
}
