package com.juliasoft.beedeedee.ger;

import java.util.SortedSet;

/**
 * A function mapping each integer value to the least value of its equivalence
 * class.
 */
public class LeaderFunction {

	private E equivalenceClasses;

	public LeaderFunction(E equivalenceClasses) {
		this.equivalenceClasses = equivalenceClasses;
	}

	public int get(int var) {
		for (SortedSet<Integer> eqClass : equivalenceClasses) {
			if (eqClass.contains(var)) {
				return eqClass.first();
			}
		}
		return var;
	}

}
