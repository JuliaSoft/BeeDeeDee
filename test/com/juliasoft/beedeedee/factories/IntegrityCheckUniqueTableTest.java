package com.juliasoft.beedeedee.factories;

import static org.junit.Assert.*;

import org.junit.Test;

public class IntegrityCheckUniqueTableTest {

	@Test
	public void test() {
		IntegrityCheckUniqueTable ut = new IntegrityCheckUniqueTable(10, 10, null);
		int bdd = ut.get(1, 2, 3);
		System.err.println(ut.var(bdd));
	}

}
