package com.nhi.experiment.engine;

public class DecisionEngine {

    private static final double CONFIDENCE_THRESHOLD = 75.0;

    public DecisionResult determineDecision(
            double riskScore,
            double confidence) {

        if (confidence < CONFIDENCE_THRESHOLD) {
            return new DecisionResult(
                    "Investigate – Low Confidence",
                    "LOW_CONFIDENCE"
            );
        }

        if (riskScore >= 80) {
            return new DecisionResult(
                    "Critical",
                    "CRITICAL"
            );
        }

        if (riskScore >= 60) {
            return new DecisionResult(
                    "High",
                    "HIGH"
            );
        }

        if (riskScore >= 40) {
            return new DecisionResult(
                    "Investigate",
                    "INVESTIGATE"
            );
        }

        if (riskScore >= 20) {
            return new DecisionResult(
                    "Monitor / Remediate",
                    "MONITOR_REMEDIATE"
            );
        }

        return new DecisionResult(
                "Low",
                "LOW"
        );
    }

    public record DecisionResult(
            String decision,
            String category) {
    }
}