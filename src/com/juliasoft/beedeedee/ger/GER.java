package com.juliasoft.beedeedee.ger;

import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.juliasoft.beedeedee.bdd.BDD;
import com.juliasoft.beedeedee.bdd.ReplacementWithExistingVarException;

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
	 * Finds the maximum variable index in this GER.
	 * 
	 * @return the maximum variable index, -1 for terminal nodes
	 */
	public int maxVar() {
		return Math.max(l.maxVar(), n.maxVar());
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
			if (nOld != null)
				nOld.free();

			nOld = nNew.copy();
			eNew.addPairs(nNew.equivVars());
//			LeaderFunction leaderFunctionNew = new LeaderFunction(eNew);
//			nNew.squeezeEquivWith(leaderFunctionNew);
			nNew = nNew.renameWithLeader(eNew);
		} while (!nNew.isEquivalentTo(nOld) || !eNew.equals(eOld));

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

	/**
	 * Computes the implication of this GER with another. It uses the identity
	 * g1 -> g2 = !g1 | g2.
	 * 
	 * @param other the other GER
	 * @return the implication
	 */
	public GER imp(GER other) {
		GER notG1 = not();
		GER or = notG1.or(other);
		notG1.free();
		return or;
	}

	@Override
	public String toString() {
		return l + System.lineSeparator() + n;
	}

	public GER exist(int var) {
		E lNew = l.copy();
		lNew.removeVar(var);
		BDD exist;
		if (l.containsVar(var)) {
			int nextLeader = l.nextLeader(var);
			Map<Integer, Integer> renaming = new HashMap<>();
			renaming.put(var, nextLeader);
			exist = n.replace(renaming);
		} else {
			exist = n.exist(var);
		}
		return new GER(exist, lNew);
	}

	public GER replace(Map<Integer, Integer> renaming) {
		BitSet nVars = n.vars();
		for (Integer v : renaming.values()) {
			if (l.containsVar(v) || nVars.get(v)) {
				System.out.println(this);
				System.out.println(renaming);
				throw new ReplacementWithExistingVarException(v);
			}
		}
		E eNew = l.copy();
		eNew.replace(renaming);
		BDD nNew;
		try {
			nNew = n.replace(renaming);
		} catch (ReplacementWithExistingVarException e) {
			System.out.println(n);
			System.out.println(renaming);
			throw e;
		}
		BDD old = nNew;
		nNew = nNew.renameWithLeader(eNew);
		old.free();
		return new GER(nNew, eNew);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((l == null) ? 0 : l.hashCode());
		result = prime * result + ((n == null) ? 0 : n.hashCodeAux());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GER other = (GER) obj;
		if (l == null) {
			if (other.l != null)
				return false;
		} else if (!l.equals(other.l))
			return false;
		if (n == null) {
			if (other.n != null)
				return false;
		} else if (!n.isEquivalentTo(other.n))
			return false;
		return true;
	}
}
