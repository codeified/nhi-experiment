package com.nhi.experiment.model;

import java.util.List;

/**
 * Holds the output of the candidate v2.0 NHI risk calculation.
 *
 * This class represents the mathematical calculation only. It does not
 * determine the final decision category, calculate confidence, or assign
 * a priority rank. Those responsibilities belong to separate components.
 *
 * For complete evidence, riskScore contains the calculated point score.
 * If one or more dimensions are missing, riskScore is null and riskMin
 * and riskMax represent the possible risk range under the fixed weights.
 *
 * Risk bounds are mathematical limits, not statistical confidence intervals.
 *
 * @param threatScore     calculated Threat score, or null if incomplete
 * @param exposureScore   calculated Exposure score, or null if incomplete
 * @param impactScore     calculated Impact score, or null if incomplete
 * @param riskScore       final point score, or null when evidence is missing
 * @param riskMin         lower bound of the possible overall risk score
 * @param riskMax         upper bound of the possible overall risk score
 * @param missingDimensions names of dimensions with unavailable evidence
 */
public record RiskCalculationV2(
        Double threatScore,
        Double exposureScore,
        Double impactScore,
        Double riskScore,
        double riskMin,
        double riskMax,
        List<String> missingDimensions
) {

    /**
     * Creates an immutable result.
     *
     * A defensive copy prevents callers from modifying the list of missing
     * dimensions after the calculation result has been created.
     */
    public RiskCalculationV2 {
        missingDimensions = List.copyOf(missingDimensions);
    }

    /**
     * Indicates whether every required evidence dimension was available.
     *
     * @return true if no evidence dimensions are missing; false otherwise
     */
    public boolean isComplete() {
        return missingDimensions.isEmpty();
    }
}