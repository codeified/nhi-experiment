package com.nhi.experiment.service;

import com.nhi.experiment.engine.EvidenceCompletenessEngine;
import com.nhi.experiment.engine.DecisionEngineV2;
import com.nhi.experiment.engine.RiskCalculatorV2;
import com.nhi.experiment.model.Evidence;
import com.nhi.experiment.model.RiskAssessmentV2;
import com.nhi.experiment.model.RiskCalculationV2;

import java.util.Objects;

/**
 * Coordinates the v2.0 NHI assessment process.
 *
 * Processing sequence:
 * 1. Calculate risk or fixed-weight risk bounds.
 * 2. Calculate evidence confidence independently.
 * 3. Determine the decision and provisional priority category.
 * 4. Assemble the final assessment result.
 *
 * This service does not assign priority ranks because ranking requires
 * comparing multiple NHI assessments. The v1.0 AssessmentService remains
 * available for the experimental reference model.
 */
public class AssessmentServiceV2 {

    private final RiskCalculatorV2 riskCalculator;
    private final EvidenceCompletenessEngine evidenceCompletenessEngine;
    private final DecisionEngineV2 decisionEngine;

    /**
     * Creates the v2.0 service using the project's existing confidence engine
     * and the new v2.0 calculation and decision components.
     */
    public AssessmentServiceV2() {
        this(
                new RiskCalculatorV2(),
                new EvidenceCompletenessEngine(),
                new DecisionEngineV2()
        );
    }

    /**
     * Allows dependencies to be supplied explicitly for testing.
     */
    public AssessmentServiceV2(
            RiskCalculatorV2 riskCalculator,
            EvidenceCompletenessEngine evidenceCompletenessEngine,
            DecisionEngineV2 decisionEngine
    ) {
        this.riskCalculator = Objects.requireNonNull(riskCalculator);
        this.evidenceCompletenessEngine = Objects.requireNonNull(evidenceCompletenessEngine);
        this.decisionEngine = Objects.requireNonNull(decisionEngine);
    }

    /**
     * Assesses one NHI using the candidate v2.0 model.
     *
     * @param nhiId identifier of the NHI being assessed
     * @param evidence its risk values and evidence-quality metadata
     * @return assessment with risk, confidence and decision information
     */
    public RiskAssessmentV2 assess(String nhiId, Evidence evidence) {
        if (nhiId == null || nhiId.isBlank()) {
            throw new IllegalArgumentException(
                    "NHI ID must not be blank"
            );
        }

        Objects.requireNonNull(evidence, "Evidence must not be null");

        RiskCalculationV2 calculation =
                riskCalculator.calculate(evidence);

        double confidence = evidenceCompletenessEngine.calculate(evidence);

        DecisionEngineV2.DecisionResult decision =
                decisionEngine.decide(calculation, confidence);

        return new RiskAssessmentV2(
                nhiId,
                calculation.threatScore(),
                calculation.exposureScore(),
                calculation.impactScore(),
                calculation.riskScore(),
                calculation.riskMin(),
                calculation.riskMax(),
                calculation.missingDimensions(),
                confidence,
                null, // Rank will be assigned by a batch-ranking component.
                decision.decision(),
                decision.decisionCategory(),
                decision.priorityCategory(),
                decision.incompleteEvidence(),
                decision.requiresInvestigation(),
                decision.reason()
        );
    }
}