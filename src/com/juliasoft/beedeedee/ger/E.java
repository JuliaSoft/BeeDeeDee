package com.juliasoft.beedeedee.ger;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.juliasoft.beedeedee.bdd.Assignment;

/**
 * A set of equivalence classes.
 */
public class E implements Iterable<BitSet> {

	private Set<BitSet> equivalenceClasses;

	public E() {
		equivalenceClasses = new HashSet<>();
	}

	private Set<BitSet> intersect(Set<BitSet> equiv1, Set<BitSet> equiv2) {
		Set<BitSet> intersection = new HashSet<>();
		for (BitSet set1 : equiv1) {
			for (BitSet set2 : equiv2) {
				BitSet partialIntersection = new BitSet();
				partialIntersection.or(set1);
				partialIntersection.and(set2);
				if (partialIntersection.cardinality() > 1) {
					intersection.add(partialIntersection);
				}
			}
		}
		return intersection;
	}

	/**
	 * Computes the intersection of two E's.
	 * 
	 * @param other the other set
	 * @return the resulting set
	 */
	public E intersect(E other) {
		E result = new E();
		result.equivalenceClasses = intersect(equivalenceClasses, other.equivalenceClasses);
		return result;
	}

	/**
	 * Adds an integer equivalence class to this set.
	 * 
	 * @param equivalenceClass the class to add
	 */
	void add(BitSet equivalenceClass) {
		equivalenceClasses.add(equivalenceClass);
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
		for (BitSet bs : equivalenceClasses) {
			for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
				for (int j = bs.nextSetBit(i + 1); j >= 0; j = bs.nextSetBit(j + 1)) {
					pairs.add(new Pair(i, j));
				}
			}
		}
		return pairs;
	}

	/**
	 * Subtracts the pairs in the other set from this set.
	 * 
	 * @param other the other set
	 * @return the list of the pairs of this set not contained in the other
	 */
	public List<Pair> subtract(E other) {
		List<Pair> myPairs = pairs();
		List<Pair> otherPairs = other.pairs();
		ArrayList<Pair> result = new ArrayList<>();
		for (Pair pair : myPairs) {
			if (!otherPairs.contains(pair)) {
				result.add(pair);
			}
		}
		return result;
	}

	/**
	 * Adds an equivalence class (set of integers) to this set.
	 * 
	 * @param integers the integers forming the equivalence class
	 */
	public void addClass(int... integers) {
		BitSet class1 = new BitSet();
		for (int i : integers) {
			class1.set(i);
		}
		add(class1);
	}

	/**
	 * Adds pairs to this set.
	 * 
	 * @param pairs the pairs to add
	 */
	public void addPairs(Iterable<Pair> pairs) {
		for (Pair pair : pairs) {
			addPair(pair);
		}
	}

	/**
	 * Adds a pair to this set.
	 * 
	 * @param pair the pair to add
	 */
	// TODO ugly code! use union-find data structure?
	public void addPair(Pair pair) {
		BitSet c1 = findClass(pair.first);
		BitSet c2 = findClass(pair.second);

		if (c1 != null) {
			if (c2 != null) {
				if (!c1.equals(c2)) {
					// join classes
					c1.or(c2);
					equivalenceClasses.remove(c2);
				}
			} else {
				c1.set(pair.second);
			}
		} else {
			if (c2 != null) {
				c2.set(pair.first);
			} else {
				addClass(pair.first, pair.second);
			}
		}
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
		return obj instanceof E && ((E) obj).equivalenceClasses.equals(equivalenceClasses);
	}

	@Override
	public int hashCode() {
		return equivalenceClasses.hashCode();
	}

	/**
	 * @return a copy of this set.
	 */
	public E copy() {
		E e = new E();
		for (BitSet eqClass : equivalenceClasses) {
			BitSet eqClassCopy = new BitSet();
			eqClassCopy.or(eqClass);
			e.add(eqClassCopy);
		}
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

	public boolean containsVar(int i) {
		for (BitSet eqClass : equivalenceClasses) {
			if (eqClass.get(i)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Updates the given assignment with information on equivalent variables.
	 * 
	 * @param a the assignment to update
	 */
	void updateAssignment(Assignment a) {
		classIteration: for (BitSet eqClass : equivalenceClasses) {
			for (int i = eqClass.nextSetBit(0); i >= 0; i = eqClass.nextSetBit(i + 1)) {
				boolean holds = false;
				try {
					holds = a.holds(i);
				} catch (Exception e) {
					// ignore exception if variable not in assignment
				}
				if (holds) {
					setAll(a, eqClass, true);
					continue classIteration;
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
}
