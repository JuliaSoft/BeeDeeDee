package com.juliasoft.beedeedee.ger;

import java.util.Collection;

import com.juliasoft.beedeedee.bdd.BDD;
import com.juliasoft.beedeedee.factories.Factory;
import com.juliasoft.beedeedee.factories.ResizingAndGarbageCollectedFactory;
import com.juliasoft.beedeedee.factories.ResizingAndGarbageCollectedFactory.GarbageCollectionListener;
import com.juliasoft.beedeedee.factories.ResizingAndGarbageCollectedFactory.ResizeListener;

/**
 * A BDD factory using the ER representation.
 */
public class ERFactory extends Factory {

	private ResizingAndGarbageCollectedFactory factory;

	public ERFactory(int utSize, int cacheSize) {
		factory = Factory.mkResizingAndGarbageCollected(utSize, cacheSize);
	}

	@Override
	public int nodesCount() {
		return factory.nodesCount();
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
	public void printStatistics() {
		factory.printStatistics();
	}

	@Override
	public void printNodeTable() {
		factory.printNodeTable();
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
