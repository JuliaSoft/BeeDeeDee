package com.juliasoft.beedeedee.ger;

import java.util.Collection;

import com.juliasoft.beedeedee.bdd.BDD;
import com.juliasoft.beedeedee.factories.Factory;
import com.juliasoft.beedeedee.factories.ResizingAndGarbageCollectedFactory;
import com.juliasoft.beedeedee.factories.ResizingAndGarbageCollectedFactory.GarbageCollectionListener;
import com.juliasoft.beedeedee.factories.ResizingAndGarbageCollectedFactory.ResizeListener;

/**
 * A BDD factory using the GER representation.
 */
public class GERFactory extends Factory {

	private ResizingAndGarbageCollectedFactory factory;
	private VarsCache varsCache;

	public GERFactory(int utSize, int cacheSize) {
		factory = Factory.mkResizingAndGarbageCollected(utSize, cacheSize);
		varsCache = new VarsCache(cacheSize);
	}

	@Override
	public int nodesCount() {
		return factory.nodesCount();
	}

	@Override
	public BDD makeVar(int i) {
		return new BDDGer(factory.makeVar(i), varsCache);
	}

	@Override
	public BDD makeNotVar(int i) {
		return new BDDGer(factory.makeNotVar(i), varsCache);
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
		return new BDDGer(factory.makeZero(), varsCache);
	}

	@Override
	public BDD makeOne() {
		return new BDDGer(factory.makeOne(), varsCache);
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

	public double setMinFreeNodes(double minFreeNodes) {
		return factory.setMinFreeNodes(minFreeNodes);
	}

	public int setMaxIncrease(int maxIncrease) {
		return factory.setMaxIncrease(maxIncrease);
	}

	public double setIncreaseFactor(double increaseFactor) {
		return factory.setIncreaseFactor(increaseFactor);
	}

	public double setCacheRatio(double cacheRatio) {
		return factory.setCacheRatio(cacheRatio);
	}

	public void setGarbageCollectionListener(GarbageCollectionListener listener) {
		factory.setGarbageCollectionListener(listener);
	}

	public void setResizeListener(ResizeListener listener) {
		factory.setResizeListener(listener);
	}

}
