package com.vvk.banque.domain.exceptions;

public class EmailException implements RuntimeException {

       public EmailException(String msg) { 

    // making this here in order to actually rstrict susp domain in future - have elaborate steps in place for security
                super(msg);
       }
 }
