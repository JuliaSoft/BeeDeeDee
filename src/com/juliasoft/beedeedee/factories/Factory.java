/* 
  Copyright 2014 Julia s.r.l.
    
  This file is part of BeeDeeDee.

  BeeDeeDee is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  BeeDeeDee is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with BeeDeeDee.  If not, see <http://www.gnu.org/licenses/>.
*/
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

	/**
	 * Constructs a factory with automatic resizing and garbage collection.
	 * 
	 * @param utSize the initial size of the node table
	 * @param cacheSize the size of the caches
	 * @param numberOfPreallocatedVars the number of variables to preallocate
	 * @return an instance of the factory
	 */

	public static ResizingAndGarbageCollectedFactory mkResizingAndGarbageCollected(int utSize, int cacheSize, int numberOfPreallocatedVars) {
		return new ResizingAndGarbageCollectedFactoryImpl(utSize, cacheSize, numberOfPreallocatedVars);
	}

	/**
	 * Call this method when the factory is no longer needed.
	 */
	public abstract void done();

	/**
	 * @return the maximum variable index used so far
	 */
	public abstract int getMaxVar();

	/**
	 * @return the number of non-freed BDD instances created so far
	 */
	public abstract int bddCount();
}
