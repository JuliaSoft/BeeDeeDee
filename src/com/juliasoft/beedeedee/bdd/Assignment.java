package com.juliasoft.beedeedee.bdd;

/**
 * A truth assignment to variables.
 */

public interface Assignment {

	/**
	 * Determine if the given BDD variable holds in this assignment.
	 *
	 * @param var the variable
	 * @return true if and only if {@code var} holds in this assignment
	 * @throws IndexOutOfBoundsException if the variable does not belong to this assignment
	 */

	public boolean holds(BDD var) throws IndexOutOfBoundsException;

	/**
	 * @return a <em>minterm</em> representation of the assignment
	 */
	
	public BDD toBDD();	
}