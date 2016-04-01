package com.juliasoft.beedeedee.factories;

import static org.junit.Assert.*;

import org.junit.Test;

public class IntegrityCheckFactoryTest {

	@Test
	public void testChecksum() {
		IntegrityCheckFactory factory = new IntegrityCheckFactory(10, 10, 0, false);
		long checksum1 = factory.computeChecksum();
		factory.makeVar(2);
		long checksum2 = factory.computeChecksum();
		assertNotEquals(checksum1, checksum2);
	}

}
