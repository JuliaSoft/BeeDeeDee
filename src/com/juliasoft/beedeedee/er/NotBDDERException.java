package com.juliasoft.beedeedee.er;

@SuppressWarnings("serial")
public class NotBDDERException extends RuntimeException {
	public NotBDDERException() {
		super("The method argument is not of class BDDER");
	}
}