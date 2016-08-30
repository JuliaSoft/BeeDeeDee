package com.juliasoft.beedeedee.factories;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.juliasoft.beedeedee.bdd.Assignment;
import com.juliasoft.beedeedee.bdd.BDD;
import com.juliasoft.beedeedee.bdd.ReplacementWithExistingVarException;
import com.juliasoft.beedeedee.bdd.UnsatException;
import com.juliasoft.beedeedee.factories.EquivalenceRelation.Filter;

/**
 * A BDD factory using the ER representation.
 */
public class ERFactory extends Factory {

	public ERFactory(int utSize, int cacheSize) {
		super(utSize, cacheSize);
	}

	@Override
	public BDD makeZero() {
		try (GCLock lock = new GCLock()) {
			return new BDDER(ZERO, EquivalenceRelation.empty, false);
		}
	}

	@Override
	public BDD makeOne() {
		try (GCLock lock = new GCLock()) {
			return new BDDER(ONE, EquivalenceRelation.empty, false);
		}
	}

	@Override
	public BDD makeVar(int v) {
		try (GCLock lock = new GCLock()) {
			return new BDDER(innerMakeVar(v), EquivalenceRelation.empty, false);
		}
	}

	@Override
	public BDD makeNotVar(int v) {
		try (GCLock lock = new GCLock()) {
			return new BDDER(innerMakeNotVar(v), EquivalenceRelation.empty, false);
		}
	}

	@Override
	public int nodeCount(Collection<BDD> bdds) {
		throw new RuntimeException("Not yet implemented"); //TODO
	}

	private class UsefulLeaders implements Filter {
		private final int bdd;
		private UsefulLeaders(int bdd) {
			this.bdd = bdd;
		}
	
		@Override
		public boolean accept(BitSet eqClass) {
			return accept(eqClass, bdd);
		}
	
		private boolean accept(BitSet eqClass, int bdd) {
			if (bdd < FIRST_NODE_NUM)
				return false;
			else {
				int var = ut.var(bdd);
				return (eqClass.nextSetBit(0) != var && eqClass.get(var))
					|| accept(eqClass, ut.low(bdd)) || accept(eqClass, ut.high(bdd));
			}
		}
	}

	private class RenamerWithLeader {
		private final EquivalenceRelation equivalenceRelations;
		private final int resultId;
		private final int maxVar;
		private final RenameWithLeaderInternalCache rwlic;
	
		private RenamerWithLeader(int id, EquivalenceRelation equivalenceRelations) {
			this.equivalenceRelations = equivalenceRelations;
			this.maxVar = equivalenceRelations.maxVar();
			this.rwlic = new RenameWithLeaderInternalCache(20);
			this.resultId = renameWithLeader(id, 0, new BitSet());
		}
	
		private int renameWithLeader(final int bdd, final int level, final BitSet t) {
			int var;
			if (bdd < FIRST_NODE_NUM || (var = ut.var(bdd)) > maxVar)
				return bdd;
	
			int cached = rwlic.get(bdd, level, t);
			if (cached >= 0)
				return cached;
	
			Filter filter = new UsefulLeaders(bdd);
			// we further filter here, since some equivalence class might be irrelevant
			// from the residual bdd, but not for the whole bdd
			int minLeader = equivalenceRelations.getMinLeaderGreaterOrEqualtTo(level, var, filter), result, leader;
			if (minLeader >= 0) {
				BitSet augmented = (BitSet) t.clone();
				augmented.set(minLeader);
				result = MK(minLeader++, renameWithLeader(bdd, minLeader, t), renameWithLeader(bdd, minLeader, augmented));
			}
			else if ((leader = equivalenceRelations.getLeader(var, filter)) < 0)
				result = MK(var++, renameWithLeader(ut.low(bdd), var, t), renameWithLeader(ut.high(bdd), var, t));
			else if (leader == var) {
				BitSet augmented = (BitSet) t.clone();
				augmented.set(var);
				result = MK(var++, renameWithLeader(ut.low(bdd), var, t), renameWithLeader(ut.high(bdd), var, augmented));
			}
			else if (t.get(leader))
				result = renameWithLeader(ut.high(bdd), var + 1, t);
			else
				result = renameWithLeader(ut.low(bdd), var + 1, t);
	
			rwlic.put(bdd, level, t, result);
			return result;
		}

		private class RenameWithLeaderInternalCache {
			private final int size;
			private final int[] cache;
			private final int[] levels;
			private final BitSet[] ts;
			private final int[] results;

			private RenameWithLeaderInternalCache(int size) {
				this.size = size;
				this.cache = new int[size];
				this.levels = new int[size];
				this.ts = new BitSet[size];
				Arrays.fill(cache, -1);
				this.results = new int[size];
			}

			private int get(int f, int level, BitSet t) {
				int pos = hash(f, level, t);
				if (cache[pos] == f && levels[pos] == level && ts[pos].equals(t))
					return results[pos];

				return -1;
			}

			private void put(int f, int level, BitSet t, int res) {
				int pos = hash(f, level, t);

				cache[pos] = f;
				levels[pos] = level;
				ts[pos] = (BitSet) t.clone();
				results[pos] = res;
			}

			private int hash(int f, int level, BitSet t) {
				return Math.abs((f ^ (level << 24) ^ t.hashCode()) % size);
			}
		}
	}

	public static class EquivResult {
		private BitSet entailed;
		private BitSet disentailed;
		private ArrayList<Pair> equiv;
		private final static EquivResult emptyEquivResult = new EquivResult();

		private EquivResult() {
			this(new BitSet(), new BitSet(), new ArrayList<Pair>());
		}

		private EquivResult(EquivResult parent) {
			this(parent.entailed, parent.disentailed, parent.equiv);
		}

		private EquivResult(BitSet entailed, BitSet disentailed, ArrayList<Pair> equiv) {
			this.entailed = entailed;
			this.disentailed = disentailed;
			this.equiv = equiv;
		}
	}

	private class EquivVarsCalculator {
		private final EquivCache equivCache = ut.getEquivCache();
		private final Iterable<Pair> result;

		private EquivVarsCalculator(int id) {
			this.result = equivVars(id).equiv;
		}

		@SuppressWarnings("unchecked")
		private EquivResult equivVars(int bdd) {
			if (bdd < FIRST_NODE_NUM)
				return EquivResult.emptyEquivResult;

			EquivResult result = equivCache.get(bdd);
			if (result != null)
				return result;

			int var = ut.var(bdd);
			int low = ut.low(bdd);
			int high = ut.high(bdd);

			if (high == ZERO) {
				if (low != ONE) {
					result = new EquivResult(equivVars(low));
					result.disentailed = (BitSet) result.disentailed.clone();
					result.disentailed.set(var);
					int maxd = result.disentailed.length() - 1;
					if (var != maxd) {
						result.equiv = (ArrayList<Pair>) result.equiv.clone();
						result.equiv.add(new Pair(var, maxd));
					}
				}
				else {
					result = new EquivResult();
					result.disentailed.set(var);
				}
			}
			else if (low == ZERO) {
				if (high != ONE) {
					result = new EquivResult(equivVars(high));
					result.entailed = (BitSet) result.entailed.clone();
					result.entailed.set(var);
					int maxe = result.entailed.length() - 1;
					if (var != maxe) {
						result.equiv = (ArrayList<Pair>) result.equiv.clone();
						result.equiv.add(new Pair(var, maxe));
					}
				}
				else {
					result = new EquivResult();
					result.entailed.set(var);
				}
			}
			else if (high != ONE && low != ONE) {
				EquivResult resultTrue = equivVars(high);
				EquivResult resultFalse = equivVars(low);
				result = new EquivResult(resultTrue);
				result.entailed = (BitSet) result.entailed.clone();
				result.entailed.and(resultFalse.entailed);
				result.disentailed = (BitSet) result.disentailed.clone();
				result.disentailed.and(resultFalse.disentailed);
				// size does matter for efficiency
				if (resultFalse.equiv.size() < result.equiv.size()) {
					ArrayList<Pair> temp = result.equiv;
					result.equiv = (ArrayList<Pair>) resultFalse.equiv.clone();
					result.equiv.retainAll(temp);
				}
				else {
					result.equiv = (ArrayList<Pair>) result.equiv.clone();
					result.equiv.retainAll(resultFalse.equiv);
				}
				BitSet intersection = (BitSet) resultTrue.entailed.clone();
				intersection.and(resultFalse.disentailed);
				int length = intersection.length();
				if (length > 0)
					result.equiv.add(new Pair(var, length - 1));
			}
			else
				result = EquivResult.emptyEquivResult;

			equivCache.put(bdd, result);

			return result;
		}
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
			this(id, EquivalenceRelation.empty, true);
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

		private BDDER(int id, EquivalenceRelation l, boolean shouldNormalize) {
			super(id);

			if (id == ZERO)
				l = EquivalenceRelation.empty;

			this.l = l;

			if (shouldNormalize)
				normalize();
		}

		private void normalize() {
			if (id >= FIRST_NODE_NUM && (id >= NUMBER_OF_PREALLOCATED_NODES || !l.isEmpty())) {
				EquivalenceRelation eNew = l, eOld;
				int newId = id, oldId;

				do {
					eNew = (eOld = eNew).addPairs(new EquivVarsCalculator(newId).result);
					newId = renameWithLeader(oldId = newId, eNew);
				}
				while (newId != oldId || eNew != eOld);

				setId(newId);
				this.l = eNew;

				if (newId == ZERO) { // && !l.isEmpty()) {
					l = EquivalenceRelation.empty;
					//TODO throw new RuntimeException("normalizzazione di false con l = " + l);
				}
			}
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
		public boolean isVar() {
			return super.isVar() && l.isEmpty();
		}

		@Override
		public boolean isNotVar() {
			return super.isNotVar() && l.isEmpty();
		}

		private BDDER or(BDDER other, boolean intoThis) {
			int or;
			EquivalenceRelation newL;
			if (isZero()) {
				or = other.id;
				newL = other.l;
			} else if (other.isZero()) {
				or = id;
				newL = l;
			} else {
				or = orOfPairsInDifference(other);
				newL = l.intersection(other.l);
			}

			if (intoThis) {
				setId(or);
				l = newL;

				return this;
			} else
				return new BDDER(or, newL, false);
		}

		/**
		 * Subtracts the pairs in the other set from this set.
		 * 
		 * @param other the other set
		 */

		private int orOfPairsInDifference(BDDER other) {
			Collection<Pair> myPairs = l.pairs();
			Collection<Pair> otherPairs = other.l.pairs();

			BitSet toRemoveFromThis = new BitSet();
			for (Pair pair: myPairs)
				if (otherPairs.contains(pair))
					toRemoveFromThis.set(pair.second);

			BitSet toRemoveFromOther = new BitSet();
			for (Pair pair: otherPairs)
				if (myPairs.contains(pair))
					toRemoveFromOther.set(pair.second);

			int squeezedBDD1 = new EquivalenceSqueezer().squeezedId;
			for (Pair pair: myPairs)
				if (!toRemoveFromThis.get(pair.first) && !toRemoveFromThis.get(pair.second))
					squeezedBDD1 = innerAnd(squeezedBDD1, innerBiimp(innerMakeVar(pair.first), innerMakeVar(pair.second)));

			int squeezedBDD2 = other.new EquivalenceSqueezer().squeezedId;
			for (Pair pair: otherPairs)
				if (!toRemoveFromOther.get(pair.first) && !toRemoveFromOther.get(pair.second))
					squeezedBDD2 = innerAnd(squeezedBDD2, innerBiimp(innerMakeVar(pair.first), innerMakeVar(pair.second)));

			return innerOr(squeezedBDD1, squeezedBDD2);
		}

		@Override
		public BDD or(BDD other) {
			try (GCLock lock = new GCLock()) {
				return or((BDDER) other, false);
			}
		}

		@Override
		public BDD orWith(BDD other) {
			try (GCLock lock = new GCLock()) {
				or((BDDER) other, true);
			}

			other.free();
			return this;
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
				else if (ut.high(bdd) == ZERO)
					result = squeezeEquiv(ut.low(bdd));
				else
					result = squeezeEquiv(ut.high(bdd));

				cache.put(bdd, l, result);
				return result;
			}
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
		private BDDER and(BDDER other, boolean intoThis) {
			EquivalenceRelation newL = l.addClasses(other.l);
			int newId;
			boolean normalize = true;
			if (id == ONE && other.id == ONE) {
				newId = id;
				l = newL;
				normalize = false;
			}

			newId = innerAnd(id, other.id);
			if (intoThis) {
				setId(newId);
				l = newL;
				if (normalize) {
					normalize();
				}

				return this;
			}
			else
				return new BDDER(newId, newL, normalize);
		}

		private BDDER andNoNormalization(BDDER other, boolean intoThis) {
			if (intoThis) {
				setId(innerAnd(id, other.id));
				l = l.addClasses(other.l);

				return this;
			}
			else
				return new BDDER(innerAnd(id, other.id), l.addClasses(other.l), false);
		}

		@Override
		public BDD and(BDD other) {
			try (GCLock lock = new GCLock()) {
				return and((BDDER) other, false);
			}
		}

		@Override
		public BDD andWith(BDD other) {
			try (GCLock lock = new GCLock()) {
				and((BDDER) other, true);
			}

			other.free();
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
		private BDDER xor(BDDER other, boolean intoThis) {
			if (intoThis) {
				BDDER and = andNoNormalization(other, false);
				or(other, true);
				BDDER notAnd = and.notNoNormalization(true);
				and(notAnd, true);
				notAnd.free();
		
				return this;
			}
			else {
				BDDER and = andNoNormalization(other, false);
				BDDER or = or(other, false);
				BDDER notAnd = and.notNoNormalization(true);
				or.and(notAnd, true);
				notAnd.free();
		
				return or;
			}
		}

		@Override
		public BDD xor(BDD other) {
			try (GCLock lock = new GCLock()) {
				return xor((BDDER) other, false);
			}
		}

		@Override
		public BDD xorWith(BDD other) {
			try (GCLock lock = new GCLock()) {
				xor((BDDER) other, true);
			}

			other.free();
			return this;
		}

		@Override
		public BDD nand(BDD other) {
			try (GCLock lock = new GCLock()) {
				return andNoNormalization((BDDER) other, false).not(true);
			}
		}

		@Override
		public BDD nandWith(BDD other) {
			try (GCLock lock = new GCLock()) {
				andNoNormalization((BDDER) other, true);
				not(true);
			}

			other.free();
			return this;
		}

		/**
		 * Computes negation of this ER. It uses the identity !(L & n) = !L | !n =
		 * !p1 | !p2 | ... | !pn | !n. The result is normalized.
		 * 
		 * @return the negation
		 */

		private BDDER not(boolean intoThis) {
			int not = innerNot(id);
			for (BitSet bs: l)
				for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1))
					for (int j = bs.nextSetBit(i + 1); j >= 0; j = bs.nextSetBit(j + 1))
						not = innerOr(not, innerNot(innerBiimp(innerMakeVar(i), innerMakeVar(j))));

			if (intoThis) {
				setId(not);
				l = EquivalenceRelation.empty;
				if (!isVar() && !isNotVar())
					normalize();
				return this;
			}
			else
				return new BDDER(not, EquivalenceRelation.empty, !isVar() && !isNotVar());
		}

		private BDDER notNoNormalization(boolean intoThis) {
			int not = innerNot(id);
			for (BitSet bs: l)
				for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1))
					for (int j = bs.nextSetBit(i + 1); j >= 0; j = bs.nextSetBit(j + 1))
						not = innerOr(not, innerNot(innerBiimp(innerMakeVar(i), innerMakeVar(j))));

			if (intoThis) {
				setId(not);
				l = EquivalenceRelation.empty;
				return this;
			}
			else
				return new BDDER(not, EquivalenceRelation.empty, false);
		}

		@Override
		public BDD not() {
			try (GCLock lock = new GCLock()) {
				return not(false);
			}
		}

		@Override
		public BDD notWith() {
			try (GCLock lock = new GCLock()) {
				return not(true);
			}
		}

		/**
		 * Computes the implication of this BDDER with another. It uses the identity g1 -> g2 = !g1 | g2.
		 * 
		 * @param other the other BDDER
		 * @return the implication
		 */
		private BDDER imp(BDDER other, boolean intoThis) {
			if (intoThis)
				return notNoNormalization(true).or(other, true);
			else
				return notNoNormalization(false).or(other, true);
		}

		@Override
		public BDD imp(BDD other) {
			try (GCLock lock = new GCLock()) {
				return imp((BDDER) other, false);
			}
		}

		@Override
		public BDD impWith(BDD other) {
			try (GCLock lock = new GCLock()) {
				imp((BDDER) other, true);
			}

			other.free();
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
		private BDDER biimp(BDDER other, boolean intoThis) {
			if ((isVar() && other.isVar()) || (isNotVar() && other.isNotVar())) {
				int var1 = ut.var(id);
				int var2 = ut.var(other.id);
				if (var1 > var2) {
					int temp = var1;
					var1 = var2;
					var2 = temp;
				}

				if (intoThis) {
					setId(ONE);
					l = new EquivalenceRelation(new int[][] {{var1, var2}});
					return this;
				}
				else
					return new BDDER(ONE, new EquivalenceRelation(new int[][] {{var1, var2}}), false);
			}

			BDDER notG2 = other.not(false);
			BDDER or2 = notG2.or(this, true);

			if (intoThis) {
				notNoNormalization(true);
				or(other, true);
				and(or2, true);
				or2.free();

				return this;
			}
			else {
				BDDER notG1 = notNoNormalization(false);
				BDDER or1 = notG1.or(other, true);
				BDDER and = or1.and(or2, true);
				or2.free();

				return and;
			}
		}

		@Override
		public BDD biimp(BDD other) {
			try (GCLock lock = new GCLock()) {
				return biimp((BDDER) other, false);
			}
		}

		@Override
		public BDD biimpWith(BDD other) {
			try (GCLock lock = new GCLock()) {
				biimp((BDDER) other, true);
			}

			other.free();
			return this;
		}

		@Override
		public BDD copy() {
			try (GCLock lock = new GCLock()) {
				return new BDDER(this);
			}
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
				if (!vars.get(leader))
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
			try (GCLock lock = new GCLock()) {
				return exist_(var);
			}
		}

		@Override
		public BDD exist(BDD vars) {
			try (GCLock lock = new GCLock()) {
				return exist_(vars.vars());
			}
		}

		@Override
		public BDD exist(BitSet vars) {
			try (GCLock lock = new GCLock()) {
				return exist_(vars);
			}
		}

		private BDDER exist_(int var) {
			if (l.containsVar(var)) {
				Map<Integer, Integer> renaming = new HashMap<>();
				renaming.put(var, l.nextLeader(var));
				int exist = innerReplace(id, renaming, renaming.hashCode()); // requires normalized representation
				return new BDDER(exist, l.removeVar(var), true);
			}
			else
				return new BDDER(innerExist(id, var), l, true);
		}

		private BDDER exist_(BitSet vars) {
			EquivalenceRelation lNew = l;
			BitSet quantifiedVars = new BitSet();
			Map<Integer, Integer> renaming = new HashMap<>();

			for (int i = vars.nextSetBit(0); i >= 0; i = vars.nextSetBit(i + 1))
				if (l.containsVar(i)) {
					int nextLeader = l.nextLeader(i, vars);
					if (nextLeader < 0)
						quantifiedVars.set(i);
					else
						renaming.put(i, nextLeader);

					lNew = lNew.removeVar(i);
				}
				else
					quantifiedVars.set(i);

			int exist;

			if (!renaming.isEmpty()) {
				exist = innerReplace(id, renaming, renaming.hashCode());  // requires normalized representation

				if (quantifiedVars.isEmpty())
					// no need to normalize in this case
					return new BDDER(exist, lNew, false);
				else
					exist = innerQuantify(exist, quantifiedVars, true, quantifiedVars.hashCode());
			}
			else if (quantifiedVars.isEmpty())
				return new BDDER(id, l, false);
			else
				exist = innerQuantify(id, quantifiedVars, true, quantifiedVars.hashCode());

			return new BDDER(exist, lNew, true);
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
			BitSet nVars = super.vars();
			for (Integer v: renaming.values())
				if ((l.containsVar(v) || nVars.get(v)) && !renaming.containsKey(v))
					throw new ReplacementWithExistingVarException(v);

			try (GCLock lock = new GCLock()) {
				int nNew = innerReplace(id, renaming, renaming.hashCode());

				// perform "simultaneous" substitution
				renaming = new HashMap<>(renaming);
				Map<Integer, Integer> varsOnTheRighSide = splitRenaming(renaming);
				EquivalenceRelation eNew = l.replace(varsOnTheRighSide);	// these renamings need to be performed first
				eNew = eNew.replace(renaming);

				return new BDDER(renameWithLeader(nNew, eNew), eNew, true);
			}
		}

		@Override
		public BDD replaceWith(Map<Integer, Integer> renaming) {
			BitSet nVars = super.vars();
			for (Integer v: renaming.values())
				if ((l.containsVar(v) || nVars.get(v)) && !renaming.containsKey(v))
					throw new ReplacementWithExistingVarException(v);

			// perform "simultaneous" substitution
			renaming = new HashMap<>(renaming);
			Map<Integer, Integer> varsOnTheRighSide = splitRenaming(renaming);
			EquivalenceRelation eNew = l.replace(varsOnTheRighSide);	// these renamings need to be performed first
			eNew = eNew.replace(renaming);

			try (GCLock lock = new GCLock()) {
				int nNew = innerReplace(id, renaming, renaming.hashCode());

				setId(renameWithLeader(nNew, eNew));
				l = eNew;
				normalize();
			}

			return this;
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

		private int renameWithLeader(int id, EquivalenceRelation equivalenceRelations) {
			RenameWithLeaderCache cache = ut.getRWLCache();
			equivalenceRelations = equivalenceRelations.filter(new UsefulLeaders(id));
			int result = cache.get(id, equivalenceRelations);
			if (result >= 0)
				return result;
			else
				return new RenamerWithLeader(id, equivalenceRelations).resultId;
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
			else
				try (GCLock lock = new GCLock()) {
					return ((BDDImpl) other).getId() == getFullBDD();
				}
		}

		/**
		 * @return the full BDD, containing also equivalence constraints in l.
		 */
		private int getFullBDD() {
			int full = id;
			for (Pair pair: l.pairs())
				full = innerAnd(full, innerBiimp(innerMakeVar(pair.first), innerMakeVar(pair.second)));

			return full;
		}

		@Override
		public int hashCodeAux() {
			return super.hashCodeAux() ^ l.hashCode();
		}

		@Override
		public int var() {
			try (GCLock lock = new GCLock()) {
				return ut.var(getFullBDD());
			}
		}

		@Override
		public BDDER high() {
			try (GCLock lock = new GCLock()) {
				return new BDDER(ut.high(getFullBDD()));
			}
		}

		@Override
		public BDDER low() {
			try (GCLock lock = new GCLock()) {
				return new BDDER(ut.low(getFullBDD()));
			}
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

		boolean isNormalized() {
			try (GCLock lock = new GCLock()) {
				BDDER norm = new BDDER(getId(), l, true);
				boolean result = isEquivalentTo(norm);
				norm.free();
				return result;
			}
		}

		EquivalenceRelation getEquiv() {
			return l;
		}
	}
}