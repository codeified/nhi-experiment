package com.nhi.experiment.service;

import com.nhi.experiment.model.Evidence;
import com.nhi.experiment.model.EvidenceQuality;
import com.nhi.experiment.model.RiskAssessmentV2;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AssessmentServiceV2Test {

    private final AssessmentServiceV2 service =
            new AssessmentServiceV2();

    @Test
    void zeroBehaviorScoreIsValidAndCanStillBeHighRisk() {
        Evidence evidence = completeEvidence();
        evidence.setBehavior(0.0);

        RiskAssessmentV2 result =
                service.assess("nhi-test-001", evidence);

        assertEquals(28.75, result.threatScore(), 0.01);
        assertEquals(84.38, result.exposureScore(), 0.01);
        assertEquals(92.00, result.impactScore(), 0.01);

        assertEquals(64.03, result.riskScore(), 0.01);
        assertEquals(64.03, result.riskMin(), 0.01);
        assertEquals(64.03, result.riskMax(), 0.01);

        assertEquals(100.0, result.confidence(), 0.01);
        assertEquals("HIGH", result.decisionCategory());
        assertEquals("High", result.decision());
        assertTrue(result.missingDimensions().isEmpty());
        assertFalse(result.incompleteEvidence());
    }

    @Test
    void missingBehaviorProducesRiskUncertainDecision() {
        Evidence evidence = completeEvidence();
        evidence.setBehavior(null);
        evidence.setBehaviorQuality(EvidenceQuality.MISSING);

        RiskAssessmentV2 result =
                service.assess("nhi-test-002", evidence);

        assertNull(result.riskScore());
        assertEquals(64.03, result.riskMin(), 0.01);
        assertEquals(84.03, result.riskMax(), 0.01);

        assertEquals(87.5, result.confidence(), 0.01);
        assertEquals("RISK_UNCERTAIN", result.decisionCategory());
        assertEquals(
                "Investigate – Risk Uncertain",
                result.decision()
        );

        // The upper bound reaches Critical, so the provisional priority
        // category must not hide the possible Critical risk.
        assertEquals("CRITICAL", result.priorityCategory());
        assertTrue(result.requiresInvestigation());
        assertTrue(result.incompleteEvidence());
    }

    private Evidence completeEvidence() {
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