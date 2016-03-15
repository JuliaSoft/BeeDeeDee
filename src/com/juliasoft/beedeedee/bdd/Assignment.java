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

/**
 * A truth assignment to variables.
 */
public interface Assignment {

	/**
	 * Determines if the given BDD variable holds in this assignment.
	 *
	 * @param var the variable
	 * @return true if and only if {@code var} holds in this assignment
	 * @throws IndexOutOfBoundsException if the variable does not belong to this
	 *             assignment. This means that the variable can be assigned any
	 *             value
	 */
	public boolean holds(BDD var) throws IndexOutOfBoundsException;

	/**
	 * Determines if the given BDD variable holds in this assignment.
	 *
	 * @param i the variable index
	 * @return true if and only if the variable indexed by {@code i} holds in
	 *         this assignment
	 * @throws IndexOutOfBoundsException if the variable does not belong to this
	 *             assignment. This means that the variable can be assigned any
	 *             value
	 */
	public boolean holds(int i);

	/**
	 * @return a <em>minterm</em> representation of the assignment
	 */
	public BDD toBDD();

	/**
	 * Adds a mapping for the given variable.
	 * 
	 * @param var the variable index
	 * @param value the value to assign to the variable in this assignment
	 */
	public void put(int var, boolean value);

}
