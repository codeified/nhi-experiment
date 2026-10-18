package com.nhi.experiment.engine;

import com.nhi.experiment.model.Evidence;
import com.nhi.experiment.model.EvidenceQuality;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EvidenceCompletenessEngineTest {

    private final EvidenceCompletenessEngine evidenceCompletenessEngine =
            new EvidenceCompletenessEngine();
    private final RiskCalculatorV2Test calculatorTest = new RiskCalculatorV2Test();
    @Test
    void completeEvidenceShouldProduce100PercentConfidence() {

        Evidence evidence = createEvidence(
                EvidenceQuality.COMPLETE,
                EvidenceQuality.COMPLETE,
                EvidenceQuality.COMPLETE,
                EvidenceQuality.COMPLETE,
                EvidenceQuality.COMPLETE,
                EvidenceQuality.COMPLETE,
                EvidenceQuality.COMPLETE,
                EvidenceQuality.COMPLETE
        );

        assertEquals(
                100.0,
                evidenceCompletenessEngine.calculate(evidence)
        );
    }

    @Test
    void sixCompleteEvidenceDimensionsShouldProduce75PercentConfidence() {

        Evidence evidence = createEvidence(
                EvidenceQuality.COMPLETE,
                EvidenceQuality.COMPLETE,
                EvidenceQuality.COMPLETE,
                EvidenceQuality.COMPLETE,
                EvidenceQuality.COMPLETE,
                EvidenceQuality.COMPLETE,
                EvidenceQuality.MISSING,
                EvidenceQuality.MISSING
        );

        assertEquals(
                75.0,
                evidenceCompletenessEngine.calculate(evidence)
        );

        assertTrue(
                evidenceCompletenessEngine.isSufficient(
                        evidenceCompletenessEngine.calculate(evidence)
                )
        );
    }

    @Test
    void fiveCompleteEvidenceDimensionsShouldBeBelowThreshold() {

        Evidence evidence = createEvidence(
                EvidenceQuality.COMPLETE,
                EvidenceQuality.COMPLETE,
                EvidenceQuality.COMPLETE,
                EvidenceQuality.COMPLETE,
                EvidenceQuality.COMPLETE,
                EvidenceQuality.MISSING,
                EvidenceQuality.MISSING,
                EvidenceQuality.MISSING
        );

        double confidence =
                evidenceCompletenessEngine.calculate(evidence);

        assertEquals(62.5, confidence);
        assertFalse(
                evidenceCompletenessEngine.isSufficient(confidence)
        );
    }

    @Test
    void partialEvidenceShouldProduce50PercentContribution() {

        Evidence evidence = createEvidence(
                EvidenceQuality.PARTIAL,
                EvidenceQuality.MISSING,
                EvidenceQuality.MISSING,
                EvidenceQuality.MISSING,
                EvidenceQuality.MISSING,
                EvidenceQuality.MISSING,
                EvidenceQuality.MISSING,
                EvidenceQuality.MISSING
        );

        assertEquals(
                6.25,
                evidenceCompletenessEngine.calculate(evidence)
        );
    }

    @Test
    void onePartialDimensionProduces9375PercentConfidence() {
        Evidence evidence = calculatorTest.completeEvidence();
        evidence.setBehaviorQuality(EvidenceQuality.PARTIAL);

        EvidenceCompletenessEngine engine = new EvidenceCompletenessEngine();

        assertEquals(93.75, engine.calculate(evidence), 0.01);
    }

    private Evidence createEvidence(
            EvidenceQuality privilege,
            EvidenceQuality authentication,
            EvidenceQuality behavior,
            EvidenceQuality sensitiveAccess,
            EvidenceQuality observedExposure,
            EvidenceQuality misconfiguration,
            EvidenceQuality resourceCriticality,
            EvidenceQuality assetSensitivity) {

        Evidence evidence = new Evidence();

        evidence.setPrivilegeQuality(privilege);
        evidence.setAuthenticationQuality(authentication);
        evidence.setBehaviorQuality(behavior);

        evidence.setSensitiveAccessQuality(sensitiveAccess);
        evidence.setObservedExposureQuality(observedExposure);
        evidence.setMisconfigurationQuality(misconfiguration);

        evidence.setResourceCriticalityQuality(resourceCriticality);
        evidence.setAssetSensitivityQuality(assetSensitivity);

        return evidence;
    }
}