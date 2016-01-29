package com.juliasoft.beedeedee.ger;

import java.util.Collection;

import com.juliasoft.beedeedee.bdd.BDD;
import com.juliasoft.beedeedee.factories.Factory;

/**
 * A BDD factory using the GER representation.
 */
public class GERFactory extends Factory {

	private Factory factory;

	public GERFactory(int utSize, int cacheSize) {
		factory = Factory.mkResizingAndGarbageCollected(utSize, cacheSize);
	}

	@Override
	public int nodesCount() {
		return factory.nodesCount();
	}

	@Override
	public BDD makeVar(int i) {
		return new BDDGer(factory.makeVar(i));
	}

	@Override
	public BDD makeNotVar(int i) {
		return new BDDGer(factory.makeNotVar(i));
	}

	@Override
	public void printStatistics() {
		factory.printStatistics();
	}

	@Override
	public void printNodeTable() {
		factory.printNodeTable();
	}

	@Override
	public BDD makeZero() {
		return new BDDGer(factory.makeZero());
	}

	@Override
	public BDD makeOne() {
		return new BDDGer(factory.makeOne());
	}

	@Override
	public int nodeCount(Collection<BDD> bdds) {
		// TODO Auto-generated method stub
		throw new RuntimeException("Not yet implemented");
	}

	@Override
	public void done() {
		factory.done();
	}

	@Override
	public int getMaxVar() {
		return factory.getMaxVar();
	}

	@Override
	public int bddCount() {
		return factory.bddCount();
	}

}
