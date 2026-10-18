package com.nhi.experiment.service;

import com.nhi.experiment.model.Evidence;
import com.nhi.experiment.model.EvidenceQuality;
import com.nhi.experiment.model.NhiIdentity;
import com.nhi.experiment.model.RiskAssessment;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AssessmentServiceTest {

    private final AssessmentService assessmentService =
            new AssessmentService();

    @Test
    void completeHighRiskNhiShouldProduceHighConfidenceAssessment() {

        NhiIdentity nhi = new NhiIdentity(
                "NHI-001",
                "svc-payment-prod",
                "SERVICE_ACCOUNT",
                "Payment Application",
                "PRODUCTION",
                "IAM-Team",
                "ACTIVE"
        );

        Evidence evidence = new Evidence();

        /*
         * Threat
         */
        evidence.setPrivilegeLevel(90.0);
        evidence.setAuthentication(75.0);
        evidence.setBehavior(90.0);

        /*
         * Exposure
         */
        evidence.setSensitiveAccess(95.0);
        evidence.setObservedExposure(70.0);
        evidence.setMisconfiguration(40.0);

        /*
         * Impact
         */
        evidence.setResourceCriticality(95.0);
        evidence.setAssetSensitivity(85.0);

        /*
         * Evidence quality
         */
        evidence.setPrivilegeQuality(EvidenceQuality.COMPLETE);
        evidence.setAuthenticationQuality(EvidenceQuality.COMPLETE);
        evidence.setBehaviorQuality(EvidenceQuality.COMPLETE);

        evidence.setSensitiveAccessQuality(EvidenceQuality.COMPLETE);
        evidence.setObservedExposureQuality(EvidenceQuality.COMPLETE);
        evidence.setMisconfigurationQuality(EvidenceQuality.COMPLETE);

        evidence.setResourceCriticalityQuality(EvidenceQuality.COMPLETE);
        evidence.setAssetSensitivityQuality(EvidenceQuality.COMPLETE);

        RiskAssessment result =
                assessmentService.assess(nhi, evidence);

        assertEquals("NHI-001", result.getNhiId());

        assertEquals(100.0, result.getConfidence());

        assertEquals(
                "CRITICAL",
                result.getDecisionCategory()
        );
    }
}