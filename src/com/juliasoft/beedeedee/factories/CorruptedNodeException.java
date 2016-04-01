package com.juliasoft.beedeedee.factories;

public class CorruptedNodeException extends RuntimeException {

	public CorruptedNodeException(int id) {
		super("Node " + id + " is corrupted!");
	}

}
