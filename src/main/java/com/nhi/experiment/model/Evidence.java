package com.nhi.experiment.model;

public class Evidence {

    /*
     * Threat
     */
    private Double privilege;
    private Double authentication;
    private Double behavior;

    /*
     * Exposure
     */
    private Double sensitiveAccess;
    private Double observedExposure;
    private Double misconfiguration;

    /*
     * Impact
     */
    private Double resourceCriticality;
    private Double assetSensitivity;

    private EvidenceQuality privilegeQuality;
    private EvidenceQuality authenticationQuality;
    private EvidenceQuality behaviorQuality;

    private EvidenceQuality sensitiveAccessQuality;
    private EvidenceQuality observedExposureQuality;
    private EvidenceQuality misconfigurationQuality;

    private EvidenceQuality resourceCriticalityQuality;
    private EvidenceQuality assetSensitivityQuality;

    public Evidence() {
    }

    public Double getPrivilegeLevel() {
        return privilege;
    }

    public void setPrivilegeLevel(Double privilege) {
        this.privilege = privilege;
    }

    public Double getAuthentication() {
        return authentication;
    }

    public void setAuthentication(Double authentication) {
        this.authentication = authentication;
    }

    public Double getBehavior() {
        return behavior;
    }

    public void setBehavior(Double behavior) {
        this.behavior = behavior;
    }

    public Double getSensitiveAccess() {
        return sensitiveAccess;
    }

    public void setSensitiveAccess(Double sensitiveAccess) {
        this.sensitiveAccess = sensitiveAccess;
    }

    public Double getObservedExposure() {
        return observedExposure;
    }

    public void setObservedExposure(Double observedExposure) {
        this.observedExposure = observedExposure;
    }

    public Double getMisconfiguration() {
        return misconfiguration;
    }

    public void setMisconfiguration(Double misconfiguration) {
        this.misconfiguration = misconfiguration;
    }

    public Double getResourceCriticality() {
        return resourceCriticality;
    }

    public void setResourceCriticality(Double resourceCriticality) {
        this.resourceCriticality = resourceCriticality;
    }

    public Double getAssetSensitivity() {
        return assetSensitivity;
    }

    public void setAssetSensitivity(Double assetSensitivity) {
        this.assetSensitivity = assetSensitivity;
    }

    public EvidenceQuality getPrivilegeQuality() {
        return privilegeQuality;
    }

    public void setPrivilegeQuality(EvidenceQuality privilegeQuality) {
        this.privilegeQuality = privilegeQuality;
    }

    public EvidenceQuality getAuthenticationQuality() {
        return authenticationQuality;
    }

    public void setAuthenticationQuality(EvidenceQuality authenticationQuality) {
        this.authenticationQuality = authenticationQuality;
    }

    public EvidenceQuality getBehaviorQuality() {
        return behaviorQuality;
    }

    public void setBehaviorQuality(EvidenceQuality behaviorQuality) {
        this.behaviorQuality = behaviorQuality;
    }

    public EvidenceQuality getSensitiveAccessQuality() {
        return sensitiveAccessQuality;
    }

    public void setSensitiveAccessQuality(EvidenceQuality sensitiveAccessQuality) {
        this.sensitiveAccessQuality = sensitiveAccessQuality;
    }

    public EvidenceQuality getObservedExposureQuality() {
        return observedExposureQuality;
    }

    public void setObservedExposureQuality(EvidenceQuality observedExposureQuality) {
        this.observedExposureQuality = observedExposureQuality;
    }

    public EvidenceQuality getMisconfigurationQuality() {
        return misconfigurationQuality;
    }

    public void setMisconfigurationQuality(EvidenceQuality misconfigurationQuality) {
        this.misconfigurationQuality = misconfigurationQuality;
    }

    public EvidenceQuality getResourceCriticalityQuality() {
        return resourceCriticalityQuality;
    }

    public void setResourceCriticalityQuality(EvidenceQuality resourceCriticalityQuality) {
        this.resourceCriticalityQuality = resourceCriticalityQuality;
    }

    public EvidenceQuality getAssetSensitivityQuality() {
        return assetSensitivityQuality;
    }

    public void setAssetSensitivityQuality(EvidenceQuality assetSensitivityQuality) {
        this.assetSensitivityQuality = assetSensitivityQuality;
    }
}