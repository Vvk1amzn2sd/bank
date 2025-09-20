package com.vvk.banque.domain.exceptions;

public class InvalidSelfTransferException extends RuntimeException {
       public InvalidSelfTransferException(String msg) {
                     super(msg);
         }
   }
