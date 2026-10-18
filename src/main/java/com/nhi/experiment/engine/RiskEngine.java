package com.nhi.experiment.engine;

import com.nhi.experiment.model.Evidence;
import com.nhi.experiment.model.RiskAssessment;

public class RiskEngine {

    /*
     * ==============================
     * TOP-LEVEL RISK WEIGHTS
     * ==============================
     */

    private static final double THREAT_WEIGHT = 0.40;
    private static final double EXPOSURE_WEIGHT = 0.35;
    private static final double IMPACT_WEIGHT = 0.25;

    /*
     * ==============================
     * THREAT WEIGHTS
     * ==============================
     *
     * T = 0.25P + 0.25A + 0.50B
     */

    private static final double PRIVILEGE_WEIGHT = 0.25;
    private static final double AUTHENTICATION_WEIGHT = 0.25;
    private static final double BEHAVIOR_WEIGHT = 0.50;

    /*
     * ==============================
     * EXPOSURE WEIGHTS
     * ==============================
     *
     * E = 0.40S + 0.40O + 0.20M
     */

    private static final double SENSITIVE_ACCESS_WEIGHT = 0.40;
    private static final double OBSERVED_EXPOSURE_WEIGHT = 0.40;
    private static final double MISCONFIGURATION_WEIGHT = 0.20;

    /*
     * ==============================
     * IMPACT WEIGHTS
     * ==============================
     *
     * I = 0.70R + 0.30As
     */

    private static final double RESOURCE_CRITICALITY_WEIGHT = 0.70;
    private static final double ASSET_SENSITIVITY_WEIGHT = 0.30;

    /*
     * ==============================
     * PUBLIC API
     * ==============================
     */

    public RiskAssessment calculate(
            String nhiId,
            Evidence evidence) {

        double threat = calculateThreat(evidence);
        double exposure = calculateExposure(evidence);
        double impact = calculateImpact(evidence);

        double risk =
                (THREAT_WEIGHT * threat)
                        + (EXPOSURE_WEIGHT * exposure)
                        + (IMPACT_WEIGHT * impact);

        RiskAssessment assessment = new RiskAssessment();

        assessment.setNhiId(nhiId);
        assessment.setThreatScore(round(threat));
        assessment.setExposureScore(round(exposure));
        assessment.setImpactScore(round(impact));
        assessment.setRiskScore(round(risk));

        return assessment;
    }

    /*
     * ==============================
     * THREAT
     * ==============================
     */

    private double calculateThreat(Evidence evidence) {

        return
                (PRIVILEGE_WEIGHT * required(evidence.getPrivilegeLevel()))
                        +
                        (AUTHENTICATION_WEIGHT * required(evidence.getAuthentication()))
                        +
                        (BEHAVIOR_WEIGHT * required(evidence.getBehavior()));
    }

    /*
     * ==============================
     * EXPOSURE
     * ==============================
     */

    private double calculateExposure(Evidence evidence) {

        return
                (SENSITIVE_ACCESS_WEIGHT *
                        required(evidence.getSensitiveAccess()))
                        +
                        (OBSERVED_EXPOSURE_WEIGHT *
                                required(evidence.getObservedExposure()))
                        +
                        (MISCONFIGURATION_WEIGHT *
                                required(evidence.getMisconfiguration()));
    }

    /*
     * ==============================
     * IMPACT
     * ==============================
     */

    private double calculateImpact(Evidence evidence) {

        return
                (RESOURCE_CRITICALITY_WEIGHT *
                        required(evidence.getResourceCriticality()))
                        +
                        (ASSET_SENSITIVITY_WEIGHT *
                                required(evidence.getAssetSensitivity()));
    }

    /*
     * ==============================
     * VALIDATION
     * ==============================
     */

    private double required(Double value) {

        if (value == null) {
            throw new IllegalArgumentException(
                    "Required evidence value cannot be null"
            );
        }

        if (value < 0 || value > 100) {
            throw new IllegalArgumentException(
                    "Evidence score must be between 0 and 100"
            );
        }

        return value;
    }

    /*
     * ==============================
     * ROUNDING
     * ==============================
     */

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}