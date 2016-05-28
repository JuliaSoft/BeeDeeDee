package com.juliasoft.beedeedee.ger;

import java.util.BitSet;

/**
 * A {@link LeaderFunction} excluding a particular variable from the set of
 * leaders.
 */
public class ExcludingLeaderFunction extends LeaderFunction {

	private int var;

	/**
	 * Constructs an ExcludingLeaderFunction.
	 * 
	 * @param equivalenceClasses the equivalence relation to consider
	 * @param var the variable to exclude from leaders
	 */
	public ExcludingLeaderFunction(E equivalenceClasses, int var) {
		super(equivalenceClasses);
		this.var = var;
	}

	@Override
	protected int leader(BitSet eqClass) {
		int l = eqClass.nextSetBit(0);
		if (l == var) {
			l = eqClass.nextSetBit(l + 1);
		}
		return l;
	}

	@Override
	public boolean minLIB(int c, int v) {
		if (c == var) {
			c = Integer.MAX_VALUE;
		}
		if (v == var) {
			v = Integer.MAX_VALUE;
		}
		int minLeader = getLeaders().nextSetBit(c);
		return minLeader >= c && minLeader < v;
	}
}
