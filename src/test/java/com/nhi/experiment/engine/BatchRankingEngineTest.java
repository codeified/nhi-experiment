package com.nhi.experiment.engine;

import com.nhi.experiment.model.RiskAssessmentV2;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import com.nhi.experiment.model.RankedRiskAssessmentV2;

class BatchRankingEngineTest {

    private final BatchRankingEngine engine = new BatchRankingEngine();

    /**
     * Complete assessments with distinct risk scores should receive exact,
     * descending ranks.
     */
    @Test
    void ranksCompleteAssessmentsByDescendingRisk() {
        List<RiskAssessmentV2> assessments = List.of(
                assessment("nhi-c", 30.0, 30.0, 30.0, List.of(), 100.0),
                assessment("nhi-a", 90.0, 90.0, 90.0, List.of(), 100.0),
                assessment("nhi-b", 70.0, 70.0, 70.0, List.of(), 100.0)
        );

        var ranked = engine.rank(assessments);

        assertEquals("nhi-a", ranked.get(0).assessment().nhiId());
        assertEquals(1, ranked.get(0).priorityRank());
        assertEquals(1, ranked.get(0).bestPossibleRank());
        assertEquals(1, ranked.get(0).worstPossibleRank());

        assertEquals("nhi-b", ranked.get(1).assessment().nhiId());
        assertEquals(2, ranked.get(1).priorityRank());

        assertEquals("nhi-c", ranked.get(2).assessment().nhiId());
        assertEquals(3, ranked.get(2).priorityRank());
    }

    /**
     * Overlapping ranges must not be presented as a precise relative order.
     */
    @Test
    void overlappingRiskIntervalsProducePossibleRankRanges() {
        List<RiskAssessmentV2> assessments = List.of(
                assessment("nhi-a", 85.0, 85.0, 85.0, List.of(), 100.0),
                assessment("nhi-b", null, 70.0, 90.0,
                        List.of("B_BEHAVIOR"), 87.5),
                assessment("nhi-c", 60.0, 60.0, 60.0, List.of(), 100.0)
        );

        var ranked = engine.rank(assessments);

        var a = find(ranked, "nhi-a");
        var b = find(ranked, "nhi-b");
        var c = find(ranked, "nhi-c");

        assertNull(a.priorityRank());
        assertEquals(1, a.bestPossibleRank());
        assertEquals(2, a.worstPossibleRank());
        assertEquals("RANK_UNCERTAIN", a.rankingStatus());

        assertNull(b.priorityRank());
        assertEquals(1, b.bestPossibleRank());
        assertEquals(2, b.worstPossibleRank());
        assertEquals("RANK_UNCERTAIN", b.rankingStatus());

        assertEquals(3, c.priorityRank());
        assertEquals(3, c.bestPossibleRank());
        assertEquals(3, c.worstPossibleRank());
        assertEquals("RANK_STABLE_ON_REPORTED_SCORES", c.rankingStatus());
    }

    @Test
    void equalScoresShareCompetitionRank() {
        var ranked = engine.rank(List.of(
                assessment("nhi-b", 90.0, 90.0, 90.0,
                        List.of(), 100.0),
                assessment("nhi-a", 90.0, 90.0, 90.0,
                        List.of(), 100.0),
                assessment("nhi-c", 70.0, 70.0, 70.0,
                        List.of(), 100.0)
        ));

        var a = find(ranked, "nhi-a");
        var b = find(ranked, "nhi-b");
        var c = find(ranked, "nhi-c");

        assertEquals(1, a.priorityRank());
        assertEquals(1, b.priorityRank());
        assertEquals(3, c.priorityRank());
    }

    @Test
    void incompleteEvidenceCanStillHaveStableRank() {
        var ranked = engine.rank(List.of(
                assessment("nhi-high", 95.0, 95.0, 95.0,
                        List.of(), 100.0),
                assessment("nhi-middle", null, 62.0, 68.0,
                        List.of("B_BEHAVIOR"), 87.5),
                assessment("nhi-low", 20.0, 20.0, 20.0,
                        List.of(), 100.0)
        ));

        var middle = find(ranked, "nhi-middle");

        assertEquals(2, middle.bestPossibleRank());
        assertEquals(2, middle.worstPossibleRank());
        assertEquals(2, middle.priorityRank());
        assertEquals(
                "RANK_STABLE_ON_REPORTED_SCORES",
                middle.rankingStatus()
        );
    }


    @Test
    void equalScoresUseIdForDisplayOrderButShareCompetitionRank() {
        var ranked = engine.rank(List.of(
                assessment("nhi-b", 50.0, 50.0, 50.0, List.of(), 100.0),
                assessment("nhi-a", 50.0, 50.0, 50.0, List.of(), 100.0)
        ));

        assertEquals("nhi-a", ranked.get(0).assessment().nhiId());
        assertEquals(1, ranked.get(0).priorityRank());

        assertEquals("nhi-b", ranked.get(1).assessment().nhiId());
        assertEquals(1, ranked.get(1).priorityRank());
    }


    @Test
    void emptyBatchReturnsEmptyList() {
        assertTrue(engine.rank(List.of()).isEmpty());
    }

    @Test
    void rejectsDuplicateNhiIds() {
        assertThrows(
                IllegalArgumentException.class,
                () -> engine.rank(List.of(
                        assessment("same-id", 50.0, 50.0, 50.0,
                                List.of(), 100.0),
                        assessment("same-id", 60.0, 60.0, 60.0,
                                List.of(), 100.0)
                ))
        );
    }

    @Test
    void rejectsInvalidRiskBounds() {
        assertThrows(
                IllegalArgumentException.class,
                () -> engine.rank(List.of(
                        assessment("nhi-invalid", null, 80.0, 40.0,
                                List.of("B_BEHAVIOR"), 87.5)
                ))
        );
    }

    private RankedRiskAssessmentV2 find(
            List<com.nhi.experiment.model.RankedRiskAssessmentV2> ranked,
            String nhiId
    ) {
        return ranked.stream()
                .filter(item -> item.assessment().nhiId().equals(nhiId))
                .findFirst()
                .orElseThrow();
    }

    /**
     * Builds a minimal assessment fixture for ranking tests.
     * Decision labels are supplied as representative values; the ranking
     * engine operates on the risk bounds and ID, not on these labels.
     */
    private RiskAssessmentV2 assessment(
            String nhiId,
            Double riskScore,
            double riskMin,
            double riskMax,
            List<String> missing,
            double confidence
    ) {
        boolean incomplete = !missing.isEmpty() || confidence < 100.0;
        boolean uncertain = riskScore == null;

        return new RiskAssessmentV2(
                nhiId,
                null,
                null,
                null,
                riskScore,
                riskMin,
                riskMax,
                missing,
                confidence,
                null,
                uncertain ? "Investigate – Risk Uncertain" : "Test",
                uncertain ? "RISK_UNCERTAIN" : "TEST",
                riskMax >= 80.0 ? "CRITICAL" : "TEST",
                incomplete,
                uncertain,
                "Ranking test fixture"
        );
    }
}