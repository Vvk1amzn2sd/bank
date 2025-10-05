# BANK-CORE / ATLAS-Capstone
by **Vivek Samant**

---

## Purpose 💡
Provide a minimal but production-grade banking transaction kernel that:
* Runs entirely from a **Linux terminal** (no IDE or external frameworks required).
* Respects **Hexagonal Architecture**, CQRS, and Event-Sourcing principles.
* Keeps **ACID guarantees** on balances while publishing an **immutable audit trail**.

---

## Repository Snapshot

bank/
├── bank-core/ ← maven wrapper present, yet pure javac path documented
├── Banking_SRS.odt ← formal specification (single commit, GPG-signed)
└── readme.md


---

## Technology & Constraints 🛠️
* **Language**: **Java 17+** (OpenJDK 25 tested)
* **Build**: **Pure `javac`** (Maven optional, no external frameworks in core)
* **OS**: Any Linux, macOS, or Windows with Java
* **Architecture**: **Hexagonal** – domain core, application services, adapters
* **Persistence**:
    * SQL store (SQLite / PostgreSQL) → *Balances, Uniqueness Constraints*
    * NoSQL store (DynamoDB / MongoDB) → *Append-Only Event Audit*
* **Security**: BCrypt password hashes (optional), no plain-text secrets

---

## Current Status ✅

| Milestone | Date | State |
| :--- | :--- | :--- |
| Value-Objects design lock | 19-Sep | ✅ |
| Domain layer + unit tests | 21-Sep | ✅ (tag `v1.0.0`) |
| Application ports & services | 03-Oct | ✅ compiles clean |
| **CLI adapter + SQLite driver** | 04-Oct | ✅ **Shipped** as `final-dev-release-cli-inmemory-db-v2` |
| **DOCKER image + Azure CI** | 05-Oct | ✅ **Deployed** (CLI Shell over ngrok) |

---

## Quick Start – Complete Walk-Through 🚀

### 1. Live Demo (CLI Shell over HTTP Tunnel)

The core banking shell, compiled with **pure `javac`**, is currently running in the cloud (Azure App Service) and is exposed live via a temporary public HTTP tunnel using **ngrok**. This setup allows remote interaction with the shell's command-line interface.

| Component | URL | Status |
| :--- | :--- | :--- |
| **Live CLI HTTP Tunnel** | **`https://7c907085fd54.ngrok-free.app`** | **LIVE** |

### 2. How to Use the CLI

The shell offers a menu-driven interface. Note the strict constraints enforced by the domain logic:

| Menu Option | Action | Domain Constraint |
| :---: | :--- | :--- |
| **[1] Create customer** | **SIGNUP**: Creates a new customer account. | **Customer ID** must be **3 CAPITAL LETTERS** (e.g., `SAM`). |
| **[2] Open account** | Creates a new account linked to a customer. | **Account Number** must be a **unique 5-digit number** and cannot start with `0` (e.g., `12345`). |
| **[3] Deposit** | **[4] Withdraw** | **[5] Transfer** | **[6] Check balance** | **[7] Exit** |

### 3. Clone & Compile

**A. Download & Run JAR:**

1.  Download the JAR:
    ```
    [https://github.com/Vvk1amzn2sd/bank/releases/download/final-dev-release-cli-inmemory-db-v2/bank-core-11.00.jar](https://github.com/Vvk1amzn2sd/bank/releases/download/final-dev-release-cli-inmemory-db-v2/bank-core-11.00.jar)
    ```
2.  Run:
    ```bash
    java -jar bank-core-11.00.jar
    ```

**B. Clone & Enter:**

```bash
git clone [https://github.com/Vvk1amzn2sd/bank.git](https://github.com/Vvk1amzn2sd/bank.git)
cd bank/bank-core

C. Compile without Maven (Pure Terminal):
Bash

# Compile domain + application layers
javac -d out -cp src/main/java \
  src/main/java/com/vvk/banque/domain/**/*.java \
  src/main/java/com/vvk/banque/app/**/*.java

# (Optional) Package domain classes into reusable jar
jar cf domain.jar -C out/com/vvk/banque/domain .

D. Run Unit Tests:
Bash

javac -d out/test -cp out:src/test/java src/test/java/**/*.java
java -cp out:out/test org.junit.platform.console.ConsoleLauncher --scan-classpath
# (All greens before any adapter work – gate kept.)

E. Build & Run CLI Adapter (Locally):
Bash

javac -d out -cp out src/main/java/com/vvk/banque/cli/*.java
java -cp out com.vvk.banque.cli.BankShell

F. Live Demo Setup (Replication):
To replicate the ngrok live demo for panel evaluators, you must run the following two processes simultaneously:

    Terminal 1 (Run BankShell on local port 8080):
    Bash

java -cp out com.vvk.banque.cli.BankShell --port 8080

Terminal 2 (Start ngrok Tunnel to expose local port):
Bash

    ngrok http 8080
    → Copy the HTTPS URL for panel evaluators

Architecture Insights

Hexagonal Layer Map

This project strictly follows the Ports and Adapters (Hexagonal) pattern. The current entry point is the Command Line Interface (CLI) Shell.

in-ports (CLI Shell) → app-services → domain-aggregates
                               ↓
out-ports (SQL, NoSQL, EventBus) ← adapters implement contracts

Event Flow (Immutable Audit Path)

The CQRS and Event-Sourcing principles guarantee a decoupled domain core and an immutable audit trail.

    Command hits the Application Service.

    Service loads Aggregate via AccountEventStorePort#loadEvents.

    Aggregate mutates state and emits a Domain Event.

    Service saves event via saveEvent and publishes via EventPublisherPort.

    Outbound adapter writes the event to the NoSQL store → Immutable Audit Row.

Persistence Schema Reference

SQL Schema (accounts, customers)

The SQL store maintains current state for balances and uniqueness constraints.
SQL

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

NoSQL Collection (Audit Log)

The NoSQL store acts as the append-only event audit log, representing the source of truth for all transactions.
JSON

{
  "_id": "<Event-UUID>",
  "eventType": "MoneyDeposited",
  "accountId": "<Account-UUID>",
  "amount": 5000,
  "timestamp": "2025-10-03T18:42:00Z"
}

Contributing (Developer Diary) ✍️

    Keep the javac path working – do not add framework dependencies without providing a dual build path.

    Every new adapter must live in its own package under com.vvk.banque.adapter..

    Unit-test before push – gate on mvn test and plain javac compile.

    Update this readme only after the adapter layer is compile-clean.

Roadmap (Next Milestones) 📅

    CLI adapter + SQLite driver ✅

    REST adapter (Spring-Boot optional) – Next major focus for standard API access.

    LOAD test + panel demo – Using the new API layer.

Contact

    Mail → samant.vvek@gmail.com / samantvs@amazon.com

Final Note

    “Built on a terminal, survives in a container.”


