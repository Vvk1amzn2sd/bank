package com.vvk.banque.domain.exceptions;

public class AccountOpenedException extends RuntimeException {
	
	public AccountOpenedException(String message) {
		super(message);
	}
}
	
