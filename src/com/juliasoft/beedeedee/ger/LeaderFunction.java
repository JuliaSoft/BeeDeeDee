package com.juliasoft.beedeedee.ger;

import java.util.BitSet;

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
		for (BitSet eqClass : equivalenceClasses) {
			if (eqClass.get(var)) {
				return eqClass.nextSetBit(0);
			}
		}
		return var;
	}

	/**
	 * Finds the minimum leader that is greater or equal to c
	 * 
	 * @param c
	 * @return the minimum leader >= c
	 */
	public int minLeader(int c) {
		int min = Integer.MAX_VALUE;
		for (BitSet eqClass : equivalenceClasses) {
			int leader = eqClass.nextSetBit(0);
			if (leader >= c && leader < min) {
				min = leader;
			}
		}

		return min;
	}
}
