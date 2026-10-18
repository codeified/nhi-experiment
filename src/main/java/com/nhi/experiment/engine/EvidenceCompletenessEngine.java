package com.nhi.experiment.engine;

import com.nhi.experiment.model.Evidence;
import com.nhi.experiment.model.EvidenceQuality;

public class EvidenceCompletenessEngine {

    private static final double CONFIDENCE_THRESHOLD = 0.0;

    public double calculate(Evidence evidence) {

        double total =
                quality(evidence.getPrivilegeQuality())
                        + quality(evidence.getAuthenticationQuality())
                        + quality(evidence.getBehaviorQuality())
                        + quality(evidence.getSensitiveAccessQuality())
                        + quality(evidence.getObservedExposureQuality())
                        + quality(evidence.getMisconfigurationQuality())
                        + quality(evidence.getResourceCriticalityQuality())
                        + quality(evidence.getAssetSensitivityQuality());

        return round((total / 8.0) * 100.0);
    }

    public boolean isSufficient(double confidence) {
        return confidence >= CONFIDENCE_THRESHOLD;
    }

    private double quality(EvidenceQuality quality) {

        if (quality == null) {
            return 0.0;
        }

        return quality.getWeight();
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}