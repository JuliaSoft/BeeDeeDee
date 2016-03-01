package com.juliasoft.beedeedee.ger;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import com.juliasoft.beedeedee.bdd.BDD;
import com.juliasoft.beedeedee.factories.Factory;

// TODO / FIXME this representation doesn't separate ground variables (yet?)
public class GER {

	private BDD n;
	private E l;
	private LeaderFunction leaderFunction;

	/**
	 * Constructs a GER representation using (TODO copy bdd?) the given bdd and
	 * set of equivalence classes.
	 * 
	 * @param n a bdd, saved by reference
	 * @param l a set of equivalence classes
	 */
	public GER(BDD n, E l) {
		this.l = l;
		leaderFunction = new LeaderFunction(l);
		this.n = n; // TODO copy? sq.With
	}

	public GER(BDD n) {
		this(n, new E());
	}

	BDD getN() {
		return n;
	}

	/**
	 * Frees resources allocated to this GER. Call this method when the ger is
	 * no more needed.
	 * 
	 * TODO the bdd is saved by reference, and calling this method can thus
	 * invalidate a shared object
	 */
	public void free() {
		n.free();
		l = null;
		leaderFunction = null;
	}

	/**
	 * Computes conjunction of this GER with another. The resulting GER is the
	 * normalized version of that having as E the union of the two E's, and as n
	 * the conjunction of the two bdds.
	 * The result is normalized.
	 * 
	 * @param other the other GER
	 * @return the conjunction
	 */
	public GER and(GER other) {
		BDD and = n.and(other.n);
		E e = new E();
		e.addPairs(l.pairs());
		e.addPairs(other.l.pairs());
		GER andGer = new GER(and, e);
		GER result = andGer.normalize();
		andGer.free();
		return result;
	}

	/**
	 * Computes disjunction of this GER with another. The resulting E is the
	 * intersection of the two E's, in the sense detailed in
	 * {@link E#intersect(E)}. The resulting bdd (n) is the disjunction of the
	 * two input "squeezed" bdds, each enriched (in 'and') with biimplications
	 * expressing pairs not present in the other bdd.
	 * 
	 * @param other the other GER
	 * @return the disjunction
	 */
	public GER or(GER other) {
		BDD n1 = computeNforOr(this, other);
		BDD n2 = computeNforOr(other, this);

		BDD or = n1.or(n2);
		n1.free();
		n2.free();
		E equiv = l.intersect(other.l);

		return new GER(or, equiv);
	}

	private BDD computeNforOr(GER ger1, GER ger2) {
		BDD squeezedBDD = ger1.getSqueezedBDD();
		List<Pair> subtract = ger1.l.subtract(ger2.l);
		for (Pair pair : subtract) {
			BDD biimp = squeezedBDD.getFactory().makeVar(pair.first);
			biimp.biimpWith(squeezedBDD.getFactory().makeVar(pair.second));
			squeezedBDD.andWith(biimp);
		}
		return squeezedBDD;
	}

	/*
	 * Identity-based operations
	 */

	/**
	 * Computes negation of this GER. It uses the identity !(L & n) = !L | !n =
	 * !p1 | !p2 | ... | !pn | !n. The result is normalized.
	 * 
	 * @return the negation
	 */
	public GER not() {
		BDD not = n.not();
		for (Pair pair : l.pairs()) {
			BDD eq = not.getFactory().makeVar(pair.first);
			eq.biimpWith(not.getFactory().makeVar(pair.second));
			eq.notWith();
			not.orWith(eq);
		}
		GER notGer = new GER(not);
		GER result = notGer.normalize();
		notGer.free();
		return result;
	}

	/**
	 * Computes XOR of this GER with another. It uses the identity g1 x g2 = (g1
	 * | g2) & !(g1 & g2). The result is normalized.
	 * 
	 * TODO use another identity?
	 * 
	 * @param other the other GER
	 * @return the xor
	 */
	public GER xor(GER other) {
		GER or = or(other);
		GER and = and(other);
		GER notAnd = and.not();
		GER result = or.and(notAnd);
		or.free();
		and.free();
		notAnd.free();
		return result;
	}

	public E getEquiv() {
		return l;
	}

	/**
	 * Computes the "squeezed" version of the bdd. This version doesn't contain
	 * equivalent variables, see {@link BDD#squeezeEquiv(LeaderFunction)}.
	 * 
	 * @return the squeezed bdd
	 */
	public BDD getSqueezedBDD() {
		return n.squeezeEquiv(leaderFunction);
	}

	/**
	 * Computes the set of variables entailed by the given BDD.
	 * 
	 * @param f the BDD
	 * @return the set of entailed variables
	 */
	BitSet varsEntailed(BDD f) {
		return varsEntailedAux(f, new BitSet(), universe(f), true);
	}

	private BitSet varsEntailedAux(BDD f, BitSet s, BitSet i, boolean entailed) {
		BDD orig = f;
		BDD oldf = f;
		while (!(f.isZero() || f.isOne())) {
			s.set(f.var());
			BDD child = entailed ? f.high() : f.low();
			varsEntailedAux(child, s, i, entailed);
			child.free();
			s.clear(f.var());
			oldf = f;
			f = entailed ? f.low() : f.high();
			if (oldf != orig) {
				oldf.free();
			}
		}
		if (f.isOne()) {
			i.and(s);
		}
		if (f != orig) {
			f.free();
		}
		return i;
	}

	/**
	 * Computes the set of variables disentailed by the given BDD.
	 * 
	 * @param f the BDD
	 * @return the set of disentailed variables
	 */
	BitSet varsDisentailed(BDD f) {
		return varsEntailedAux(f, new BitSet(), universe(f), false);
	}

	/**
	 * Computes the set of all variable indexes up to max var index created so
	 * far.
	 * 
	 * @param f the BDD from which to compute maxVar
	 * @return the set of all variable indexes
	 */
	private BitSet universe(BDD f) {
		BitSet u = new BitSet();
		int maxVar = f.getFactory().getMaxVar();
		if (maxVar > 0) {
			for (int i = 0; i <= maxVar; i++) {
				u.set(i);
			}
		}
		return u;
	}

	/**
	 * Finds pairs of equivalent variables in the given BDD.
	 * 
	 * @param f the BDD
	 * @return a list of pairs
	 */
	public List<Pair> equivVars(BDD f) {
		int maxVar = maxVar(f);
		return equivVars(f, generatePairs(maxVar));
	}

	private List<Pair> equivVars(BDD f, List<Pair> universePairs) {
		if (f.isOne()) {
			return new ArrayList<>();
		}
		if (f.isZero()) {
			return universePairs;
		}

		List<Pair> pairs = new ArrayList<>();
		BDD high = f.high();
		BDD low = f.low();
		BitSet varsEntailedByHigh = varsEntailed(high);
		varsEntailedByHigh.and(varsDisentailed(low));
		for (int i = varsEntailedByHigh.nextSetBit(0); i >= 0; i = varsEntailedByHigh.nextSetBit(i + 1)) {
			pairs.add(new Pair(f.var(), i));
		}

		List<Pair> equivVars = equivVars(high, universePairs);
		equivVars.retainAll(equivVars(low, universePairs));

		high.free();
		low.free();

		pairs.addAll(equivVars);
		return pairs;
	}

	/**
	 * Generates all ordered pairs of variables up to the given maxVar.
	 * 
	 * @param maxVar the maximum variable index
	 * @return the list of generated pairs
	 */
	List<Pair> generatePairs(int maxVar) {
		List<Pair> pairs = new ArrayList<>();
		for (int i = 0; i < maxVar; i++) {
			for (int j = i + 1; j <= maxVar; j++) {
				pairs.add(new Pair(i, j));
			}
		}
		return pairs;
	}

	/**
	 * Finds the maximum variable index in the given BDD.
	 * 
	 * @param f the BDD
	 * @return the maximum variable index, -1 for terminal nodes
	 */
	int maxVar(BDD f) {
		if (f.isZero() || f.isOne()) {
			return -1;
		}
		BDD low = f.low();
		int maxVar = Math.max(f.var(), maxVar(low));
		low.free();
		BDD high = f.high();
		maxVar = Math.max(maxVar, maxVar(high));
		high.free();
		return maxVar;
	}

	/**
	 * Produces a normalized version of this GER.
	 * 
	 * @return a normalized version of this GER.
	 */
	public GER normalize() {
		E eNew = l;
		BDD nNew = n.copy();
		E eOld;
		BDD nOld = null;
		do {
			eOld = eNew.copy();
			if (nOld != null) {
				nOld.free();
			}
			nOld = nNew.copy();
			List<Pair> equivVars = equivVars(nNew);
			eNew.addPairs(equivVars);
//			LeaderFunction leaderFunctionNew = new LeaderFunction(eNew);
//			nNew.squeezeEquivWith(leaderFunctionNew);
			nNew = renameWithLeader(nNew, eNew);
		} while (!eNew.equals(eOld) || !nNew.isEquivalentTo(nOld));
		nOld.free();
		return new GER(nNew, eNew);
	}

	/**
	 * @return the full BDD, containing also equivalence constraints in l.
	 */
	public BDD getFullBDD() {
		BDD full = n.copy();
		for (Pair pair : l.pairs()) {
			BDD biimp = n.getFactory().makeVar(pair.first);
			biimp.biimpWith(n.getFactory().makeVar(pair.second));
			full.andWith(biimp);
		}
		return full;
	}

	public GER copy() {
		return new GER(n.copy(), l.copy());
	}

	// TODO move in factory
	BDD renameWithLeader(BDD f, E r) {
		BitSet t = new BitSet();
		return renameWithLeader(f, r, 1, t);
	}

	private BDD renameWithLeader(BDD f, E r, int c, BitSet t) {
		int var = f.var();
		LeaderFunction lf = new LeaderFunction(r);
		int maxVar = r.maxVar();
		BitSet leaders = lf.getLeaders(); // TODO move outside - pass as param
		if (maxVar < var || f.isOne() || f.isZero()) {
			return f;
		}
		BitSet augmented = new BitSet();
		augmented.or(t);
		int minLeader = leaders.nextSetBit(c);
		if (minLeader > 0 && minLeader < var) {
			augmented.set(minLeader);
			c = minLeader;
			return mkNode(minLeader, renameWithLeader(f, r, c + 1, augmented), renameWithLeader(f, r, c + 1, t));
		}
		c = var;
		if (!r.containsVar(var)) {
			return mkNode(var, renameWithLeader(f.high(), r, c + 1, t), renameWithLeader(f.low(), r, c + 1, t));
		}
		int l = lf.get(var);
		if (l == var) {
			augmented.set(c);
			return mkNode(var, renameWithLeader(f.high(), r, c + 1, augmented), renameWithLeader(f.low(), r, c + 1, t));
		}
		if (t.get(l)) {
			return renameWithLeader(f.high(), r, c + 1, t);
		}
		return renameWithLeader(f.low(), r, c + 1, t);
	}

	// FIXME use MK in Factory
	private BDD mkNode(int var, BDD high, BDD low) {
		Factory factory = low.getFactory();
		BDD makeVar = factory.makeVar(var);
		BDD makeNotVar = factory.makeNotVar(var);
		BDD bdd = makeVar.andWith(high).orWith(makeNotVar.andWith(low));
		return bdd;
	}

	public long satCount() {
		BitSet vars = n.vars();
		int c = 1;
		for (BitSet eqClass : l) {
			int leader = eqClass.nextSetBit(0);
			if (vars.get(leader)) {
				continue;
			}
			c *= 2;
		}
		return c * n.satCount(vars.cardinality() - 1);
	}

	public BitSet vars() {
		BitSet res = new BitSet();
		for (BitSet eqClass : l) {
			res.or(eqClass);
		}
		res.or(n.vars());
		return res;
	}

	/**
	 * Computes the biimplication of this GER with another. It uses the identity
	 * g1 <-> g2 = (!g1 | g2) & (!g2 | g1).
	 * 
	 * TODO use another identity?
	 * 
	 * @param other the other GER
	 * @return the biimplication
	 */
	public GER biimp(GER other) {
		GER notG1 = not();
		GER notG2 = other.not();

		GER or1 = notG1.or(other);
		GER or2 = notG2.or(this);

		GER and = or1.and(or2);

		notG1.free();
		notG2.free();
		or1.free();
		or2.free();
		return and;
	}
}
