package com.juliasoft.beedeedee.ger;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.BitSet;

import org.junit.Before;
import org.junit.Test;

import com.juliasoft.beedeedee.bdd.BDD;

public class VarsCacheTest {

	private VarsCache varsCache;

	@Before
	public void setUp() {
		varsCache = new VarsCache(10);
	}

	@Test
	public void testGet1() {
		BDD f = mock(BDD.class);
		when(f.hashCodeAux()).thenReturn(1);

		BitSet s = new BitSet();
		s.set(1, 2);
		BitSet i = new BitSet();

		assertNull(varsCache.get(f, s, i, false));
	}

	@Test
	public void testGet2() {
		BDD f = mock(BDD.class);
		when(f.hashCodeAux()).thenReturn(1);

		BitSet s = new BitSet();
		s.set(1, 2);
		BitSet i = new BitSet();
		BitSet expectedResult = new BitSet();
		expectedResult.set(1);
		varsCache.put(f, s, i, false, expectedResult);

		BitSet result = varsCache.get(f, s, i, false);
		assertEquals(expectedResult, result);
	}

	@Test
	public void testGet3() {
		BDD f = mock(BDD.class);
		when(f.hashCodeAux()).thenReturn(5);

		BitSet s = new BitSet();
		BitSet i = new BitSet();
		i.set(5);
		BitSet expectedResult = new BitSet();
		expectedResult.set(1, 2);
		varsCache.put(f, s, i, false, expectedResult);

		BitSet result = varsCache.get(f, s, i, false);
		assertEquals(expectedResult, result);
	}

}
