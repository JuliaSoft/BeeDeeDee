package com.juliasoft.beedeedee.er;

import java.util.Collection;

import com.juliasoft.beedeedee.bdd.BDD;
import com.juliasoft.beedeedee.factories.Factory;

/**
 * A BDD factory using the ER representation.
 */
public class ERFactory extends Factory {

	private Factory factory;

	public ERFactory(int utSize, int cacheSize) {
		super(utSize, cacheSize);
		factory = Factory.mk(utSize, cacheSize);
	}

	@Override
	public BDD makeVar(int i) {
		return new BDDER(factory.makeVar(i));
	}

	@Override
	public BDD makeNotVar(int i) {
		return new BDDER(factory.makeNotVar(i));
	}

	@Override
	public BDD makeZero() {
		return new BDDER(factory.makeZero());
	}

	@Override
	public BDD makeOne() {
		return new BDDER(factory.makeOne());
	}

	@Override
	public int nodeCount(Collection<BDD> bdds) {
		// TODO Auto-generated method stub
		throw new RuntimeException("Not yet implemented");
	}
}
