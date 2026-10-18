
package com.nhi.experiment.experiment;

import com.nhi.experiment.engine.RiskCalculatorV2;
import com.nhi.experiment.model.RiskCalculationV2;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BaselineCalculatorIntegrationTest {

    private final RiskCalculatorV2 calculator = new RiskCalculatorV2();

    @Test
    void allCompleteBaselineScenariosShouldMatchIndependentRiskFormula() {
        for (BaselineScenario scenario : BaselineScenarios.all()) {
            RiskCalculationV2 actual =
                    calculator.calculate(scenario.toEvidence());

            double expected = round(referenceRisk(scenario));

            assertNotNull(
                    actual.riskScore(),
                    scenario.id() + " should have a point score with complete evidence"
            );

            assertEquals(
                    expected,
                    actual.riskScore(),
                    0.001,
                    "Risk score mismatch for " + scenario.id()
            );

            assertTrue(
                    actual.missingDimensions().isEmpty(),
                    scenario.id() + " should have no missing dimensions"
            );
        }
    }

    /**
     * Independent implementation of the agreed v2 scoring formula.
     * Expected severity labels are not inputs to this calculation.
     */
    private double referenceRisk(BaselineScenario s) {
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

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
