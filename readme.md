/*-- BANK-CORE / ATLAS-Capstone --*/  
by Vivek Samant  

/*-- PURPOSE --*/  
Provide a minimal but production-grade banking transaction kernel that  
- runs entirely from a Linux terminal (no IDE)  
- respects hexagonal architecture, CQRS and event-sourcing  
- keeps ACID guarantees on balances while publishing an immutable audit trail  

/*-- REPOSITORY SNAPSHOT --*/  
bank/  
├── bank-core/ ← maven wrapper present, yet pure javac path documented  
├── Banking_SRS.odt ← formal specification (single commit, GPG-signed)  
└── readme.md  

/*-- TECHNOLOGY & CONSTRAINTS --*/  
- **Language**: Java 17+ (OpenJDK 25 tested)  
- **Build**: `javac` (Maven optional)  
- **OS**: Any Linux, macOS, or Windows with Java  
- **Architecture**: Hexagonal – domain core, application services, adapters  
- **Persistence**:  
  - SQL store (SQLite / PostgreSQL) → balances, uniqueness constraints  
  - NoSQL store (DynamoDB / MongoDB) → append-only event audit  
- **Security**: BCrypt password hashes (optional), no plain-text secrets  

/*-- CURRENT STATUS --*/  
| Milestone                     | Date     | State |  
|-------------------------------|----------|-------|  
| Value-Objects design lock     | 19-Sep   | ✅    |  
| Domain layer + unit tests     | 21-Sep   | ✅ (tag v1.0.0) |  
| Application ports & services  | 03-Oct   | ✅ compiles clean |  
| CLI adapter + SQLite driver   | 04-Oct   | ✅ shipped as `final-dev-release-cli-inmemory-db-v2` |  

/*-- QUICK START – COMPLETE WALK-THROUGH --*/  

**5.1 Clone & enter:**  
```bash
git clone https://github.com/Vvk1amzn2sd/bank.git
cd bank/bank-core

5.2 Compile without Maven (pure terminal):

# Compile domain + app layers
javac -d out -cp src/main/java \
  src/main/java/com/vvk/banque/domain/**/*.java \
  src/main/java/com/vvk/banque/app/**/*.java

# (Optional) Package domain classes into reusable jar
jar cf domain.jar -C out/com/vvk/banque/domain .

5.3 Run existing unit tests:

javac -d out/test -cp out:src/test/java src/test/java/**/*.java
java -cp out:out/test org.junit.platform.console.ConsoleLauncher --scan-classpath
(All greens before any adapter work – gate kept.)

5.4 Build & run CLI adapter:

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

Terminal 1:   
java -cp out com.vvk.banque.cli.BankShell --port 8080

Terminal 2:
ngrok http 8080
→ Copy the HTTPS URL for panel evaluators   

/-- HEXAGON LAYER MAP --/
in-ports (CLI / REST) → app-services → domain-aggregates
↓
out-ports (SQL, NoSQL, EventBus) ← adapters implement contracts   

/-- EVENT FLOW (audit path) --/   

    Command hits application service  
    Service loads aggregate via AccountEventStorePort#loadEvents  
    Aggregate mutates, emits domain event  
    Service saves event via saveEvent + publishes via EventPublisherPort  
    Outbound adapter writes to NoSQL → immutable audit row
     

/-- SQL SCHEMA (reference) --/   

CREATE TABLE accounts (
  account_id VARCHAR(36) PRIMARY KEY,
  customer_id VARCHAR(36) NOT NULL,
  type VARCHAR(10) CHECK (type IN ('CHECKING','SAVINGS')),
  balance BIGINT NOT NULL CHECK (balance >= 0)
);

CREATE TABLE customers (
  customer_id VARCHAR(36) PRIMARY KEY,
  name VARCHAR(120),
  email VARCHAR(120) UNIQUE,
  pwd_hash VARCHAR(60)
);

/-- NoSQL COLLECTION (single) --/ 

{
  "_id": "",
  "eventType": "MoneyDeposited",
  "accountId": "",
  "amount": 5000,
  "timestamp": "2025-10-03T18:42:00Z"
}

/-- CONTRIBUTING (developer diary) --/   

    Keep javac path working – do not add framework deps without dual path.  
    Every new adapter lives in its own package under com.vvk.banque.adapter.  
    Unit-test before push – gate on mvn test and plain javac compile.  
    Update this readme only after adapter layer is compile-clean.
     

/-- ROADMAP (by 5 Oct 2025) --/
→ CLI adapter + SQLite driver ✅
→ REST adapter (Spring-Boot optional)
→ DOCKER image + AWS/Azure CI
→ LOAD test + panel demo   

/-- CONTACT --/
Mail → samant.vvek@gmail.com  / samantvs@amazon.com    

/-- FINAL NOTE --/
“Built on a terminal, survives in a container.”
``` 

