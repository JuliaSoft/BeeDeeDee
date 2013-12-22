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