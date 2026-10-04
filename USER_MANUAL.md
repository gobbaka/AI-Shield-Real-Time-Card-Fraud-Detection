# 🛡️ AI Shield — User Manual & Evaluator Quick Start Guide

Welcome to **AI Shield – Real-Time AI/ML Card Fraud Detection & Step-Up Owner Verification System**. This manual provides an easy-to-follow guide for running, demonstrating, and evaluating the full platform.

---

## ⚡ 1-Click Startup (For Windows)

Simply double-click the launcher script in the project root:
```cmd
START_AI_SHIELD.bat
```
*This will automatically launch the Spring Boot backend on port 8080, launch the Angular frontend on port 4200, and open `http://localhost:4200` in your default browser within 8 seconds.*

---

## 🔑 Pre-Configured Demo Accounts

| Role | Email / Login ID | Password | 2FA OTP Code | Access Capabilities |
| :--- | :--- | :--- | :--- | :--- |
| **🛡️ Admin** | `pradeep@example.com` | `password123` | `123456` | Full Fraud Operations Command Center, Live Streaming, Anomaly Analysis, Rule Engine Config, AI Governance Model Card |
| **💳 Cardholder** | `priya@example.com` | `password123` | `123456` | Cardholder Dashboard, My Cards, 1-Click Card Freeze/Unfreeze, Travel Mode Notices, Transaction History |

> **User-Friendly Tip**: On the login page, you can click the **`⚡ 1-Click Fast Demo Login`** buttons to enter immediately without typing credentials or OTP!

---

## 🔄 Instant Role Switcher

Inside the application, you can switch between **Admin Operations** and **Cardholder Portal** at any time by clicking the **`🔄 Switch to Cardholder (Priya)`** or **`🔄 Switch to Admin (Pradeep)`** button located at the bottom-left sidebar of every screen.

---

## 🎮 Recommended 60-Second Demo Flow for Evaluators / Viva

### Step 1: Login to Admin Command Center (`/dashboard`)
1. Click **`⚡ 1-Click Admin (Pradeep)`**.
2. Observe the **Real-Time Fraud Command Center** displaying live monitored volume, safe transaction rate, and ML anomaly engine metrics.
3. Click **`Authorize / Ingest Transaction`** at the top right to open the payment simulator terminal.
4. Try the pre-set attack buttons:
   - **`🪙 Crypto Spike (₹1.85L)`**: Dispatches an overseas cryptocurrency spike. Watch the AI ONNX model evaluate high risk ($\ge 90$) and block the transaction with explainable reasons.
   - **`☕ Safe Daily (₹3.5K)`**: Dispatches a standard grocery swipe. Watch the AI approve it instantly with minimal risk.

### Step 2: Test Cardholder Travel Mode (`/user/cards`)
1. Click **`🔄 Switch to Cardholder (Priya)`** on the bottom-left sidebar.
2. Navigate to **`My Cards`** from the navigation menu.
3. View your protected debit/credit cards with live balance, spending limits, and masked PANs.
4. Test **`✈️ Add Travel Notice`**:
   - Destination Country: `United Arab Emirates`
   - Destination City: `Dubai`
   - Dates: Next 7 days
   - Click **Save Travel Notice**.
5. When a transaction occurs in Dubai, the system dynamically checks your active itinerary and **safely permits the purchase** without triggering false-positive geo-velocity locks!

### Step 3: Test 1-Click Emergency Card Freeze (`/user/cards`)
1. On the card details card, toggle the **`Freeze Card`** switch.
2. The card status instantly flips to `FROZEN`.
3. Any subsequent transaction attempted on that card will be immediately rejected with reason `CARD_LOCKED_BY_USER`.
4. Click **`Unfreeze Card`** to restore active status in real time.

### Step 4: Test Out-of-Band Smartphone Step-Up 2FA (`/dashboard`)
1. When a borderline high-value swipe is detected, look at the bottom-right of the screen.
2. An interactive **Mobile Smartphone Simulator** pops up with an incoming bank SMS alert:
   > *"🚨 BANK SECURITY FRAUD ALERT: Dear Priya, a suspicious transaction was detected..."*
3. Click **`🔐 YES, I AUTHORIZED IT`** to verify via simulated FIDO2 biometric passkey, or **`🛑 NO, THIS WAS NOT ME`** to lock the card instantly.

### Step 5: Inspect AI Governance & ML Model Card (`/settings`)
1. Navigate to **`Settings`** in the Admin sidebar.
2. Scroll to the **`AI Governance & Production Model Card`** section.
3. Review the live metadata loaded from the production ONNX model:
   - Model Name: `RandomForest-v3.0-Production`
   - PR-AUC: `1.0000` | ROC-AUC: `0.9998`
   - Inference Engine: `Microsoft ONNX Runtime (Native SIMD / C++)`
   - 10-Feature Normalized Signature.

---

## 🧪 Automated End-to-End Verification Suite

To run the automated Python scenario verification test suite:
```powershell
python ml/src/test_scenarios_e2e.py
```
*Expected Output: `All 4 Financial Fraud Scenarios passed with 100% precision!`*

---

## 🛠️ Architecture Summary

- **Frontend**: Angular 19 (Standalone Components, TypeScript, CSS Grid Cyber Theme)
- **Backend**: Spring Boot 3.3.4 (Java 21, Spring Security, JWT, JPA)
- **AI/ML Engine**: Microsoft ONNX Runtime 1.19.2 (RandomForest Ensemble, 10-Feature Vector, Native In-Process Inference $<1.5\text{ms}$)
- **Database**: H2 In-Memory / PostgreSQL compatible with AES-256 GCM encrypted PANs.
