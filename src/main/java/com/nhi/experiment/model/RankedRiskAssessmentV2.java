package com.nhi.experiment.model;

/**
 * Contains one NHI assessment together with its batch-ranking information.
 *
 * priorityRank is populated only when the NHI's position is stable across
 * the mathematical risk bounds of the assessed population.
 *
 * bestPossibleRank is the best rank supported by the risk bounds.
 * worstPossibleRank is the worst rank supported by the risk bounds.
 *
 * A rank interval represents uncertainty about relative ordering. It is
 * not a confidence interval and does not represent a probability.
 *
 * Ranking is conditional on the supplied numeric scores and bounds.
 * PARTIAL evidence quality remains separately visible in the assessment;
 * this component does not estimate numeric error for partial evidence.
 *
 * @param assessment        original v2.0 assessment
 * @param priorityRank      stable rank, or null when ordering is uncertain
 * @param bestPossibleRank best possible position, where 1 is highest priority
 * @param worstPossibleRank worst possible position
 * @param rankingStatus    stable or uncertain ranking status
 */
public record RankedRiskAssessmentV2(
        RiskAssessmentV2 assessment,
        Integer priorityRank,
        int bestPossibleRank,
        int worstPossibleRank,
        String rankingStatus
) {
}