package com.juliasoft.beedeedee.ger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A set of equivalence classes.
 */
public class E implements Iterable<SortedSet<Integer>> {

	private Set<SortedSet<Integer>> equivalenceClasses;

	public E() {
		equivalenceClasses = new HashSet<>();
	}

	private Set<SortedSet<Integer>> intersect(Set<SortedSet<Integer>> equiv1, Set<SortedSet<Integer>> equiv2) {
		Set<SortedSet<Integer>> intersection = new HashSet<>();
		for (SortedSet<Integer> set1 : equiv1) {
			for (SortedSet<Integer> set2 : equiv2) {
				SortedSet<Integer> partialIntersection = new TreeSet<>(set1);
				partialIntersection.retainAll(set2);
				if (partialIntersection.size() > 1) {
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
	private void add(SortedSet<Integer> equivalenceClass) {
		equivalenceClasses.add(equivalenceClass);
	}

	public boolean isEmpty() {
		return equivalenceClasses.isEmpty();
	}

	public int size() {
		return equivalenceClasses.size();
	}

	public Iterator<SortedSet<Integer>> iterator() {
		return equivalenceClasses.iterator();
	}

	/**
	 * Generates a list containing all pairs of equivalent variables
	 * 
	 * @return the list of pairs
	 */
	public List<Pair> pairs() {
		ArrayList<Pair> pairs = new ArrayList<>();
		for (SortedSet<Integer> sortedSet : equivalenceClasses) {
			for (Integer second : sortedSet) {
				for (Integer first : sortedSet.headSet(second)) {
					pairs.add(new Pair(first, second));
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
		SortedSet<Integer> class1 = new TreeSet<>();
		for (int i : integers) {
			class1.add(i);
		}
		add(class1);
	}

	/**
	 * Adds pairs to this set.
	 * 
	 * @param pairs the list of pairs to add
	 */
	public void addPairs(List<Pair> pairs) {
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
		SortedSet<Integer> c1 = find(pair.first);
		SortedSet<Integer> c2 = find(pair.second);

		if (c1 != null) {
			if (c2 != null) {
				if (!c1.equals(c2)) {
					// join classes
					c1.addAll(c2);
					equivalenceClasses.remove(c2);
				}
			} else {
				c1.add(pair.second);
			}
		} else {
			if (c2 != null) {
				c2.add(pair.first);
			} else {
				addClass(pair.first, pair.second);
			}
		}
	}

	private SortedSet<Integer> find(int n) {
		for (SortedSet<Integer> eqClass : equivalenceClasses) {
			if (eqClass.contains(n)) {
				return eqClass;
			}
		}
		return null;
	}

	// FIXME specialized equals - efficiency?
	@Override
	public boolean equals(Object obj) {
		E other = (E) obj;
		Set<SortedSet<Integer>> e = new HashSet<>(equivalenceClasses);
		e.removeAll(other.equivalenceClasses);
		return e.isEmpty();
	}

	/**
	 * @return a copy of this set.
	 */
	public E copy() {
		E e = new E();
		for (SortedSet<Integer> eqClass : equivalenceClasses) {
			e.add(new TreeSet<>(eqClass));
		}
		return e;
	}

	@Override
	public String toString() {
		return equivalenceClasses.toString();
	}
}
