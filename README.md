# Arbiter Control Plane

Arbiter is an ultra-low latency, cryptographically secure double-entry ledger built for high-throughput financial environments. It combines a robust Java 21 backend with a React-based High-Frequency Trading (HFT) cockpit frontend to provide real-time state vectors, immutable financial records, and ISO-20022 compliance.

## Key Features

- **Immutable Double-Entry Journal**: Cryptographically enforced balancing of all financial movements.
- **ISO-20022 Compliance**: Native parsing and validation of international banking message standards (pacs.008).
- **Idempotency & Durability**: Crash-safe file-backed ledger with strict idempotency guards against duplicate transactions.
- **Real-Time Telemetry**: Streaming transaction logs, TPS monitoring, and cluster administration.
- **Fail-Fast Security**: Strict startup enforcement of cryptographically secure API keys.

---

## Tech Stack

- **Language**: Java 21
- **HTTP Server**: Core JDK `com.sun.net.httpserver` (Zero external framework bloat)
- **Frontend**: React 18 with Vite
- **Styling**: Tailwind CSS v4
- **Persistence**: File-backed append-only journal
- **Deployment**: Docker, GitHub Actions, AWS/Render

---

## Prerequisites

- Java 21 or higher (via SDKMAN, Homebrew, or apt)
- Node.js 20 or higher
- npm (version 10+)
- Unix-based terminal (Linux/macOS or WSL) for executing build scripts

---

## Getting Started

### 1. Clone the Repository

```bash
git clone https://github.com/Gaurav711cgu/ARBITOR.git
cd ARBITOR
```

### 2. Environment Setup

The backend requires a strict API key initialization for security.

```bash
export ARBITER_API_KEY="local-dev-secret-key-12345678"
export ARBITER_DATA_DIR="./data"
```

### 3. Start the Backend

The backend utilizes simple bash scripts for compilation and execution.

```bash
./scripts/run.sh
```

This compiles the Java source code to `out/classes` and binds the HTTP server to `localhost:8080`.

### 4. Start the Frontend

In a separate terminal session, start the Vite development server.

```bash
cd frontend
npm ci
npm run dev
```

Navigate to `http://localhost:3000` to view the HFT Cockpit. The frontend is configured to proxy `/api` requests to the local backend running on port 8080.

---

## Architecture

### Directory Structure

```text
├── src/main/java/com/arbiter/
│   ├── api/           # HTTP Handlers and Security Middleware
│   ├── domain/        # Core Models (Account, JournalEntry, FraudScore)
│   ├── fraud/         # Heuristic Fraud Scoring Engines
│   ├── iso20022/      # XML Parsers for Financial Standards
│   ├── ledger/        # Immutable Ledger & Reconciliation Engine
│   └── settlement/    # Distributed Saga Orchestration
├── frontend/
│   ├── src/           
│   │   ├── components/ # React UI Components (Terminal, LedgerExplorer)
│   │   ├── App.tsx     # Main Routing and Application Shell
│   │   └── index.css   # Tailwind v4 Configuration
│   └── vite.config.ts  # Vite configuration and proxy settings
├── scripts/           # Build, Test, and Execution shell scripts
├── .github/           # CI/CD Workflows and GitOps Automation
└── Dockerfile         # Multi-stage container build definition
```

### Request Lifecycle

1. The React frontend queries `/api/metrics` or `/api/transactions` via `fetch()`.
2. The Vite proxy forwards the request to `localhost:8080`.
3. The JDK HTTP server intercepts the request. `ApiAuth.java` strictly enforces the presence and validity of the `X-API-Key` header.
4. Requests hit the `LedgerService`, which validates idempotency, calculates balances, and generates double-entry records.
5. The transaction is atomically flushed to disk via `FileLedgerRepository`.

---

## Environment Variables

| Variable | Required | Description | Minimum Length |
|----------|----------|-------------|----------------|
| `ARBITER_API_KEY` | **Yes** | Cryptographic key required for all API authentication. | 24 characters |
| `ARBITER_DATA_DIR` | No | Path to the directory where the ledger append-log is stored. Defaults to `data/`. | N/A |

*Note: The application will immediately crash on startup (Fail-Fast) if `ARBITER_API_KEY` is undefined or shorter than 24 characters.*

---

## API Documentation

All endpoints reside under the root path `/` (proxied via `/api` in local development). All requests require the `X-API-Key` header.

### Authentication Header
```http
X-API-Key: your-production-api-key-here
```

### Endpoints

#### 1. Retrieve Accounts
Fetches all active accounts and their current balances.

- **Method**: `GET`
- **Path**: `/accounts`
- **Response**: `200 OK`
```json
[
  {
    "accountId": "0x892...1a9",
    "balance": 14299012.50,
    "currency": "USD"
  }
]
```

#### 2. Create Account
Provisions a new ledger account.

- **Method**: `POST`
- **Path**: `/accounts`
- **Content-Type**: `application/json`
- **Body**:
```json
{
  "accountId": "0xNewAccount123",
  "currency": "USD"
}
```
- **Response**: `201 Created`
```json
{
  "accountId": "0xNewAccount123",
  "balance": 0.00,
  "currency": "USD"
}
```

#### 3. Settle Payment (JSON)
Executes a synchronous double-entry transfer between two accounts.

- **Method**: `POST`
- **Path**: `/payments`
- **Content-Type**: `application/json`
- **Body**:
```json
{
  "endToEndId": "TX-99812-A",
  "debtorAccountId": "0xSource",
  "creditorAccountId": "0xDestination",
  "amount": "5000.00",
  "currency": "USD",
  "purposeCode": "INVS"
}
```
- **Response**: `200 OK`
```json
{
  "settlementId": "TX-99812-A",
  "status": "SETTLED",
  "timestamp": "2023-10-25T14:32:01Z"
}
```

#### 4. Settle Payment (ISO-20022 XML)
Executes a payment using the international pacs.008 banking standard.

- **Method**: `POST`
- **Path**: `/payments/iso20022`
- **Content-Type**: `application/xml`
- **Body**: Standard `pacs.008.001.08` XML payload.
- **Response**: `200 OK` (Returns standard settlement JSON).

#### 5. Retrieve Metrics
Fetches real-time system performance and ledger state.

- **Method**: `GET`
- **Path**: `/metrics`
- **Response**: `200 OK`
```json
{
  "status": "OPERATIONAL",
  "uptime": "14d 2h 9m",
  "globalJournalSum": 14592000.00,
  "tps": 4200
}
```

#### 6. Live Transactions Stream
Fetches the most recent 20 transactions applied to the ledger.

- **Method**: `GET`
- **Path**: `/transactions`
- **Response**: `200 OK`
```json
[
  {
    "id": "TX-01923",
    "type": "pacs.008",
    "amount": "4,210.50",
    "currency": "USD",
    "status": "SETTLED"
  }
]
```

---

## Deployment

Arbiter is designed to deploy seamlessly as a monolithic Docker container. The included multi-stage `Dockerfile` compiles the Vite React frontend, compiles the Java backend, and bundles them into a single Alpine image where the Java server statically serves the React assets.

### Option 1: Render.com (Recommended)
A `render.yaml` file is included for instant deployment.
1. Connect this repository to Render as a Blueprint.
2. Render will automatically provision the container, mount a 10GB persistent disk to `/data`, and inject a secure `ARBITER_API_KEY`.

### Option 2: AWS App Runner / Fargate
1. Connect your repository to AWS App Runner or ECS.
2. Specify the build source as the included `Dockerfile`.
3. Provide the `ARBITER_API_KEY` environment variable.
4. (Optional) Mount an EFS volume to `/data` if persistence across container restarts is required.

---

## Available Scripts

| Command | Description |
|---------|-------------|
| `./scripts/build.sh` | Compiles the Java source code via `javac`. |
| `./scripts/run.sh` | Compiles and executes the backend server. |
| `./scripts/test.sh` | Executes backend unit and integration tests. |
| `npm run build` | (In `/frontend`) Compiles the React application for production. |
| `npm run typecheck` | (In `/frontend`) Validates TypeScript integrity. |

---

## CI/CD Automation

This repository is instrumented with rigorous GitHub Actions workflows (`.github/workflows/`):

- **Arbiter Core CI (`ci.yml`)**: Automatically triggers on pushes and PRs to `main`. It provisions an Ubuntu runner, verifies Java 21 compilation, verifies Node 20 frontend builds, and strictly halts on failure.
- **Stale Issue Manager (`stale.yml`)**: Automates issue triage to maintain repository hygiene.
- **Auto-Rebase (`auto-rebase.yml`)**: Enables GitOps control—commenting `/rebase` on any Pull Request automatically rebases it against `main`.
