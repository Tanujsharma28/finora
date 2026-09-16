# FINORA — Smart Banking & Personal Finance Platform

![CI](https://github.com/Tanujsharma28/finora/actions/workflows/ci.yml/badge.svg)

A production-grade backend system demonstrating event-driven architecture, distributed transactions, real-time processing, and fraud detection — built for the 2027 Java developer placement cycle.

---

## Architecture Overview

┌─────────────┐ REST API ┌──────────────────┐
│ React + │ ◄────────────► │ Spring Boot 3 │
│ TypeScript │ WebSocket │ (Port 8080) │
└─────────────┘ └────────┬─────────┘
│
┌──────────────────────┼──────────────────────┐
▼ ▼ ▼
┌─────────────┐ ┌──────────────┐ ┌──────────────┐
│ MySQL │ │ Kafka │ │ Redis │
│ (Port 3306)│ │ (Port 9092) │ │ (Port 6379) │
└─────────────┘ └──────┬───────┘ └──────────────┘
│
┌────────────────────┼────────────────────┐
▼ ▼ ▼
┌─────────────┐ ┌──────────────────┐ ┌─────────────┐
│ Fraud │ │ Notification │ │ Analytics │
│ Detection │ │ Consumer │ │ Consumer │
└─────────────┘ └──────────────────┘ └─────────────┘


## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 21, Spring Boot 3.3.5 |
| Database | MySQL 8.0 (Flyway migrations) |
| Messaging | Apache Kafka 3.7 |
| Caching | Redis 7 |
| Real-time | WebSocket (STOMP + SockJS) |
| Security | Spring Security + JWT (HS512) |
| Frontend | React + TypeScript + Vite + Tailwind CSS v4 |
| DevOps | Docker, Docker Compose, GitHub Actions CI |

---

## Features

### Core Banking
- **JWT Authentication** — Register, login, BCrypt hashing, stateless sessions
- **Account Management** — Create accounts, atomic balance updates (race-condition safe)
- **Transactions** — Create DEBIT/CREDIT transactions with pagination

### Event-Driven Pipeline (Kafka)
Three independent consumer groups process every transaction:
1. **FraudDetectionConsumer** — Rule-based spike detection (10x baseline, MIN_BASELINE ₹500, risk score 0–100) → WebSocket push on final status
2. **NotificationConsumer** — Real-time WebSocket push to `/topic/notifications/{accountId}` at PENDING stage
3. **AnalyticsConsumer** — Per-account, per-category spending totals stored in Redis Hash

### P2P Transfer with Saga Pattern

Debit leg commit → Credit leg commit → COMPLETED
Credit fail → Compensating tx → COMPENSATED (money returned)
Debit fail → No commit → FAILED

- Idempotency key — duplicate requests are no-ops
- Self-invocation fix — `@Transactional(REQUIRES_NEW)` methods in separate bean (`TransferLegService`)

### Recurring Transfers
- Frequencies: DAILY / WEEKLY / MONTHLY
- `@Scheduled` job runs nightly at 2 AM, picks up `next_run_date <= today` ACTIVE transfers
- Executes via same `TransferService` (idempotency key: `recurring-{id}-{date}`)
- Status lifecycle: ACTIVE → PAUSED → CANCELLED

### Fraud Reversal
- FLAGGED transactions can be reversed via `POST /api/transactions/{id}/reverse`
- Creates compensating CREDIT transaction, restores balance, marks original as REVERSED

### Real-time UI
- WebSocket auto-updates transaction status without page refresh
- Toast notifications for PENDING and FLAGGED events
- Live dot indicator on dashboard

---

## Database Schema

```sql
users           — UUID PK, BCrypt password
accounts        — UUID PK, DECIMAL(15,2) balance, atomic adjustBalance()
transactions    — UUID PK, DEBIT/CREDIT, PENDING/COMPLETED/FLAGGED/REVERSED
transfers       — Saga state, idempotency_key UNIQUE, debit/credit tx FKs
recurring_transfers — frequency, next_run_date, ACTIVE/PAUSED/CANCELLED
fraud_flags     — risk score, flag reason
```

---

## Running Locally

### Prerequisites
- Java 21, Maven 3.9+
- Docker Desktop
- MySQL 8.0 running on `localhost:3306`

### Environment Variables (Windows `setx`)

DB_URL=jdbc:mysql://localhost:3306/finora_db
DB_USERNAME=root
DB_PASSWORD=your_password
JWT_SECRET=your_512bit_secret


### Start
```bash
# Backend + Kafka + Redis
cd finora-backend
docker compose up

# Frontend (separate terminal)
cd finora-frontend
npm run dev
# → localhost:5173
```

### Test Credentials

Email: tanuj@finora.com
Password: SecurePass123


---

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register user |
| POST | `/api/auth/login` | Login, returns JWT |
| GET | `/api/accounts?userId=` | Get user accounts |
| POST | `/api/accounts` | Create account |
| GET | `/api/transactions?accountId=` | Get transactions (paginated) |
| POST | `/api/transactions` | Create transaction |
| POST | `/api/transactions/{id}/reverse` | Reverse flagged transaction |
| POST | `/api/transfers` | P2P transfer (Saga) |
| GET | `/api/recurring-transfers?accountId=` | List recurring transfers |
| POST | `/api/recurring-transfers` | Create recurring transfer |
| PATCH | `/api/recurring-transfers/{id}/pause` | Pause |
| PATCH | `/api/recurring-transfers/{id}/resume` | Resume |
| DELETE | `/api/recurring-transfers/{id}` | Cancel |
| GET | `/api/analytics/{accountId}` | Category-wise spending from Redis |

---

## Tests

```bash
mvn test
```

| Test Class | Coverage |
|-----------|----------|
| `FraudDetectionServiceTest` | Normal tx, flagged tx, baseline floor |
| `TransferServiceTest` | Completed, compensated, failed, idempotency |
| `TransactionFlowIntegrationTest` | Full E2E: register → login → account → transactions → fraud |

---

## CI/CD

GitHub Actions pipeline on every push:
- Spins up MySQL + Redis + Kafka service containers
- Runs `mvn clean package` (all tests)
- Builds Docker image

```yaml
# .github/workflows/ci.yml
```