package com.vvk.banque.domain.exceptions;

public class InvalidTransferException extends RuntimeException {
    public InvalidTransferException(String msg) { super(msg); }
}
