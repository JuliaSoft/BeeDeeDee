package com.juliasoft.beedeedee.er;

public class NotBDDGerException extends RuntimeException {
	public NotBDDGerException() {
		super("The method argument is not of class " + BDDER.class.getName());
	}
}
