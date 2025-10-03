BANK-CORE / ATLAS-Capstone

by Vivek Samant

1. PURPOSE

Provide a minimal but production-grade banking transaction kernel that

    runs entirely from a Linux terminal (no IDE)
    respects hexagonal architecture, CQRS and event-sourcing
    keeps ACID guarantees on balances while publishing an immutable audit trail

2. REPOSITORY SNAPSHOT


--------------------------------
|bank/                
| ---------------------------- |
|├── bank-core/    ← maven wrapper present, yet pure javac path documented         
|├── Banking_SRS.odt   ← formal specification (single commit, GPG-signed)    
||---use-case.puml
|└── readme.md             
|                   
--------------------------------



3. TECHNOLOGY & CONSTRAINTS

    Language : Java 17 (OpenJDK)
    Build    : javac (instructions below); Maven optional
    OS       : any Linux distribution with bash and make
    Architecture : Hexagonal – domain core, application services, adapters
    Persistence  :
    – SQL store (SQLite / PostgreSQL) → balances, uniqueness constraints
    – NoSQL store (DynamoDB / MongoDB) → append-only event audit
    Security : BCrypt password hashes (optional goal), no plain-text secrets

4. CURRENT STATUS
-----------------------------------------------------------------------
| Milestone                    | Date   | State                       |
| ---------------------------- | ------ | --------------------------- |
| Value-Objects design lock    | 19-Sep | ✅                          |
| Domain layer + unit tests    | 21-Sep | ✅ (tag `v1.0.0`)           |
| Application ports & services | 03-Oct | ✅ compiles clean           |
| **Next**                     | 04-Oct | CLI adapter + SQLite driver |
-----------------------------------------------------------------------

5. QUICK START – COMPLETE WALK-THROUGH

5.1 Clone & enter:

bash'
git clone https://github.com/Vvk1amzn2sd/bank.git
cd bank/bank-core

5.2 Compile without Maven (pure terminal)

bash'
# compile domain + app layers
javac -d out -cp src/main/java \
  src/main/java/com/vvk/banque/domain/**/*.java \
  src/main/java/com/vvk/banque/app/**/*.java

# package domain classes into reusable jar (optional)
jar cf domain.jar -C out/com/vvk/banque/domain .

5.3 Run existing unit tests

bash'
javac -d out/test -cp out:src/test/java src/test/java/**/*.java
java -cp out:out/test org.junit.platform.console.ConsoleLauncher --scan-classpath

(All greens before any adapter work – gate kept.)

5.4 Build CLI adapter (once pushed)

bash'
javac -d out -cp out src/main/java/com/vvk/banque/cli/*.java
java -cp out com.vvk.banque.cli.BankShell

Menu will offer:
[1] Create customer
[2] Open account
[3] Deposit
[4] Withdraw
[5] Transfer
[6] Check balance
[7] Exit

5.5 Live demo via ngrok (one-liner)

# terminal 1
java -cp out com.vvk.banque.cli.BankShell --port 8080

# terminal 2
ngrok http 8080
# copy https URL for panel evaluators

6. HEXAGON LAYER MAP

in-ports  (CLI / REST)  → app-services → domain-aggregates  
                          ↓
out-ports (SQL, NoSQL, EventBus) ← adapters implement contracts


7. EVENT FLOW (audit path)

    Command hits application service
    Service loads aggregate via AccountEventStorePort#loadEvents
    Aggregate mutates, emits domain event
    Service saves event via saveEvent + publishes via EventPublisherPort
    Outbound adapter writes to NoSQL → immutable audit row

8. SQL SCHEMA (reference)

sql'

CREATE TABLE accounts (
    account_id  VARCHAR(36) PRIMARY KEY,
    customer_id VARCHAR(36) NOT NULL,
    type        VARCHAR(10) CHECK (type IN ('CHECKING','SAVINGS')),
    balance     BIGINT      NOT NULL CHECK (balance >= 0)
);

CREATE TABLE customers (
    customer_id VARCHAR(36) PRIMARY KEY,
    name        VARCHAR(120),
    email       VARCHAR(120) UNIQUE,
    pwd_hash    VARCHAR(60)
);

9. NoSQL COLLECTION(single)

json'
{
  "_id"      : "<eventUuid>",
  "eventType": "MoneyDeposited",
  "accountId": "<uuid>",
  "amount"   : 5000,
  "timestamp": "2025-10-03T18:42:00Z"
}

10. CONTRIBUTING (developer diary)

    Keep javac path working – do not add framework deps without dual path.
    Every new adapter lives in its own package under com.vvk.banque.adapter.
    Unit-test before push – gate on mvn test and plain javac compile.
    Update this readme only after adapter layer is compile-clean.


11. ROADMAP

    -> CLI adapter + SQLite driver(by 4th oct)
    -> REST adapter (Spring-Boot optional)(by 4th oct)
    -> DOCKER image + aws/azure CI(by 4th oct)
    -> LOAD test + panel demo 

12. CONTACT

Mail  → samant.vvek@gmail.com / samantvs@amazon.com@<provider>



/*--------******************************************************---------------------*/
“Built on a terminal, survives in a container.”
/*--------******************************************************---------------------*/
