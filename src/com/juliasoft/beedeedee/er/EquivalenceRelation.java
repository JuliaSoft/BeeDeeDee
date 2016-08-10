package com.juliasoft.beedeedee.er;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.juliasoft.beedeedee.bdd.Assignment;

/**
 * A set of equivalence classes.
 */
public class EquivalenceRelation implements Iterable<BitSet> {
	private final List<BitSet> equivalenceClasses;

	public EquivalenceRelation() {
		this.equivalenceClasses = new ArrayList<>();
	}

	private EquivalenceRelation(List<BitSet> equivalenceClasses) {
		this.equivalenceClasses = equivalenceClasses;
	}

	public EquivalenceRelation(EquivalenceRelation parent) {
		this.equivalenceClasses = new ArrayList<>(parent.equivalenceClasses);
	}

	public EquivalenceRelation(EquivalenceRelation parent, Filter filter) {
		this.equivalenceClasses = new ArrayList<>();
		
		for (BitSet eqClass: parent)
			if (filter.accept(eqClass))
				equivalenceClasses.add(eqClass);
	}

	public EquivalenceRelation(int[][] classes) {
		this.equivalenceClasses = new ArrayList<>();
		for (int[] clazz: classes) {
			BitSet added = new BitSet();
			for (int i: clazz)
				added.set(i);

			equivalenceClasses.add(added);
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
		return equivalenceClasses.isEmpty();
	}

	public int size() {
		return equivalenceClasses.size();
	}

	public Iterator<BitSet> iterator() {
		return equivalenceClasses.iterator();
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
	boolean addPairs(Iterable<Pair> pairs) {
		boolean changed = false;
		for (Pair pair : pairs)
			changed |= addPair(pair);

		return changed;
	}

	/**
	 * Adds a pair to this set.
	 * 
	 * @param pair the pair to add
	 */
	// TODO ugly code! use union-find data structure?
	boolean addPair(Pair pair) {
		BitSet c1 = findClass(pair.first);
		BitSet c2 = findClass(pair.second);

		if (c1 != null) {
			if (c2 != null) {
				if (!c1.equals(c2)) {
					// join classes
					c1.or(c2);
					equivalenceClasses.remove(c2);
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
				equivalenceClasses.add(eqClass);
				return true;
			}
	}

	void addClasses(EquivalenceRelation other) {
		for (BitSet added: other.equivalenceClasses)
			addClass(added);
	}

	private void addClass(BitSet added) {
		BitSet intersected = null;
		List<BitSet> toRemove = new ArrayList<>();

		for (int pos = 0; pos < equivalenceClasses.size(); pos++) {
			BitSet cursor = equivalenceClasses.get(pos);

			if (cursor.intersects(added)) {
				if (intersected == null) {
					equivalenceClasses.set(pos, intersected = (BitSet) cursor.clone());
					intersected.or(added);
				}
				else {
					intersected.or(cursor);
					toRemove.add(cursor);
				}
			}
		}

		if (intersected == null)
			equivalenceClasses.add((BitSet) added.clone());
		else
			for (BitSet merged: toRemove)
				equivalenceClasses.remove(merged);
	}

	private BitSet findClass(int n) {
		for (BitSet eqClass : equivalenceClasses) {
			if (eqClass.get(n)) {
				return eqClass;
			}
		}
		return null;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof EquivalenceRelation && ((EquivalenceRelation) obj).equivalenceClasses.equals(equivalenceClasses);
	}

	@Override
	public int hashCode() {
		return equivalenceClasses.hashCode();
	}

	/**
	 * @return a copy of this set.
	 */
	public EquivalenceRelation copy() {
		EquivalenceRelation e = new EquivalenceRelation();
		for (BitSet eqClass : equivalenceClasses)
			equivalenceClasses.add((BitSet) eqClass.clone());

		return e;
	}

	@Override
	public String toString() {
		return equivalenceClasses.toString();
	}

	public int maxVar() {
		int max = 0;
		for (BitSet eqClass : equivalenceClasses) {
			max = Math.max(max, eqClass.length() - 1);
		}
		return max;
	}

	/**
	 * Updates the given assignment with information on equivalent variables.
	 * 
	 * @param a the assignment to update
	 */
	void updateAssignment(Assignment a) {
		classIteration: for (BitSet eqClass : equivalenceClasses) {
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

	private void setAll(Assignment a, BitSet eqClass, boolean value) {
		for (int i = eqClass.nextSetBit(0); i >= 0; i = eqClass.nextSetBit(i + 1)) {
			a.put(i, value);
		}
	}

	public void removeVar(int var) {
		BitSet c = findClass(var);
		if (c != null) {
			if (c.cardinality() > 2) {
				c.clear(var);
			} else {
				equivalenceClasses.remove(c);
			}
		}
	}

	public int nextLeader(int var) {
		return findClass(var).nextSetBit(var + 1);
	}

	public int nextLeader(int var, BitSet excludedVars) {
		BitSet c = findClass(var);
		int leader = c.nextSetBit(0);
		while (excludedVars.get(leader) || leader == var) {
			leader = c.nextSetBit(leader + 1);
			if (leader < 0) {
				break;
			}
		}
		return leader;
	}

	public void replace(Map<Integer, Integer> renaming) {
		for (Integer i : renaming.keySet()) {
			BitSet c = findClass(i);
			if (c != null) {
				c.clear(i);
				c.set(renaming.get(i));
			}
		}
	}

	public boolean containsVar(int var) {
		for (BitSet eqClass : equivalenceClasses) {
			if (eqClass.get(var)) {
				return true;
			}
		}
		return false;
	}

	public int getLeader(int var) {
		for (BitSet eqClass : equivalenceClasses) {
			if (eqClass.get(var)) {
				return eqClass.nextSetBit(0);
			}
		}
		return var;
	}

	public int getLeaderOfNonSingleton(int var, Filter filter) {
		for (BitSet eqClass : equivalenceClasses) {
			if (eqClass.get(var) && filter.accept(eqClass)) {
				return eqClass.nextSetBit(0);
			}
		}
		return -1;
	}

	/**
	 * Finds the minimum leader that is greater or equal to c
	 * 
	 * @param c
	 * @return the minimum leader >= c
	 */
	public int minLeaderGreaterOrEqualtTo(int c) {
		int min = Integer.MAX_VALUE;
		for (BitSet eqClass : equivalenceClasses) {
			int leader = eqClass.nextSetBit(0);
			if (leader >= c && leader < min)
				min = leader;
		}

		return min;
	}

	public static interface Filter {
		public boolean accept(BitSet eqClass);
	}

	public int minLeaderGreaterOrEqualtTo(int c, int var, Filter filter) {
		int min = -1;
		for (BitSet eqClass : equivalenceClasses) {
			int leader = eqClass.nextSetBit(0);
			if (leader >= c && (min < 0 || leader < min) && leader < var && (filter == null || filter.accept(eqClass)))
				min = leader;
		}

		return min;
	}
}