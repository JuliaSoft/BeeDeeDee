package com.juliasoft.beedeedee.factories;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.juliasoft.beedeedee.bdd.Assignment;
import com.juliasoft.beedeedee.bdd.BDD;
import com.juliasoft.beedeedee.bdd.ReplacementWithExistingVarException;

@SuppressWarnings("unused")
public class FactoryTest {

	private static final int INITIAL_NODE_NUM = 10;
	private Factory factory;
	private BDD x1;
	private BDD x2;
	private BDD x3;
	private BDD x4;
	private BDD x5;
	private Map<Integer, Integer> renaming;

	@Before
	public void setUp() {
		factory = new ResizingAndGarbageCollectedFactoryImpl(INITIAL_NODE_NUM, 2);
		
		x1 = factory.makeVar(1);
		x2 = factory.makeVar(2);
		x3 = factory.makeVar(3);
		x4 = factory.makeVar(4);
		x5 = factory.makeVar(5);

		renaming = new HashMap<Integer, Integer>();
	}

	@After
	public void cleanUp() {
		factory.done();
	}

/*
	@Test
	public void testGC() {
		BDD[] bdds = new BDD[INITIAL_NODE_NUM];
		for (int i = 3; i < INITIAL_NODE_NUM - 1; i++) {
			bdds[i] = factory.makeVar(i);
			if (i < INITIAL_NODE_NUM - 3)
				bdds[i].free();
		}
		System.out.println(factory.nodesCount());
		factory.printUT();
		BDD and = bdds[INITIAL_NODE_NUM - 3].and(bdds[INITIAL_NODE_NUM - 2]);
		factory.printUT();
		BDD or = bdds[INITIAL_NODE_NUM - 2].or(and);
		System.out.println(and + " " + or);
	}
	*/
	
//	@Test
	private void test() {
		BDD biimp = x1.biimp(x2);
//		System.out.println(biimp);
//		factory.printNodeTable();		
		
		BDD and = x1.and(x2);
//		System.out.println(and);
//		factory.printNodeTable();

		BDD or = and.or(x3);
//		System.out.println(or);
//		factory.printNodeTable();
		
		BDD nand = and.nand(or);
//		System.out.println(nand);
//		factory.printNodeTable();
	}

//	@Test
	private void testConcurrency() throws InterruptedException, ExecutionException {
		
		int threads = 100;
		
		ExecutorCompletionService<BDD> ecs = new ExecutorCompletionService<BDD>(
				Executors.newCachedThreadPool());
		
		final CountDownLatch cdl = new CountDownLatch(threads);
		
		for (int t = 0; t < threads; t++) {
			ecs.submit(new Callable<BDD>() {
				
				@Override
				public BDD call() throws InterruptedException  {
					
					// wait others to maximize concurrency
					cdl.countDown();
					cdl.await();
					
					BDD res = factory.makeZero();
					for (int i = 0; i < 21; i += 2) {
						BDD t1 = factory.makeVar(i);
						BDD t2 = factory.makeVar(i + 1);
						BDD temp = t1.and(t2);
						res.orWith(temp);
						t1.free();
						t2.free();
					}
					
					return res;
				}
			});
		}
		
		for (int t = 0; t < threads; t++) {
			System.out.println(ecs.take().get());
			// TODO assert equivalence
		}

	}
	
//	@Test
	private void testRestrict1() {
		// ex. from page 20 of Andersen's notes
		
		BDD biimp = x1.biimp(x2);
		BDD or = biimp.or(x3);
		
//		factory.printNodeTable();
		
		BDD restriction = or.restrict(2, false);
		
//		factory.printNodeTable();
	}
	
//	@Test
	private void testRestrict2() {
		BDD biimp = x1.biimp(x2);
		BDD or = biimp.or(x3);
		
//		factory.printNodeTable();
		
		BDD var = x1.and(x2.not());
		
//		factory.printNodeTable();
		
		BDD restriction = or.restrict(var);
		
//		factory.printNodeTable();
	}
	
//	@Test
	private void testExist1() {
		
		BDD and = x1.and(x2);
		BDD or = and.or(x3);
		
//		factory.printNodeTable();
		
		BDD exist = or.exist(2);

//		factory.printNodeTable();
	}
	
//	@Test
	private void testExist2() {
		BDD and = x1.and(x2);
		BDD or = and.or(x3);
		
		BDD var = x2;
		
//		factory.printNodeTable();
		
		BDD exist = or.exist(x2);

//		factory.printNodeTable();
//		System.out.println(exist);
	}
	@Test
	public void testExist3() {
		BDD bdd = x1.and(x2).or(x3);
//		factory.printNodeTable();
//		System.out.println(bdd);
	
		BDD var = x2.and(x3);
//		factory.printNodeTable();
		System.out.println(bdd.exist(var));
	}
	
	//	@Test
	private void testXor() {
		BDD xor = x1.xor(x2);
//		factory.printNodeTable();
//		System.out.println(xor);
	}
	
	@Test
	public void testVarProfile1() {
		BDD and = x1.and(x2);
		
		assertEquals(1, and.varProfile()[1]);
		assertEquals(1, and.varProfile()[2]);
	}
	
	@Test
	public void testVarProfile2() {
		BDD xor = x1.xor(x2);
		
		assertEquals(1, xor.varProfile()[1]);
		assertEquals(2, xor.varProfile()[2]);
	}

	@Test
	public void testVarProfile3() {
		BDD biimp = x1.biimp(x2);
		BDD or = biimp.or(x3);
		
		assertEquals(1, or.varProfile()[1]);
		assertEquals(2, or.varProfile()[2]);
		assertEquals(1, or.varProfile()[3]);
	}

	@Test
	public void testPathCount() {
		BDD biimp = x1.biimp(x2);
		BDD or = biimp.or(x3);
		
		assertEquals(4, or.pathCount());
	}

	@Test
	public void testNodeCount() {
		BDD biimp = x1.biimp(x2);
		BDD or = biimp.or(x3);
		BDD restrict = or.restrict(2, false);

		assertEquals(4, factory.nodeCount(Arrays.asList(x3, or)));
		assertEquals(5, factory.nodeCount(Arrays.asList(or, restrict)));
		assertEquals(7, factory.nodeCount(Arrays.asList(or, biimp)));
	}

	@Test
	public void testReplace1() {
		BDD and = x1.and(x2);

		Assignment a = and.anySat();
		
		assertTrue(a.holds(x1));
		assertTrue(a.holds(x2));
		
		// rename 1->3, 2->5
		renaming.put(1, 3);
		renaming.put(2, 5);
		
		BDD andR = and.replace(renaming);
		a = andR.anySat();
		
		assertTrue(a.holds(x3));
		assertTrue(a.holds(x5));
	}
	
	@Test
	public void testReplace2() {
		Assignment a = x1.anySat();
		assertTrue(a.holds(x1));
		
		// rename 1->3
		renaming.put(1, 3);

		BDD x1R = x1.replace(renaming);
		a = x1R.anySat();
		
		assertTrue(a.holds(x3));
	}
	
	@Test
	public void testReplace3() {
		BDD and = x1.and(x2);

		// rename 1->3
		renaming.put(1, 3);
		
		BDD andR = and.replace(renaming);
		Assignment a = andR.anySat();
		
		assertTrue(a.holds(x3));
		assertTrue(a.holds(x2));	// 2 not replaced
	}
	
	@Test
	public void testReplace4() {
		BDD and = x1.and(x2);

		// rename 1->3, 2->5
		renaming.put(1, 3);
		renaming.put(2, 5);
		
		and.replaceWith(renaming);
		Assignment a = and.anySat();
		
		assertTrue(a.holds(x3));
		assertTrue(a.holds(x5));
	}
	
	@Test
	public void testReplace5() {
		BDD and = x1.and(x2);

		// rename 1->5, 2->4
		// this subverts order!
		renaming.put(1, 5);
		renaming.put(2, 4);
		
		and.replaceWith(renaming);
		Assignment a = and.anySat();
		
		assertTrue(a.holds(x5));
		assertTrue(a.holds(x4));
		// TODO assert 4 before 5
		
//		factory.printNodeTable();
	}

	@Test (expected=ReplacementWithExistingVarException.class)
	public void testReplace6() {
		BDD and = x1.and(x2);

		renaming.put(1, 2);
		
		and.replaceWith(renaming);
	}
	
	@Test (expected=ReplacementWithExistingVarException.class)
	public void testReplace7() {
		BDD and = x1.and(x2);

		renaming.put(2, 1);
		
		and.replaceWith(renaming);
	}

	@Test
	public void testCompose() {
		BDD and = x1.and(x2);
		BDD comp = and.compose(x3, 1);

		assertCompose(comp, and, x3, 1);
	}

	private void assertCompose(BDD compose, BDD bdd, BDD other, int var) {
		BDD expected = other.and(bdd.restrict(var, true))
				.or(other.not().and(bdd.restrict(var, false)));
		
		assertTrue(compose.isEquivalentTo(expected));
	}

	@Test
	public void testSimplify() {
		BDD and = x1.and(x2);
		BDD or = and.or(x3);
		
		BDD d = x1.nand(x2);

		BDD simplified = or.simplify(d);

		assertTrue(or.and(d).isEquivalentTo(simplified.and(d)));
	}
	
	@Test
	public void testIte() {
		BDD biimp = x1.biimp(x2);
		BDD or = biimp.or(x3);
		BDD xor = x2.xor(x4);
		
		BDD ite = biimp.ite(or, xor);
		BDD expected = biimp.and(or).or(biimp.not().and(xor));

		assertTrue(ite.isEquivalentTo(expected));
	}
}
