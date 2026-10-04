import os
import json
import joblib
import numpy as np
import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.linear_model import LogisticRegression
from sklearn.ensemble import RandomForestClassifier
from xgboost import XGBClassifier
from sklearn.metrics import (
    precision_score, recall_score, f1_score, roc_auc_score,
    average_precision_score, confusion_matrix, classification_report
)

FEATURE_NAMES = [
    'amount_norm',
    'amount_ratio',
    'time_entropy',
    'merchant_risk',
    'velocity_10m',
    'velocity_1h',
    'impossible_travel',
    'cross_city',
    'cold_start',
    'rule_score_norm'
]

def train_and_evaluate():
    print("=" * 80)
    print("AI SHIELD — REAL MACHINE LEARNING FRAUD DETECTION PIPELINE")
    print("=" * 80)
    
    # 1. Load Dataset
    data_path = 'ml/data/card_transactions_dataset.csv'
    if not os.path.exists(data_path):
        raise FileNotFoundError(f"Dataset not found at {data_path}. Run dataset_builder.py first.")
        
    df = pd.read_csv(data_path)
    print(f"Loaded dataset: {len(df):,} records, {len(FEATURE_NAMES)} features.")
    
    X = df[FEATURE_NAMES].values.astype(np.float32)
    y = df['is_fraud'].values.astype(np.int32)
    
    # 2. Train / Validation / Test Split (70% Train, 15% Val, 15% Test)
    X_train_val, X_test, y_train_val, y_test = train_test_split(
        X, y, test_size=0.15, random_state=42, stratify=y
    )
    X_train, X_val, y_train, y_val = train_test_split(
        X_train_val, y_train_val, test_size=0.17647, random_state=42, stratify=y_train_val
    )
    
    print(f"Train set: {len(X_train):,} samples ({np.sum(y_train)} frauds, {np.sum(y_train)/len(X_train)*100:.2f}%)")
    print(f"Validation set: {len(X_val):,} samples ({np.sum(y_val)} frauds, {np.sum(y_val)/len(X_val)*100:.2f}%)")
    print(f"Test set: {len(X_test):,} samples ({np.sum(y_test)} frauds, {np.sum(y_test)/len(X_test)*100:.2f}%)")
    print("-" * 80)
    
    # Calculate scale_pos_weight for imbalance
    pos_weight = float(np.sum(y_train == 0) / max(1, np.sum(y_train == 1)))
    
    # 3. Model Zoo Definitions
    models = {
        'LogisticRegression_Balanced': LogisticRegression(
            class_weight='balanced',
            max_iter=1000,
            random_state=42
        ),
        'RandomForest_Ensemble': RandomForestClassifier(
            n_estimators=100,
            max_depth=10,
            min_samples_split=5,
            class_weight='balanced_subsample',
            n_jobs=-1,
            random_state=42
        ),
        'XGBoost_GradientBoosted': XGBClassifier(
            n_estimators=120,
            max_depth=5,
            learning_rate=0.08,
            scale_pos_weight=pos_weight,
            eval_metric='aucpr',
            random_state=42
        )
    }
    
    results = {}
    fitted_models = {}
    
    os.makedirs('ml/evaluation_results', exist_ok=True)
    os.makedirs('ml/models', exist_ok=True)
    
    for name, model in models.items():
        print(f"Training {name}...")
        model.fit(X_train, y_train)
        fitted_models[name] = model
        
        # Predict Probabilities on Test Set
        if hasattr(model, "predict_proba"):
            y_probs = model.predict_proba(X_test)[:, 1]
        else:
            y_probs = model.decision_function(X_test)
            
        y_preds = (y_probs >= 0.5).astype(int)
        
        # Metrics Calculation
        roc_auc = float(roc_auc_score(y_test, y_probs))
        pr_auc = float(average_precision_score(y_test, y_probs))
        precision = float(precision_score(y_test, y_preds, zero_division=0))
        recall = float(recall_score(y_test, y_preds, zero_division=0))
        f1 = float(f1_score(y_test, y_preds, zero_division=0))
        cm = confusion_matrix(y_test, y_preds).tolist()
        tn, fp, fn, tp = confusion_matrix(y_test, y_preds).ravel()
        
        fpr = float(fp / (fp + tn)) if (fp + tn) > 0 else 0.0
        fnr = float(fn / (fn + tp)) if (fn + tp) > 0 else 0.0
        
        results[name] = {
            'model_name': name,
            'precision': round(precision, 4),
            'recall': round(recall, 4),
            'f1_score': round(f1, 4),
            'roc_auc': round(roc_auc, 4),
            'pr_auc': round(pr_auc, 4),
            'confusion_matrix': {
                'true_negative': int(tn),
                'false_positive': int(fp),
                'false_negative': int(fn),
                'true_positive': int(tp)
            },
            'false_positive_rate': round(fpr, 4),
            'false_negative_rate': round(fnr, 4)
        }
        
        print(f"  -> PR-AUC: {pr_auc:.4f} | ROC-AUC: {roc_auc:.4f} | Recall: {recall:.4f} | Precision: {precision:.4f} | F1: {f1:.4f}")
        print(f"     Confusion Matrix: TP={tp}, FP={fp}, FN={fn}, TN={tn}")
        print("-" * 80)
        
    # 4. Model Selection (Highest PR-AUC and balanced F1)
    champion_name = max(results.keys(), key=lambda k: (results[k]['pr_auc'], results[k]['f1_score']))
    champion_model = fitted_models[champion_name]
    print(f"CHAMPION MODEL SELECTED: {champion_name}")
    print(f"Champion PR-AUC: {results[champion_name]['pr_auc']:.4f} | ROC-AUC: {results[champion_name]['roc_auc']:.4f}")
    
    # Save Results & Champion Model
    with open('ml/evaluation_results/model_comparison.json', 'w') as f:
        json.dump(results, f, indent=2)
        
    joblib.dump(champion_model, 'ml/models/champion_model.joblib')
    
    # Save feature names
    with open('ml/models/feature_names.json', 'w') as f:
        json.dump(FEATURE_NAMES, f, indent=2)
        
    print("Saved evaluation results to ml/evaluation_results/model_comparison.json")
    print("Saved champion model to ml/models/champion_model.joblib")
    
if __name__ == '__main__':
    train_and_evaluate()
