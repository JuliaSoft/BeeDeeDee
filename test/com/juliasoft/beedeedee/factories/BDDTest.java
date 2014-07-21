package com.juliasoft.beedeedee.factories;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.juliasoft.beedeedee.bdd.BDD;

public class BDDTest {

	private ResizingAndGarbageCollectedFactory factory;

	@Before
	public void setUp() {
		factory = new ResizingAndGarbageCollectedFactoryImpl(2, 2);
	}

	@After
	public void cleanUp() {
		factory.done();
	}

	/*
	@Test
	public void testOpWithSame() {
		BDD x1 = factory.makeVar(1);
		
		x1.orWith(x1);	// 1 L0 H1
		factory.printUT();
		assertEquals(1, factory.getRefCount(x1.getId()));
		
		BDD x2 = factory.makeVar(2);
		int idBeforeX2 = x2.getId();
		x2.impWith(x2);	// ONE
		assertEquals(0, factory.getRefCount(idBeforeX2));
		
		BDD x3 = factory.makeVar(3);
		x3.andWith(x3);	// 3 L0 H1
		assertEquals(1, factory.getRefCount(x3.getId()));

		BDD x4 = factory.makeVar(4);
		int idBeforeX4 = x4.getId();
		factory.printUT();
		System.out.println("nand");
		x4.nandWith(x4);	// 4 L1 H0
		factory.printUT();
		assertEquals(0, factory.getRefCount(idBeforeX4));
		assertEquals(1, factory.getRefCount(x4.getId()));
	}

	@Test
	public void testOpWith() {
		BDD x1 = factory.makeVar(1);
		BDD x2 = factory.makeVar(2);
		int idBeforeX1 = x1.getId();
		x1.andWith(x2);	// 1 L0 Hx2
		assertEquals(-1, x2.getId());	// freed
		assertEquals(0, factory.getRefCount(idBeforeX1));
		assertEquals(1, factory.getRefCount(x1.getId()));
		
		BDD x3 = factory.makeVar(3);
		BDD x4 = factory.makeVar(4);
		int idBeforeX3 = x3.getId();
		x3.orWith(x4);	// 3 Lx4 H1
		assertEquals(-1, x4.getId());	// freed
		assertEquals(0, factory.getRefCount(idBeforeX3));
		assertEquals(1, factory.getRefCount(x3.getId()));
		
		BDD x5 = factory.makeVar(5);
		BDD x6 = factory.makeVar(6);
		int idBeforeX5 = x5.getId();
		x5.impWith(x6);	// 5 L1 Hx6
		assertEquals(-1, x6.getId());	// freed
		assertEquals(0, factory.getRefCount(idBeforeX5));
		assertEquals(1, factory.getRefCount(x5.getId()));

		BDD x7 = factory.makeVar(7);
		BDD x8 = factory.makeVar(8);
		int idBeforeX7 = x7.getId();
		x7.nandWith(x8);	// 7 L1 H!x8
		assertEquals(-1, x8.getId());	// freed
		assertEquals(0, factory.getRefCount(idBeforeX7));
		assertEquals(1, factory.getRefCount(x7.getId()));
	}
	
	@Test
	public void testNegWith() {
		BDD x = factory.makeVar(1);
	
		int idBefore = x.getId();
		x.negWith();
		assertEquals(0, factory.getRefCount(idBefore));
		assertEquals(1, factory.getRefCount(x.getId()));
	}
	*/
//	@Test
	private void testImpWith() {
		BDD x1 = factory.makeVar(1);
		BDD x2 = factory.makeVar(2);
		
		factory.printNodeTable();
	
		BDD imp = x1.impWith(x2);
		
		factory.printNodeTable();
		x2 = factory.makeVar(2);
		imp.andWith(x2);
		factory.printNodeTable();
	}
	
//	@Test
	private void testBiImpWith() {
		BDD x1 = factory.makeVar(1);
		BDD x2 = factory.makeVar(2);
		BDD x3 = factory.makeVar(3);
		BDD x4 = factory.makeVar(4);

		BDD imp13 = x1.imp(x3);
		BDD imp31 = x3.impWith(x1);

		BDD imp24 = x2.imp(x4);
		BDD imp42 = x4.impWith(x2);

		BDD biimp13 = imp13.andWith(imp31);
		BDD biimp24 = imp24.andWith(imp42);

		biimp13.andWith(biimp24);

		factory.printNodeTable();
	}
	
    @Test
    public void testLowHighEquals() {
        BDD res = factory.makeVar(1);
        System.out.println("x1");
        factory.printNodeTable();
        BDD x2 = factory.makeVar(2);
        System.out.println("x1 ... x2");
        factory.printNodeTable();

        res.orWith(x2);
        System.out.println("x1 Or x2");
        factory.printNodeTable();
        // ((ResizingUniqueTable)factory.ut).gc();

        BDD x3 = factory.makeVar(3);
        System.out.println("x1 Or x2 ... x3");
        factory.printNodeTable();

        res.andWith(x3.copy());
        System.out.println("(x1 OR x2) AND x3 ... x3");
        factory.printNodeTable();

        BDD res2 = factory.makeVar(1).notWith();
        System.out.println("(x1 OR x2) AND x3 ... x3 ... NOTx1");
        factory.printNodeTable();

        res2.andWith(x3);
        System.out.println("(x1 OR x2) AND x3 .... NOTx1 AND x3");
        factory.printNodeTable();

        res.orWith(res2);
        System.out.println("((x1 OR x2) AND x3) OR (NOTx1 AND x3)");
        factory.printNodeTable();

        // now res represents
        // ( ( 1 OR 2 ) AND 3 ) OR ( Not1 AND 3 )
        // which is equivalent to 3,
        // thus at this point the only (single!) reference must be to variable 3
        factory.gc();
        factory.printNodeTable();
    }
}