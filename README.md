# 🛡️ AI Shield — Enterprise Credit Card Fraud Detection & Step-Up Verification System

[![Spring Boot 3.3.4](https://img.shields.io/badge/Spring_Boot-3.3.4-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Angular 19](https://img.shields.io/badge/Angular-19-DD0031?style=for-the-badge&logo=angular&logoColor=white)](https://angular.dev/)
[![Java 21/24](https://img.shields.io/badge/Java-21%20%2F%2024-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![OpenAPI 3.0](https://img.shields.io/badge/OpenAPI-3.0%20Swagger-85EA2D?style=for-the-badge&logo=swagger&logoColor=black)](http://localhost:8080/swagger-ui/index.html)
[![FIDO2 / WebAuthn](https://img.shields.io/badge/Security-FIDO2%20%2F%20WebAuthn-0052CC?style=for-the-badge&logo=shield&logoColor=white)](https://fidoalliance.org/)

**AI Shield** is a production-oriented, full-stack Credit Card Fraud Detection and Prevention platform. It combines a **Hybrid AI Fraud Engine** (Random Forest Anomaly Scorer + Rule Evaluator), an **Admin Operations Center**, and an **Automated Cardholder Step-Up SMS Verification Workflow** in compliance with banking regulations.

---

## 🌟 Key Architecture & Workflows

### 1. Hybrid AI Fraud Detection Engine
* **Random Forest ML Scorer**: Generates probabilistic anomaly scores based on amount deviation, category velocity, off-hours timing, and hardware footprint.
* **Rule Evaluator (Heuristics)**:
  * `RULE_HIGH_AMOUNT`: Critical transaction spikes (> ₹1,00,000 or 3.5x customer baseline).
  * `RULE_VELOCITY_SPIKE`: Rapid swipe frequency (> 3 swipes in < 10 minutes).
  * `RULE_IMPOSSIBLE_TRAVEL`: Cross-city/country swipe anomalies under 60 minutes.
  * `RULE_UNKNOWN_DEVICE`: Unrecognized hardware fingerprints or proxy/TOR IPs.
  * `RULE_RISKY_MERCHANT`: High-risk merchant categories (Crypto Exchanges, Wire Forex, Gambling).
* **Explainable AI Breakdown**: Provides full transparency on why a transaction was approved, flagged, or blocked.

---

### 2. Cardholder Step-Up Verification Flow (Zero Raw Biometrics)
When a transaction is flagged as suspicious:
```
[ Card Transaction Ingestion ]
            ↓
[ AI Fraud Engine Risk Analysis ]
            ↓
[ Generate 32-Byte Cryptographic Challenge (Replay-Protected) ]
            ↓
[ Dispatch Real SMS Alert to Cardholder's Linked Mobile Number ]
            ↓
    "Did you authorize this transaction?"
            ↓
    ┌───────────────────────────┬───────────────────────────┐
    │ [YES, I AUTHORIZED IT]    │ [NO, THIS WAS NOT ME]     │
    ↓                           ↓                           ↓
[ Trigger Step-Up Auth ]   [ Mark CUSTOMER_REPORTED_FRAUD ]
[ Validate Challenge Token ] [ Auto-Freeze / Block Card ]
[ Mark OWNER_VERIFIED ]     [ Escalate High-Priority Alert ]
```

---

## 🚀 Quick Start Guide

### Option 1: 1-Click Launcher (Windows)
Double-click `start-project.bat` in the project root directory. It will:
1. Start the Spring Boot REST backend on `http://localhost:8080`
2. Start the Angular frontend on `http://localhost:4200`
3. Open the browser to the Admin Dashboard and Swagger UI automatically.

### Option 2: Manual Start

#### Backend (Spring Boot):
```bash
cd backend
mvnw spring-boot:run
```
* Backend API: `http://localhost:8080`
* Swagger UI Docs: `http://localhost:8080/swagger-ui/index.html`
* H2 Database Console: `http://localhost:8080/h2-console` (`jdbc:h2:mem:frauddb`, user: `sa`, password: *(empty)*)

#### Frontend (Angular):
```bash
cd frontend/fraud-detection-ui
npm install
npm start
```
* Frontend Portal: `http://localhost:4200`

---

## 🔑 Default Credentials & Roles

| Role | Email | Password | Name |
| :--- | :--- | :--- | :--- |
| **Administrator** | `pradeep@example.com` | `password123` | Pradeep |
| **Customer** | `rahul@example.com` | `password123` | Rahul Sharma |
| **Customer** | `priya@example.com` | `password123` | Priya Singh |
| **Customer** | `arjun@example.com` | `password123` | Arjun Kumar |

---

## 📱 Interactive Attack Scenario Presets

On the **Admin Dashboard Ingestion Terminal**, select any scenario preset to test real-time AI scoring:

1. **Crypto Attack (₹1,85,000)**: Flags critical high amount + crypto category $\rightarrow$ Risk Score 98% (Declined & Blocked).
2. **Impossible Travel (Dubai)**: Flags London/Dubai IP within minutes of India swipe $\rightarrow$ Risk Score 92% (Flagged for Step-Up Verification).
3. **High Velocity Bot**: Flags 4 rapid swipes in under 2 minutes $\rightarrow$ Risk Score 85% (Verification Required).
4. **Safe Retail Purchase (₹2,400)**: Normal baseline transaction $\rightarrow$ Risk Score 8% (Approved).

---

## 📂 Core Endpoints Catalog

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/v1/auth/login` | JWT Authentication & token generation |
| `POST` | `/api/v1/transactions` | Ingest transaction & execute AI engine scoring |
| `GET` | `/api/v1/transactions` | Retrieve all audited transactions |
| `POST` | `/api/v1/transactions/{id}/block` | Freeze transaction & linked credit card |
| `GET` | `/api/v1/alerts` | List active fraud investigation alerts |
| `PATCH` | `/api/v1/alerts/{id}/status` | Update alert status (`Reviewing`, `Blocked`, `Resolved`) |
| `GET` | `/api/v1/customers` | Customer risk directory & card status |
| `GET` | `/api/v1/verifications/pending` | List pending cardholder verifications |
| `POST` | `/api/v1/verifications/{id}/step-up` | Cardholder approves charge via FIDO2/WebAuthn |
| `POST` | `/api/v1/verifications/{id}/reject` | Cardholder reports unauthorized charge & freezes card |
| `POST` | `/api/v1/verifications/{id}/send-sms` | Dispatches real bank SMS alert to linked phone |

---

## 🧪 Automated Testing
Run the backend test suite (100% passing):
```bash
cd backend
mvnw test
```
