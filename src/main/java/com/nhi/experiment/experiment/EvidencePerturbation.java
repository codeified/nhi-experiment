
package com.nhi.experiment.experiment;

import com.nhi.experiment.model.Evidence;
import com.nhi.experiment.model.EvidenceQuality;

public class EvidencePerturbation {

    public enum Type {
        MISSING,
        DEGRADED,
        INCORRECT
    }

    public enum Dimension {
        PRIVILEGE,
        AUTHENTICATION,
        BEHAVIOR,
        SENSITIVE_ACCESS,
        OBSERVED_EXPOSURE,
        MISCONFIGURATION,
        RESOURCE_CRITICALITY,
        ASSET_SENSITIVITY
    }

    /**
     * Creates a copy so the baseline evidence remains unchanged.
     */
    public Evidence apply(
            Evidence baseline,
            Dimension dimension,
            Type type) {

        Evidence result = copyOf(baseline);

        switch (dimension) {
            case PRIVILEGE -> perturb(
                    result.getPrivilegeLevel(),
                    type,
                    result::setPrivilegeLevel,
                    result::setPrivilegeQuality);

            case AUTHENTICATION -> perturb(
                    result.getAuthentication(),
                    type,
                    result::setAuthentication,
                    result::setAuthenticationQuality);

            case BEHAVIOR -> perturb(
                    result.getBehavior(),
                    type,
                    result::setBehavior,
                    result::setBehaviorQuality);

            case SENSITIVE_ACCESS -> perturb(
                    result.getSensitiveAccess(),
                    type,
                    result::setSensitiveAccess,
                    result::setSensitiveAccessQuality);

            case OBSERVED_EXPOSURE -> perturb(
                    result.getObservedExposure(),
                    type,
                    result::setObservedExposure,
                    result::setObservedExposureQuality);

            case MISCONFIGURATION -> perturb(
                    result.getMisconfiguration(),
                    type,
                    result::setMisconfiguration,
                    result::setMisconfigurationQuality);

            case RESOURCE_CRITICALITY -> perturb(
                    result.getResourceCriticality(),
                    type,
                    result::setResourceCriticality,
                    result::setResourceCriticalityQuality);

            case ASSET_SENSITIVITY -> perturb(
                    result.getAssetSensitivity(),
                    type,
                    result::setAssetSensitivity,
                    result::setAssetSensitivityQuality);
        }

        return result;
    }

    private void perturb(
            Double value,
            Type type,
            java.util.function.Consumer<Double> valueSetter,
            java.util.function.Consumer<EvidenceQuality> qualitySetter) {

        switch (type) {
            case MISSING -> {
                valueSetter.accept(null);
                qualitySetter.accept(EvidenceQuality.MISSING);
            }

            case DEGRADED -> {
                if (value == null) {
                    throw new IllegalArgumentException(
                            "Cannot degrade missing evidence");
                }
                qualitySetter.accept(EvidenceQuality.PARTIAL);
            }

            case INCORRECT -> {
                if (value == null) {
                    throw new IllegalArgumentException(
                            "Cannot corrupt missing evidence");
                }

                // Deterministic perturbation: invert the score.
                valueSetter.accept(100.0 - value);
            }
        }
    }

    private Evidence copyOf(Evidence source) {
        Evidence copy = new Evidence();

        copy.setPrivilegeLevel(source.getPrivilegeLevel());
        copy.setAuthentication(source.getAuthentication());
        copy.setBehavior(source.getBehavior());

        copy.setSensitiveAccess(source.getSensitiveAccess());
        copy.setObservedExposure(source.getObservedExposure());
        copy.setMisconfiguration(source.getMisconfiguration());

        copy.setResourceCriticality(source.getResourceCriticality());
        copy.setAssetSensitivity(source.getAssetSensitivity());

        copy.setPrivilegeQuality(source.getPrivilegeQuality());
        copy.setAuthenticationQuality(source.getAuthenticationQuality());
        copy.setBehaviorQuality(source.getBehaviorQuality());

        copy.setSensitiveAccessQuality(source.getSensitiveAccessQuality());
        copy.setObservedExposureQuality(source.getObservedExposureQuality());
        copy.setMisconfigurationQuality(source.getMisconfigurationQuality());

        copy.setResourceCriticalityQuality(
                source.getResourceCriticalityQuality());
        copy.setAssetSensitivityQuality(
                source.getAssetSensitivityQuality());

        return copy;
    }
}
