package com.vvk.banque.domain.exceptions;

public class PasswordException extends RuntimeException {

// leaving here custom xcption so can define prmeter for setting pw later, (strong, weak etc.) time scarce now, hence not doin that

     public PasswordException(String msg) {
           super(msg);
      }
}
