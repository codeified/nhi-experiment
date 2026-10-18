package com.nhi.experiment.engine;

import com.nhi.experiment.model.RiskCalculationV2;

/**
 * Makes a decision using v2.0 risk results and evidence confidence.
 *
 * Decision thresholds retain the current project thresholds:
 * Critical >= 80, High >= 60, Investigate >= 40,
 * Monitor/Remediate >= 20, and Low < 20.
 *
 * Confidence below 75% results in Investigate - Low Confidence.
 * When confidence is sufficient but missing-evidence risk bounds span
 * multiple categories, the result is Investigate - Risk Uncertain.
 *
 * For an uncertain range, priorityCategory reflects the highest plausible
 * category. This is a provisional triage signal, not a final risk score.
 */
public class DecisionEngineV2 {

    private static final double MIN_CONFIDENCE = 75.0;

    public record DecisionResult(
            String decision,
            String decisionCategory,
            String priorityCategory,
            boolean requiresInvestigation,
            boolean incompleteEvidence,
            String reason
    ) {}

    /**
     * Determines an assessment outcome from calculated risk and confidence.
     *
     * @param calculation risk score or fixed-weight risk bounds
     * @param confidence evidence confidence from 0 to 100
     * @return decision details and the provisional priority category
     */
    public DecisionResult decide(
            RiskCalculationV2 calculation,
            double confidence
    ) {
        if (calculation == null) {
            throw new IllegalArgumentException(
                    "Risk calculation must not be null"
            );
        }

        if (!Double.isFinite(confidence)
                || confidence < 0.0
                || confidence > 100.0) {
            throw new IllegalArgumentException(
                    "Confidence must be between 0 and 100"
            );
        }

        String lowerCategory = categoryFor(calculation.riskMin());
        String upperCategory = categoryFor(calculation.riskMax());

        // Use the upper bound for conservative provisional triage.
        String priorityCategory = upperCategory;

        boolean incompleteEvidence =
                !calculation.isComplete() || confidence < 100.0;

        // Preserve the existing low-confidence decision rule.
        if (confidence < MIN_CONFIDENCE) {
            return new DecisionResult(
                    "Investigate – Low Confidence",
                    "LOW_CONFIDENCE",
                    priorityCategory,
                    true,
                    true,
                    "Confidence is below 75%. Validate evidence before "
                            + "relying on the assessment."
            );
        }

        /*
         * Even if aggregate confidence is sufficient, missing evidence
         * might change the decision category. Do not conceal that uncertainty.
         */
        if (!lowerCategory.equals(upperCategory)) {
            return new DecisionResult(
                    "Investigate – Risk Uncertain",
                    "RISK_UNCERTAIN",
                    priorityCategory,
                    true,
                    true,
                    "Risk bounds span "
                            + lowerCategory + " to " + upperCategory
                            + ". Resolve missing evidence before treating "
                            + "the category or rank as final."
            );
        }

        String decision = labelFor(lowerCategory);

        boolean requiresInvestigation =
                lowerCategory.equals("INVESTIGATE");

        String reason;
        if (incompleteEvidence) {
            reason = "Risk remains within the " + lowerCategory
                    + " category, but evidence is incomplete or partial.";
        } else {
            reason = "Complete, high-confidence evidence supports the "
                    + lowerCategory + " category.";
        }

        return new DecisionResult(
                decision,
                lowerCategory,
                priorityCategory,
                requiresInvestigation,
                incompleteEvidence,
                reason
        );
    }

    /**
     * Maps a numeric risk score to the project's decision category.
     */
    private static String categoryFor(double risk) {
        if (risk >= 80.0) {
            return "CRITICAL";
        }
        if (risk >= 60.0) {
            return "HIGH";
        }
        if (risk >= 40.0) {
            return "INVESTIGATE";
        }
        if (risk >= 20.0) {
            return "MONITOR_REMEDIATE";
        }
        return "LOW";
    }

    /**
     * Provides the display label for a known risk category.
     */
    private static String labelFor(String category) {
        return switch (category) {
            case "CRITICAL" -> "Critical";
            case "HIGH" -> "High";
            case "INVESTIGATE" -> "Investigate";
            case "MONITOR_REMEDIATE" -> "Monitor / Remediate";
            default -> "Low";
        };
    }
}