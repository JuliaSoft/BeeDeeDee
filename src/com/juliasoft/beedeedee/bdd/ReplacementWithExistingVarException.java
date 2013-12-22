package com.juliasoft.beedeedee.bdd;

/**
 * Thrown by {@code replace*()} methods when a variable gets replaced with one already in the BDD.
 */

@SuppressWarnings("serial")
public class ReplacementWithExistingVarException extends RuntimeException {

	private final int varNum;

	public ReplacementWithExistingVarException(int varNum) {
		this.varNum = varNum;
	}

	public int getVarNum() {
		return varNum;
	}

	@Override
	public String toString() {
		return "trying to replace with variable " + varNum + " which is already in the BDD";
	}
}
