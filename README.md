<div align="center">
  <img src="frontend/public/favicon.svg" alt="Arbiter Logo" width="120" />
  <h1>Arbiter Control Plane</h1>
  <p><strong>High-Frequency Double-Entry Ledger & Financial Telemetry System</strong></p>

  <p>
    <img src="https://img.shields.io/badge/Java-21-orange.svg" alt="Java 21" />
    <img src="https://img.shields.io/badge/Javalin-6.3-purple.svg" alt="Javalin" />
    <img src="https://img.shields.io/badge/React-18-blue.svg" alt="React" />
    <img src="https://img.shields.io/badge/Tailwind-v4-38B2AC.svg" alt="Tailwind CSS" />
  </p>
</div>

## Overview

Arbiter is an ultra-low latency, cryptographically secure double-entry ledger built for high-throughput financial environments. It combines a robust Java/Javalin backend with a cybernetic, "HFT Cockpit" React frontend to provide real-time state vectors and immutable financial records.

### Key Features
* **Immutable Double-Entry Journal:** Cryptographically enforced balancing of all financial movements.
* **ISO-20022 Compliance:** Native parsing and validation of international banking message standards (pacs.008, etc.).
* **Real-time Telemetry:** Streaming analytics, TPS monitoring, and cluster administration terminal.
* **Idempotency & Durability:** Crash-safe file-backed ledger with strict idempotency guards against duplicate transactions.
* **Cybernetic UI:** A meticulously designed React dashboard featuring liquid glass elements, perpetual micro-interactions, and tabular-locked metrics.

---

## Tech Stack

* **Backend:** Java 21+, Javalin 6.3, SLF4J, Jackson
* **Frontend:** React, Vite, Tailwind CSS v4, Framer Motion, Phosphor Icons
* **Persistence:** File-backed append-only journal (`.arbiter-ledger/`)
* **Deployment:** Docker, statically hosted frontend

---

## Prerequisites

To run Arbiter locally, you will need:
* **Java 21** or higher (via SDKMAN or Homebrew)
* **Node.js 20** or higher
* **npm** or **yarn**

---

## Getting Started

### 1. Clone the Repository
```bash
git clone https://github.com/your-username/arbiter.git
cd arbiter
```

### 2. Start the Backend
The Java backend is managed by simple bash scripts. It will automatically build and start the Javalin server on port 8080.
```bash
# Provide a local API key for security
ARBITER_API_KEY="dev_local_secret" ./scripts/run.sh
```

### 3. Start the Frontend
In a new terminal window, start the Vite development server:
```bash
cd frontend
npm install
npm run dev
```
Open [http://localhost:3001](http://localhost:3001) in your browser.

---

## Architecture

### Directory Structure
```
├── src/main/java/com/arbiter/
│   ├── api/           # HTTP Handlers and Security Middleware
│   ├── domain/        # Core Models (Account, JournalEntry, FraudScore)
│   ├── fraud/         # Heuristic Fraud Scoring Engines
│   ├── iso20022/      # XML Parsers for Financial Standards
│   ├── ledger/        # Immutable Ledger & Reconciliation Engine
│   └── settlement/    # Distributed Saga Orchestration
├── frontend/
│   ├── src/           
│   │   ├── components/ # React Components (Terminal, Nav)
│   │   ├── App.tsx     # Main Cockpit Dashboard
│   │   └── index.css   # Tailwind v4 Configuration & CSS Variables
│   └── DESIGN.md      # Semantic Design System documentation
├── scripts/           # Build, Test, and CI shell scripts
└── .arbiter-ledger/   # Local persistent data directory
```

### Request Lifecycle
1. The **React Frontend** queries `localhost:8080/metrics` or `/accounts` via `fetch()`.
2. The **Javalin API** intercepts the request and validates the `X-API-Key` header.
3. Requests hit the `LedgerService`, which validates idempotency, checks balances, and creates double-entry journal records.
4. Data is appended durably to the `FileLedgerRepository`.

---

## API Reference

The Arbiter Control Plane exposes a RESTful API protected by API Keys.

### Authentication
All API requests must include the `X-API-Key` header.
```bash
curl -H "X-API-Key: dev_local_secret" http://localhost:8080/accounts
```

### Endpoints

#### 1. Retrieve Accounts
Returns a list of all active ledger accounts and their current balances.
* **Method:** `GET`
* **Path:** `/accounts`
* **Response:**
  ```json
  [
    {
      "accountId": "0x892...1a9",
      "balance": 14299012.50,
      "currency": "USD"
    }
  ]
  ```

#### 2. Health & Telemetry
Returns cluster status and uptime vectors.
* **Method:** `GET`
* **Path:** `/health`
* **Response:**
  ```json
  {
    "status": "OPERATIONAL",
    "uptime": "14d 2h 9m",
    "version": "1.0.0"
  }
  ```

---

## Deployment

### Frontend (Vercel)
1. Go to Vercel and create a new project linked to your GitHub repo.
2. Set the Root Directory to `frontend`.
3. Vercel will automatically detect Vite (`npm run build`).

### Backend (Docker / Render / Railway)
The root directory includes a production-ready `Dockerfile`.
1. Connect your repository to Render or Railway.
2. The platform will automatically build the Java application from the `Dockerfile`.
3. Set the `ARBITER_API_KEY` Environment Variable in your deployment dashboard.
4. Update the Frontend to point to your new live backend URL instead of `localhost:8080`.

