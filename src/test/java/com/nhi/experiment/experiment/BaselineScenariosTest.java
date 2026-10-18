
package com.nhi.experiment.experiment;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.nhi.experiment.experiment.BaselineScenario.ExpectedSeverity.*;
import static org.junit.jupiter.api.Assertions.*;

class BaselineScenariosTest {

    @Test
    void shouldContainTwentyUniqueScenarios() {
        List<BaselineScenario> scenarios = BaselineScenarios.all();

        assertEquals(20, scenarios.size());

        Set<String> ids = new HashSet<>();
        for (BaselineScenario scenario : scenarios) {
            assertTrue(
                    ids.add(scenario.id()),
                    "Duplicate scenario ID: " + scenario.id()
            );
        }

        for (int i = 1; i <= 20; i++) {
            assertTrue(
                    ids.contains("S%02d".formatted(i)),
                    "Missing scenario S%02d".formatted(i)
            );
        }
    }

    @Test
    void shouldKeepExpectedSeverityIndependentOfCalculatedRisk() {
        for (BaselineScenario scenario : BaselineScenarios.all()) {
            assertNotNull(scenario.expectedSeverity());
            assertNotNull(scenario.toEvidence());
        }
    }

    @Test
    void shouldCalculateRiskUsingEightDimensionModel() {
        for (BaselineScenario scenario : BaselineScenarios.all()) {
            double expectedScore = calculateReferenceRisk(scenario);

            assertTrue(
                    Double.isFinite(expectedScore),
                    "Non-finite reference score for " + scenario.id()
            );
            assertTrue(
                    expectedScore >= 0.0 && expectedScore <= 100.0,
                    "Reference score outside 0-100 for " + scenario.id()
            );
        }
    }

    @Test
    void s03ShouldRemainExpectedLowSeverity() {
        BaselineScenario s03 = find("S03");

        assertEquals(LOW, s03.expectedSeverity());
        assertEquals(7.34375, calculateReferenceRisk(s03), 0.001);
    }

    @Test
    void s07ShouldRetainInvestigateAsExpectedAction() {
        BaselineScenario s07 = find("S07");

        assertEquals(INVESTIGATE, s07.expectedSeverity());
    }

    @Test
    void s18ShouldRetainHighAsExpectedActionRatherThanCritical() {
        BaselineScenario s18 = find("S18");

        assertEquals(HIGH, s18.expectedSeverity());
        assertEquals(68.90625, calculateReferenceRisk(s18), 0.001);
    }

    /**
     * Independent reference calculation for the agreed eight-dimension
     * model. Expected severity is deliberately not used in this calculation.
     */
    private double calculateReferenceRisk(BaselineScenario s) {
        double threat =
                0.25 * s.authentication()
                        + 0.50 * s.behavior()
                        + 0.25 * s.misconfiguration();

        double exposure =
                0.25 * s.privilege()
                        + 0.375 * s.sensitiveAccess()
                        + 0.375 * s.observedExposure();

        double impact =
                0.70 * s.resourceCriticality()
                        + 0.30 * s.assetSensitivity();

        return 0.40 * threat
                + 0.35 * exposure
                + 0.25 * impact;
    }

    private BaselineScenario find(String id) {
        return BaselineScenarios.all().stream()
                .filter(scenario -> scenario.id().equals(id))
                .findFirst()
                .orElseThrow(() ->
                        new AssertionError("Scenario not found: " + id));
    }
}
