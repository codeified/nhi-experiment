package com.nhi.experiment.model;

import java.util.List;

/**
 * Represents the complete v2.0 assessment output for one NHI.
 *
 * <p>The assessment contains the risk calculation, evidence confidence,
 * missing dimensions, decision, and provisional priority category.</p>
 *
 * <p>{@code incompleteEvidence} is true when at least one risk dimension
 * is missing or has evidence quality below COMPLETE. It describes evidence
 * status, not the correctness of the risk score or decision.</p>
 *
 * <p>{@code priorityRank} remains null until the assessment is compared
 * with other NHIs in a batch. A single assessment cannot establish its
 * relative rank.</p>
 */

public record RiskAssessmentV2(
        String nhiId,
        Double threatScore,
        Double exposureScore,
        Double impactScore,
        Double riskScore,
        double riskMin,
        double riskMax,
        List<String> missingDimensions,
        double confidence,
        Integer priorityRank,
        String decision,
        String decisionCategory,
        String priorityCategory,
        /**
         * True when evidence is missing or any dimension has quality below COMPLETE.
         * This flag does not independently determine whether investigation is required.
         */
        boolean incompleteEvidence,
        boolean requiresInvestigation,
        String decisionReason
) {
    public RiskAssessmentV2 {
        missingDimensions = List.copyOf(missingDimensions);
    }
}