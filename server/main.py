"""
ICS 3202: Artificial Intelligence - Project Deliverable 2 (CAT 2)
Integrated Road Accident Prediction & Reporting System (Pulse Crash Alert)
Machine Learning Inference API using FastAPI and Scikit-Learn DecisionTreeClassifier
"""

import csv
import os
import uuid
from typing import Optional
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from sklearn.tree import DecisionTreeClassifier
from sklearn.preprocessing import MinMaxScaler
from sklearn.model_selection import train_test_split

app = FastAPI(
    title="Pulse Crash Alert - AI Predictive Dispatch API",
    description="Real-time crash severity classification engine trained on Kenya Accidents Database (NTSA/clean_data.csv)",
    version="1.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Feature definitions from clean_data.csv
FEATURE_NAMES = ['HOUR', 'AGE', 'CAUSE CODE', 'VICTIM_CODE', 'GENDER_CODE', 'COUNTY_CODE']

model: Optional[DecisionTreeClassifier] = None
scaler: Optional[MinMaxScaler] = None
model_accuracy: float = 0.7534


def load_and_train_model():
    global model, scaler, model_accuracy

    possible_paths = [
        os.path.join(os.path.dirname(__file__), "clean_data.csv"),
        os.path.join(os.path.dirname(__file__), "data", "clean_data.csv"),
        r"C:\Users\ogamb\Downloads\clean_data.csv",
    ]

    csv_path = None
    for p in possible_paths:
        if os.path.exists(p):
            csv_path = p
            break

    if not csv_path:
        print("[WARN] clean_data.csv not found at standard paths. Training with embedded Kenya boundaries.")
        return

    print(f"[INFO] Training DecisionTreeClassifier from: {csv_path}")
    rows = []
    labels = []

    with open(csv_path, mode="r", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for r in reader:
            rows.append([float(r[col]) for col in FEATURE_NAMES])
            labels.append(int(float(r["SEVERITY"])))

    scaler = MinMaxScaler()
    X_scaled = scaler.fit_transform(rows)

    X_train, X_test, y_train, y_test = train_test_split(
        X_scaled, labels, test_size=0.2, random_state=42, stratify=labels
    )

    clf = DecisionTreeClassifier(max_depth=4, random_state=42)
    clf.fit(X_train, y_train)

    score = clf.score(X_test, y_test)
    model_accuracy = round(float(score), 4)
    model = clf
    print(f"[SUCCESS] DecisionTreeClassifier trained successfully. Test Accuracy: {model_accuracy * 100:.2f}%")


@app.on_event("startup")
def on_startup():
    load_and_train_model()


class CrashReportRequest(BaseModel):
    latitude: float
    longitude: float
    gpsAccuracyMeters: float = 5.0
    hour: int = Field(default=12, ge=0, le=23)
    age: int = Field(default=30, ge=1, le=100)
    causeCode: int = Field(default=26)
    victimCode: int = Field(default=4)  # 0: Driver, 1: Motorcyclist, 3: Passenger, 4: Pedestrian
    genderCode: int = Field(default=0)  # 0: Female, 1: Male, 2: Multiple
    countyCode: int = Field(default=33)  # 33 = Nairobi
    casualtyCount: int = Field(default=1, ge=1)
    imageBase64: Optional[str] = None


class DispatchTriageResponse(BaseModel):
    incidentId: str
    severity: int
    confidence: float
    dispatchStatus: str


@app.get("/")
def root():
    return {
        "system": "Pulse Crash Alert AI Server",
        "model": "DecisionTreeClassifier (max_depth=4, random_state=42)",
        "dataset": "Kenya Accidents Database (clean_data.csv)",
        "status": "online"
    }


@app.get("/api/v1/health")
def health_check():
    return {
        "status": "healthy",
        "model_loaded": model is not None,
        "test_accuracy": f"{model_accuracy * 100:.2f}%",
        "features": FEATURE_NAMES
    }


@app.post("/api/v1/report-crash", response_model=DispatchTriageResponse)
def report_crash(report: CrashReportRequest):
    incident_id = f"NTSA-{uuid.uuid4().hex[:6].upper()}"

    # Determine multi-victim gender flag from clean_data.csv schema
    effective_gender_code = 2 if report.casualtyCount > 1 else report.genderCode

    if model is not None and scaler is not None:
        raw_features = [[
            float(report.hour),
            float(report.age),
            float(report.causeCode),
            float(report.victimCode),
            float(effective_gender_code),
            float(report.countyCode),
        ]]

        scaled_features = scaler.transform(raw_features)
        pred = int(model.predict(scaled_features)[0])
        probas = model.predict_proba(scaled_features)[0]
        confidence = float(probas[pred])
    else:
        # Exact deterministic rule mapping from DecisionTree_Base.ipynb
        if report.victimCode <= 3.50:
            if report.causeCode <= 11.50:
                if report.countyCode <= 13.50:
                    pred = 0 if report.victimCode <= 2.0 else 1
                    confidence = 0.704 if pred == 0 else 0.714
                else:
                    pred = 0 if report.age <= 18.50 else 1
                    confidence = 1.000 if pred == 0 else 0.688
            else:
                if effective_gender_code <= 1.50:
                    pred = 0
                    confidence = 0.728 if report.countyCode > 32.50 else 0.603
                else:
                    pred = 1
                    confidence = 1.000
        else:
            if report.causeCode <= 58.50:
                if report.causeCode <= 54.00:
                    pred = 0
                    confidence = 0.960 if effective_gender_code <= 1.50 else 0.667
                else:
                    pred = 1
                    confidence = 1.000
            else:
                pred = 0
                confidence = 1.000

    # Multi-casualty domain safety override
    if report.casualtyCount > 1:
        pred = 1
        confidence = min(0.98, max(confidence, 0.92 + report.casualtyCount * 0.02))

    dispatch_status = "DISPATCHED_RED_ALERT" if pred == 1 else "ROUTINE_MONITORING"

    return DispatchTriageResponse(
        incidentId=incident_id,
        severity=pred,
        confidence=round(confidence, 2),
        dispatchStatus=dispatch_status
    )


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
