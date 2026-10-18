package com.nhi.experiment.model;

public class RiskAssessment {

    private String nhiId;

    private Double threatScore;
    private Double exposureScore;
    private Double impactScore;

    private Double riskScore;
    private Integer priorityRank;

    private Double confidence;

    private String decision;
    private String decisionCategory;

    public RiskAssessment() {
    }

    public String getNhiId() {
        return nhiId;
    }

    public void setNhiId(String nhiId) {
        this.nhiId = nhiId;
    }

    public Double getThreatScore() {
        return threatScore;
    }

    public void setThreatScore(Double threatScore) {
        this.threatScore = threatScore;
    }

    public Double getExposureScore() {
        return exposureScore;
    }

    public void setExposureScore(Double exposureScore) {
        this.exposureScore = exposureScore;
    }

    public Double getImpactScore() {
        return impactScore;
    }

    public void setImpactScore(Double impactScore) {
        this.impactScore = impactScore;
    }

    public Double getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Double riskScore) {
        this.riskScore = riskScore;
    }

    public Integer getPriorityRank() {
        return priorityRank;
    }

    public void setPriorityRank(Integer priorityRank) {
        this.priorityRank = priorityRank;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public String getDecisionCategory() {
        return decisionCategory;
    }

    public void setDecisionCategory(String decisionCategory) {
        this.decisionCategory = decisionCategory;
    }
}