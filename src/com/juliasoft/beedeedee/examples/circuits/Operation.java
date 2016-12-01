package com.juliasoft.beedeedee.examples.circuits;
import java.util.ArrayList;

public class Operation {
	private String op;
	private ArrayList<String> args;
	
	public Operation(String op, ArrayList<String> args) {
		this.op = op;
		this.args = args;
	}

	String getOp() {
		return op;
	}

	ArrayList<String> getArgs() {
		return args;
	}

	@Override
	public String toString() {
		String s = op + "(";
		for (String a : args) {
			s += a + ", ";
		}
		return s.substring(0, s.length()-2) + ")";
	}
}
