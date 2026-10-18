package com.nhi.experiment.service;

import com.nhi.experiment.engine.EvidenceCompletenessEngine;
import com.nhi.experiment.engine.DecisionEngine;
import com.nhi.experiment.engine.RiskEngine;
import com.nhi.experiment.model.Evidence;
import com.nhi.experiment.model.NhiIdentity;
import com.nhi.experiment.model.RiskAssessment;

public class AssessmentService {

    private final RiskEngine riskEngine;
    private final EvidenceCompletenessEngine evidenceCompletenessEngine;
    private final DecisionEngine decisionEngine;

    public AssessmentService() {
        this.riskEngine = new RiskEngine();
        this.evidenceCompletenessEngine = new EvidenceCompletenessEngine();
        this.decisionEngine = new DecisionEngine();
    }

    public RiskAssessment assess(
            NhiIdentity nhi,
            Evidence evidence) {

        RiskAssessment assessment =
                riskEngine.calculate(
                        nhi.getNhiId(),
                        evidence
                );

        double confidence =
                evidenceCompletenessEngine.calculate(evidence);

        DecisionEngine.DecisionResult decision =
                decisionEngine.determineDecision(
                        assessment.getRiskScore(),
                        confidence
                );

        assessment.setConfidence(confidence);
        assessment.setDecision(decision.decision());
        assessment.setDecisionCategory(decision.category());

        return assessment;
    }
}