package com.nhi.experiment.experiment;

import com.nhi.experiment.service.AssessmentServiceV2;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BaselineAssessmentIntegrationTest {

    @Test
    void s03ShouldProduceExpectedRiskScoreInV2Engine() {
        BaselineScenario s03 = BaselineScenarios.all().stream()
                .filter(s -> s.id().equals("S03"))
                .findFirst()
                .orElseThrow();

        var assessment = new AssessmentServiceV2()
                .assess(s03.id(), s03.toEvidence());

        // Expected score must be calculated from the actual v2.0 weights.
        double expectedScore =
                0.10 * s03.authentication()
                        + 0.20 * s03.behavior()
                        + 0.10 * s03.misconfiguration()
                        + 0.0875 * s03.privilege()
                        + 0.13125 * s03.sensitiveAccess()
                        + 0.13125 * s03.observedExposure()
                        + 0.175 * s03.resourceCriticality()
                        + 0.075 * s03.assetSensitivity();

        assertEquals(expectedScore, assessment.riskScore(), 0.01);
        assertEquals(100.0, assessment.confidence(), 0.01);
    }
}