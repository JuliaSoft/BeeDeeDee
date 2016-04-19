package com.juliasoft.beedeedee.ger;

public final class Pair {
	public final int first;
	public final int second;

	public Pair(int first, int second) {
		this.first = first;
		this.second = second;
	}

	// eclipse generated
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + first;
		result = prime * result + second;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Pair && ((Pair) obj).first == first && ((Pair) obj).second == second;
	}

	@Override
	public String toString() {
		return "(" + first + ", " + second + ")";
	}
}