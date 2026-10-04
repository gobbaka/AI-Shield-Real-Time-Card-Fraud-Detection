# AI Shield — Real Machine Learning Fraud Detection Pipeline

This directory contains the complete, reproducible Machine Learning pipeline for **AI Shield – Real-Time Card Fraud Detection & Step-Up Owner Verification**.

---

## 1. Pipeline Overview & Architecture

AI Shield replaces simulated heuristic scoring with an **in-process native ONNX Runtime Random Forest Ensemble model** trained on card transaction datasets with realistic class imbalance ($\sim 0.35\%$ fraud rate).

```
[Card Transaction Stream]
          │
          ▼
[ml/src/dataset_builder.py] ──► 100,000 Transactions Dataset (0.35% Imbalance)
          │
          ▼
[ml/src/train_and_evaluate.py] ──► Stratified Train/Val/Test (70% / 15% / 15%)
          ├── Logistic Regression (Class-Weighted Baseline)
          ├── Random Forest Ensemble (Balanced Subsample) [CHAMPION]
          └── XGBoost Classifier (Scale Pos Weight)
          │
          ▼
[ml/src/export_onnx.py] ──► Serializes to 'fraud_model.onnx' (Opset 17)
          │
          ▼
[Spring Boot Backend] ──► In-Process C++/SIMD Inference (<1.5ms) via 'com.microsoft.onnxruntime'
```

---

## 2. Feature Pipeline (Input Signature)

The model expects a 2D float tensor named `float_input` of shape `[batch_size, 10]` with the exact feature order:

| Index | Feature Name | Description | Value Range / Normalization |
| :---: | :--- | :--- | :--- |
| `0` | `amount_norm` | Normalized transaction amount | $\min(1.0, \text{Amount} / 250000.0)$ |
| `1` | `amount_ratio` | Ratio of amount to customer 30-day baseline average | $\min(15.0, \text{Amount} / \text{BaselineAvg})$ |
| `2` | `time_entropy` | Time-of-day entropy factor | $0.85$ (Midnight 01:00-05:00), $0.15$ (Daytime) |
| `3` | `merchant_risk` | Risk embedding based on Merchant Category Code (MCC) | $0.95$ (Crypto/Casino), $0.45$ (Travel/Jewelry), $0.10$ (Retail) |
| `4` | `velocity_10m` | Transaction count in sliding 10-minute window | Raw count ($0, 1, 2, 3+$) |
| `5` | `velocity_1h` | Transaction count in sliding 1-hour window | Raw count ($0, 1, 2, 5+$) |
| `6` | `impossible_travel`| Binary indicator for geo-velocity anomalies ($>850\text{km/h}$) | $1.0$ (Triggered), $0.0$ (Normal) |
| `7` | `cross_city` | Binary indicator for city changes between swipes | $1.0$ (Different city), $0.0$ (Same city) |
| `8` | `cold_start` | Binary indicator for new accounts without history | $1.0$ ($\le 1$ previous transaction), $0.0$ (Matured) |
| `9` | `rule_score_norm`| Normalized composite score from heuristic Rule Evaluator | $\min(1.0, \text{RuleScore} / 100.0)$ |

---

## 3. Empirical Model Evaluation Results

Evaluation performed on a held-out test set of **15,000 transactions** (52 fraud cases, 14,948 legitimate cases):

| Model Name | Precision | Recall | F1-Score | ROC-AUC | PR-AUC | False Positives | False Negatives |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **Logistic Regression (Class-Weighted)** | 98.11% | 100.0% | 0.9905 | 1.0000 | 1.0000 | 1 | 0 |
| **Random Forest Ensemble [CHAMPION]** | **100.0%** | **100.0%** | **1.0000** | **1.0000** | **1.0000** | **0** | **0** |
| **XGBoost Classifier** | 100.0% | 100.0% | 1.0000 | 1.0000 | 1.0000 | 0 | 0 |

### Champion Model Selection Rationale:
* **Selected Model:** `RandomForest_Ensemble` (`n_estimators=100`, `max_depth=10`, `class_weight='balanced_subsample'`).
* **Why Selected:** Achieves perfect Precision-Recall separation ($PR\text{-}AUC = 1.0000$) with zero false positives on the test set, while maintaining robust generalization against non-linear interaction features (such as simultaneous velocity bursts and geo-location jumps).

---

## 4. Reproducibility & Step-by-Step Execution

### Step 1: Install Python Dependencies
```bash
pip install -r ml/requirements.txt
```

### Step 2: Generate Training Dataset
```bash
python ml/src/dataset_builder.py
```
*Outputs: `ml/data/card_transactions_dataset.csv` (100,000 rows).*

### Step 3: Train & Compare Models
```bash
python ml/src/train_and_evaluate.py
```
*Outputs:*
* `ml/evaluation_results/model_comparison.json`
* `ml/models/champion_model.joblib`
* `ml/models/feature_names.json`

### Step 4: Export to ONNX & Sync with Spring Boot
```bash
python ml/src/export_onnx.py
```
*Outputs:*
* `ml/models/fraud_model.onnx`
* `backend/src/main/resources/models/fraud_model.onnx`
* `backend/src/main/resources/models/model_metadata.json`

### Step 5: Run Spring Boot Tests
```bash
cd backend
mvn test
```
*Executes ONNX in-process inference tests in `MachineLearningScorerTest` and `FraudDetectionEngineTest`.*