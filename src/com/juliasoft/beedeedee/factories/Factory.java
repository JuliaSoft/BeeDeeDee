package com.juliasoft.beedeedee.factories;

import java.util.Collection;

import com.juliasoft.beedeedee.bdd.BDD;

/**
 * A factory for Binary Decision Diagrams.
 */

public abstract class Factory {

	/**
	 * @return the current number of nodes in the factory
	 */
	
 	public abstract int nodesCount();

	/**
	 * Constructs a BDD representing a single variable.
	 * 
	 * @param i the number of the variable
	 * @return the variable as a BDD object 
	 */

	public abstract BDD makeVar(int i);

	/**
	 * Constructs a BDD representing the negation of a single variable.
	 * 
	 * @param i the number of the variable
	 * @return the negation of the variable as a BDD object 
	 */

	public abstract BDD makeNotVar(int i);

	public abstract void printStatistics();

	public abstract void printNodeTable();

	/**
	 * @return a BDD object representing the constant zero
	 */
	
	public abstract BDD makeZero();

	/**
	 * @return a BDD object representing the constant one
	 */
	
	public abstract BDD makeOne();

	/**
	 * Counts the nodes in a collection of BDDs.
	 * Shared nodes are counted only once.
	 * 
	 * @param bdds the collection of BDDs
	 * @return the total number of nodes
	 */
	
	public abstract int nodeCount(Collection<BDD> bdds);

	/**
	 * Constructs a factory with automatic resizing and garbage collection.
	 * 
	 * @param utSize the initial size of the node table
	 * @param cacheSize the size of the caches
	 * @return an instance of the factory
	 */

	public static ResizingAndGarbageCollectedFactory mkResizingAndGarbageCollected(int utSize, int cacheSize) {
		return new ResizingAndGarbageCollectedFactoryImpl(utSize, cacheSize);
	}

	public abstract void done();
}