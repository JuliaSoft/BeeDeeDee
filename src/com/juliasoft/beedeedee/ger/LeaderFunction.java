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

}
