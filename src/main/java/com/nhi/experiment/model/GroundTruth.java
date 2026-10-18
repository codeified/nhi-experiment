package com.nhi.experiment.model;

public class GroundTruth {

    private String nhiId;

    private Double trueRiskScore;
    private Integer truePriorityRank;

    private String trueDecisionCategory;

    public GroundTruth() {
    }

    public GroundTruth(
            String nhiId,
            Double trueRiskScore,
            Integer truePriorityRank,
            String trueDecisionCategory) {

        this.nhiId = nhiId;
        this.trueRiskScore = trueRiskScore;
        this.truePriorityRank = truePriorityRank;
        this.trueDecisionCategory = trueDecisionCategory;
    }

    public String getNhiId() {
        return nhiId;
    }

    public void setNhiId(String nhiId) {
        this.nhiId = nhiId;
    }

    public Double getTrueRiskScore() {
        return trueRiskScore;
    }

    public void setTrueRiskScore(Double trueRiskScore) {
        this.trueRiskScore = trueRiskScore;
    }

    public Integer getTruePriorityRank() {
        return truePriorityRank;
    }

    public void setTruePriorityRank(Integer truePriorityRank) {
        this.truePriorityRank = truePriorityRank;
    }

    public String getTrueDecisionCategory() {
        return trueDecisionCategory;
    }

    public void setTrueDecisionCategory(String trueDecisionCategory) {
        this.trueDecisionCategory = trueDecisionCategory;
    }
}