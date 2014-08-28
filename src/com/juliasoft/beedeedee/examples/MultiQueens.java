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
package com.juliasoft.beedeedee.examples;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import checkers.nullness.quals.Inner0NonNull;
import checkers.nullness.quals.Inner1NonNull;
import checkers.nullness.quals.NonNull;

import com.juliasoft.beedeedee.bdd.Assignment;
import com.juliasoft.beedeedee.bdd.BDD;
import com.juliasoft.beedeedee.bdd.UnsatException;
import com.juliasoft.beedeedee.factories.Factory;
import com.juliasoft.beedeedee.factories.ResizingAndGarbageCollectedFactory;
import com.juliasoft.beedeedee.factories.ResizingAndGarbageCollectedFactory.GarbageCollectionListener;
import com.juliasoft.beedeedee.factories.ResizingAndGarbageCollectedFactory.ResizeListener;

/**
 * Example program computing more n-queens problem BDDs in parallel.
 */
public class MultiQueens {

	private static int utSize = 1000 * 1000;
	private static int cacheSize = 100000;
	private static @NonNull ResizingAndGarbageCollectedFactory factory;

	public static void main(String[] args) {
		if (args.length < 1) {
			System.out.println("Usage: java -jar beedeedee.jar N1 N2 ... [-u<TableSize>] [-c<CacheSize>]"
					+ "\nwhere each N is the size of an N-queens problem.");
		}
		
		ArrayList<Integer> ens = new ArrayList<Integer>();
		
		for (String a : args) {
			if (a.startsWith("-u")) {
				utSize = Integer.parseInt(a.substring(2));
			}
			else if (a.startsWith("-c")) {
				cacheSize = Integer.parseInt(a.substring(2));
			}
			else {
				ens.add(Integer.parseInt(a));
			}
		}

		factory = Factory.mkResizingAndGarbageCollected(utSize, cacheSize);
		
		factory.setGarbageCollectionListener(new GarbageCollectionListener() {
			@Override
			public void onStart(int num, int size, int free, long totalTime) {
				System.out.print("GC " + (num+1) + "...");// + size + " " + free + " " + totalTime);
			}
			
			@Override
			public void onStop(int num, int size, int free, long time, long totalTime) {
				System.out.println("Done. Size:" + size + ". Free:" + free + ". Time:" + time + "/" + totalTime);
			}
		});

		factory.setResizeListener(new ResizeListener() {
			@Override
			public void onStart(int num, int oldSize, int newSize, long totalTime) {
				System.out.print("Resize " + (num+1) + "...");// + num + ". " + oldSize + " " + newSize + " " + totalTime);
			}
			
			@Override
			public void onStop(int num, int oldSize, int newSize, long time, long totalTime) {
				System.out.println(" Done. Old size:" + oldSize + ". New size:" + newSize + ". Time:" + time + "/" + totalTime);
			}
		});
		
		factory.setCacheRatio(0);
		factory.setMaxIncrease(10000000);
		
		Set<QueensThread> queens = new HashSet<QueensThread>();

		for (Integer n : ens) {
			QueensThread queen = new QueensThread(n);
			queens.add(queen);
			queen.start();
		}

		for (QueensThread queen: queens)
			try {
				queen.join();
			}
			catch (InterruptedException e) {}

		factory.done();
	}


	private static class QueensThread extends Thread {

		private final int N;
		private final @Inner0NonNull @Inner1NonNull BDD[][] X; /* BDD variable array */

		public QueensThread(int N) {
			super("Queens solver for size " + N);

			this.N = N;
			this.X = new BDD[N][N];
		}

		@Override
		public void run() {
			super.run();
			queens();
		}
		
		private void queens() {
			BDD queen = factory.makeOne();

			int i, j;

			/* Build variable array */
			//System.out.println("Creating variables...");
			for (i = 0; i < N; i++) {
				for (j = 0; j < N; j++) {
					X[i][j] = factory.makeVar(i * N + j);
				}
			}

			/* Place a queen in each row */
			//System.out.println("Adding row constraints...");
			for (i = 0; i < N; i++) {
				BDD e = factory.makeZero();
				for (j = 0; j < N; j++) {
					e.orWith(X[i][j].copy());	// 'or' with a copy
				}
				queen.andWith(e);
			}

			/* Build requirements for each variable(field) */
			for (i = 0; i < N; i++)
				for (j = 0; j < N; j++) {
					synchronized (MultiQueens.QueensThread.class) {
						//System.out.println("Adding placement constraints for position " + i + "," + j + "    \r");
					}
					BDD a = factory.makeOne(), b = factory.makeOne(), c = factory.makeOne(), d = factory.makeOne();
					int k, l;

					/* No one in the same column */
					for (l = 0; l < N; l++) {
						if (l != j) {
							BDD nand = X[i][l].nand(X[i][j]);
							a.andWith(nand);
						}
					}

					/* No one in the same row */
					for (k = 0; k < N; k++) {
						if (k != i) {
							BDD nand = X[i][j].nand(X[k][j]);
							b.andWith(nand);
						}
					}

					/* No one in the same up-right diagonal */
					for (k = 0; k < N; k++) {
						int ll = k - i + j;
						if (ll >= 0 && ll < N) {
							if (k != i) {
								BDD nand = X[i][j].nand(X[k][ll]);
								c.andWith(nand);
							}
						}
					}

					/* No one in the same down-right diagonal */
					for (k = 0; k < N; k++) {
						int ll = i + j - k;
						if (ll >= 0 && ll < N) {
							if (k != i) {
								BDD nand = X[i][j].nand(X[k][ll]);
								d.andWith(nand);
							}
						}
					}

					c.andWith(d);
					b.andWith(c);
					a.andWith(b);
					queen.andWith(a);
				}
			
			//long count = queen.satCount();
			
			synchronized (MultiQueens.QueensThread.class) {
				System.out.println("\n**************************************************\n");
				System.out.println("Built BDD for " + N + "-queens.");
//				System.out.println("\nqueen = " + queen + "; factory.nodesCount() = " + factory.nodesCount());
//				factory.printStatistics();
				System.out.println("There are " + queen.satCount(N * N - 1) + " solutions.");

				try {
					printAssignment(queen.anySat());
				}
				catch (UnsatException e) {
					System.out.println("Unsatisfiable");
				}

				System.out.println("\n**************************************************\n");
			}
			
			/*
			System.out.println("Finding all assignments...\n");
			for (Assignment a : queen.allSat()) {
				printAssignment(a);
				System.out.println();
			}
			*/
			
			queen.free();
		}

		private void printAssignment(Assignment assignment) {
			System.out.println("Here is one solution:\n");
			for (int i = 0; i < N; i++) {
				for (int j = 0; j < N; j++)
					System.out.print("  " + (assignment.holds(X[i][j]) ? 'Q' : '-'));

				System.out.println();
			}
		}
	}
}
