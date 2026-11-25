package com.strathmore.predictivedispatch.ml

import com.strathmore.predictivedispatch.network.CrashReportRequest
import com.strathmore.predictivedispatch.network.DispatchTriageResponse
import java.util.UUID

/**
 * On-Device Decision Tree Classifier
 *
 * Implements the exact decision rules and tree structure derived from training a
 * Scikit-Learn DecisionTreeClassifier(max_depth=4, random_state=42) on the Kenya Accidents
 * Database (clean_data.csv / kenya-accidents-database.xlsx) in ICS 3202: Artificial Intelligence
 * Project Deliverable 2 (DecisionTree_Base.ipynb).
 *
 * Features:
 *  - X[0]: HOUR (0..23)
 *  - X[1]: AGE (30.0=Adult 'A', 12.0=Juvenile 'J')
 *  - X[2]: CAUSE CODE (10=Head-on/Overtaking, 26=Speeding/Loss of Control, 54..58=Dangerous Crossing, 98=Hit & Run)
 *  - X[3]: VICTIM_CODE (0=Driver, 1=Motorcyclist/Cyclist, 2=Other, 3=Passenger, 4=Pedestrian)
 *  - X[4]: GENDER_CODE (0=Female, 1=Male, 2=Multiple)
 *  - X[5]: COUNTY_CODE (33=Nairobi)
 *
 * Target:
 *  - SEVERITY = 1: Priority Severe Trauma Alert (Requires immediate Red-Alert ambulance dispatch)
 *  - SEVERITY = 0: Moderate Incident (Single casualty / Non-critical routine monitoring)
 */
object DecisionTreeEngine {

    /**
     * Executes the exact Decision Tree classifier on-device and returns a complete triage response.
     */
    fun predictSeverity(request: CrashReportRequest): DispatchTriageResponse {
        var isSevere: Boolean
        var confidence: Double

        // Multi-casualty incidents (NO. > 1) are defined as Severe in dataset schema
        val effectiveGenderCode = if (request.casualtyCount > 1) 2 else request.genderCode

        // Exact Scikit-Learn Decision Tree (max_depth=4, random_state=42) evaluation:
        if (request.victimCode <= 3.50) {
            // DRIVER (0), MOTORCYCLIST (1), OTHER (2), PASSENGER (3)
            if (request.causeCode <= 11.50) {
                // Head-on collision or overtaking (e.g. Cause Code 10)
                if (request.countyCode <= 13.50) {
                    if (request.victimCode <= 2.0) {
                        isSevere = false
                        confidence = 0.704
                    } else {
                        // High passenger impact in head-on crash
                        isSevere = true
                        confidence = 0.714
                    }
                } else {
                    // Urban/Corridor counties (e.g. Nairobi = 33 > 13.50)
                    if (request.age <= 18.50) {
                        isSevere = false
                        confidence = 1.000
                    } else {
                        // Adult victim in head-on crash -> Critical Trauma
                        isSevere = true
                        confidence = 0.688
                    }
                }
            } else {
                // CAUSE CODE > 11.50 (Speeding 26, Hit & Run 98, Loss of control)
                if (effectiveGenderCode <= 1.50) {
                    // Single casualty (Male / Female)
                    if (request.countyCode <= 32.50) {
                        isSevere = false
                        confidence = 0.603
                    } else {
                        // Nairobi (33 > 32.50) single victim speed crash
                        isSevere = false
                        confidence = 0.728
                    }
                } else {
                    // Multiple casualties (Gender Code = 2) in speeding/high impact
                    isSevere = true
                    confidence = 1.000
                }
            }
        } else {
            // PEDESTRIAN (4)
            if (request.causeCode <= 58.50) {
                if (request.causeCode <= 54.00) {
                    if (effectiveGenderCode <= 1.50) {
                        isSevere = false
                        confidence = 0.960
                    } else {
                        isSevere = false
                        confidence = 0.667
                    }
                } else {
                    // High-speed pedestrian collision / dangerous crossing (54 < Cause Code <= 58)
                    isSevere = true
                    confidence = 1.000
                }
            } else {
                // CAUSE CODE > 58.50 (Hit & Run 98 / Other 99)
                if (request.countyCode <= 11.50) {
                    if (request.countyCode <= 10.50) {
                        isSevere = false
                        confidence = 1.000
                    } else {
                        isSevere = false
                        confidence = 0.857
                    }
                } else {
                    isSevere = false
                    confidence = 1.000
                }
            }
        }

        // Domain safety override: Multi-casualty crashes (NO. > 1) always demand Priority Red Alert
        if (request.casualtyCount > 1) {
            isSevere = true
            confidence = (0.92 + (request.casualtyCount * 0.02).coerceAtMost(0.08))
        }

        val incidentId = "NTSA-" + UUID.randomUUID().toString().substring(0, 6).uppercase()
        val dispatchStatus = if (isSevere) "DISPATCHED_RED_ALERT" else "ROUTINE_MONITORING"

        return DispatchTriageResponse(
            incidentId = incidentId,
            severity = if (isSevere) 1 else 0,
            confidence = (confidence * 100).toInt() / 100.0,
            dispatchStatus = dispatchStatus
        )
    }
}
