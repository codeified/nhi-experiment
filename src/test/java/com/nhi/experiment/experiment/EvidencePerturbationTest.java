
package com.nhi.experiment.experiment;

import com.nhi.experiment.model.Evidence;
import com.nhi.experiment.model.EvidenceQuality;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EvidencePerturbationTest {

    private final EvidencePerturbation perturbation =
            new EvidencePerturbation();

    @Test
    void missingEvidenceShouldClearValueAndMarkMissing() {
        Evidence baseline = baseline();

        Evidence result = perturbation.apply(
                baseline,
                EvidencePerturbation.Dimension.PRIVILEGE,
                EvidencePerturbation.Type.MISSING);

        assertNull(result.getPrivilegeLevel());
        assertEquals(
                EvidenceQuality.MISSING,
                result.getPrivilegeQuality());

        // Original baseline must remain unchanged.
        assertEquals(90.0, baseline.getPrivilegeLevel());
    }

    @Test
    void degradedEvidenceShouldBecomePartial() {
        Evidence baseline = baseline();

        Evidence result = perturbation.apply(
                baseline,
                EvidencePerturbation.Dimension.PRIVILEGE,
                EvidencePerturbation.Type.DEGRADED);

        assertEquals(90.0, result.getPrivilegeLevel());
        assertEquals(
                EvidenceQuality.PARTIAL,
                result.getPrivilegeQuality());
    }

    @Test
    void incorrectEvidenceShouldBeInverted() {
        Evidence baseline = baseline();

        Evidence result = perturbation.apply(
                baseline,
                EvidencePerturbation.Dimension.PRIVILEGE,
                EvidencePerturbation.Type.INCORRECT);

        assertEquals(10.0, result.getPrivilegeLevel());
        assertEquals(
                EvidenceQuality.COMPLETE,
                result.getPrivilegeQuality());

        assertEquals(90.0, baseline.getPrivilegeLevel());
    }

    private Evidence baseline() {
        Evidence evidence = new Evidence();

        evidence.setPrivilegeLevel(90.0);
        evidence.setAuthentication(75.0);
        evidence.setBehavior(90.0);

        evidence.setSensitiveAccess(95.0);
        evidence.setObservedExposure(70.0);
        evidence.setMisconfiguration(40.0);

        evidence.setResourceCriticality(95.0);
        evidence.setAssetSensitivity(85.0);

        evidence.setPrivilegeQuality(EvidenceQuality.COMPLETE);
        evidence.setAuthenticationQuality(EvidenceQuality.COMPLETE);
        evidence.setBehaviorQuality(EvidenceQuality.COMPLETE);

        evidence.setSensitiveAccessQuality(EvidenceQuality.COMPLETE);
        evidence.setObservedExposureQuality(EvidenceQuality.COMPLETE);
        evidence.setMisconfigurationQuality(EvidenceQuality.COMPLETE);

        evidence.setResourceCriticalityQuality(EvidenceQuality.COMPLETE);
        evidence.setAssetSensitivityQuality(EvidenceQuality.COMPLETE);

        return evidence;
    }
}
