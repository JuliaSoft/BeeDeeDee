package com.juliasoft.beedeedee.er;

public class NotBDDERException extends RuntimeException {
	public NotBDDERException() {
		super("The method argument is not of class " + BDDER.class.getName());
	}
}
