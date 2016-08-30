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

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import com.juliasoft.beedeedee.factories.Factory.GarbageCollectionListener;
import com.juliasoft.beedeedee.factories.Factory.ResizeListener;
import com.juliasoft.utils.concurrent.Executors;

public class ResizingAndGarbageCollectedUniqueTable extends SimpleUniqueTable {

	/**
	 * The maximal number of nodes that is added at each resize operation.
	 */

	private volatile int maxIncrease = 1000000;

	/**
	 * The factor by which the table of nodes gets increased at each resize.
	 */

	private volatile double increaseFactor = 2;

	/**
	 * The cache ratio for the operator caches. When the node table grows,
	 * operator caches will also grow to maintain the ratio.
	 */

	private volatile double cacheRatio = 0.5;

	/**
	 * The minimum percentage of nodes to be reclaimed after a garbage collection.
	 * If this percentage is not reclaimed, the node table will be grown.
	 * The range for this value is 0..1.
	 */

	private volatile double minFreeNodes = 0.2;

	private final Factory factory;

	private long totalResizeTime;

	private final AtomicInteger hashCodeAuxCounter = new AtomicInteger();

	/**
	 * The number of resize operations performed so far.
	 */

	private int numOfResizes;

	private long totalGCTime;

	/**
	 * The number of garbage collections performed so far.
	 */

	private int numOfGCs;

	/**
	 * The garbage collection listener, if any.
	 */

	private GarbageCollectionListener gcListener;

	/**
	 * The resize collection listener, if any.
	 */

	private ResizeListener resizeListener;

	private final Object[] getLocks = new Object[1000];

	private final ReentrantLock[] gcLocks = new ReentrantLock[5000];

	private final Object[] updateLocks = new Object[1000];

	private int nextGCLocks;

	ResizingAndGarbageCollectedUniqueTable(int size, int cacheSize, Factory factory) {
		super(size, cacheSize);

		for (int pos = 0; pos < getLocks.length; pos++)
			getLocks[pos] = new Object();

		for (int pos = 0; pos < gcLocks.length; pos++)
			gcLocks[pos] = new ReentrantLock();

		for (int pos = 0; pos < updateLocks.length; pos++)
			updateLocks[pos] = new Object();

		this.factory = factory;
	}

	@Override
	public final int get(int var, int low, int high) {
		do {
			int size = this.size, pos = hash(var, low, high, size);

			int result = getOptimistic(var, low, high, pos);
			if (result >= 0)
				return result;

			Object myLock;
			synchronized (myLock = getLocks[pos % getLocks.length]) {
				// if the size changed, it means that a resize occurred while
				// we were trying to access the critical section: in that case
				// we have to recompute the hashcode, since it might have changed
				if (size == this.size || pos == hash(var, low, high))
					return expandTable(var, low, high, myLock, pos);
			}
		}
		while (true);
	}

	public ReentrantLock getGCLock() {
		return gcLocks[nextGCLocks = (nextGCLocks + 1) % gcLocks.length];
	}

	int expandTable(int var, int low, int high, Object myLock, int pos) {
		int bin = H[pos];

		if (bin < 0) {	// empty bin, created node is first
			int allocationPoint = nextPos(myLock);
			setAt(allocationPoint, var, low, high);
			// recompute hash, could have been changed by intervening resize
			// triggered by current position request
			pos = hash(var, low, high);
			return H[pos] = allocationPoint;
		}
		else	// append to collision list
			while (true) {
				if (isVarLowHigh(bin, var, low, high))
					return bin;

				int old = bin;
				if ((bin = next(bin)) < 0) {
					int allocationPoint = nextPos(myLock);
					setAt(allocationPoint, var, low, high);
					setNext(old, allocationPoint);
					return allocationPoint;
				}
			}
	}

	private int getOptimistic(int var, int low, int high, int pos) {
		int bin;

		if ((bin = H[pos]) < 0)
			return -1;
		else
			while (true) {
				if (isVarLowHigh(bin, var, low, high))
					return bin;

				if ((bin = next(bin)) < 0)
					return -1;
			}
	}

	private final Object nextPosLock = new Object();

	private final Object resizeInProgressLock = new Object();
	private volatile boolean resizeInProgress;

	private final Object gcInProgressLock = new Object();
	private volatile boolean gcInProgress;

	private int nextPos(Object myLock) {
		while (true) {
			// size cannot increase here, since resize is not allowed when a thread is here
			int size = getSize();
	
			synchronized (nextPosLock) {
				if (nextPos < size)
					return nextPos++;
			}
	
			if (!isResizeInProgress())
				resize();
			else
				try {
					myLock.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
		}
	}

	private boolean isResizeInProgress() {
		synchronized (resizeInProgressLock) {
			if (resizeInProgress)
				return true;

			resizeInProgress = true;
	
			return false;
		}
	}

	private void resize() {
		// we precompute as much as we can outside the critical section
		ResizeData data = new ResizeData(this);

		lockAllAndResize(0, data);

		// and after it
		postResize(data);
	}

	private void lockAllAndResize(int pos, ResizeData data) {
		if (pos < getLocks.length)
			synchronized (getLocks[pos]) {
				lockAllAndResize(pos + 1, data);

				// we need a notifyAll() since there might be
				// more threads waiting on the same lock
				getLocks[pos].notifyAll();
			}
		else {
			innerResize(data);
			resizeInProgress = false;
		}
	}

	protected void setAt(int where, int varNumber, int lowNode, int highNode) {
		int pos = where * getNodeSize();
		int[] table = ut;

		table[pos++] = varNumber;
		table[pos++] = lowNode;
		table[pos++] = highNode;
		table[pos++] = -1;
		table[pos] = hashCodeAuxCounter.getAndIncrement();
	}

	@Override
	public String toString() {
		ReentrantLock lock = getGCLock();
		lock.lock();
		try {
			return super.toString();
		}
		finally {
			lock.unlock();
		}
	}

	@Override
	public String toDot() {
		ReentrantLock lock = getGCLock();
		lock.lock();
		try {
			return super.toDot();
		}
		finally {
			lock.unlock();
		}
	}

	void setGarbageCollectionListener(GarbageCollectionListener listener) {
		this.gcListener = listener;
	}

	void setResizeListener(ResizeListener listener) {
		this.resizeListener = listener;
	}

	/**
	 * Performs a garbage collection of this table.
	 */

	protected void gc() {
		if (!isGCInProgress()) {
			boolean success = getAllLocksAndGC(0);

			gcInProgress = false;

			if (!success && !isResizeInProgress())
				resize();

			gcRequired = false;
		}
	}

	private boolean gcRequired;

	private boolean isGCInProgress() {
		synchronized (gcInProgressLock) {
			if (gcInProgress)
				return true;
			
			gcInProgress = true;
			return false;
		}
	}

	private boolean getAllLocksAndGC(int pos) {
		for (ReentrantLock lock: gcLocks)
			lock.lock();

		int size = getSize();
		long start = System.currentTimeMillis();

		GarbageCollectionListener listener = gcListener;
		if (listener != null)
			listener.onStart(numOfGCs, size, size - nextPos, totalGCTime);

		// find live nodes and compact the unique table
		boolean[] aliveNodes = new boolean[size];
		factory.markAliveNodes(aliveNodes);

		int collected = compactTable(aliveNodes);
	
		// update hash table
		Arrays.fill(H, -1);
		updateHashTable();

		long gcTime = System.currentTimeMillis() - start;
		totalGCTime += gcTime;
		numOfGCs++;

		listener = gcListener;
		if (listener != null)
			listener.onStop(numOfGCs, size, size - nextPos, gcTime, totalGCTime);

		for (ReentrantLock lock: gcLocks)
			lock.unlock();

		return collected > nextPos * minFreeNodes;
	}

	protected void scheduleGC() {
		this.gcRequired = true;
	}

	/**
	 * Checks if we are going to need to perform a garbage collection, and runs it if so.
	 */

	protected void gcIfAlmostFull() {
		if (gcRequired && size - nodesCount() < size * 0.02) // do we need to do gc?
			gc();
	}
	
	protected int setMaxIncrease(int maxIncrease) {
		int oldMaxIncrease = this.maxIncrease;
		this.maxIncrease = maxIncrease;

		return oldMaxIncrease;
	}

	/**
	 * Sets the factor by which the table of nodes is increased at each
	 * resize operation. The maximal increase still applies.
	 *
	 * @param increaseFactor the factor
	 * @return the old increase factor
	 */

	protected double setIncreaseFactor(double increaseFactor) {
		double oldIncreaseFactor = this.increaseFactor;
		this.increaseFactor = increaseFactor;

		return oldIncreaseFactor;
	}

	/**
	 * Sets the cache ratio for the operator caches. When the node table grows,
	 * operator caches will also grow to maintain the ratio.
	 *
	 * @param cacheRatio the cache ratio
	 * @return the old cache ratio
	 */

	protected double setCacheRatio(double cacheRatio) {
		double oldCacheRatio = this.cacheRatio;
		this.cacheRatio = cacheRatio;

		return oldCacheRatio;
	}

	/**
	 * Sets the minimum percentage of nodes to be reclaimed after a garbage collection.
	 * If this percentage is not reclaimed, the node table will be grown.
	 * The range of x is 0..1. The default is .20.
	 *
	 * @param minFreeNodes the percentage
	 * @return the old percentage
	 */

	protected double setMinFreeNodes(double minFreeNodes) {
		double oldMinFreeNodes = this.minFreeNodes;
		this.minFreeNodes = minFreeNodes;

		return oldMinFreeNodes;
	}

	private class ResizeData {
		private final long start;
		private final int oldSize;
		private final int newSize;
		private final int[] newH;
		private final int[] newUt;
		private final ComputationCache computationCache;
		private final RestrictCache restrictCache;
		private final QuantCache quantCache;
		private final ReplaceCache replaceCache;
		private final EquivCache equivCache;
		private final RenameWithLeaderCache rwlCache;
		private final SqueezeEquivCache squeezeEquivCache;

		private ResizeData(ResizingAndGarbageCollectedUniqueTable table) {
			start = System.currentTimeMillis();
			oldSize = table.getSize();
			newSize = oldSize * (table.increaseFactor - 1) > table.maxIncrease ?
				oldSize + table.maxIncrease : (int) (oldSize * table.increaseFactor);
			int oldCacheSize = table.getCacheSize();
			int newCacheSize = newSize * table.cacheRatio > oldCacheSize ?
				((int) (newSize * table.cacheRatio)) : oldCacheSize;

			ResizeListener listener = table.resizeListener;
			if (listener != null)
				listener.onStart(table.numOfResizes, oldSize, newSize, table.totalResizeTime);

			newH = new int[newSize];
			Arrays.fill(newH, -1);
			newUt = new int[newSize * getNodeSize()];

			int sizeOfSmallCaches = Math.max(1, newCacheSize / 20);
			computationCache = new ComputationCache(newCacheSize);
			restrictCache = new RestrictCache(sizeOfSmallCaches);
			replaceCache = new ReplaceCache(sizeOfSmallCaches);
			quantCache = new QuantCache(sizeOfSmallCaches);
			equivCache = new EquivCache(sizeOfSmallCaches);
			rwlCache = new RenameWithLeaderCache(sizeOfSmallCaches);
			squeezeEquivCache = new SqueezeEquivCache(sizeOfSmallCaches);
		}
	}

	private void innerResize(ResizeData data) {
		System.arraycopy(ut, 0, data.newUt, 0, nextPos * getNodeSize());

		// TODO is this instruction order mandatory according to the JMM?
		this.ut = data.newUt;
		this.H = data.newH;
		this.size = data.newSize;
		this.computationCache = data.computationCache;
		this.restrictCache = data.restrictCache;
		this.replaceCache = data.replaceCache;
		this.quantCache = data.quantCache;
		this.equivCache = data.equivCache;
		this.rwlCache = data.rwlCache;
		this.squeezeEquivCache = data.squeezeEquivCache;

		updateHashTable();
	}

	private void postResize(ResizeData data) {
		long resizeTime = System.currentTimeMillis() - data.start;
		totalResizeTime += resizeTime;
		numOfResizes++;

		ResizeListener listener = resizeListener;
		if (listener != null)
			listener.onStop(numOfResizes, data.oldSize, data.newSize, resizeTime, totalResizeTime);
	}

	void updateHashTable() {
		if (size >= 600000) {
			parallelUpdateHashTable();
			return;
		}

		for (int i = nextPos - 1, index = i * getNodeSize() + VAR_OFFSET; i >= 0; i--, index -= getNodeSize())
			// we only consider valid entries
			if (ut[index] >= 0) {
				int pos = hash(ut[index], ut[index + 1], ut[index + 2]);

				setNext(i, H[pos]);
				H[pos] = i;
			}
	}

	private final static int total = Runtime.getRuntime().availableProcessors();

	private void parallelUpdateHashTable() {

		class HashTableUpdater implements Runnable {
			private final int offset;

			private HashTableUpdater(int offset) {
				this.offset = offset;
			}

			@Override
			public void run() {

				for (int i = nextPos - 1 - offset, index = i * getNodeSize() + VAR_OFFSET; i >= 0; i -= total, index -= getNodeSize() * total)
					// we only consider valid entries
					if (ut[index] >= 0) {
						int pos = hash(ut[index], ut[index + 1], ut[index + 2]);

						synchronized (updateLocks[pos % updateLocks.length]) {
							setNext(i, H[pos]);
							H[pos] = i;
						}
					}
			}
		}

		HashTableUpdater[] slaves = new HashTableUpdater[total];
		for (int num = 0; num < total; num++)
			slaves[num] = new HashTableUpdater(num);

		Executors.parallelise(slaves);
	}

	int compactTable(boolean[] aliveNodes) {
		int collected = 0;
		int[] newPositions = new int[size];

		for (int oldCursor = 0, newCursor = 0; oldCursor < nextPos; oldCursor++)
			if (aliveNodes[oldCursor]) {
				// copy node to new position
				if (collected > 0)
					setVarLowHighHash(newCursor, var(oldCursor), newPositions[low(oldCursor)], newPositions[high(oldCursor)], hashCodeAux(oldCursor));

				newPositions[oldCursor] = newCursor++;
				aliveNodes[oldCursor] = false;
			}
			else
				collected++;

		// change indices of external BDD objects
		factory.updateIndicesOfAllBDDsCreatedSoFar(newPositions);

		synchronized (nextPosLock) {
			nextPos -= collected;
		}

		computationCache.clear();
		restrictCache.clear();
		replaceCache.clear();
		quantCache.clear();
		equivCache.clear();
		rwlCache.clear();
		squeezeEquivCache.clear();

		return collected;
	}

	protected void setVarLowHighHash(int node, int varNumber, int lowNode, int highNode, int hca) {
		int pos = node * getNodeSize();

		ut[pos++] = varNumber;
		ut[pos++] = lowNode;
		ut[pos++] = highNode;
		ut[++pos] = hca;
	}

	public EquivCache getEquivCache() {
		return equivCache;
	}

	public RenameWithLeaderCache getRWLCache() {
		return rwlCache;
	}

	public SqueezeEquivCache getSqueezeEquivCache() {
		return squeezeEquivCache;
	}
}
