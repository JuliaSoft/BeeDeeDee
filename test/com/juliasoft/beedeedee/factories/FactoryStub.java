package com.juliasoft.beedeedee.factories;

import com.juliasoft.beedeedee.bdd.BDD;


public class FactoryStub extends ResizingAndGarbageCollectedFactoryImpl {

	private int[] handles = new int[10];
	
	FactoryStub(int utSize, int cacheSize) {
		super(utSize, cacheSize);
	}

	@Override
	public int nodesCount() {
		return 0;
	}

	@Override
	public void printStatistics() {
	}

//	@Override
//	public BDD makeVar(int i) {
//		return null;
//	}

	@Override
	public void printNodeTable() {
	}

	@Override
	public BDD makeZero() {
		return null;
	}

	@Override
	public BDD makeOne() {
		return null;
	}

	@Override
	public void gc() {
	}

	void insertHandles(int... handles) {
		this.handles = handles;
	}
}