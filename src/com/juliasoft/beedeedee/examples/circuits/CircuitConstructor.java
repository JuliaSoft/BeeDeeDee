package com.juliasoft.beedeedee.examples.circuits;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.juliasoft.beedeedee.bdd.BDD;
import com.juliasoft.beedeedee.examples.circuits.jccgen.CircuitParser;
import com.juliasoft.beedeedee.examples.circuits.jccgen.ParseException;
import com.juliasoft.beedeedee.factories.Factory;
import com.juliasoft.beedeedee.factories.Factory.GarbageCollectionListener;
import com.juliasoft.beedeedee.factories.Factory.ResizeListener;

/**
 * Parses .bench files of ITC99 benchmark circuits, and builds the transition
 * relation. See Jerry R. Burch, Edmund M. Clarke, and David E. Long.
 * Representing circuits more efficiently in symbolic model checking. In DAC,
 * pages 403–407, 1991.
 * 
 * In order to resolve compilation errors, please process the file circuit.jj
 * with JavaCC.
 */
public class CircuitConstructor {

	private static final class PartialCalculator implements Callable<BDD> {
		private int id;
		private int numberOfThreads;
		private String[] dffsArray;

		public PartialCalculator(int id, int numberOfThreads, String[] dffsArray) {
			this.id = id;
			this.numberOfThreads = numberOfThreads;
			this.dffsArray = dffsArray;
		}

		@Override
		public BDD call() throws Exception {
			BDD partialTransitionRelation = fact.makeOne();
			for (int i = id; i < dffsArray.length; i += numberOfThreads) {
				String key = dffsArray[i];
				BDD biimp = bdds.get(key).biimp(dffs.get(key));
				partialTransitionRelation.andWith(biimp);
			}
			return partialTransitionRelation;
		}
	}

	// name -> bdd map
	private static HashMap<String, BDD> bdds = new HashMap<String, BDD>();
	// name -> operation map
	private static HashMap<String, Operation> opMap;
	private static Factory fact;
	private static int varNumber = 0;
	// name -> dffs map (a dff, d flip flop, is a memory)
	private static HashMap<String, BDD> dffs = new HashMap<>();

	public static void main(String[] args)
			throws FileNotFoundException, ParseException, InterruptedException, ExecutionException {

		/*
		 * parse file args[0]
		 */
		File f = new File(args[0]);
		FileReader fr = new FileReader(f);
		CircuitParser cp = new CircuitParser(fr);
		cp.Start();

		int numberOfThreads = 1;
		/*
		 * pass number of threads in args[1] for parallel computation
		 */
		if (args.length > 1) {
			numberOfThreads = Integer.parseInt(args[1]);
		}

		/*
		 * create bdd factory, set listeners
		 */
		fact = Factory.mk(50_000_000, 100_000);
		fact.setMaxIncrease(1_000_000);

		fact.setGarbageCollectionListener(new GarbageCollectionListener() {

			@Override
			public void onStop(int num, int size, int free, long time, long totalTime) {
				System.out.println(" done. " + size + " " + free + " " + totalTime);
			}

			@Override
			public void onStart(int num, int size, int free, long totalTime) {
				System.out.print("\nGC... " + num);
			}
		});
		fact.setResizeListener(new ResizeListener() {

			@Override
			public void onStop(int num, int oldSize, int newSize, long time, long totalTime) {
				System.out.println(" done. " + oldSize + " " + newSize + " " + time + "/" + totalTime);
			}

			@Override
			public void onStart(int num, int size, int free, long totalTime) {
				System.out.print("\nResize... " + num);
			}
		});

		/*
		 * create bdds for input vars
		 */
		for (String input : cp.getInputs()) {
			bdds.put(input, fact.makeVar(varNumber++));
		}

		opMap = cp.getOpMap();

		/*
		 * calculate saved operations
		 */
		for (String op : opMap.keySet()) {
			calc(op);
		}

		/*
		 * build transition relation
		 * (v0' <-> f_0(V)) & (v1' <-> f_1(V)) & ... & (v_{n-1}' <-> f__{n-1}(V))
		 */
		ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
		String[] dffsArray = dffs.keySet().toArray(new String[0]);
		ArrayList<Future<BDD>> futures = new ArrayList<>();
		for (int i = 0; i < numberOfThreads; i++) {
			PartialCalculator partialCalculator = new PartialCalculator(i, numberOfThreads, dffsArray);
			futures.add(executorService.submit(partialCalculator));
		}

		BDD transitionRelation = fact.makeOne();
		for (Future<BDD> future : futures) {
			transitionRelation.andWith(future.get());
		}

		executorService.shutdown();

		fact.done();
	}

	private static BDD calc(String name) {
		if (bdds.containsKey(name)) {
			return bdds.get(name);
		}
		Operation o = opMap.get(name);

		BDD bdd = null;
		String op;

		switch (o.getOp()) {
		case "DFF":
			op = o.getArgs().get(0);
			String dffName = name + "DFF";
			bdds.put(dffName, fact.makeVar(varNumber++)); // 'next state' variable
			bdds.put(name, fact.makeVar(varNumber++)); // 'current state' variable
			bdd = calc(op);
			dffs.put(dffName, bdd);
			break;
		case "NOT":
			op = o.getArgs().get(0);
			bdd = calc(op).not();
			break;
		case "AND":
			bdd = fact.makeOne();
			for (String subOp : o.getArgs()) {
				bdd = bdd.and(calc(subOp));
			}
			break;
		case "NAND":
			bdd = fact.makeZero();
			for (String subOp : o.getArgs()) {
				bdd = bdd.nand(calc(subOp));
			}
			break;
		case "OR":
			bdd = fact.makeZero();
			for (String subOp : o.getArgs()) {
				bdd = bdd.or(calc(subOp));
			}
			break;
		case "NOR":
			bdd = fact.makeZero();
			for (String subOp : o.getArgs()) {
				bdd = bdd.or(calc(subOp)).not();
			}
			break;
		default:
			throw new UnsupportedOperationException("Unsupported op: " + o.getOp());
		}

		bdds.put(name, bdd);
		return bdd;
	}

}
