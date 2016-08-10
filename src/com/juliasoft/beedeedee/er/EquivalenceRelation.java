package com.juliasoft.beedeedee.er;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.juliasoft.beedeedee.bdd.Assignment;

/**
 * A set of equivalence classes.
 */
public class EquivalenceRelation implements Iterable<BitSet> {
	private final BitSet[] equivalenceClasses;

	private EquivalenceRelation(List<BitSet> equivalenceClasses) {
		this.equivalenceClasses = new BitSet[equivalenceClasses.size()];
		int pos = 0;
		for (BitSet eqClass: equivalenceClasses)
			this.equivalenceClasses[pos++] = eqClass;
	}

	public EquivalenceRelation() {
		this.equivalenceClasses = new BitSet[0];
	}

	public EquivalenceRelation(EquivalenceRelation parent) {
		int length = parent.equivalenceClasses.length;
		this.equivalenceClasses = new BitSet[length];
		for (int pos = 0; pos < length; pos++)
			this.equivalenceClasses[pos] = (BitSet) parent.equivalenceClasses[pos].clone();
	}

	public EquivalenceRelation(EquivalenceRelation parent, Filter filter) {
		this(filterClasses(parent, filter));
	}

	private static List<BitSet> filterClasses(EquivalenceRelation parent, Filter filter) {
		List<BitSet> equivalenceClasses = new ArrayList<>();
		for (BitSet eqClass: parent)
			if (filter.accept(eqClass))
				equivalenceClasses.add(eqClass);

		return equivalenceClasses;
	}

	public EquivalenceRelation(int[][] classes) {
		this.equivalenceClasses = new BitSet[classes.length];

		for (int pos = 0; pos < classes.length; pos++) {
			BitSet added = new BitSet();
			for (int i: classes[pos])
				added.set(i);

			equivalenceClasses[pos] = added;
		}
	}

	/**
	 * Computes the intersection of two E's.
	 * 
	 * @param other the other set
	 * @return the resulting set
	 */

	public EquivalenceRelation intersection(EquivalenceRelation other) {
		List<BitSet> intersection = new ArrayList<>();
		for (BitSet set1 : equivalenceClasses) {
			for (BitSet set2 : other.equivalenceClasses) {
				BitSet element = new BitSet();
				element.or(set1);
				element.and(set2);
				if (element.cardinality() > 1)
					intersection.add(element);
			}
		}

		return new EquivalenceRelation(intersection);
	}

	public boolean isEmpty() {
		return equivalenceClasses.length == 0;
	}

	public int size() {
		return equivalenceClasses.length;
	}

	@Override
	public Iterator<BitSet> iterator() {
		return Arrays.asList((BitSet[]) equivalenceClasses).iterator();
	}

	/**
	 * Generates a list containing all pairs of equivalent variables
	 * 
	 * @return the list of pairs
	 */
	public List<Pair> pairs() {
		ArrayList<Pair> pairs = new ArrayList<>();
		for (BitSet bs : equivalenceClasses)
			for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1))
				for (int j = bs.nextSetBit(i + 1); j >= 0; j = bs.nextSetBit(j + 1))
					pairs.add(new Pair(i, j));

		return pairs;
	}

	/**
	 * Subtracts the pairs in the other set from this set.
	 * 
	 * @param other the other set
	 * @return the list of the pairs of this set not contained in the other
	 */
	public List<Pair> pairsInDifference(EquivalenceRelation other) {
		List<Pair> myPairs = pairs();
		List<Pair> otherPairs = other.pairs();
		BitSet toRemove = new BitSet();
		List<Pair> result = new ArrayList<>();
		for (Pair pair : myPairs) {
			if (otherPairs.contains(pair)) {
				toRemove.set(pair.second);
			}
		}
		for (Pair pair : myPairs) {
			if (!toRemove.get(pair.first) && !toRemove.get(pair.second)) {
				result.add(pair);
			}
		}
		return result;
	}

	/**
	 * Adds pairs to this set.
	 * 
	 * @param pairs the pairs to add
	 */
	public EquivalenceRelation addPairs(Iterable<Pair> pairs) {
		List<BitSet> newEquivalenceClasses = new ArrayList<>();
		for (BitSet eqClass: equivalenceClasses)
			newEquivalenceClasses.add((BitSet) eqClass.clone());

		for (Pair pair: pairs)
			addPair(pair, newEquivalenceClasses);

		return new EquivalenceRelation(newEquivalenceClasses);
	}

	/**
	 * Adds a pair to this set.
	 * 
	 * @param pair the pair to add
	 */
	// TODO ugly code! use union-find data structure?
	private static boolean addPair(Pair pair, List<BitSet> where) {
		BitSet c1 = findClass(pair.first, where);
		BitSet c2 = findClass(pair.second, where);

		if (c1 != null) {
			if (c2 != null) {
				if (!c1.equals(c2)) {
					// join classes
					c1.or(c2);
					where.remove(c2);
					return true;
				}
				else
					return false;
			}
			else {
				c1.set(pair.second);
				return true;
			}
		}
		else
			if (c2 != null) {
				c2.set(pair.first);
				return true;
			}
			else {
				BitSet eqClass = new BitSet();
				eqClass.set(pair.first);
				eqClass.set(pair.second);
				where.add(eqClass);
				return true;
			}
	}

	public EquivalenceRelation addClasses(EquivalenceRelation other) {
		List<BitSet> newEquivalenceClasses = new ArrayList<>();
		for (BitSet eqClass: equivalenceClasses)
			newEquivalenceClasses.add((BitSet) eqClass.clone());

		for (BitSet added: other.equivalenceClasses)
			addClass(added, newEquivalenceClasses);

		return new EquivalenceRelation(newEquivalenceClasses);
	}

	private static void addClass(BitSet added, List<BitSet> where) {
		BitSet intersected = null;
		List<BitSet> toRemove = new ArrayList<>();

		for (int pos = 0; pos < where.size(); pos++) {
			BitSet cursor = where.get(pos);

			if (cursor.intersects(added)) {
				if (intersected == null) {
					where.set(pos, intersected = (BitSet) cursor.clone());
					intersected.or(added);
				}
				else {
					intersected.or(cursor);
					toRemove.add(cursor);
				}
			}
		}

		if (intersected == null)
			where.add((BitSet) added.clone());
		else
			for (BitSet merged: toRemove)
				where.remove(merged);
	}

	private static BitSet findClass(int n, BitSet[] where) {
		for (BitSet eqClass: where)
			if (eqClass.get(n))
				return eqClass;

		return null;
	}

	private static BitSet findClass(int n, List<BitSet> where) {
		for (BitSet eqClass: where)
			if (eqClass.get(n))
				return eqClass;

		return null;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof EquivalenceRelation &&
			Arrays.deepEquals(((EquivalenceRelation) obj).equivalenceClasses, equivalenceClasses);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(equivalenceClasses);
	}

	/**
	 * @return a copy of this set.
	 */
	public EquivalenceRelation copy() {
		List<BitSet> equivalenceClasses = new ArrayList<>();
		for (BitSet eqClass: this.equivalenceClasses)
			equivalenceClasses.add((BitSet) eqClass.clone());

		return new EquivalenceRelation(equivalenceClasses);
	}

	@Override
	public String toString() {
		return equivalenceClasses.toString();
	}

	public int maxVar() {
		int max = 0;
		for (BitSet eqClass: equivalenceClasses)
			max = Math.max(max, eqClass.length() - 1);

		return max;
	}

	/**
	 * Updates the given assignment with information on equivalent variables.
	 * 
	 * @param a the assignment to update
	 */
	void updateAssignment(Assignment a) {
		classIteration:
		for (BitSet eqClass: equivalenceClasses) {
			for (int i = eqClass.nextSetBit(0); i >= 0; i = eqClass.nextSetBit(i + 1)) {
				try {
					if (a.holds(i)) {
						setAll(a, eqClass, true);
						continue classIteration;
					}
				} catch (Exception e) {
					// ignore exception if variable not in assignment
				}
			}
			setAll(a, eqClass, false);
		}
	}

	private static void setAll(Assignment a, BitSet eqClass, boolean value) {
		for (int i = eqClass.nextSetBit(0); i >= 0; i = eqClass.nextSetBit(i + 1))
			a.put(i, value);
	}

	public EquivalenceRelation removeVar(int var) {
		BitSet c = findClass(var, equivalenceClasses);
		if (c != null)
			if (c.cardinality() > 2) {
				c.clear(var);
				return this;
			}
			else {
				List<BitSet> newEquivalenceClasses = new ArrayList<>();
				for (BitSet eqClass: equivalenceClasses)
					if (!eqClass.equals(c))
						newEquivalenceClasses.add(eqClass);

				return new EquivalenceRelation(newEquivalenceClasses);
			}
		else
			return this;
	}

	public int nextLeader(int var) {
		return findClass(var, equivalenceClasses).nextSetBit(var + 1);
	}

	public int nextLeader(int var, BitSet excludedVars) {
		BitSet c = findClass(var, equivalenceClasses);
		int leader = c.nextSetBit(0);
		while (excludedVars.get(leader) || leader == var) {
			leader = c.nextSetBit(leader + 1);
			if (leader < 0)
				break;
		}
		return leader;
	}

	public void replace(Map<Integer, Integer> renaming) {
		for (Integer i: renaming.keySet()) {
			BitSet c = findClass(i, equivalenceClasses);
			if (c != null) {
				c.clear(i);
				c.set(renaming.get(i));
			}
		}
	}

	public boolean containsVar(int var) {
		for (BitSet eqClass: equivalenceClasses)
			if (eqClass.get(var))
				return true;

		return false;
	}

	public int getLeader(int var) {
		for (BitSet eqClass: equivalenceClasses)
			if (eqClass.get(var))
				return eqClass.nextSetBit(0);

		return var;
	}

	public int getLeader(int var, Filter filter) {
		for (BitSet eqClass: equivalenceClasses)
			if (eqClass.get(var) && filter.accept(eqClass))
				return eqClass.nextSetBit(0);

		return -1;
	}

	/**
	 * Finds the minimum leader that is greater or equal to c
	 * 
	 * @param c
	 * @return the minimum leader >= c, or -1 if it does not exist
	 */
	public int getMinLeaderGreaterOrEqualtTo(int c, int var, Filter filter) {
		int min = -1;
		for (BitSet eqClass: equivalenceClasses) {
			int leader = eqClass.nextSetBit(0);
			if (leader >= c && (min < 0 || leader < min) && leader < var && filter.accept(eqClass))
				min = leader;
		}
	
		return min;
	}

	public static interface Filter {
		public boolean accept(BitSet eqClass);
	}
}