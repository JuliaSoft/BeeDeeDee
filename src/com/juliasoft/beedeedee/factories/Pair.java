package com.juliasoft.beedeedee.factories;

public final class Pair {
	public final int first;
	public final int second;

	public Pair(int first, int second) {
		this.first = first;
		this.second = second;
	}

	@Override
	public int hashCode() {
		return first | (second << 16);
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