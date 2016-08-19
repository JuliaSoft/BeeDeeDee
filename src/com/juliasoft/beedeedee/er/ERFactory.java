package com.juliasoft.beedeedee.er;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.juliasoft.beedeedee.bdd.Assignment;
import com.juliasoft.beedeedee.bdd.BDD;
import com.juliasoft.beedeedee.bdd.ReplacementWithExistingVarException;
import com.juliasoft.beedeedee.bdd.UnsatException;
import com.juliasoft.beedeedee.factories.Factory;

/**
 * A BDD factory using the ER representation.
 */
public class ERFactory extends Factory {

	public ERFactory(int utSize, int cacheSize) {
		super(utSize, cacheSize);
	}

	@Override
	public int nodeCount(Collection<BDD> bdds) {
		throw new RuntimeException("Not yet implemented"); //TODO
	}

	@Override
	protected BDDImpl mk(int id) {
		return new BDDER(id);
	}

	/**
	 * A {@link BDD} implementation that separates information on equivalent
	 * variables from a robdd.
	 */
	public class BDDER extends BDDImpl {
		private EquivalenceRelation l;

		/**
		 * Constructs a BDDER by normalizing the given bdd.
		 * 
		 * @param bdd the starting BDD, which is immediately freed if not equal to
		 *            (same as) normalized
		 */
		BDDER(int id) {
			this(id, new EquivalenceRelation());

			BDDER normalized = normalize();
			setId(normalized.id);
			l = normalized.l;
		}

		/**
		 * Non-normalizing constructor.
		 * 
		 * @param id the BDD
		 * @param l the equivalence relation
		 */
		BDDER(int id, EquivalenceRelation l) {
			super(id);

			this.l = l;
		}

		/**
		 * Produces a normalized version of this BDDER.
		 * 
		 * @return a normalized version of this BDDER.
		 */
		BDDER normalize() {
			EquivalenceRelation eNew = l, eOld;
			BDD nNew = super.copy();
			BDD nOld = null;

			do {
				if (nOld != null)
					nOld.free();

				nOld = nNew;
				eOld = eNew;
				eNew = eNew.addPairs(nNew.equivVars());
				nNew = nNew.renameWithLeader(eNew);
			}
			while (!eNew.equals(eOld) || !nNew.isEquivalentTo(nOld));

			if (nOld != nNew)
				nOld.free();

			return new BDDER(((BDDImpl) nNew).getId(), eNew);
		}

		@Override
		public void free() {
			super.free();
			l = null;
		}

		@Override
		public boolean isOne() {
			return super.isOne() && l.isEmpty();
		}

		@Override
		public BDD or(BDD other) {
			if (!(other instanceof BDDER)) {
				// TODO or convert transparently to BDDER?
				throw new NotBDDERException();
			}
			BDDER otherBddEr = (BDDER) other;
			return or_(otherBddEr);
		}

		@Override
		public BDD orWith(BDD other) {
			if (!(other instanceof BDDER)) {
				// TODO or convert transparently to BDDER?
				throw new NotBDDERException();
			}
			BDDER otherBddEr = (BDDER) other;
			BDDER or_ = or_(otherBddEr);
			setId(or_.getId());
			l = or_.l;
			otherBddEr.free();

			return this;
		}

		/**
		 * Computes disjunction of this BDDER with another. The resulting l is the
		 * intersection of the two l's, in the sense detailed in
		 * {@link EquivalenceRelation#intersection(EquivalenceRelation)}. The resulting bdd (n) is the disjunction of the
		 * two input "squeezed" bdds, each enriched (in 'and') with biimplications
		 * expressing pairs not present in the other bdd.
		 * 
		 * @param other the other BDDER
		 * @return the disjunction
		 */
		BDDER or_(BDDER other) {
			BDD n1 = computeNforOr(this, other);
			BDD n2 = computeNforOr(other, this);

			n1.orWith(n2);
			n2.free();
			return new BDDER(((BDDImpl) n1).getId(), l.intersection(other.l));
		}

		private BDD computeNforOr(BDDER er1, BDDER er2) {
			BDD squeezedBDD = er1.getSqueezedBDD();
			List<Pair> subtract = er1.l.pairsInDifference(er2.l);
			for (Pair pair: subtract) {
				BDD biimp = makeVarBDDImpl(pair.first);
				biimp.biimpWith(makeVarBDDImpl(pair.second));
				squeezedBDD.andWith(biimp);
			}
			return squeezedBDD;
		}

		/**
		 * Computes the "squeezed" version of the bdd. This version doesn't contain
		 * equivalent variables, see {@link BDD#squeezeEquiv(LeaderFunction)}.
		 * 
		 * @return the squeezed bdd
		 */
		BDD getSqueezedBDD() {
			return super.squeezeEquiv(l);
		}

		@Override
		public BDD and(BDD other) {
			if (!(other instanceof BDDER)) {
				// TODO or convert transparently to BDDER?
				throw new NotBDDERException();
			}
			return and_((BDDER) other);
		}

		@Override
		public BDD andWith(BDD other) {
			if (!(other instanceof BDDER)) {
				// TODO or convert transparently to BDDER?
				throw new NotBDDERException();
			}
			BDDER otherBddEr = (BDDER) other;
			BDDER and_ = and_(otherBddEr);
			setId(and_.getId());
			l = and_.l;
			otherBddEr.free();

			return this;
		}

		/**
		 * Computes conjunction of this BDDER with another. The resulting BDDER is the
		 * normalized version of that having as l the union of the two l's, and as n
		 * the conjunction of the two bdds.
		 * The result is normalized.
		 * 
		 * @param other the other ER
		 * @return the conjunction
		 */
		BDDER and_(BDDER other) {
			BDD and = super.and(other);
			EquivalenceRelation ln = l.addClasses(other.l);
			BDDER andEr = new BDDER(((BDDImpl) and).getId(), ln);
			BDDER result = andEr.normalize();
			andEr.free();
			return result;
		}

		@Override
		public BDD xor(BDD other) {
			if (!(other instanceof BDDER)) {
				// TODO or convert transparently to BDDER?
				throw new NotBDDERException();
			}
			BDDER otherBddEr = (BDDER) other;
			return xor_(otherBddEr);
		}

		@Override
		public BDD xorWith(BDD other) {
			if (!(other instanceof BDDER)) {
				// TODO or convert transparently to BDDER?
				throw new NotBDDERException();
			}
			BDDER otherBddEr = (BDDER) other;
			BDDER xor_ = xor_(otherBddEr);
			setId(((BDDImpl) xor_).getId());
			l = xor_.l;
			otherBddEr.free();

			return this;
		}

		/**
		 * Computes XOR of this BDDER with another. It uses the identity g1 x g2 = (g1
		 * | g2) & !(g1 & g2). The result is normalized.
		 * 
		 * TODO use another identity?
		 * 
		 * @param other the other BDDER
		 * @return the xor
		 */
		BDDER xor_(BDDER other) {
			BDDER or = or_(other);
			BDDER and = and_(other);
			BDDER notAnd = and.not_();
			BDDER result = or.and_(notAnd);
			or.free();
			and.free();
			notAnd.free();
			return result;
		}

		/**
		 * Computes negation of this ER. It uses the identity !(L & n) = !L | !n =
		 * !p1 | !p2 | ... | !pn | !n. The result is normalized.
		 * 
		 * @return the negation
		 */
		BDDER not_() {
			BDD not = super.not();
			for (Pair pair: l.pairs()) {
				BDD eq = makeVarBDDImpl(pair.first);
				eq.biimpWith(makeVarBDDImpl(pair.second));
				eq.notWith();
				not.orWith(eq);
			}
			return new BDDER(((BDDImpl) not).getId());
		}

		@Override
		public BDD nand(BDD other) {
			if (!(other instanceof BDDER)) {
				// TODO or convert transparently to BDDER?
				throw new NotBDDERException();
			}
			BDDER otherBddEr = (BDDER) other;
			BDDER and_ = and_(otherBddEr);
			BDDER not_ = and_.not_();
			and_.free();

			return not_;
		}

		@Override
		public BDD nandWith(BDD other) {
			if (!(other instanceof BDDER)) {
				// TODO or convert transparently to BDDER?
				throw new NotBDDERException();
			}

			BDDER otherBddEr = (BDDER) other;
			BDDER and_ = and_(otherBddEr);
			BDDER not_ = and_.not_();
			and_.free();
			setId(((BDDImpl) not_).getId());
			l = not_.l;
			otherBddEr.free();

			return this;
		}

		@Override
		public BDD not() {
			return not_();
		}

		@Override
		public BDD notWith() {
			BDDER not_ = not_();
			setId(((BDDImpl) not_).getId());
			l = not_.l;

			return this;
		}

		@Override
		public BDD imp(BDD other) {
			if (!(other instanceof BDDER)) {
				// TODO or convert transparently to BDDER?
				throw new NotBDDERException();
			}
			BDDER otherBddEr = (BDDER) other;
			return imp_(otherBddEr);
		}

		@Override
		public BDD impWith(BDD other) {
			if (!(other instanceof BDDER)) {
				// TODO or convert transparently to BDDER?
				throw new NotBDDERException();
			}
			BDDER otherBddEr = (BDDER) other;
			BDDER imp_ = imp_(otherBddEr);
			setId(((BDDImpl) imp_).getId());
			l = imp_.l;
			otherBddEr.free();

			return this;
		}

		/**
		 * Computes the implication of this BDDER with another. It uses the identity
		 * g1 -> g2 = !g1 | g2.
		 * 
		 * @param other the other BDDER
		 * @return the implication
		 */
		BDDER imp_(BDDER other) {
			BDDER notG1 = not_();
			BDDER or_ = notG1.or_(other);
			notG1.free();
			return or_;
		}

		@Override
		public BDD biimp(BDD other) {
			if (!(other instanceof BDDER)) {
				// TODO or convert transparently to BDDER?
				throw new NotBDDERException();
			}
			BDDER otherBddEr = (BDDER) other;
			return biimp_(otherBddEr);
		}

		@Override
		public BDD biimpWith(BDD other) {
			if (!(other instanceof BDDER)) {
				// TODO or convert transparently to BDDER?
				throw new NotBDDERException();
			}
			BDDER otherBddEr = (BDDER) other;
			BDDER biimp_ = biimp_(otherBddEr);
			setId(((BDDImpl) biimp_).getId());
			l = biimp_.l;
			otherBddEr.free();

			return this;
		}

		/**
		 * Computes the biimplication of this BDDER with another. It uses the identity
		 * g1 <-> g2 = (!g1 | g2) & (!g2 | g1).
		 * 
		 * TODO use another identity?
		 * 
		 * @param other the other BDDER
		 * @return the biimplication
		 */
		BDDER biimp_(BDDER other) {
			BDDER notG1 = not_();
			BDDER notG2 = other.not_();

			BDDER or1 = notG1.or_(other);
			BDDER or2 = notG2.or_(this);

			BDDER and = or1.and_(or2);

			notG1.free();
			notG2.free();
			or1.free();
			or2.free();
			return and;
		}

		@Override
		public BDD copy() {
			return new BDDER(((BDDImpl) super.copy()).getId(), l);
		}

		@Override
		public Assignment anySat() throws UnsatException {
			Assignment anySat = super.anySat();
			l.updateAssignment(anySat);
			return anySat;
		}

		@Override
		public List<Assignment> allSat() {
			throw new UnsupportedOperationException();
		}

		@Override
		public long satCount() {
			return satCount_();
		}

		private long satCount_() {
			BitSet vars = super.vars();
			int c = 1;
			for (BitSet eqClass: l) {
				int leader = eqClass.nextSetBit(0);
				if (vars.get(leader))
					continue;

				c *= 2;
			}

			return c * super.satCount(vars.cardinality() - 1);
		}


		@Override
		public long satCount(int maxVar) {
			// TODO FIXME check this
			return satCount_();
		}

		@Override
		public BDD restrict(int var, boolean value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public BDD restrict(BDD var) {
			throw new UnsupportedOperationException();
		}

		@Override
		public BDD restrictWith(BDD var) {
			throw new UnsupportedOperationException();
		}

		@Override
		public BDD exist(int var) {
			return exist_(var);
		}

		@Override
		public BDD exist(BDD var) {
			return exist_(var.vars());
		}

		@Override
		public BDD exist(BitSet var) {
			return exist_(var);
		}

		private BDDER exist_(int var) {
			BDD exist;
			if (l.containsVar(var)) {
				int nextLeader = l.nextLeader(var);
				Map<Integer, Integer> renaming = new HashMap<>();
				renaming.put(var, nextLeader);
				exist = super.replace(renaming);	// requires normalized representation
			}
			else
				exist = super.exist(var);

			BDDER existEr = new BDDER(((BDDImpl) exist).getId(), l.removeVar(var));
			BDDER normalized = existEr.normalize();
			existEr.free();

			return normalized;
		}

		private BDDER exist_(BitSet vars) {
			EquivalenceRelation lNew = l;
			BitSet quantifiedVars = new BitSet();
			Map<Integer, Integer> renaming = new HashMap<>();

			for (int i = vars.nextSetBit(0); i >= 0; i = vars.nextSetBit(i + 1)) {
				if (l.containsVar(i)) {
					int nextLeader = l.nextLeader(i, vars);
					if (nextLeader < 0)
						quantifiedVars.set(i);
					else
						renaming.put(i, nextLeader);
				}
				else
					quantifiedVars.set(i);

				lNew = lNew.removeVar(i);
			}

			BDD exist = this;
			if (!renaming.isEmpty()) {
				exist = super.replace(renaming);	// requires normalized representation

				if (!quantifiedVars.isEmpty())
					exist = exist.exist(quantifiedVars);
			}
			else if (!quantifiedVars.isEmpty())
				exist = super.exist(quantifiedVars);
				

			BDDER existEr = new BDDER(((BDDImpl) exist).getId(), lNew);
			BDDER normalized = existEr.normalize();
			existEr.free();

			return normalized;
		}

		@Override
		public BDD forAll(int var) {
			throw new UnsupportedOperationException();
		}

		@Override
		public BDD forAll(BDD var) {
			throw new UnsupportedOperationException();
		}

		@Override
		public BDD simplify(BDD d) {
			throw new UnsupportedOperationException();
		}

		@Override
		public int[] varProfile() {
			throw new UnsupportedOperationException();
		}

		@Override
		public BDD replace(Map<Integer, Integer> renaming) {
			return replace_(renaming);
		}

		@Override
		public BDD replaceWith(Map<Integer, Integer> renaming) {
			BDDER replaceEr = replace_(renaming);
			setId(((BDDImpl) replaceEr).getId());
			l = replaceEr.l;

			return this;
		}

		BDDER replace_(Map<Integer, Integer> renaming) {
			BitSet nVars = super.vars();
			for (Integer v: renaming.values())
				if ((l.containsVar(v) || nVars.get(v)) && !renaming.keySet().contains(v))
					throw new ReplacementWithExistingVarException(v);

			BDD nNew = super.replace(renaming);

			// perform "simultaneous" substitution
			renaming = new HashMap<>(renaming);
			Map<Integer, Integer> varsOnTheRighSide = splitRenaming(renaming);
			EquivalenceRelation eNew = l.replace(varsOnTheRighSide);	// these renamings need to be performed first
			eNew = eNew.replace(renaming);

			BDD old = nNew;
			nNew = nNew.renameWithLeader(eNew);
			old.free();
			BDDER replaceEr = new BDDER(((BDDImpl) nNew).getId(), eNew);
			BDDER normalized = replaceEr.normalize();
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

			for (Integer i : values)
				if (renaming.keySet().contains(i)) {
					varsOnTheRighSide.put(i, renaming.get(i));
					renaming.remove(i);
				}

			return varsOnTheRighSide;
		}

		@Override
		public long pathCount() {
			throw new UnsupportedOperationException();
		}

		@Override
		public BDD ite(BDD thenBDD, BDD elseBDD) {
			throw new UnsupportedOperationException();
		}

		@Override
		public BDD relProd(BDD other, BDD var) {
			throw new UnsupportedOperationException();
		}

		@Override
		public BDD compose(BDD other, int var) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isEquivalentTo(BDD other) {
			if (!(other instanceof BDDER)) {
				return equivalentBDDs(other, getFullBDD());
			}
			BDDER o = (BDDER) other;
			return l.equals(o.l) && super.isEquivalentTo(o);
		}

		/**
		 * @return the full BDD, containing also equivalence constraints in l.
		 */
		BDD getFullBDD() {
			BDD full = super.copy();
			for (Pair pair: l.pairs()) {
				BDD biimp = makeVarBDDImpl(pair.first);
				biimp.biimpWith(makeVarBDDImpl(pair.second));
				full.andWith(biimp);
			}
			return full;
		}

		boolean equivalentBDDs(BDD bdd1, BDD bdd2) {
			if (bdd1.isOne()) {
				return bdd2.isOne();
			}
			if (bdd1.isZero()) {
				return bdd2.isZero();
			}
			return bdd1.var() == bdd2.var() && equivalentBDDs(bdd1.low(), bdd2.low())
					&& equivalentBDDs(bdd1.high(), bdd2.high());
		}

		@Override
		public int hashCodeAux() {
			return super.hashCodeAux() ^ l.hashCode();
		}

		@Override
		public int var() {
			BDD fullBDD = getFullBDD();
			int var = fullBDD.var();
			fullBDD.free();
			return var;
		}

		@Override
		public BDDER high() {
			BDD fullBDD = getFullBDD();
			BDD high = fullBDD.high();
			fullBDD.free();
			return new BDDER(((BDDImpl) high).getId());
		}

		@Override
		public BDDER low() {
			BDD fullBDD = getFullBDD();
			BDD low = fullBDD.low();
			fullBDD.free();
			return new BDDER(((BDDImpl) low).getId());
		}

		@Override
		public BDD squeezeEquiv(EquivalenceRelation r) {
			throw new UnsupportedOperationException();
		}

		@Override
		public BDD squeezeEquivWith(EquivalenceRelation r) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Factory getFactory() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String toString() {
			return l + System.lineSeparator() + super.toString();
		}

		@Override
		public BitSet vars() {
			BitSet res = super.vars();
			for (BitSet eqClass: l)
				res.or(eqClass);

			return res;
		}

		@Override
		public int maxVar() {
			return Math.max(l.maxVar(), super.maxVar());
		}

		@Override
		public BDD renameWithLeader(EquivalenceRelation r) {
			return new BDDER(((BDDImpl) getFullBDD().renameWithLeader(r)).getId());
		}

		@Override
		public Set<Pair> equivVars() {
			return getFullBDD().equivVars();
		}

		boolean isNormalized() {
			BDDER norm = normalize();
			return isEquivalentTo(norm);
		}

		EquivalenceRelation getEquiv() {
			return l;
		}
	}
}