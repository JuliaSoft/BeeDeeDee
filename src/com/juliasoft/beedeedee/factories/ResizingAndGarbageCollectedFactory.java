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

/**
 * A factory with automatic resizing and garbage collection.
 */

public abstract class ResizingAndGarbageCollectedFactory extends Factory {

	/**
	 * Runs the garbage collection.
	 */

	public abstract void gc();

	/**
	 * Sets the maximal increase for the number nodes in the table of nodes
	 * of this factory.
	 *
	 * @param maxIncrease the maximal increase
	 * @return the old maximal increase
	 */

	public abstract int setMaxIncrease(int maxIncrease);

	/**
	 * Sets the factor by which the table of nodes is increased at each
	 * resize operation. The max increase coefficient still applies.
	 *
	 * @param increaseFactor the factor
	 * @return the old increase factor
	 */

	public abstract double setIncreaseFactor(double increaseFactor);

	/**
	 * Sets the cache ratio for the operator caches. When the node table grows,
	 * operator caches will also grow to maintain the ratio.
	 *
	 * @param cacheRatio the cache ratio
	 * @return the old cache ratio
	 */

	public abstract double setCacheRatio(double cacheRatio);

	/**
	 * Sets the minimum percentage of nodes to be reclaimed after a garbage collection.
	 * If this percentage is not reclaimed, the node table will be grown.
	 * The range of x is 0..1. The default is .20.
	 *
	 * @param minFreeNodes the percentage
	 * @return the old percentage
	 */

	public abstract double setMinFreeNodes(double minFreeNodes);

	/**
	 * Sets the listener of garbage collection operations.
	 *
	 * @param listener the listener
	 */

	public abstract void setGarbageCollectionListener(GarbageCollectionListener listener);

	/**
	 * Sets the listener of resize operations.
	 *
	 * @param listener the listener
	 */

	public abstract void setResizeListener(ResizeListener listener);

	public static interface GarbageCollectionListener {

		/**
		 * Called when a garbage collection operation is about to start.
		 * 
		 * @param num the progressive number of the garbage collection operation
		 * @param size the number of nodes in the garbage collected table
		 * @param free the number of free nodes after the operation
		 * @param totalTime the cumulative garbage collection time up to now
		 */

		public void onStart(int num, int size, int free, long totalTime);

		/**
		 * Called when a garbage collection operation has been performed.
		 * 
		 * @param num the progressive number of the garbage collection operation
		 * @param size the number of nodes in the garbage collected table
		 * @param free the number of free nodes after the operation
		 * @param time the time required for the garbage collection
		 * @param totalTime the cumulative garbage collection time up to now
		 */

		public void onStop(int num, int size, int free, long time, long totalTime);
	}

	public static interface ResizeListener {

		/**
		 * Called when a resize operation is about to start.
		 * 
		 * @param num the progressive number of the resize operation
		 * @param oldSize the old size of the table
		 * @param newSize the new size of the table
		 * @param totalTime the cumulative resize time up to now
		 */

		public void onStart(int num, int oldSize, int newSize, long totalTime);

		/**
		 * Called when a resize operation has been performed.
		 * 
		 * @param num the progressive number of the resize operation
		 * @param oldSize the old size of the table
		 * @param newSize the new size of the table
		 * @param time the time required for the resize
		 * @param totalTime the cumulative resize time up to now
		 */

		public void onStop(int num, int oldSize, int newSize, long time, long totalTime);
	}
}
