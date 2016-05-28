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
				return leader(eqClass);
			}
		}
		return var;
	}

	public BitSet getLeaders() {
		BitSet leaders = new BitSet();
		for (BitSet eqClass : equivalenceClasses) {
			leaders.set(leader(eqClass));
		}
		return leaders;
	}

	protected int leader(BitSet eqClass) {
		return eqClass.nextSetBit(0);
	}

	public boolean minLIB(int c, int var) {
		int minLeader = getLeaders().nextSetBit(c);
		return minLeader >= c && minLeader < var;
	}
}
