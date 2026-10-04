import os
import json
import shutil
import joblib
import numpy as np
import onnxruntime as rt
from skl2onnx import convert_sklearn
from skl2onnx.common.data_types import FloatTensorType

def export_to_onnx():
    print("=" * 80)
    print("AI SHIELD — EXPORTING TRAINED CHAMPION MODEL TO ONNX")
    print("=" * 80)
    
    # 1. Load Champion Model
    model_path = 'ml/models/champion_model.joblib'
    if not os.path.exists(model_path):
        raise FileNotFoundError(f"Champion model not found at {model_path}. Run train_and_evaluate.py first.")
        
    model = joblib.load(model_path)
    
    # 2. Load Feature Names
    with open('ml/models/feature_names.json', 'r') as f:
        feature_names = json.load(f)
        
    n_features = len(feature_names)
    print(f"Model loaded: {type(model).__name__} with {n_features} input features.")
    
    # 3. Define Input Signature for ONNX [batch_size, 10]
    initial_type = [('float_input', FloatTensorType([None, n_features]))]
    
    # 4. Convert Scikit-Learn Model to ONNX
    onnx_model = convert_sklearn(
        model, 
        initial_types=initial_type, 
        target_opset=17,
        options={type(model): {'zipmap': False}}  # Output raw probability tensor array
    )
    
    onnx_path = 'ml/models/fraud_model.onnx'
    with open(onnx_path, "wb") as f:
        f.write(onnx_model.SerializeToString())
    print(f"ONNX model successfully serialized to: {onnx_path}")
    
    # 5. Verify Model Inference with ONNX Runtime in Python
    sess = rt.InferenceSession(onnx_path)
    input_name = sess.get_inputs()[0].name
    print(f"Verified ONNX session. Input node name: '{input_name}', shape: {sess.get_inputs()[0].shape}")
    
    # Test Sample 1: Low-Risk Normal Transaction
    # [amount_norm=0.01, amount_ratio=0.8, time_entropy=0.15, merchant_risk=0.10, velocity_10m=0, velocity_1h=0, impossible_travel=0, cross_city=0, cold_start=0, rule_score_norm=0.05]
    sample_low = np.array([[0.01, 0.8, 0.15, 0.10, 0.0, 0.0, 0.0, 0.0, 0.0, 0.05]], dtype=np.float32)
    pred_low = sess.run(None, {input_name: sample_low})
    prob_low = float(pred_low[1][0][1]) if len(pred_low) > 1 and pred_low[1].ndim > 1 else float(pred_low[0][0])
    print(f"Test Inference (Normal Tx): P(Fraud) = {prob_low:.4f} (Expected: ~0.00)")
    
    # Test Sample 2: Critical High-Risk Fraud Transaction
    # [amount_norm=0.85, amount_ratio=8.5, time_entropy=0.85, merchant_risk=0.95, velocity_10m=4, velocity_1h=5, impossible_travel=1, cross_city=1, cold_start=1, rule_score_norm=0.95]
    sample_high = np.array([[0.85, 8.5, 0.85, 0.95, 4.0, 5.0, 1.0, 1.0, 1.0, 0.95]], dtype=np.float32)
    pred_high = sess.run(None, {input_name: sample_high})
    prob_high = float(pred_high[1][0][1]) if len(pred_high) > 1 and pred_high[1].ndim > 1 else float(pred_high[0][0])
    print(f"Test Inference (Critical Fraud Tx): P(Fraud) = {prob_high:.4f} (Expected: >0.85)")
    
    # 6. Copy Model and Metadata into Spring Boot backend resources
    target_models_dir = 'backend/src/main/resources/models'
    os.makedirs(target_models_dir, exist_ok=True)
    target_onnx = os.path.join(target_models_dir, 'fraud_model.onnx')
    shutil.copyfile(onnx_path, target_onnx)
    print(f"Copied ONNX model to Spring Boot backend: {target_onnx}")
    
    # 7. Generate Metadata JSON
    with open('ml/evaluation_results/model_comparison.json', 'r') as f:
        comp = json.load(f)
        
    metadata = {
        "model_name": "RandomForest-v3.0-Production",
        "algorithm": "Random Forest Ensemble (Balanced Subsample)",
        "framework": "ONNX Runtime v1.19 / Scikit-Learn v1.8",
        "input_tensor_name": input_name,
        "input_shape": [1, n_features],
        "feature_names": feature_names,
        "metrics": comp.get("RandomForest_Ensemble", {}),
        "decision_thresholds": {
            "low_risk_max": 0.39,
            "medium_risk_max": 0.79,
            "high_risk_min": 0.80
        }
    }
    
    meta_path_ml = 'ml/models/model_metadata.json'
    meta_path_backend = os.path.join(target_models_dir, 'model_metadata.json')
    
    with open(meta_path_ml, 'w') as f:
        json.dump(metadata, f, indent=2)
    with open(meta_path_backend, 'w') as f:
        json.dump(metadata, f, indent=2)
        
    print(f"Generated and synced metadata: {meta_path_backend}")
    print("=" * 80)

if __name__ == '__main__':
    export_to_onnx()
