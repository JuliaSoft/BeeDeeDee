package com.juliasoft.beedeedee.examples.queens;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.juliasoft.beedeedee.bdd.Assignment;
import com.juliasoft.beedeedee.bdd.BDD;
import com.juliasoft.beedeedee.bdd.UnsatException;
import com.juliasoft.beedeedee.factories.Factory;

public class QueenSolver extends Thread {
	private final Object lockForIndices = new Object();
	private final Lock mergeSemaphore = new ReentrantLock();
	private final int N;
	private final boolean parallel;
	private final Factory factory;
	private final BDD[][] X; /* BDD variable array */
	private int i;
	private int j;

	public QueenSolver(int N, boolean parallel, Factory factory) {
		super("Queens solver for size " + N);

		this.N = N;
		this.parallel = parallel;
		this.factory = factory;
		this.X = new BDD[N][N];
	}

	@Override
	public void run() {
		buildVariables();

		BDD queen = oneQueenInEachRow();

		Thread[] slaves = spawnSlaves(queen);
		processPairs(queen);
		awaitSlaves(slaves, queen);

		printSolution(queen);
		
		queen.free();
	}

	private Thread[] spawnSlaves(final BDD queen) {
		class Processor extends Thread {
	
			@Override
			public void run() {
				processPairs(queen);
			}
		}

		if (parallel) {
			Thread[] slaves = new Thread[Runtime.getRuntime().availableProcessors()];
			System.out.println("spawning " + slaves.length + " slaves");

			for (int pos = 0; pos < slaves.length; pos++)
				(slaves[pos] = new Processor()).start();

			return slaves;
		}
		else
			return null;
	}

	private void awaitSlaves(Thread[] slaves, BDD queen) {
		if (parallel)
			for (Thread slave: slaves)
				try {
					slave.join();
				}
				catch (InterruptedException e) {}
	}

	private void processPairs(BDD queen) {
		BDD accumulator = factory.makeOne();
		BDD constraintForPair;

		do {
			constraintForPair = constraintForNextPair();
			if (constraintForPair != null) {
				accumulator.andWith(constraintForPair);
				int count = accumulator.nodeCount();
				if (count > 7500) {
					if (parallel)
						mergeSemaphore.lock();
					queen.andWith(accumulator);
					if (parallel)
						mergeSemaphore.unlock();
					accumulator = factory.makeOne();
				}
			}
		}
		while (constraintForPair != null);

		if (parallel)
			mergeSemaphore.lock();
		queen.andWith(accumulator);
		if (parallel)
			mergeSemaphore.unlock();
	}

	private BDD constraintForNextPair() {
		int myI, myJ;

		synchronized (lockForIndices) {
			myI = i;
			myJ = j;

			if (i == N)
				return null;
			else if (j < N - 1)
				j++;
			else {
				j = 0;
				++i;
			}
		}

		BDD accumulator = factory.makeOne();
		accumulator.andWith(noMoreThanOneInEachColumn(myI, myJ));
		accumulator.andWith(noMoreThanOneInEachRow(myI, myJ));
		accumulator.andWith(noMoreThanOneInEachUpRightDiagonal(myI, myJ));
		accumulator.andWith(noMoreThanOneInEachDownRightDiagonal(myI, myJ));

		return accumulator;
	}

	private BDD noMoreThanOneInEachDownRightDiagonal(int i, int j) {
		BDD d = factory.makeOne();
		for (int k = 0; k < N; k++)
			if (k != i) {
				int ll = i + j - k;
				if (ll >= 0 && ll < N)
					d.andWith(X[i][j].nand(X[k][ll]));
			}

		return d;
	}

	private BDD noMoreThanOneInEachUpRightDiagonal(int i, int j) {
		BDD c = factory.makeOne();
		for (int k = 0; k < N; k++)
			if (k != i) {
				int ll = k - i + j;
				if (ll >= 0 && ll < N)
					c.andWith(X[i][j].nand(X[k][ll]));
			}

		return c;
	}

	private BDD noMoreThanOneInEachRow(int i, int j) {
		BDD b = factory.makeOne();
		for (int k = 0; k < N; k++)
			if (k != i)
				b.andWith(X[i][j].nand(X[k][j]));

		return b;
	}

	private BDD noMoreThanOneInEachColumn(int i, int j) {
		BDD a = factory.makeOne();
		for (int l = 0; l < N; l++)
			if (l != j)
				a.andWith(X[i][l].nand(X[i][j]));

		return a;
	}

	private BDD oneQueenInEachRow() {
		BDD constraint = factory.makeOne();

		for (int i = 0; i < N; i++) {
			BDD e = factory.makeZero();
			for (int j = 0; j < N; j++)
				e.orWith(X[i][j].copy());	// 'or' with a copy

			constraint.andWith(e);
		}

		return constraint;
	}

	private void printSolution(BDD queen) {
		Assignment assignment;

		try {
			assignment = queen.anySat();
		}
		catch (UnsatException e) {
			assignment = null;
		}

		StringBuilder sb = new StringBuilder();
		sb.append("\n**************************************************\n\n");
		sb.append("Built BDD for " + N + "-queens.\n");
		sb.append("There are " + queen.satCount(N * N - 1) + " solutions.\n");
		if (assignment != null)
			printAssignment(sb, assignment);
		else
			sb.append("Unsatisfiable");

		sb.append("\n**************************************************\n");

		System.out.println(sb);

		
		/*
		System.out.println("Finding all assignments...\n");
		for (Assignment a : queen.allSat()) {
			printAssignment(a);
			System.out.println();
		}
		*/
	}

	private void buildVariables() {
		for (int i = 0; i < N; i++)
			for (int j = 0; j < N; j++)
				X[i][j] = factory.makeVar(i * N + j);
	}

	private void printAssignment(StringBuilder sb, Assignment assignment) {
		sb.append("Here is one:\n\n");
		for (int i = 0; i < N; i++) {
			for (int j = 0; j < N; j++)
				sb.append("  " + (assignment.holds(X[i][j]) ? 'Q' : '-'));

			sb.append("\n");
		}
	}
}