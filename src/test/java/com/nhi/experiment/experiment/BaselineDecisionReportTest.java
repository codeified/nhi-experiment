
package com.nhi.experiment.experiment;

import com.nhi.experiment.model.RiskAssessmentV2;
import com.nhi.experiment.service.AssessmentServiceV2;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BaselineDecisionReportTest {

    private final AssessmentServiceV2 assessmentService =
            new AssessmentServiceV2();

    @Test
    void reportCompleteEvidenceBaselineDecisions() {
        System.out.println();
        System.out.println("=== BASELINE DECISION REPORT ===");
        System.out.printf(
                "%-5s %-22s %-22s %-10s %-10s%n",
                "ID", "Expected action", "Model category",
                "Risk", "Confidence"
        );

        int mismatches = 0;

        for (BaselineScenario scenario : BaselineScenarios.all()) {
            RiskAssessmentV2 assessment =
                    assessmentService.assess(
                            scenario.id(),
                            scenario.toEvidence()
                    );

            assertNotNull(
                    assessment.riskScore(),
                    scenario.id() + " should have a score with complete evidence"
            );

            String expected = scenario.expectedSeverity().name();
            String actual = assessment.decisionCategory().toString();

            boolean matches = expected.equals(actual);

            if (!matches) {
                mismatches++;
            }

            System.out.printf(
                    "%-5s %-22s %-22s %-10.2f %-9.1f%%%s%n",
                    scenario.id(),
                    expected,
                    actual,
                    assessment.riskScore(),
                    assessment.confidence(),
                    matches ? "" : "  <-- MISMATCH"
            );
        }

        System.out.println("-----------------------------------------------");
        System.out.println("Total scenarios: 20");
        System.out.println("Decision mismatches: " + mismatches);
        System.out.println(
                "Mismatches are evaluation findings, not automatic test failures."
        );
    }
}
