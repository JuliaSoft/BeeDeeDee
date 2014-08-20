package com.juliasoft.beedeedee.factories;

import static checkers.nullness.support.NullnessAssertions.assertNonNull;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class RestrictCacheTest {

	private RestrictCache cache;

	@Before
	public void setUp() throws Exception {
		cache = new RestrictCache(1000);
	}

	@Test
	public void test() {
		assertNonNull(cache, "This is a test: setUp() must be called before");

		assertEquals(-1, cache.get(1, 2, true));
		
		cache.put(1, 2, true, 5);
		assertEquals(5, cache.get(1, 2, true));

		cache.put(6, 8, false, 15);
		assertEquals(15, cache.get(6, 8, false));
	}
}