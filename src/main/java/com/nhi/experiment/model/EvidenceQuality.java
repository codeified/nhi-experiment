package com.nhi.experiment.model;

public enum EvidenceQuality {

    COMPLETE(1.0),
    PARTIAL(0.5),
    MISSING(0.0);

    private final double weight;

    EvidenceQuality(double weight) {
        this.weight = weight;
    }

    public double getWeight() {
        return weight;
    }
}