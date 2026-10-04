import os
import numpy as np
import pandas as pd

def generate_fraud_dataset(n_samples=100000, random_seed=42):
    np.random.seed(random_seed)
    
    n_fraud = int(n_samples * 0.0035)  # 0.35% realistic fraud rate
    n_legit = n_samples - n_fraud
    
    # --- Legitimate Features ---
    legit_amounts = np.random.lognormal(mean=7.5, sigma=1.2, size=n_legit)  # ₹500 to ₹15,000 typical
    legit_baselines = legit_amounts * np.random.uniform(0.6, 1.4, size=n_legit)
    legit_amount_ratios = np.clip(legit_amounts / np.maximum(legit_baselines, 100.0), 0.1, 3.0)
    
    p_legit_hours = np.array([
        0.01, 0.005, 0.005, 0.005, 0.005, 0.01, 0.03, 0.05,
        0.06, 0.07, 0.08, 0.08, 0.07, 0.07, 0.07, 0.07,
        0.07, 0.07, 0.08, 0.07, 0.05, 0.04, 0.02, 0.01
    ])
    p_legit_hours = p_legit_hours / np.sum(p_legit_hours)
    legit_hours = np.random.choice(range(24), size=n_legit, p=p_legit_hours)
    legit_time_entropy = np.where((legit_hours >= 1) & (legit_hours <= 5), 0.85, 0.15)
    
    legit_merchant_risk = np.random.choice([0.10, 0.45, 0.95], size=n_legit, p=[0.85, 0.13, 0.02])
    legit_velocity_10m = np.random.choice([0, 1, 2], size=n_legit, p=[0.90, 0.08, 0.02])
    legit_velocity_1h = legit_velocity_10m + np.random.choice([0, 1, 2, 3], size=n_legit, p=[0.70, 0.20, 0.08, 0.02])
    legit_impossible_travel = np.zeros(n_legit)
    legit_cross_city = np.random.choice([0, 1], size=n_legit, p=[0.92, 0.08])
    legit_cold_start = np.random.choice([0, 1], size=n_legit, p=[0.94, 0.06])

    # Realistic overlap: 3% of legit transactions are high-value luxury / travel (e.g. electronics, flights)
    legit_luxury = np.random.rand(n_legit) < 0.03
    legit_amounts[legit_luxury] = np.random.lognormal(mean=9.5, sigma=0.6, size=np.sum(legit_luxury))
    legit_amount_ratios[legit_luxury] = np.random.uniform(1.8, 3.5, size=np.sum(legit_luxury))
    legit_merchant_risk[legit_luxury] = np.random.choice([0.10, 0.45], size=np.sum(legit_luxury), p=[0.75, 0.25])
    
    # 0.2% legit transactions have VPN/network-induced travel notice flag
    legit_vpn = np.random.rand(n_legit) < 0.002
    legit_impossible_travel[legit_vpn] = 1.0

    legit_rule_score = np.clip(
        (legit_amount_ratios * 5.0) + (legit_merchant_risk * 10.0) + 
        (legit_velocity_10m * 5.0) + (legit_impossible_travel * 25.0),
        0.0, 45.0
    )
    
    # --- Fraudulent Features ---
    fraud_amounts = np.random.lognormal(mean=10.5, sigma=1.0, size=n_fraud)  # ₹30,000 to ₹2,50,000+
    fraud_baselines = np.random.lognormal(mean=7.5, sigma=0.8, size=n_fraud)
    fraud_amount_ratios = np.clip(fraud_amounts / np.maximum(fraud_baselines, 100.0), 1.5, 12.0)
    
    p_fraud_hours = np.array([
        0.08, 0.12, 0.14, 0.12, 0.10, 0.06, 0.03, 0.02,
        0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02,
        0.02, 0.02, 0.03, 0.03, 0.04, 0.04, 0.05, 0.06
    ])
    p_fraud_hours = p_fraud_hours / np.sum(p_fraud_hours)
    fraud_hours = np.random.choice(range(24), size=n_fraud, p=p_fraud_hours)
    fraud_time_entropy = np.where((fraud_hours >= 1) & (fraud_hours <= 5), 0.85, 0.15)
    
    fraud_merchant_risk = np.random.choice([0.45, 0.95], size=n_fraud, p=[0.25, 0.75])
    fraud_velocity_10m = np.random.choice([0, 1, 2, 3, 4, 5], size=n_fraud, p=[0.15, 0.15, 0.25, 0.25, 0.10, 0.10])
    fraud_velocity_1h = fraud_velocity_10m + np.random.choice([1, 2, 3, 4, 6], size=n_fraud, p=[0.20, 0.25, 0.25, 0.20, 0.10])
    fraud_impossible_travel = np.random.choice([0, 1], size=n_fraud, p=[0.55, 0.45])
    fraud_cross_city = np.where(fraud_impossible_travel == 1, 1, np.random.choice([0, 1], size=n_fraud, p=[0.30, 0.70]))
    fraud_cold_start = np.random.choice([0, 1], size=n_fraud, p=[0.70, 0.30])
    
    # 2% subtle micro fraud / card testing
    fraud_micro = np.random.rand(n_fraud) < 0.02
    fraud_amounts[fraud_micro] = np.random.lognormal(mean=7.5, sigma=0.5, size=np.sum(fraud_micro))
    fraud_amount_ratios[fraud_micro] = np.random.uniform(0.9, 1.6, size=np.sum(fraud_micro))
    fraud_velocity_10m[fraud_micro] = 0
    fraud_impossible_travel[fraud_micro] = 0
    fraud_merchant_risk[fraud_micro] = 0.45

    fraud_rule_score = np.clip(
        (fraud_amount_ratios * 6.0) + (fraud_merchant_risk * 30.0) + 
        (fraud_velocity_10m * 10.0) + (fraud_impossible_travel * 40.0) + 
        (fraud_time_entropy * 15.0), 
        30.0, 100.0
    )
    
    df_legit = pd.DataFrame({
        'amount': legit_amounts,
        'amount_norm': np.clip(legit_amounts / 250000.0, 0.0, 1.0),
        'amount_ratio': legit_amount_ratios,
        'time_entropy': legit_time_entropy,
        'merchant_risk': legit_merchant_risk,
        'velocity_10m': legit_velocity_10m,
        'velocity_1h': legit_velocity_1h,
        'impossible_travel': legit_impossible_travel,
        'cross_city': legit_cross_city,
        'cold_start': legit_cold_start,
        'rule_score_norm': legit_rule_score / 100.0,
        'is_fraud': 0
    })
    
    df_fraud = pd.DataFrame({
        'amount': fraud_amounts,
        'amount_norm': np.clip(fraud_amounts / 250000.0, 0.0, 1.0),
        'amount_ratio': fraud_amount_ratios,
        'time_entropy': fraud_time_entropy,
        'merchant_risk': fraud_merchant_risk,
        'velocity_10m': fraud_velocity_10m,
        'velocity_1h': fraud_velocity_1h,
        'impossible_travel': fraud_impossible_travel,
        'cross_city': fraud_cross_city,
        'cold_start': fraud_cold_start,
        'rule_score_norm': fraud_rule_score / 100.0,
        'is_fraud': 1
    })
    
    df = pd.concat([df_legit, df_fraud], ignore_index=True)
    df = df.sample(frac=1.0, random_state=random_seed).reset_index(drop=True)
    return df

if __name__ == '__main__':
    os.makedirs('ml/data', exist_ok=True)
    print("Generating realistic Card Fraud Dataset (100,000 transactions, 0.35% fraud rate)...")
    df = generate_fraud_dataset(100000)
    output_csv = 'ml/data/card_transactions_dataset.csv'
    df.to_csv(output_csv, index=False)
    print(f"Saved dataset to {output_csv}")
    print(f"Total Samples: {len(df):,}")
    print(f"Legitimate Samples: {len(df[df['is_fraud'] == 0]):,} ({len(df[df['is_fraud'] == 0])/len(df)*100:.2f}%)")
    print(f"Fraudulent Samples: {len(df[df['is_fraud'] == 1]):,} ({len(df[df['is_fraud'] == 1])/len(df)*100:.2f}%)")
