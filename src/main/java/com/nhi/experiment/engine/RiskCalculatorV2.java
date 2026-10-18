package com.nhi.experiment.engine;

import com.nhi.experiment.model.Evidence;
import com.nhi.experiment.model.EvidenceQuality;
import com.nhi.experiment.model.RiskCalculationV2;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Calculates NHI risk using the candidate v2.0 Threat, Exposure and Impact
 * model.
 *
 * The calculator uses fixed weights:
 *
 * Threat   = 0.25 * A + 0.50 * B + 0.25 * M
 * Exposure = 0.25 * P + 0.375 * S + 0.375 * O
 * Impact   = 0.70 * R + 0.30 * As
 *
 * Overall Risk = 0.40 * Threat + 0.35 * Exposure + 0.25 * Impact
 *
 * Missing evidence never causes weights to be redistributed. Instead, the
 * calculator preserves the original weights and calculates mathematical
 * risk bounds. A final point score is returned only when all eight
 * dimensions have available numeric values.
 *
 * A measured score of zero is valid evidence. A null score means the
 * corresponding evidence is unavailable.
 *
 * Confidence is deliberately not calculated here. Evidence quality and
 * decision-making are handled by separate components.
 *
 * This is the candidate v2.0 engine; the existing v1.0 RiskEngine should
 * remain unchanged for experimental comparison.
 */
public class RiskCalculatorV2 {

    /*
     * Fixed global weights for candidate v2.0.
     *
     * These are derived from the construct weights:
     * - Threat contributes 40% of overall Risk.
     * - Exposure contributes 35% of overall Risk.
     * - Impact contributes 25% of overall Risk.
     *
     * Keeping global weights explicit makes missing-evidence bounds
     * straightforward to calculate without reweighting observed inputs.
     */
    private static final double W_A  = 0.10000;
    private static final double W_B  = 0.20000;
    private static final double W_M  = 0.10000;
    private static final double W_P  = 0.08750;
    private static final double W_S  = 0.13125;
    private static final double W_O  = 0.13125;
    private static final double W_R  = 0.17500;
    private static final double W_AS = 0.07500;

    /**
     * Represents one risk dimension and the information needed to process it.
     *
     * @param name   descriptive dimension identifier
     * @param value  observed risk score, or null if unavailable
     * @param quality quality/availability classification for the evidence
     * @param weight fixed contribution of the dimension to overall Risk
     */
    private record Dimension(
            String name,
            Double value,
            EvidenceQuality quality,
            double weight
    ) {}

    /**
     * Calculates Threat, Exposure, Impact and overall risk information.
     *
     * For complete evidence, the method returns a point Risk score.
     *
     * When evidence is missing:
     * - The original weights remain unchanged.
     * - riskMin contains the contribution from observed dimensions.
     * - riskMax adds the maximum possible contribution of missing dimensions.
     * - riskScore is null, avoiding a misleading point estimate.
     *
     * Values marked PARTIAL still contribute their observed numeric scores.
     * Their lower evidence quality is handled independently by the
     * ConfidenceEngine.
     *
     * @param evidence the NHI's risk scores and evidence-quality metadata
     * @return the calculation result, including risk bounds and missing fields
     * @throws NullPointerException if the Evidence object itself is null
     * @throws IllegalArgumentException if evidence metadata is inconsistent
     *         or a numeric score falls outside the range 0 through 100
     */
    public RiskCalculationV2 calculate(Evidence evidence) {
        Objects.requireNonNull(evidence, "Evidence must not be null");

        /*
         * Build a consistent list of dimensions. Each dimension retains its
         * own fixed global weight so missing values can be handled without
         * changing the original scoring model.
         */
        List<Dimension> dimensions = List.of(
                new Dimension(
                        "A_AUTHENTICATION",
                        evidence.getAuthentication(),
                        evidence.getAuthenticationQuality(),
                        W_A
                ),
                new Dimension(
                        "B_BEHAVIOR",
                        evidence.getBehavior(),
                        evidence.getBehaviorQuality(),
                        W_B
                ),
                new Dimension(
                        "M_MISCONFIGURATION",
                        evidence.getMisconfiguration(),
                        evidence.getMisconfigurationQuality(),
                        W_M
                ),
                new Dimension(
                        "P_PRIVILEGE",
                        evidence.getPrivilegeLevel(),
                        evidence.getPrivilegeQuality(),
                        W_P
                ),
                new Dimension(
                        "S_SENSITIVE_ACCESS",
                        evidence.getSensitiveAccess(),
                        evidence.getSensitiveAccessQuality(),
                        W_S
                ),
                new Dimension(
                        "O_OBSERVED_EXPOSURE",
                        evidence.getObservedExposure(),
                        evidence.getObservedExposureQuality(),
                        W_O
                ),
                new Dimension(
                        "R_RESOURCE_CRITICALITY",
                        evidence.getResourceCriticality(),
                        evidence.getResourceCriticalityQuality(),
                        W_R
                ),
                new Dimension(
                        "AS_ASSET_SENSITIVITY",
                        evidence.getAssetSensitivity(),
                        evidence.getAssetSensitivityQuality(),
                        W_AS
                )
        );

        List<String> missing = new ArrayList<>();

        // Sum of each observed score multiplied by its original global weight.
        double knownContribution = 0.0;

        // Total global weight belonging to dimensions with unavailable scores.
        double missingWeight = 0.0;

        for (Dimension dimension : dimensions) {
            Double value = dimension.value();
            EvidenceQuality quality = dimension.quality();

            /*
             * A missing score must be represented by null and marked MISSING.
             * A numeric score must have a valid quality classification.
             *
             * These checks prevent missing evidence from being silently
             * treated as a measured score of zero.
             */
            if (value == null) {
                if (quality != EvidenceQuality.MISSING) {
                    throw new IllegalArgumentException(
                            dimension.name()
                                    + " has no value but its quality is "
                                    + quality
                    );
                }

                missing.add(dimension.name());
                missingWeight += dimension.weight();
                continue;
            }

            validateScore(dimension.name(), value);

            if (quality == null || quality == EvidenceQuality.MISSING) {
                throw new IllegalArgumentException(
                        dimension.name()
                                + " has a value but its evidence quality "
                                + "is null or MISSING"
                );
            }

            /*
             * COMPLETE and PARTIAL values both contribute using the original
             * weight. Evidence quality affects confidence, not the risk score.
             */
            knownContribution += dimension.weight() * value;
        }

        /*
         * Calculate each construct only when all its component dimensions
         * are available. We do not renormalize weights within an incomplete
         * construct; its score remains null.
         */
        Double threat = constructScore(
                new Double[]{
                        evidence.getAuthentication(),
                        evidence.getBehavior(),
                        evidence.getMisconfiguration()
                },
                new double[]{0.25, 0.50, 0.25}
        );

        Double exposure = constructScore(
                new Double[]{
                        evidence.getPrivilegeLevel(),
                        evidence.getSensitiveAccess(),
                        evidence.getObservedExposure()
                },
                new double[]{0.25, 0.375, 0.375}
        );

        Double impact = constructScore(
                new Double[]{
                        evidence.getResourceCriticality(),
                        evidence.getAssetSensitivity()
                },
                new double[]{0.70, 0.30}
        );

        /*
         * Missing-evidence policy:
         *
         * Lower bound = observed weighted contributions.
         * Upper bound = observed contributions plus the maximum contribution
         *                permitted by every missing dimension.
         *
         * Each missing dimension may contribute between zero and 100 times
         * its global weight. These are mathematical bounds, not predictions.
         */
        double riskMin = round(knownContribution);
        double riskMax = round(
                knownContribution + (missingWeight * 100.0)
        );

        /*
         * Only complete evidence produces a final point Risk score.
         * Otherwise consumers must use the bounds and missing-dimension list.
         */
        Double riskScore = missing.isEmpty() ? riskMin : null;

        return new RiskCalculationV2(
                threat,
                exposure,
                impact,
                riskScore,
                riskMin,
                riskMax,
                missing
        );
    }

    /**
     * Calculates a construct score when every input is available.
     *
     * @param values component scores for the construct
     * @param weights fixed within-construct weights in the same order as values
     * @return the weighted construct score, or null if any input is missing
     */
    private static Double constructScore(
            Double[] values,
            double[] weights
    ) {
        double score = 0.0;

        for (int i = 0; i < values.length; i++) {
            if (values[i] == null) {
                return null;
            }

            validateScore("Construct input", values[i]);
            score += values[i] * weights[i];
        }

        return round(score);
    }

    /**
     * Validates that a risk dimension is a finite number from 0 through 100.
     *
     * A numeric zero is allowed because it represents an observed low-risk
     * condition. Missing evidence must instead be represented by null.
     *
     * @param name descriptive name used in validation error messages
     * @param score numeric score to validate
     * @throws IllegalArgumentException if the score is non-finite or out of range
     */
    private static void validateScore(String name, double score) {
        if (!Double.isFinite(score) || score < 0.0 || score > 100.0) {
            throw new IllegalArgumentException(
                    name + " score must be between 0 and 100"
            );
        }
    }

    /**
     * Rounds calculation results to two decimal places for reporting.
     *
     * Intermediate calculations retain full precision; rounding happens
     * only when a result is returned.
     *
     * @param value numeric calculation result
     * @return value rounded to two decimal places
     */
    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}