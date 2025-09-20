package com.vvk.banque.domain.exceptions;

public class AccountOpenedException implements RuntimeException {
	
	public AccountOpenedException(String message) {
		super(message);
	}
}
	
