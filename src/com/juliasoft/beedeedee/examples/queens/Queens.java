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
package com.juliasoft.beedeedee.examples.queens;

import com.juliasoft.beedeedee.bdd.Assignment;
import com.juliasoft.beedeedee.bdd.BDD;
import com.juliasoft.beedeedee.factories.Factory;

public class Queens {
	private static int N = 11;
	private static int utSize = 1000 * 1000;
	private static int cacheSize = 100000;
	private static BDD[][] X; /* BDD variable array */

	public static void main(String[] args) {

		/*
		 * override defaults
		 */
		
		if (args.length > 0) {
			N = Integer.parseInt(args[0]);
		}
		
		if (args.length > 1) {
			utSize = Integer.parseInt(args[1]);
		}
		
		if (args.length > 2) {
			cacheSize = Integer.parseInt(args[2]);
		}

//		Factory factory = Factory.mk(utSize, cacheSize);
		Factory factory = Factory.mkER(utSize, cacheSize);
//		Factory factory = new IntegrityCheckFactory(utSize, cacheSize, true);
		BDD queen = factory.makeOne();

		int i, j;

		/* Build variable array */
		System.out.println("Creating variables...");
		X = new BDD[N][N];
		
		
		for (i = 0; i < N; i++) {
			for (j = 0; j < N; j++) {
				X[i][j] = factory.makeVar(i * N + j);
			}
		}

		/* Place a queen in each row */
		System.out.println("Adding row constraints...");
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
				System.out.print("Adding placement constraints for position " + i + "," + j + "    \r");
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
System.out.println("\n\n" + factory.bddCount() + "\n\n");
//System.out.println(factory.toDot());
		System.out.println('\n');
//		System.out.println("\n" + queen + " " + factory.nodesCount());
		factory.printStatistics();
		long count = queen.satCount();
		System.out.println("There are " + count + " solutions.");
//factory.printUT();
		if (count > -1234) {
			System.out.println("Finding a satisfying assignment...\n");
			printAssignment(queen.anySat());
		}
		/*
		System.out.println("Finding all assignments...\n");
		for (Assignment a : queen.allSat()) {
			printAssignment(a);
			System.out.println();
		}
		*/
		/*
		System.out.println("Replacing, worst case...");
		Map<Integer, Integer> renaming = new HashMap<>();
		for (j = N*N, i = j - 1; i >= 0; i--, j++) {
			renaming.put(i, j);
		}
		long start = System.currentTimeMillis();
		queen.replace(renaming);
		System.out.println("Done, ms: " + (System.currentTimeMillis() - start));
		
		System.out.println("Exist, all vars...");
		BDD vars = factory.makeOne();
		for (j = N*N; j < 2*N*N; j++) {
			vars.andWith(factory.makeVar(j));
		}
		start = System.currentTimeMillis();
		queen.exist(vars);
		factory.done();
		Executors.shutdown();
		System.out.println("Done, ms: " + (System.currentTimeMillis() - start));*/
		factory.done();
	}

	private static void printAssignment(Assignment assignment) {
		for (int i = 0; i < N; i++) {
			for (int j = 0; j < N; j++)
				System.out.print("  " + (assignment.holds(X[i][j]) ? 'Q' : '-'));

			System.out.println();
		}
	}
}