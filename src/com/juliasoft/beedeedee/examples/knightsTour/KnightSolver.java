package com.juliasoft.beedeedee.examples.knightsTour;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.juliasoft.beedeedee.bdd.BDD;
import com.juliasoft.beedeedee.factories.Factory;
import com.juliasoft.julia.checkers.nullness.Inner0NonNull;
import com.juliasoft.julia.checkers.nullness.Inner1NonNull;

/**
 * Solves the N-knights tour problem by starting from the bottom-left corner.
 * 
 * @author Fausto Spoto
 */

public class KnightSolver extends Thread {

	private final int N;
	private final Factory factory;
	private final @Inner0NonNull @Inner1NonNull BDD[][] X; /* BDD variable array */
	private final @Inner0NonNull @Inner1NonNull BDD[][] Xp; /* next BDD variable array */

	private final int availableProcessors;

	public KnightSolver(int N, boolean parallel, Factory factory) {
		super("Knights solver for size " + N);

		this.N = N;
		availableProcessors = parallel ? 4 /*Runtime.getRuntime().availableProcessors()*/ : 1;
		this.factory = factory;
		this.X = new BDD[N][N];
		this.Xp = new BDD[N][N];
	}

	@Override
	public void run() {
		buildVariables();

		BDD I = buildI();
		BDD T = buildT();
		BDD x = buildX();
		Map<Integer, Integer> renaming = buildRenaming();
		BDD R = reachableStates(I, T, x, renaming);
		printSolution(R);

		I.free();
		T.free();
		x.free();
		R.free();
	}

	private BDD reachableStates(BDD I, BDD T, BDD x, Map<Integer, Integer> renaming) {
		BDD R = factory.makeZero();
		BDD Rp = null;

		do {
			if (Rp != null)
				Rp.free();

			Rp = R;
			BDD and = T.and(R);
			BDD exist = and.exist(x);
			and.free();
			BDD replace = exist.replaceWith(renaming);

			R = I.or(replace);

			replace.free();
		}
		while (!R.isEquivalentTo(Rp));

		Rp.free();

		return R;
	}

	private void buildVariables() {
		for (int i = 0; i < N; i++)
			for (int j = 0; j < N; j++) {
				// we associate contiguous variables when they are going to be in iff
				X[i][j] = factory.makeVar(2 * (i * N + j));
				Xp[i][j] = factory.makeVar(2 * (i * N + j) + 1);

				// the following alternative is computationally explosive!
				//X[i][j] = factory.makeVar((i * N + j));
				//Xp[i][j] = factory.makeVar(N * N + (i * N + j));
			}
	}

	private Map<Integer, Integer> buildRenaming() {
		Map<Integer, Integer> renaming = new HashMap<>();

		for (int i = 0; i < N; i++)
			for (int j = 0; j < N; j++)
				renaming.put(Xp[i][j].var(), X[i][j].var());

		return renaming;
	}

	private BDD buildX() {
		BDD result = factory.makeOne();

		for (int i = 0; i < N; i++)
			for (int j = 0; j < N; j++)
				result.andWith(X[i][j].copy());

		return result;
	}

	private BDD buildXp() {
		BDD result = factory.makeOne();

		for (int i = 0; i < N; i++)
			for (int j = 0; j < N; j++)
				result.andWith(Xp[i][j].copy());

		return result;
	}

	private BDD buildM(int i, int j, int k, int l) {
		BDD result = factory.makeOne();
		
		result.andWith(X[i][j].copy());
		result.andWith(X[k][l].not());
		result.andWith(Xp[i][j].not());
		result.andWith(Xp[k][l].copy());

		for (int ip = 0; ip < N; ip++)
			for (int jp = 0; jp < N; jp++)
				if ((ip != i || jp != j) && (ip != k || jp != l))
					result.andWith(X[ip][jp].biimp(Xp[ip][jp]));

		return result;
	}

	private BDD buildT() {
		ExecutorService executorService = Executors.newCachedThreadPool();
		@SuppressWarnings("unchecked")
		Future<BDD>[] submitted = new Future[availableProcessors];
		for (int i = 0; i < availableProcessors; i++) {
			submitted[i] = executorService.submit(new PartialTProcessor(i));			
		}
		BDD result = factory.makeZero();
		for (int i = 0; i < availableProcessors; i++)
			try {
				result.orWith(submitted[i].get());
			}
			catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}			

		executorService.shutdown();
		return result;
	}

	private final class PartialTProcessor implements Callable<BDD> {
		private int n;

		public PartialTProcessor(int n) {
			this.n = n;
		}

		@Override
		public BDD call() throws Exception {
			BDD result = factory.makeZero();

			for (int i = n; i < N; i += availableProcessors)
				for (int j = 0; j < N; j++) {
					for (int k = i - 1; k <= i + 1; k += 2)
						for (int l = j - 2; l <= j + 2; l += 4)
							if (k >= 0 && k < N && l >= 0 && l < N)
								result.orWith(buildM(i, j, k, l));

					for (int k = i - 2; k <= i + 2; k += 4)
						for (int l = j - 1; l <= j + 1; l += 2)
							if (k >= 0 && k < N && l >= 0 && l < N)
								result.orWith(buildM(i, j, k, l));
				}

			return result;
		}
	}

	private BDD buildI() {
		BDD result = factory.makeOne();

		result.andWith(X[0][0].copy());

		for (int i = 0; i < N; i++)
			for (int j = 0; j < N; j++)
				if (i != 0 || j != 0)
					result.andWith(X[i][j].not());

		return result;
	}

	private void printSolution(BDD knight) {
		// we fix the value of the x' variables, so that they do not count in the number of solutions
		BDD fixXp = knight.and(buildXp());

		// this is the number of cells that can be reached
		long reachable = fixXp.satCount(2 * N * N - 1);

		StringBuilder sb = new StringBuilder();
		sb.append("\n**************************************************\n\n");
		sb.append("Built BDD for " + N + "-knights.\n");
		sb.append("The BDD of the reachable cells has " + reachable + " solutions.\n");
		sb.append("Hence the problem is " + (reachable == N * N ? "" : "not ") + "solvable.");
		sb.append("\n**************************************************\n");

		fixXp.free();

		System.out.println(sb);
	}
}