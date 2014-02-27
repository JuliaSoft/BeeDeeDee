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

interface UniqueTable {
	public int getSize();
	public int getCacheSize();
	public int nodesCount();
	public int high(int id);
	public int low(int id);
	public int var(int id);
	public int get(int var, int bdd1, int bdd2);
	public int getFromCache(Operator op, int bdd1, int bdd2);
	public void putIntoCache(Operator op, int bdd1, int bdd2, int result);
	public void printStatistics();
	public String toDot();
}