package com.nhi.experiment.experiment;

import com.nhi.experiment.model.Evidence;
import com.nhi.experiment.model.EvidenceQuality;

/**
 * Complete-evidence baseline for one NHI scenario.
 *
 * Expected severity is an independent evaluation label and is never
 * copied into the Evidence object used by the risk calculator.
 */
public record BaselineScenario(
        String id,
        String description,
        double authentication,
        double behavior,
        double misconfiguration,
        double privilege,
        double sensitiveAccess,
        double observedExposure,
        double resourceCriticality,
        double assetSensitivity,
        ExpectedSeverity expectedSeverity
) {
    public enum ExpectedSeverity {
        LOW,
        MONITOR_REMEDIATE,
        INVESTIGATE,
        HIGH,
        CRITICAL
    }

    public BaselineScenario {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Scenario ID must not be blank");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Description must not be blank");
        }
        if (expectedSeverity == null) {
            throw new IllegalArgumentException("Expected severity must not be null");
        }

        double[] values = {
                authentication, behavior, misconfiguration, privilege,
                sensitiveAccess, observedExposure,
                resourceCriticality, assetSensitivity
        };

        for (double value : values) {
            if (!Double.isFinite(value) || value < 0 || value > 100) {
                throw new IllegalArgumentException(
                        "Evidence values must be finite and between 0 and 100");
            }
        }
    }

    /**
     * Converts this baseline to the existing eight-dimension evidence model.
     * All dimensions are marked COMPLETE because this represents the
     * complete-evidence baseline, not a perturbed experiment.
     */
    public Evidence toEvidence() {
        Evidence evidence = new Evidence();

        evidence.setAuthentication(authentication);
        evidence.setAuthenticationQuality(EvidenceQuality.COMPLETE);

        evidence.setBehavior(behavior);
        evidence.setBehaviorQuality(EvidenceQuality.COMPLETE);

        evidence.setMisconfiguration(misconfiguration);
        evidence.setMisconfigurationQuality(EvidenceQuality.COMPLETE);

        evidence.setPrivilegeLevel(privilege);
        evidence.setPrivilegeQuality(EvidenceQuality.COMPLETE);

        evidence.setSensitiveAccess(sensitiveAccess);
        evidence.setSensitiveAccessQuality(EvidenceQuality.COMPLETE);

        evidence.setObservedExposure(observedExposure);
        evidence.setObservedExposureQuality(EvidenceQuality.COMPLETE);

        evidence.setResourceCriticality(resourceCriticality);
        evidence.setResourceCriticalityQuality(EvidenceQuality.COMPLETE);

        evidence.setAssetSensitivity(assetSensitivity);
        evidence.setAssetSensitivityQuality(EvidenceQuality.COMPLETE);

        return evidence;
    }
}