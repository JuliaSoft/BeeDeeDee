/* 
  Copyright 2014 Julia s.r.l.
    
  This file is part of BeeDeeDee.

  BeeDeeDee is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  BeeDeeDee is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with BeeDeeDee.  If not, see <http://www.gnu.org/licenses/>.
*/
package com.juliasoft.beedeedee.bdd;

import java.util.List;
import java.util.Map;

/**
 * A Binary Decision Diagram.
 */

public interface BDD {

	/**
	 * Releases this BDD. Its resources become eligible for garbage collection.
	 * This BDD is no more usable after this call.
	 */

	public abstract void free();

	/**
	 * @return true if this BDD represents the constant zero
	 */
	
	public abstract boolean isZero();

	/**
	 * @return true if this BDD represents the constant one
	 */
	
	public abstract boolean isOne();
	
	/**
	 * Computes the logical OR of this bdd with another one.
	 * 
	 * @param other the other bdd
	 * @return the resulting BDD object
	 */
	
	public abstract BDD or(BDD other);

	/**
	 * Computes the logical OR of this bdd with another one,
	 * store the result in this object, and frees the other.
	 * 
	 * @param other the other bdd
	 * @return the resulting BDD object (this)
	 */
	
	public abstract BDD orWith(BDD other);

	/**
	 * Computes the logical AND of this bdd with another one.
	 * 
	 * @param other the other bdd
	 * @return the resulting BDD object
	 */
	
	public abstract BDD and(BDD other);

	/**
	 * Computes the logical AND of this bdd with another one,
	 * store the result in this object, and frees the other.
	 * 
	 * @param other the other bdd
	 * @return the resulting BDD object (this)
	 */
	
	public abstract BDD andWith(BDD other);

	/**
	 * Computes the logical XOR of this bdd with another one.
	 * 
	 * @param other the other bdd
	 * @return the resulting BDD object
	 */
	
	public abstract BDD xor(BDD other);
	
	/**
	 * Computes the logical XOR of this bdd with another one,
	 * store the result in this object, and frees the other.
	 * 
	 * @param other the other bdd
	 * @return the resulting BDD object (this)
	 */
	
	public abstract BDD xorWith(BDD other);

	/**
	 * Computes the logical NAND of this bdd with another one.
	 * 
	 * @param other the other bdd
	 * @return the resulting BDD object
	 */
	
	public abstract BDD nand(BDD other);

	/**
	 * Computes the logical NAND of this bdd with another one,
	 * store the result in this object, and frees the other.
	 * 
	 * @param other the other bdd
	 * @return the resulting BDD object (this)
	 */
	
	public abstract BDD nandWith(BDD other);

	/**
	 * Computes the logical NOT of this bdd.
	 * 
	 * @return the resulting BDD object
	 */
	
	public abstract BDD not();

	/**
	 * Computes the logical NOT of this bdd and store the result in this object.
	 * 
	 * @return the resulting BDD object (this)
	 */
	
	public abstract BDD notWith();

	/**
	 * Computes the logical implication of this bdd with another one.
	 * 
	 * @param other the other bdd
	 * @return the resulting BDD object
	 */
	
	public abstract BDD imp(BDD other);

	/**
	 * Computes the logical implication of this bdd with another one,
	 * store the result in this object, and frees the other.
	 * 
	 * @param other the other bdd
	 * @return the resulting BDD object (this)
	 */
	
	public abstract BDD impWith(BDD other);

	/**
	 * Computes the logical biimplication of this bdd with another one.
	 * 
	 * @param other the other bdd
	 * @return the resulting BDD object
	 */
	
	public abstract BDD biimp(BDD other);
	
	/**
	 * Computes the logical biimplication of this bdd with another one,
	 * store the result in this object, and frees the other.
	 * 
	 * @param other the other bdd
	 * @return the resulting BDD object (this)
	 */
	
	public abstract BDD biimpWith(BDD other);

	/**
	 * @return a new BDD object representing the same diagram
	 */
	
	public abstract BDD copy();

	/**
	 * Yields a satisfying assignment for the formula represented by this bdd.
	 * 
	 * @return a satisfying assignment
	 * @throws UnsatException if there is no satisfying assignment
	 */
	
	public abstract Assignment anySat() throws UnsatException;

	/**
	 * Yields a list of the assignments satisfying the formula represented by this bdd.
	 * 
	 * @return a list of the satisfying assignments
	 */
	
	public abstract List<Assignment> allSat();

	/**
	 * Counts the number of solutions (satisfying assignments) of this bdd,
	 * considering as the last variable index the greatest encountered so far.
	 * 
	 * @return the number of solutions
	 */
	
	public abstract long satCount();

	/**
	 * Counts the number of solutions (satisfying assignments) of this bdd.
	 * 
	 * @param maxVar the last variable index to consider
	 * @return the number of solutions
	 */
	
	public abstract long satCount(int maxVar);

	/**
	 * Restricts this bdd by constraining the given variable to a value.
	 * 
	 * @param var the variable index
	 * @param value the boolean value
	 * @return the resulting bdd
	 */
	
	public abstract BDD restrict(int var, boolean value);

	/**
	 * Restricts this bdd by constraining the given variables to a value.
	 * 
	 * @param var a set of variables represented by a <em>minterm</em> BDD,
	 * a conjunction of variables in either negative (restrict to false)
	 * or positive (restrict to true) form
	 * @return the resulting bdd
	 */
	
	public abstract BDD restrict(BDD var);

	 /**
	  * Restricts this bdd by constraining the given variables to a value,
	  * storing the result in this BDD.
	  * 
	  * @param var a set of variables represented by a <em>minterm</em> BDD,
	  * a conjunction of variables in either negative (restrict to false)
	  * or positive (restrict to true) form
	  * @return this
	  */

	public abstract BDD restrictWith(BDD var);
	
	/**
	 * Existential quantification.
	 * 
	 * @param var the variable index
	 * @return the resulting BDD
	 */
	
	public abstract BDD exist(int var);

	/**
	 * Existential quantification.
	 * 
	 * @param var a set of variables represented by a positive <em>minterm</em> BDD
	 * (a conjunction of variables in positive form)
	 * @return the resulting BDD
	 */
	
	public abstract BDD exist(BDD var);
	
	/**
	 * Universal quantification.
	 * 
	 * @param var the variable index
	 * @return the resulting BDD
	 */
	
	public abstract BDD forAll(int var);

	/**
	 * Universal quantification.
	 * 
	 * @param var a set of variables represented by a positive <em>minterm</em> BDD
	 * (a conjunction of variables in positive form)
	 * @return the resulting BDD
	 */
	
	public abstract BDD forAll(BDD var);
	
	/**
	 * Simplifies this bdd to be true for every assignment satisfying d.
	 * 
	 * The resulting bdd' is such that 
	 * d and bdd = d and bdd'.
	 * 
	 * @param d the domain of interest
	 * @return the resulting bdd
	 */
	
	public abstract BDD simplify(BDD d);

	/**
	 * Computes the number of occurrences of each variable in this bdd.
	 * 
	 * @return an array mapping a variable index to its number of occurrences
	 */
	
	public abstract int[] varProfile();

	/**
	 * @return the number of nodes of this bdd
	 */
	
	public abstract int nodeCount();

	/**
	 * Renames the variables in this bdd according to the given renaming.
	 * 
	 * @param renaming a map from the old var number to the new one
	 * @return the new bdd
	 */
	
	public abstract BDD replace(Map<Integer,Integer> renaming);

	/**
	 * Renames the variables in this bdd according to the given renaming,
	 * storing the result in this BDD.
	 * 
	 * @param renaming a map from the old var number to the new one
	 * @return this
	 */
	
	public abstract BDD replaceWith(Map<Integer, Integer> renaming);

	/**
	 * Counts the number of paths leading to the one terminal of this bdd.
	 * 
	 * @return the number of paths
	 */
	
	public abstract long pathCount();
	
	/**
	 * Computes the If-Then-Else operation.
	 * 
	 * @param thenBDD the 'then' BDD
	 * @param elseBDD the 'else' BDD
	 * @return the resulting BDD
	 */
	
	public abstract BDD ite(BDD thenBDD, BDD elseBDD);

	/**
	 * The relational product.
	 * 
	 * @param other the other BDD
	 * @param var the set of variables to quantify existentially
	 * @return the resulting BDD
	 */
	
	public abstract BDD relProd(BDD other, BDD var);

	/**
	 * Functional composition.
	 * Computes this[other/var].
	 * 
	 * @param other the BDD to substitute to the variable
	 * @param var the variable number
	 * @return the resulting BDD
	 */
	
	public abstract BDD compose(BDD other, int var);
	
	public abstract boolean equalsAux(BDD other);
	
	public abstract int hashCodeAux();

	/**
	 * @return the variable number of this bdd
	 */
	
	public abstract int var();

	/**
	 * @return a BDD object representing the high branch of this bdd
	 */
	
	public abstract BDD high();

	/**
	 * @return a BDD object representing the high branch of this bdd
	 */
	
	public abstract BDD low();

}
