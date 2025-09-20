This is a Banking transaction system, purely build on linux terminal w/o any IDE dependency at all. 
tech stack: backend in java.
This adheres to modern design architecture i.e. hexagonal and also holds up SOLID principles, doesn't violate DIP.
This uses domain driven design, ports - in and out will be used . 
readme will be updated as progress is made.

/*---update on 19-sep--*/
finalised design for Value object. Money, AccountId, CustomerId.

/*---update on Sun Sep 21 01:16:37 AM IST 2025--*/
domain layer finished, tests pending for agg & exceptions. 
imp: 1. Account aggregate emits events - its a pure event emitting m/c, emitting creatison,withdraw,deposit,transfer
      2. in addition, Customer aggregate emit signup, created event, purpose is to have folks first create customer profile on webUI then hand them account.
      for audit log: event driven nature of aggregate -> re-hydrating and building entire event using stream and hence quite easy to just read entire stream @ outer hexagon and provide eventually consistent logs. 

    domain.jar aim for release by EOD today!
    ft. tree:
domain:
├── AggregatesObj
│   ├── Account.java
│   └── Customer.java
├── events
│   ├── AccountOpened.java
│   ├── CustomerSignedUp.java
│   ├── DomainEvent.java
│   ├── MoneyDeposited.java
│   ├── MoneyTransferReceive.java
│   ├── MoneyTransferSend.java
│   └── MoneyWithdrawn.java
├── exceptions
│   ├── AccountOpenedException.java
│   ├── BeneficiaryAccountNullException.java
│   ├── CustomerNameException.java
│   ├── CustomerNotFoundException.java
│   ├── EmailException.java
│   ├── InsufficientBalanceException.java
│   ├── InvalidSelfTransferException.java
│   ├── OpeningBalanceNullException.java
│   └── PositiveMoneyException.java
└── ValueObj
    ├── AccountId.java
    ├── CustomerId.java
    ├── DatabaseSequence.java
    ├── Money.java
    └── TransactionId.java
