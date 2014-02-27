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
