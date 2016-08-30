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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import com.juliasoft.beedeedee.factories.Factory;
import com.juliasoft.beedeedee.factories.Factory.GarbageCollectionListener;
import com.juliasoft.beedeedee.factories.Factory.ResizeListener;
import com.juliasoft.julia.checkers.nullness.NonNull;
import com.juliasoft.utils.concurrent.Executors;

/**
 * Example program computing more n-queens problem BDDs in parallel.
 */
public class MultiQueens {
	private static int utSize = 1000 * 1000;
	private static int cacheSize = 100000;
	private static boolean parallel = false; // does each single queen solution must be computed in parallel
	private static @NonNull Factory factory;

	public static void main(String[] args) throws InterruptedException {
		ArrayList<Integer> ens = processArgs(args);
		initFactory();
		buildSolutions(ens);
		cleanFactory();
	}

	private static ArrayList<Integer> processArgs(String[] args) {
		if (args.length < 1)
			System.out.println("Usage: java -jar beedeedee.jar N1 N2 ... [-u<TableSize>] [-c<CacheSize>] [-parallel]"
				+ "\nwhere each N is the size of an N-queens problem\n");
		
		ArrayList<Integer> ens = new ArrayList<Integer>();
		
		for (String arg: args) {
			if (arg.startsWith("-u"))
				utSize = Integer.parseInt(arg.substring(2));
			else if (arg.startsWith("-c"))
				cacheSize = Integer.parseInt(arg.substring(2));
			else if (arg.equals("-parallel"))
				parallel = true;
			else
				try {
					ens.add(Integer.parseInt(arg));
				}
				catch (NumberFormatException e) {
					System.out.println("unrecognized option " + arg);
					System.out.println("Usage: java -jar beedeedee.jar N1 N2 ... [-u<TableSize>] [-c<CacheSize>] [-parallel]"
						+ "\nwhere each N is the size of an N-queens problem\n");
				}
		}

		return ens;
	}

	private static void buildSolutions(ArrayList<Integer> ens) throws InterruptedException {
		Set<QueenSolver> queens = new HashSet<QueenSolver>();

		for (Integer n: ens) {
			QueenSolver queen = new QueenSolver(n, parallel, factory);
			queen.start();
			queens.add(queen);
		}

		for (QueenSolver queen: queens)
			queen.join();
	}

	private static void initFactory() {
		//factory = Factory.mk(utSize, cacheSize);
		factory = Factory.mkER(utSize, cacheSize);
		
		factory.setGarbageCollectionListener(new GarbageCollectionListener() {
			@Override
			public void onStart(int num, int size, int free, long totalTime) {
				System.out.print("GC " + (num + 1) + "...");// + size + " " + free + " " + totalTime);
			}
			
			@Override
			public void onStop(int num, int size, int free, long time, long totalTime) {
				System.out.println(" Done. Size:" + size + ". Free:" + free + ". Time:" + time + "/" + totalTime);
			}
		});
	
		factory.setResizeListener(new ResizeListener() {
			@Override
			public void onStart(int num, int oldSize, int newSize, long totalTime) {
				System.out.print("Resize " + (num + 1) + "...");// + num + ". " + oldSize + " " + newSize + " " + totalTime);
			}
			
			@Override
			public void onStop(int num, int oldSize, int newSize, long time, long totalTime) {
				System.out.println(" Done. Old size:" + oldSize + ". New size:" + newSize + ". Time:" + time + "/" + totalTime);
			}
		});
		
		factory.setCacheRatio(0);
		factory.setMaxIncrease(10000000);
	}

	private static void cleanFactory() {
		factory.done();
		Executors.shutdown();
	}
}