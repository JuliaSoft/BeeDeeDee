package com.juliasoft.beedeedee.factories;

@SuppressWarnings("serial")
public class CorruptedNodeException extends RuntimeException {

	public CorruptedNodeException(int id) {
		super("Node " + id + " is corrupted!");
	}

}
