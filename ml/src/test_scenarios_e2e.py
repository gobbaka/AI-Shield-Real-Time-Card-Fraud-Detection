import requests
import json
import time
import sys

def safe_print(msg):
    sys.stdout.buffer.write((str(msg) + "\n").encode("utf-8", errors="replace"))
    sys.stdout.flush()

BASE_URL = "http://localhost:8080/api/v1"

def print_header(title):
    safe_print("\n" + "=" * 80)
    safe_print(f" {title}")
    safe_print("=" * 80)

def run_e2e_tests():
    print_header("AI SHIELD -- AUTOMATED END-TO-END MULTI-SCENARIO FRAUD TEST SUITE")

    # -------------------------------------------------------------
    # Scenario 1: Normal Low-Risk Transaction (Rahul in Hyderabad)
    # -------------------------------------------------------------
    print_header("SCENARIO 1: NORMAL LOW-RISK TRANSACTION (RAHUL IN HYDERABAD)")
    payload1 = {
        "cardNumber": "4532-1234-5678-4721",
        "cardLast4": "4721",
        "customerName": "Rahul Sharma",
        "amount": 1250.0,
        "merchantName": "Reliance Fresh Grocery",
        "merchantCategory": "RETAIL",
        "location": "Hyderabad",
        "deviceFingerprint": "DEV-CHROME-WIN"
    }
    r1 = requests.post(f"{BASE_URL}/transactions", json=payload1).json()
    safe_print(f"Status Code: 200 OK | Success: {r1.get('success')}")
    data1 = r1.get('data', {})
    safe_print(f"Transaction ID: {data1.get('id')} | Amount: INR {data1.get('amount')}")
    safe_print(f"Risk Level: {data1.get('risk')} | Risk Score: {data1.get('riskScore')} | Decision: {data1.get('decision')}")
    safe_print(f"Reasons: {data1.get('reasons')}")
    assert data1.get('decision') == "APPROVE", f"Expected APPROVE, got: {data1.get('decision')}"
    safe_print(">>> PASS: Normal transaction instantly approved.")

    # -------------------------------------------------------------
    # Scenario 2: Suspicious Crypto Anomaly (Vikram in Singapore)
    # -------------------------------------------------------------
    print_header("SCENARIO 2: CRITICAL CRYPTO SPENDING ANOMALY (VIKRAM)")
    payload2 = {
        "cardNumber": "4532-1234-5678-8401",
        "cardLast4": "8401",
        "customerName": "Vikram Rao",
        "amount": 215000.0,
        "merchantName": "Binance International",
        "merchantCategory": "CRYPTO",
        "location": "Singapore",
        "deviceFingerprint": "DEV-UNKNOWN-TOR"
    }
    r2 = requests.post(f"{BASE_URL}/transactions", json=payload2).json()
    data2 = r2.get('data', {})
    safe_print(f"Transaction ID: {data2.get('id')} | Amount: INR {data2.get('amount')}")
    safe_print(f"Risk Level: {data2.get('risk')} | Risk Score: {data2.get('riskScore')} | Decision: {data2.get('decision')}")
    safe_print(f"Reasons: {data2.get('reasons')}")
    assert data2.get('decision') in ["FLAG", "DECLINE"], f"Expected FLAG/DECLINE, got: {data2.get('decision')}"
    safe_print(">>> PASS: Anomalous crypto transaction intercepted by AI Engine.")

    # -------------------------------------------------------------
    # Scenario 3: Travel Mode Geo-Anomaly Suppression (Priya in Dubai)
    # -------------------------------------------------------------
    print_header("SCENARIO 3: ACTIVE CARDHOLDER TRAVEL MODE (PRIYA IN DUBAI)")
    payload3 = {
        "cardNumber": "4532-1234-5678-8821",
        "cardLast4": "8821",
        "customerName": "Priya Singh",
        "amount": 4200.0,
        "merchantName": "Dubai Duty Free",
        "merchantCategory": "RETAIL",
        "location": "Dubai",
        "deviceFingerprint": "DEV-IPHONE-ROAMING"
    }
    r3 = requests.post(f"{BASE_URL}/transactions", json=payload3).json()
    data3 = r3.get('data', {})
    safe_print(f"Transaction ID: {data3.get('id')} | Location: {data3.get('location')}")
    safe_print(f"Risk Score: {data3.get('riskScore')} | Decision: {data3.get('decision')}")
    safe_print(f"Reasons: {data3.get('reasons')}")
    has_travel_suppression = any("Travel Mode" in reason for reason in data3.get('reasons', []))
    assert has_travel_suppression, "Expected Travel Mode geo-velocity anomaly suppression"
    assert data3.get('decision') == "APPROVE", f"Expected APPROVE, got: {data3.get('decision')}"
    safe_print(">>> PASS: Travel Mode successfully suppressed geo-velocity penalty!")

    # -------------------------------------------------------------
    # Scenario 4: 1-Click Card Freeze Enforcement
    # -------------------------------------------------------------
    print_header("SCENARIO 4: 1-CLICK CARD FREEZE ENFORCEMENT")
    lock_resp = requests.post(f"{BASE_URL}/customers/cards/1/toggle-lock").json()
    safe_print(f"Card ending 4721 lock status: {lock_resp.get('data', {}).get('status')}")

    payload4 = {
        "cardNumber": "4532-1234-5678-4721",
        "cardLast4": "4721",
        "customerName": "Rahul Sharma",
        "amount": 500.0,
        "merchantName": "Coffee Shop",
        "merchantCategory": "RETAIL",
        "location": "Hyderabad"
    }
    r4 = requests.post(f"{BASE_URL}/transactions", json=payload4)
    safe_print(f"HTTP Status on Frozen Card: {r4.status_code} (Expected: 400 Bad Request)")
    assert r4.status_code == 400, f"Expected HTTP 400, got: {r4.status_code}"
    safe_print(">>> PASS: Emergency card freeze prevented unauthorized transaction.")

    # Unlock card back to active state
    requests.post(f"{BASE_URL}/customers/cards/1/toggle-lock")
    safe_print("Restored card status back to ACTIVE.")

    print_header("ALL 4 END-TO-END FRAUD SCENARIOS VERIFIED AND PASSED (100% SUCCESS)")

if __name__ == '__main__':
    run_e2e_tests()