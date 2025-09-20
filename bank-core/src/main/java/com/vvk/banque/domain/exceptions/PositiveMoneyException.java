package com.vvk.banque.domain.exceptions;

public class PositiveMoneyException extends RuntimeException {
		
		public PositiveMoneyException(String msg) {
			super(msg);
		}
}
