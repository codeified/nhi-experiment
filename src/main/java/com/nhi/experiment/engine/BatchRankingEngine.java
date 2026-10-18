
package com.nhi.experiment.engine;

import com.nhi.experiment.model.RankedRiskAssessmentV2;
import com.nhi.experiment.model.RiskAssessmentV2;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Ranks NHI assessments without changing their risk scores or weights.
 *
 * The highest risk receives rank 1. Equal fixed scores share a competition
 * rank; for example, scores 90, 90, 70 receive ranks 1, 1, 3.
 *
 * For an assessment with risk bounds:
 * - Best rank = 1 + the number of NHIs guaranteed to have higher risk.
 * - Worst rank = 1 + the number of NHIs that could have higher risk.
 *
 * A rank is stable on the reported bounds when best and worst ranks match.
 * Risk bounds account for missing numeric evidence, not possible errors in
 * numeric values derived from partial-quality evidence.
 *
 * NHI ID is used only for deterministic display ordering, never to break
 * a risk-score tie into different priority ranks.
 */
public class BatchRankingEngine {

    private static final double EPSILON = 0.000001;

    /**
     * Calculates the possible rank interval for each assessment in the batch.
     *
     * @param assessments assessments from AssessmentServiceV2
     * @return immutable results sorted for deterministic display
     */
    public List<RankedRiskAssessmentV2> rank(
            List<RiskAssessmentV2> assessments
    ) {
        Objects.requireNonNull(assessments, "Assessments must not be null");

        if (assessments.isEmpty()) {
            return List.of();
        }

        validate(assessments);

        List<RankedRiskAssessmentV2> results = new ArrayList<>();

        for (RiskAssessmentV2 current : assessments) {
            int bestRank = 1;
            int worstRank = 1;

            for (RiskAssessmentV2 other : assessments) {
                if (current == other) {
                    continue;
                }

                // Other NHI must be ahead under every possible score.
                if (other.riskMin() > current.riskMax() + EPSILON) {
                    bestRank++;
                }

                // Other NHI could be ahead under at least one possible score.
                if (other.riskMax() > current.riskMin() + EPSILON) {
                    worstRank++;
                }
            }

            boolean stable = bestRank == worstRank;

            results.add(new RankedRiskAssessmentV2(
                    current,
                    stable ? bestRank : null,
                    bestRank,
                    worstRank,
                    stable
                            ? "RANK_STABLE_ON_REPORTED_SCORES"
                            : "RANK_UNCERTAIN"
            ));
        }

        // Stable display order only; this does not alter the calculated ranks.
        results.sort(
                Comparator.comparingInt(
                                RankedRiskAssessmentV2::bestPossibleRank)
                        .thenComparingInt(
                                RankedRiskAssessmentV2::worstPossibleRank)
                        .thenComparing(
                                Comparator.comparingDouble(
                                        (RankedRiskAssessmentV2 r) ->
                                                r.assessment().riskMin()
                                ).reversed()
                        )
                        .thenComparing(
                                r -> r.assessment().nhiId()
                        )
        );

        return List.copyOf(results);
    }

    /**
     * Checks IDs, bounds and point-score consistency before ranking.
     */
    private void validate(List<RiskAssessmentV2> assessments) {
        Set<String> ids = new HashSet<>();

        for (RiskAssessmentV2 assessment : assessments) {
            Objects.requireNonNull(
                    assessment, "An assessment must not be null");

            String id = assessment.nhiId();

            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("NHI ID must not be blank");
            }

            if (!ids.add(id)) {
                throw new IllegalArgumentException("Duplicate NHI ID: " + id);
            }

            double min = assessment.riskMin();
            double max = assessment.riskMax();

            if (!Double.isFinite(min) || !Double.isFinite(max)
                    || min < 0.0 || max > 100.0 || min > max) {
                throw new IllegalArgumentException(
                        "Invalid risk bounds for " + id);
            }

            Double score = assessment.riskScore();

            if (score != null) {
                if (!Double.isFinite(score)
                        || score < 0.0 || score > 100.0
                        || !equal(score, min)
                        || !equal(score, max)) {
                    throw new IllegalArgumentException(
                            "Point score must equal fixed bounds for " + id);
                }
            } else if (equal(min, max)) {
                throw new IllegalArgumentException(
                        "A null point score requires a nonzero range for " + id);
            }
        }
    }

    private boolean equal(double a, double b) {
        return Math.abs(a - b) <= EPSILON;
    }
}
