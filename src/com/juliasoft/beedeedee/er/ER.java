package com.juliasoft.beedeedee.er;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.juliasoft.beedeedee.bdd.BDD;
import com.juliasoft.beedeedee.bdd.ReplacementWithExistingVarException;

/**
 * A representation for Boolean functions separating information on equivalent
 * variables from the BDD.
 */
// TODO / FIXME this representation doesn't separate ground variables (yet?)
public class ER {

	private final BDD n;
	private EquivalenceRelation l;

	/**
	 * Constructs a ER representation using (TODO copy bdd?) the given bdd and
	 * set of equivalence classes.
	 * 
	 * @param n a bdd, saved by reference
	 * @param l a set of equivalence classes
	 */
	public ER(BDD n, EquivalenceRelation l) {
		this.l = l;
		this.n = n; // TODO copy? sq.With
	}

	public ER(BDD n) {
		this(n, new EquivalenceRelation());
	}

	BDD getN() {
		return n;
	}

	/**
	 * Frees resources allocated to this ER. Call this method when the er is
	 * no more needed.
	 * 
	 * TODO the bdd is saved by reference, and calling this method can thus
	 * invalidate a shared object
	 */
	public void free() {
		n.free();
		l = null;
	}

	/**
	 * Computes conjunction of this ER with another. The resulting ER is the
	 * normalized version of that having as E the union of the two E's, and as n
	 * the conjunction of the two bdds.
	 * The result is normalized.
	 * 
	 * @param other the other ER
	 * @return the conjunction
	 */
	public ER and(ER other) {
		BDD and = n.and(other.n);
		EquivalenceRelation e = new EquivalenceRelation();
		e.addPairs(l.pairs());
		e.addPairs(other.l.pairs());
		ER andGer = new ER(and, e);
		ER result = andGer.normalize();
		andGer.free();
		return result;
	}

	/**
	 * Computes disjunction of this ER with another. The resulting E is the
	 * intersection of the two E's, in the sense detailed in
	 * {@link EquivalenceRelation#intersect(EquivalenceRelation)}. The resulting bdd (n) is the disjunction of the
	 * two input "squeezed" bdds, each enriched (in 'and') with biimplications
	 * expressing pairs not present in the other bdd.
	 * 
	 * @param other the other ER
	 * @return the disjunction
	 */
	public ER or(ER other) {
		BDD n1 = computeNforOr(this, other);
		BDD n2 = computeNforOr(other, this);

		BDD or = n1.or(n2);
		n1.free();
		n2.free();
		EquivalenceRelation equiv = l.intersect(other.l);

		return new ER(or, equiv);
	}

	private BDD computeNforOr(ER er1, ER er2) {
		BDD squeezedBDD = er1.getSqueezedBDD();
		List<Pair> subtract = er1.l.subtract(er2.l);
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
	 * Computes negation of this ER. It uses the identity !(L & n) = !L | !n =
	 * !p1 | !p2 | ... | !pn | !n. The result is normalized.
	 * 
	 * @return the negation
	 */
	public ER not() {
		BDD not = n.not();
		for (Pair pair : l.pairs()) {
			BDD eq = not.getFactory().makeVar(pair.first);
			eq.biimpWith(not.getFactory().makeVar(pair.second));
			eq.notWith();
			not.orWith(eq);
		}
		ER notGer = new ER(not);
		ER result = notGer.normalize();
		notGer.free();
		return result;
	}

	/**
	 * Computes XOR of this ER with another. It uses the identity g1 x g2 = (g1
	 * | g2) & !(g1 & g2). The result is normalized.
	 * 
	 * TODO use another identity?
	 * 
	 * @param other the other ER
	 * @return the xor
	 */
	public ER xor(ER other) {
		ER or = or(other);
		ER and = and(other);
		ER notAnd = and.not();
		ER result = or.and(notAnd);
		or.free();
		and.free();
		notAnd.free();
		return result;
	}

	public EquivalenceRelation getEquiv() {
		return l;
	}

	/**
	 * Computes the "squeezed" version of the bdd. This version doesn't contain
	 * equivalent variables, see {@link BDD#squeezeEquiv(LeaderFunction)}.
	 * 
	 * @return the squeezed bdd
	 */
	public BDD getSqueezedBDD() {
		return n.squeezeEquiv(l);
	}

	/**
	 * Finds the maximum variable index in this ER.
	 * 
	 * @return the maximum variable index, -1 for terminal nodes
	 */
	public int maxVar() {
		return Math.max(l.maxVar(), n.maxVar());
	}

	/**
	 * Produces a normalized version of this ER.
	 * 
	 * @return a normalized version of this ER.
	 */
	public ER normalize() {
		EquivalenceRelation eNew = l.copy();
		BDD nNew = n.copy();
		EquivalenceRelation eOld;
		BDD nOld = null;
		do {
			eOld = eNew.copy();
			if (nOld != null)
				nOld.free();

			nOld = nNew;//.copy();
			eNew.addPairs(nNew.equivVars());
//			LeaderFunction leaderFunctionNew = new LeaderFunction(eNew);
//			nNew.squeezeEquivWith(leaderFunctionNew);
			nNew = nNew.renameWithLeader(eNew);
		} while (!nNew.isEquivalentTo(nOld) || !eNew.equals(eOld));

		nOld.free();

		return new ER(nNew, eNew);
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

	public ER copy() {
		return new ER(n.copy(), l.copy());
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
	 * Computes the biimplication of this ER with another. It uses the identity
	 * g1 <-> g2 = (!g1 | g2) & (!g2 | g1).
	 * 
	 * TODO use another identity?
	 * 
	 * @param other the other ER
	 * @return the biimplication
	 */
	public ER biimp(ER other) {
		ER notG1 = not();
		ER notG2 = other.not();

		ER or1 = notG1.or(other);
		ER or2 = notG2.or(this);

		ER and = or1.and(or2);

		notG1.free();
		notG2.free();
		or1.free();
		or2.free();
		return and;
	}

	/**
	 * Computes the implication of this ER with another. It uses the identity
	 * g1 -> g2 = !g1 | g2.
	 * 
	 * @param other the other ER
	 * @return the implication
	 */
	public ER imp(ER other) {
		ER notG1 = not();
		ER or = notG1.or(other);
		notG1.free();
		return or;
	}

	@Override
	public String toString() {
		return l + System.lineSeparator() + n;
	}

	public ER exist(int var) {
		EquivalenceRelation lNew = l.copy();
		lNew.removeVar(var);
		BDD exist;
		if (l.containsVar(var)) {
			int nextLeader = l.nextLeader(var);
			Map<Integer, Integer> renaming = new HashMap<>();
			renaming.put(var, nextLeader);
			exist = n.replace(renaming);	// requires normalized representation
		} else {
			exist = n.exist(var);
		}
		ER existEr = new ER(exist, lNew);
		ER normalized = existEr.normalize();
		existEr.free();
		return normalized;
	}

	public ER exist(BitSet vars) {
		EquivalenceRelation lNew = l.copy();
		BitSet quantifiedVars = new BitSet();
		Map<Integer, Integer> renaming = new HashMap<>();
		for (int i = vars.nextSetBit(0); i >= 0; i = vars.nextSetBit(i + 1)) {
			if (l.containsVar(i)) {
				int nextLeader = l.nextLeader(i, vars);
				if (nextLeader < 0) {
					quantifiedVars.set(i);
				} else {
					renaming.put(i, nextLeader);
				}
			} else {
				quantifiedVars.set(i);
			}
			lNew.removeVar(i);
		}

		BDD exist = n;
		if (!renaming.isEmpty()) {
			exist = exist.replace(renaming);	// requires normalized representation
		}
		if (!quantifiedVars.isEmpty()) {
			exist = exist.exist(quantifiedVars);
		}
		ER existEr = new ER(exist, lNew);
		ER normalized = existEr.normalize();
		existEr.free();
		return normalized;
	}

	public ER replace(Map<Integer, Integer> renaming) {
		BitSet nVars = n.vars();
		for (Integer v : renaming.values()) {
			if ((l.containsVar(v) || nVars.get(v)) && !renaming.keySet().contains(v)) {
				throw new ReplacementWithExistingVarException(v);
			}
		}

		BDD nNew;
		nNew = n.replace(renaming);

		// perform "simultaneous" substitution
		renaming = new HashMap<>(renaming);
		Map<Integer, Integer> varsOnTheRighSide = splitRenaming(renaming);
		EquivalenceRelation eNew = l.copy();
		eNew.replace(varsOnTheRighSide);	// these renamings need to be performed first
		eNew.replace(renaming);

		BDD old = nNew;
		nNew = nNew.renameWithLeader(eNew);
		old.free();
		ER replaceEr = new ER(nNew, eNew);
		ER normalized = replaceEr.normalize();
		replaceEr.free();
		return normalized;
	}

	/**
	 * Separates renamings affecting variables on the right side of some renaming.
	 * The original renaming map is modified.
	 * 
	 * @param renaming the original renaming map. After the execution of this method
	 * it does not contain mappings present in the returned map
	 * @return a map containing mappings renaming variables present on the right side
	 * of some other mapping
	 */
	private Map<Integer, Integer> splitRenaming(Map<Integer, Integer> renaming) {
		Map<Integer, Integer> varsOnTheRighSide = new HashMap<>();
		ArrayList<Integer> values = new ArrayList<>(renaming.values());
		for (Integer i : values) {
			if (renaming.keySet().contains(i)) {
				varsOnTheRighSide.put(i, renaming.get(i));
				renaming.remove(i);
			}
		}
		return varsOnTheRighSide;
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
		ER other = (ER) obj;
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
