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
package com.juliasoft.beedeedee.factories;

/**
 * A UniqueTable contains all the BDD nodes in a Factory.
 */
interface UniqueTable {
	
	/**
	 * Returns the size of the table.
	 * 
	 * @return the size
	 */
	public int getSize();
	
	/**
	 * Returns the size of the computation cache.
	 * 
	 * @return the size of the computation cache
	 */
	public int getCacheSize();
	
	/**
	 * Returns the number of nodes in the table.
	 * 
	 * @return the number of nodes
	 */
	public int nodesCount();
	
	/**
	 * Returns the high branch of the given node.
	 * 
	 * @param id the node id
	 * @return the high branch
	 */
	public int high(int id);

	/**
	 * Returns the low branch of the given node.
	 * 
	 * @param id the node id
	 * @return the low branch
	 */
	public int low(int id);
	
	/**
	 * Returns the variable number of the given node.
	 * 
	 * @param id the node id
	 * @return the variable number
	 */
	public int var(int id);
	
	/**
	 * Returns the node with the given attributes, creating it if not present.
	 * 
	 * @param var the variable number
	 * @param low the low branch node
	 * @param high the high branch node
	 * @return the node
	 */
	public int get(int var, int low, int high);

	/**
	 * Gets a result from the computation cache.
	 * 
	 * @param op the operator
	 * @param bdd1 the first bdd operand
	 * @param bdd2 the second bdd operand
	 * @return the result, or -1 if not present
	 */
	public int getFromCache(Operator op, int bdd1, int bdd2);
	
	/**
	 * Puts a result in the computation cache.
	 * 
	 * @param op the operator
	 * @param bdd1 the first bdd operand
	 * @param bdd2 the second bdd operand
	 * @param result the computation result
	 */
	public void putIntoCache(Operator op, int bdd1, int bdd2, int result);
	
	public void printStatistics();
	
	/**
	 * Returns a GraphViz format representation of the table.
	 * 
	 * @return a String with the table in GraphViz format
	 */
	public String toDot();
}