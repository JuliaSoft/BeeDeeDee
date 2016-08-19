package com.juliasoft.beedeedee.er;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import com.juliasoft.beedeedee.bdd.Assignment;
import com.juliasoft.beedeedee.bdd.BDD;
import com.juliasoft.beedeedee.bdd.ReplacementWithExistingVarException;
import com.juliasoft.beedeedee.bdd.UnsatException;
import com.juliasoft.beedeedee.factories.Factory;
import com.juliasoft.beedeedee.factories.SqueezeEquivCache;

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
			this(id, new EquivalenceRelation(), true);
		}

		/**
		 * Copy constructor.
		 */
		private BDDER(BDDER parent) {
			super(parent.id);

			this.l = parent.l;
		}

		/**
		 * Normalizing constructor.
		 * 
		 * @param id the BDD id
		 * @param l the equivalence relation
		 * @param shouldNormalize true if and only if normalization must be applied
		 */

		BDDER(int id, EquivalenceRelation l, boolean shouldNormalize) {
			super(id);

			if (shouldNormalize) {
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

				setId(((BDDImpl) nNew).getId());
				this.l = eNew;
			}
			else
				this.l = l;
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
			if (other instanceof BDDER)
				return or_((BDDER) other);
			else
				throw new NotBDDERException();
		}

		@Override
		public BDD orWith(BDD other) {
			if (other instanceof BDDER) {
				BDDER otherBddEr = (BDDER) other;
				BDDER or_ = or_(otherBddEr);
				setId(or_.getId());
				l = or_.l;
				otherBddEr.free();
				or_.free();

				return this;
			}
			else
				throw new NotBDDERException();
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
			BDDER result = new BDDER(((BDDImpl) n1).getId(), l.intersection(other.l), false);
			n1.free();
			return result;
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
			ReentrantLock lock = ut.getGCLock();
			lock.lock();
			try {
				return mdBDDImpl(new EquivalenceSqueezer().squeezedId);
			}
			finally {
				lock.unlock();
			}
		}

		private class EquivalenceSqueezer {
			private final SqueezeEquivCache cache = ut.getSqueezeEquivCache();
			private final int squeezedId;

			private EquivalenceSqueezer() {
				this.squeezedId = squeezeEquiv(id);
			}

			private int squeezeEquiv(int bdd) {
				if (bdd < FIRST_NODE_NUM)
					return bdd;

				int cached = cache.get(bdd, l);
				if (cached >= 0)
					return cached;

				int var = ut.var(bdd), result;
				if (l.getLeader(var) == var)
					result = MK(var, squeezeEquiv(ut.low(bdd)), squeezeEquiv(ut.high(bdd)));
				else if (ut.high(bdd) == 0)
					result = squeezeEquiv(ut.low(bdd));
				else
					result = squeezeEquiv(ut.high(bdd));

				cache.put(bdd, l, result);
				return result;
			}
		}

		@Override
		public BDD and(BDD other) {
			if (other instanceof BDDER)
				return and_((BDDER) other);
			else
				throw new NotBDDERException();
		}

		@Override
		public BDD andWith(BDD other) {
			if (other instanceof BDDER) {
				BDDER otherBddEr = (BDDER) other;
				BDDER and_ = and_(otherBddEr);
				setId(and_.getId());
				l = and_.l;
				otherBddEr.free();
				and_.free();

				return this;
			}
			else
				throw new NotBDDERException();
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
			BDDER result = new BDDER(((BDDImpl) and).getId(), ln, true);
			and.free();
			return result;
		}

		@Override
		public BDD xor(BDD other) {
			if (other instanceof BDDER)
				return xor_((BDDER) other);
			else
				throw new NotBDDERException();
		}

		@Override
		public BDD xorWith(BDD other) {
			if (other instanceof BDDER) {
				BDDER otherBddEr = (BDDER) other;
				BDDER xor_ = xor_(otherBddEr);
				setId(((BDDImpl) xor_).getId());
				l = xor_.l;
				otherBddEr.free();
				xor_.free();

				return this;
			}
			else
				throw new NotBDDERException();
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
			and.free();
			BDDER result = or.and_(notAnd);
			or.free();
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
			BDDER result = new BDDER(((BDDImpl) not).getId());
			not.free();
			return result;
		}

		@Override
		public BDD nand(BDD other) {
			if (other instanceof BDDER) {
				BDDER and_ = and_((BDDER) other);
				BDDER not_ = and_.not_();
				and_.free();

				return not_;
			}
			else
				throw new NotBDDERException();
		}

		@Override
		public BDD nandWith(BDD other) {
			if (other instanceof BDDER) {
				BDDER otherBddEr = (BDDER) other;
				BDDER and_ = and_(otherBddEr);
				otherBddEr.free();
				BDDER not_ = and_.not_();
				and_.free();
				setId(((BDDImpl) not_).getId());
				l = not_.l;
				not_.free();

				return this;
			}
			else
				throw new NotBDDERException();
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
			not_.free();

			return this;
		}

		@Override
		public BDD imp(BDD other) {
			if (other instanceof BDDER)
				return imp_((BDDER) other);
			else
				throw new NotBDDERException();
		}

		@Override
		public BDD impWith(BDD other) {
			if (other instanceof BDDER) {
				BDDER otherBddEr = (BDDER) other;
				BDDER imp_ = imp_(otherBddEr);
				setId(((BDDImpl) imp_).getId());
				l = imp_.l;
				otherBddEr.free();
				imp_.free();

				return this;
			}
			else
				throw new NotBDDERException();
		}

		/**
		 * Computes the implication of this BDDER with another. It uses the identity g1 -> g2 = !g1 | g2.
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
			if (other instanceof BDDER)
				return biimp_((BDDER) other);
			else
				throw new NotBDDERException();
		}

		@Override
		public BDD biimpWith(BDD other) {
			if (other instanceof BDDER) {
				BDDER otherBddEr = (BDDER) other;
				BDDER biimp_ = biimp_(otherBddEr);
				setId(((BDDImpl) biimp_).getId());
				l = biimp_.l;
				otherBddEr.free();
				biimp_.free();

				return this;
			}
			else
				throw new NotBDDERException();
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
			notG1.free();
			notG2.free();

			BDDER and = or1.and_(or2);
			or1.free();
			or2.free();

			return and;
		}

		@Override
		public BDD copy() {
			return new BDDER(this);
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

			BDDER result = new BDDER(((BDDImpl) exist).getId(), l.removeVar(var), true);
			exist.free();
			return result;
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

				if (!quantifiedVars.isEmpty()) {
					BDD old = exist;
					exist = exist.exist(quantifiedVars);
					old.free();
				}
			}
			else if (!quantifiedVars.isEmpty())
				exist = super.exist(quantifiedVars);
				

			BDDER result = new BDDER(((BDDImpl) exist).getId(), lNew, true);
			exist.free();
			return result;
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
			replaceEr.free();

			return this;
		}

		BDDER replace_(Map<Integer, Integer> renaming) {
			BitSet nVars = super.vars();
			for (Integer v: renaming.values())
				if ((l.containsVar(v) || nVars.get(v)) && !renaming.containsKey(v))
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
			BDDER result = new BDDER(((BDDImpl) nNew).getId(), eNew, true);
			nNew.free();
			return result;
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

			for (Integer i: values)
				if (renaming.containsKey(i)) {
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
			if (other instanceof BDDER) {
				BDDER o = (BDDER) other;
				return l.equals(o.l) && super.isEquivalentTo(o);
			}
			else {
				BDD full = getFullBDD();
				boolean result = equivalentBDDs(other, full);
				full.free();
				return result;
			}
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
			if (bdd1.isOne())
				return bdd2.isOne();
			else if (bdd1.isZero())
				return bdd2.isZero();
			else
				return bdd1.var() == bdd2.var() && equivalentBDDs(bdd1.low(), bdd2.low()) && equivalentBDDs(bdd1.high(), bdd2.high());
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
			BDDER result = new BDDER(((BDDImpl) high).getId());
			high.free();
			return result;
		}

		@Override
		public BDDER low() {
			BDD fullBDD = getFullBDD();
			BDD low = fullBDD.low();
			fullBDD.free();
			BDDER result = new BDDER(((BDDImpl) low).getId());
			low.free();
			return result;
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
			BDD full = getFullBDD();
			BDD renamed = full.renameWithLeader(r);
			full.free();
			
			BDD result = new BDDER(((BDDImpl) renamed).getId());
			renamed.free();
			return result;
		}

		@Override
		public Set<Pair> equivVars() {
			throw null;
			//return getFullBDD().equivVars();
		}

		boolean isNormalized() {
			BDDER norm = new BDDER(getId(), l, true);
			boolean result = isEquivalentTo(norm);
			norm.free();
			return result;
		}

		EquivalenceRelation getEquiv() {
			return l;
		}
	}
}