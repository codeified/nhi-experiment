
package com.nhi.experiment.engine;

import com.nhi.experiment.model.RiskCalculationV2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DecisionEngineV2Test {

    private final DecisionEngineV2 engine = new DecisionEngineV2();

    /**
     * Checks the category boundaries used by the project.
     */
    @ParameterizedTest
    @CsvSource({
            "0.0, LOW",
            "19.99, LOW",
            "20.0, MONITOR_REMEDIATE",
            "39.99, MONITOR_REMEDIATE",
            "40.0, INVESTIGATE",
            "59.99, INVESTIGATE",
            "60.0, HIGH",
            "79.99, HIGH",
            "80.0, CRITICAL",
            "100.0, CRITICAL"
    })
    void classifiesCompleteRisk(
            double score,
            String expectedCategory
    ) {
        var result = engine.decide(
                calculation(score, score, true),
                100.0
        );

        assertEquals(expectedCategory, result.decisionCategory());
    }

    /**
     * A score range spanning the Investigate/High threshold must
     * not be presented as a certain category.
     */
    @Test
    void crossingThresholdProducesRiskUncertainDecision() {
        var result = engine.decide(
                calculation(59.0, 61.0, false),
                87.5
        );

        assertEquals("RISK_UNCERTAIN", result.decisionCategory());
        assertEquals("Investigate – Risk Uncertain", result.decision());
        assertEquals("HIGH", result.priorityCategory());
        assertTrue(result.requiresInvestigation());
    }

    /**
     * High confidence does not make the decision certain if the bounds
     * cross categories.
     */
    @Test
    void confidenceAtThresholdDoesNotOverrideRiskUncertainty() {
        var result = engine.decide(
                calculation(79.0, 81.0, false),
                75.0
        );

        assertEquals("RISK_UNCERTAIN", result.decisionCategory());
    }

    /**
     * If the whole interval remains in one category, report that category,
     * while retaining the incomplete-evidence flag.
     */
    @Test
    void intervalWithinOneCategoryRetainsThatCategory() {
        var result = engine.decide(
                calculation(61.0, 79.0, false),
                87.5
        );

        assertEquals("HIGH", result.decisionCategory());
        assertEquals("High", result.decision());
        assertTrue(result.incompleteEvidence());
    }

    /**
     * The existing below-75% confidence rule takes precedence.
     */
    @Test
    void lowConfidenceRequiresInvestigation() {
        var result = engine.decide(
                calculation(10.0, 30.0, false),
                62.5
        );

        assertEquals("LOW_CONFIDENCE", result.decisionCategory());
        assertEquals("Investigate – Low Confidence", result.decision());
        assertTrue(result.requiresInvestigation());
    }

    private RiskCalculationV2 calculation(
            double min,
            double max,
            boolean complete
    ) {
        return new RiskCalculationV2(
                complete ? min : null,
                complete ? min : null,
                complete ? min : null,
                complete && min == max ? min : null,
                min,
                max,
                complete ? List.of() : List.of("B_BEHAVIOR")
        );
    }
}
