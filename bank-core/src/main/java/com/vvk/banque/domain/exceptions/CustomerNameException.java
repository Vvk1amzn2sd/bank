package com.vvk.banque.domain.exceptions;

public class CustomerNameException implements RuntimeException {

       public CustomerNameException(String msg) { 

    // making this here in order to actually restrict naming in future - have elaborate namin' convention
                super(msg);
       }
 }
