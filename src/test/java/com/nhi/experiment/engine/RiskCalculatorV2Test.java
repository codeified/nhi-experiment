
package com.nhi.experiment.engine;

import com.nhi.experiment.model.Evidence;
import com.nhi.experiment.model.EvidenceQuality;
import com.nhi.experiment.model.RiskCalculationV2;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RiskCalculatorV2Test {

    private final RiskCalculatorV2 calculator = new RiskCalculatorV2();

    private static final Map<String, Double> WEIGHTS =
            createWeights();

    private static Map<String, Double> createWeights() {
        Map<String, Double> weights = new LinkedHashMap<>();
        weights.put("A", 0.10000);
        weights.put("B", 0.20000);
        weights.put("M", 0.10000);
        weights.put("P", 0.08750);
        weights.put("S", 0.13125);
        weights.put("O", 0.13125);
        weights.put("R", 0.17500);
        weights.put("AS", 0.07500);
        return weights;
    }

    /**
     * Verifies the documented v2.0 formulas against a known complete record.
     */
    @Test
    void calculatesCompleteEvidence() {
        RiskCalculationV2 result = calculator.calculate(completeEvidence());

        assertTrue(result.isComplete());
        assertEquals(73.75, result.threatScore(), 0.01);
        assertEquals(84.38, result.exposureScore(), 0.01);
        assertEquals(92.00, result.impactScore(), 0.01);
        assertEquals(82.03, result.riskScore(), 0.01);
        assertEquals(result.riskScore(), result.riskMin(), 0.01);
        assertEquals(result.riskScore(), result.riskMax(), 0.01);
    }

    /**
     * Every dimension must widen the bounds by its own original global
     * weight multiplied by the 0-100 score range.
     */
    @Test
    void eachMissingDimensionUsesItsOriginalWeight() {
        for (Map.Entry<String, Double> entry : WEIGHTS.entrySet()) {
            Evidence evidence = completeEvidence();
            setDimension(evidence, entry.getKey(),
                    null, EvidenceQuality.MISSING);

            RiskCalculationV2 result = calculator.calculate(evidence);

            assertNull(result.riskScore(), entry.getKey());
            assertFalse(result.isComplete(), entry.getKey());
            assertEquals(
                    100.0 * entry.getValue(),
                    result.riskMax() - result.riskMin(),
                    0.02,
                    entry.getKey() + " range width"
            );
            assertEquals(1, result.missingDimensions().size());
        }
    }

    /**
     * A measured zero must remain a valid score for every dimension.
     * It must not create a risk range or mark evidence as missing.
     */
    @Test
    void everyDimensionCanHaveAValidMeasuredZero() {
        Evidence baselineEvidence = completeEvidence();
        double baselineRisk =
                calculator.calculate(baselineEvidence).riskScore();

        Map<String, Double> originalValues = originalValues();

        for (Map.Entry<String, Double> entry : originalValues.entrySet()) {
            Evidence evidence = completeEvidence();
            setDimension(evidence, entry.getKey(),
                    0.0, EvidenceQuality.COMPLETE);

            RiskCalculationV2 result = calculator.calculate(evidence);

            assertTrue(result.isComplete(), entry.getKey());
            assertNotNull(result.riskScore(), entry.getKey());
            assertEquals(
                    result.riskMin(),
                    result.riskMax(),
                    0.01,
                    entry.getKey() + " bounds"
            );

            double expected = baselineRisk
                    - entry.getValue() * WEIGHTS.get(entry.getKey());

            assertEquals(
                    expected,
                    result.riskScore(),
                    0.02,
                    entry.getKey() + " zero contribution"
            );
        }
    }

    /**
     * Missing Behavior and Resource Criticality create a combined
     * range equal to their combined global weights.
     */
    @Test
    void multipleMissingDimensionsCombineTheirBounds() {
        Evidence evidence = completeEvidence();
        setDimension(evidence, "B", null, EvidenceQuality.MISSING);
        setDimension(evidence, "R", null, EvidenceQuality.MISSING);

        RiskCalculationV2 result = calculator.calculate(evidence);

        assertNull(result.riskScore());
        assertEquals(2, result.missingDimensions().size());
        assertEquals(
                37.5,
                result.riskMax() - result.riskMin(),
                0.02
        );
    }

    /**
     * If every dimension is missing, no point score can be justified.
     * The full mathematical range remains 0-100.
     */
    @Test
    void allEvidenceMissingProducesFullRange() {
        Evidence evidence = completeEvidence();

        for (String dimension : WEIGHTS.keySet()) {
            setDimension(evidence, dimension,
                    null, EvidenceQuality.MISSING);
        }

        RiskCalculationV2 result = calculator.calculate(evidence);

        assertFalse(result.isComplete());
        assertNull(result.threatScore());
        assertNull(result.exposureScore());
        assertNull(result.impactScore());
        assertNull(result.riskScore());

        assertEquals(0.0, result.riskMin(), 0.01);
        assertEquals(100.0, result.riskMax(), 0.01);
        assertEquals(8, result.missingDimensions().size());
    }

    /**
     * PARTIAL quality does not silently alter the original risk weights.
     * Confidence is evaluated separately by ConfidenceEngine.
     */
    @Test
    void partialEvidenceRetainsItsObservedRiskContribution() {
        Evidence complete = completeEvidence();
        double baselineRisk = calculator.calculate(complete).riskScore();

        Evidence partial = completeEvidence();
        partial.setBehaviorQuality(EvidenceQuality.PARTIAL);

        RiskCalculationV2 result = calculator.calculate(partial);

        assertNotNull(result.riskScore());
        assertEquals(baselineRisk, result.riskScore(), 0.01);
        assertEquals(result.riskMin(), result.riskMax(), 0.01);
    }

    /**
     * Inconsistent metadata is rejected rather than interpreted as zero.
     */
    @Test
    void rejectsMissingValueMarkedComplete() {
        Evidence evidence = completeEvidence();
        evidence.setBehavior(null);
        evidence.setBehaviorQuality(EvidenceQuality.COMPLETE);

        assertThrows(
                IllegalArgumentException.class,
                () -> calculator.calculate(evidence)
        );
    }

    @Test
    void rejectsNumericValueMarkedMissing() {
        Evidence evidence = completeEvidence();
        evidence.setBehaviorQuality(EvidenceQuality.MISSING);

        assertThrows(
                IllegalArgumentException.class,
                () -> calculator.calculate(evidence)
        );
    }

    @Test
    void rejectsOutOfRangeAndNonFiniteValues() {
        for (double invalid : new double[]{-1.0, 101.0,
                Double.NaN, Double.POSITIVE_INFINITY}) {
            Evidence evidence = completeEvidence();
            evidence.setBehavior(invalid);

            assertThrows(
                    IllegalArgumentException.class,
                    () -> calculator.calculate(evidence)
            );
        }
    }

    private Map<String, Double> originalValues() {
        Map<String, Double> values = new LinkedHashMap<>();
        values.put("A", 75.0);
        values.put("B", 90.0);
        values.put("M", 40.0);
        values.put("P", 90.0);
        values.put("S", 95.0);
        values.put("O", 70.0);
        values.put("R", 95.0);
        values.put("AS", 85.0);
        return values;
    }

    /**
     * Sets the score and quality for a single named evidence dimension.
     */
    private void setDimension(
            Evidence evidence,
            String dimension,
            Double value,
            EvidenceQuality quality
    ) {
        switch (dimension) {
            case "A" -> {
                evidence.setAuthentication(value);
                evidence.setAuthenticationQuality(quality);
            }
            case "B" -> {
                evidence.setBehavior(value);
                evidence.setBehaviorQuality(quality);
            }
            case "M" -> {
                evidence.setMisconfiguration(value);
                evidence.setMisconfigurationQuality(quality);
            }
            case "P" -> {
                evidence.setPrivilegeLevel(value);
                evidence.setPrivilegeQuality(quality);
            }
            case "S" -> {
                evidence.setSensitiveAccess(value);
                evidence.setSensitiveAccessQuality(quality);
            }
            case "O" -> {
                evidence.setObservedExposure(value);
                evidence.setObservedExposureQuality(quality);
            }
            case "R" -> {
                evidence.setResourceCriticality(value);
                evidence.setResourceCriticalityQuality(quality);
            }
            case "AS" -> {
                evidence.setAssetSensitivity(value);
                evidence.setAssetSensitivityQuality(quality);
            }
            default -> throw new IllegalArgumentException(
                    "Unknown dimension: " + dimension
            );
        }
    }

    public Evidence completeEvidence() {
        Evidence e = new Evidence();

        e.setPrivilegeLevel(90.0);
        e.setAuthentication(75.0);
        e.setBehavior(90.0);
        e.setSensitiveAccess(95.0);
        e.setObservedExposure(70.0);
        e.setMisconfiguration(40.0);
        e.setResourceCriticality(95.0);
        e.setAssetSensitivity(85.0);

        e.setPrivilegeQuality(EvidenceQuality.COMPLETE);
        e.setAuthenticationQuality(EvidenceQuality.COMPLETE);
        e.setBehaviorQuality(EvidenceQuality.COMPLETE);
        e.setSensitiveAccessQuality(EvidenceQuality.COMPLETE);
        e.setObservedExposureQuality(EvidenceQuality.COMPLETE);
        e.setMisconfigurationQuality(EvidenceQuality.COMPLETE);
        e.setResourceCriticalityQuality(EvidenceQuality.COMPLETE);
        e.setAssetSensitivityQuality(EvidenceQuality.COMPLETE);

        return e;
    }
}
