package com.nhi.experiment.experiment;

import com.nhi.experiment.engine.BatchRankingEngine;
import com.nhi.experiment.model.RankedRiskAssessmentV2;
import com.nhi.experiment.model.RiskAssessmentV2;
import com.nhi.experiment.service.AssessmentServiceV2;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Generates the complete-evidence batch-ranking report for the
 * canonical S01-S20 baseline scenarios.
 *
 * Expected labels and operational actions are independent evaluation
 * references. They are not passed into the risk calculator or decision engine.
 *
 * Risk contributors are descriptive decompositions of the existing V2 score:
 * each evidence value is multiplied by its existing overall model weight.
 * This report does not modify scoring, thresholds, or decisions.
 */
class BaselineBatchRankingReportTest {

    private static final int TOP_RISK_CONTRIBUTORS = 3;

    private record FactorContribution(
            String factor,
            double evidenceValue,
            double weightedPoints) {
    }

    @Test
    void shouldGenerateCompleteEvidenceBatchRankingReport() {
        AssessmentServiceV2 assessmentService = new AssessmentServiceV2();
        BatchRankingEngine rankingEngine = new BatchRankingEngine();
        List<RiskAssessmentV2> assessments = new ArrayList<>();

        // Assess every canonical baseline scenario.
        for (BaselineScenario scenario : BaselineScenarios.all()) {
            RiskAssessmentV2 assessment = assessmentService.assess(
                    scenario.id(), scenario.toEvidence());

            assertNotNull(assessment.riskScore(),
                    scenario.id() + " should have a risk score");
            assertTrue(assessment.missingDimensions().isEmpty(),
                    scenario.id() + " should have no missing dimensions");
            assertEquals(assessment.riskScore(), assessment.riskMin(), 0.000001,
                    scenario.id() + " risk score should equal risk minimum");
            assertEquals(assessment.riskScore(), assessment.riskMax(), 0.000001,
                    scenario.id() + " risk score should equal risk maximum");

            // Independently verify that the report's weighted factor
            // contributions reconcile to the production V2 risk score.
            assertEquals(
                    assessment.riskScore(),
                    roundToTwoDecimals(totalWeightedContribution(scenario)),
                    0.000001,
                    scenario.id() + " factor contributions must reconcile to RiskCalculatorV2"
            );

            assessments.add(assessment);
        }

        assertEquals(20, assessments.size(),
                "The baseline must contain exactly 20 assessments");

        List<RankedRiskAssessmentV2> rankedAssessments =
                rankingEngine.rank(assessments);
        assertEquals(20, rankedAssessments.size());

        System.out.println();
        System.out.println("NHI BASELINE BATCH-RANKING REPORT");
        System.out.println("Model: Risk Calculator V2 | Evidence: Complete | Scenarios: "
                + rankedAssessments.size());
        System.out.println("Expected label is independent of the calculated category.");
        System.out.println("Risk contributors show the top "
                + TOP_RISK_CONTRIBUTORS
                + " positive weighted contributions to the existing risk score; "
                + "they do not represent additional scoring rules.");
        System.out.println();

        int matches = 0;
        int mismatches = 0;
        int unstableRanks = 0;

        for (RankedRiskAssessmentV2 ranked : rankedAssessments) {
            RiskAssessmentV2 assessment = ranked.assessment();
            BaselineScenario scenario = BaselineScenarios.all().stream()
                    .filter(s -> s.id().equals(assessment.nhiId()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError(
                            "No baseline scenario found for " + assessment.nhiId()));

            String expected = scenario.expectedSeverity().name();
            String calculated = assessment.decisionCategory();
            boolean matchesExpected = expected.equals(calculated);
            if (matchesExpected) {
                matches++;
            } else {
                mismatches++;
            }

            boolean stable = ranked.bestPossibleRank() == ranked.worstPossibleRank();
            if (!stable) {
                unstableRanks++;
            }

            String riskBounds = String.format(Locale.ROOT, "%.2f-%.2f",
                    assessment.riskMin(), assessment.riskMax());
            String confidence = String.format(Locale.ROOT, "%.1f%%",
                    assessment.confidence());
            String priorityRank = ranked.priorityRank() == null
                    ? "-" : ranked.priorityRank().toString();
            String reason = assessment.decisionReason() == null
                    || assessment.decisionReason().isBlank()
                    ? "Not specified by engine" : assessment.decisionReason();
            String result = matchesExpected ? "MATCH" : "MISMATCH";

            System.out.println("----------------------------------------------------------------");
            System.out.println("Scenario ID:                 " + scenario.id());
            System.out.println("Scenario description:         " + scenario.description());
            System.out.println("Risk score:                   "
                    + String.format(Locale.ROOT, "%.2f", assessment.riskScore()));
            System.out.println("Risk bounds:                  " + riskBounds);
            System.out.println("Confidence:                   " + confidence);
            System.out.println("Calculated category:          " + calculated);
            System.out.println("Decision reason:              " + reason);
            System.out.println("Top risk contributors:         "
                    + topRiskContributors(scenario));
            System.out.println("Priority rank:                " + priorityRank
                    + " (possible range " + ranked.bestPossibleRank()
                    + "-" + ranked.worstPossibleRank() + ")");
            System.out.println("Expected label:               " + expected);
            System.out.println("Expected operational action:  "
                    + expectedOperationalAction(scenario.id()));
            System.out.println("Category comparison:          " + result);
        }

        System.out.println("----------------------------------------------------------------");
        System.out.println("SUMMARY");
        System.out.println("Total scenarios:       " + rankedAssessments.size());
        System.out.println("Category matches:      " + matches);
        System.out.println("Category mismatches:   " + mismatches);
        System.out.println("Category agreement:    " + String.format(Locale.ROOT, "%.1f%%",
                100.0 * matches / rankedAssessments.size()));
        System.out.println("Unstable rank bounds:  " + unstableRanks);
        System.out.println("Expected CRITICAL scenarios: 0 of 20");
        System.out.println();

        assertEquals(0, unstableRanks,
                "Complete-evidence baseline ranks should be stable");
    }

    /**
     * Calculates the unrounded sum of all eight weighted contributions using
     * the production RiskCalculatorV2 overall weights.
     */
    private static double totalWeightedContribution(BaselineScenario scenario) {
        return scenario.authentication() * 0.10
                + scenario.behavior() * 0.20
                + scenario.misconfiguration() * 0.10
                + scenario.privilege() * 0.0875
                + scenario.sensitiveAccess() * 0.13125
                + scenario.observedExposure() * 0.13125
                + scenario.resourceCriticality() * 0.175
                + scenario.assetSensitivity() * 0.075;
    }

    private static double roundToTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    /**
     * Returns the three largest positive weighted contributions to the
     * existing composite risk score. A contribution is evidenceValue
     * multiplied by that dimension's existing overall V2 weight.
     */
    private static String topRiskContributors(BaselineScenario scenario) {
        List<FactorContribution> contributions = List.of(
                new FactorContribution("Authentication",
                        scenario.authentication(), scenario.authentication() * 0.10),
                new FactorContribution("Behavior",
                        scenario.behavior(), scenario.behavior() * 0.20),
                new FactorContribution("Misconfiguration",
                        scenario.misconfiguration(), scenario.misconfiguration() * 0.10),
                new FactorContribution("Privilege",
                        scenario.privilege(), scenario.privilege() * 0.0875),
                new FactorContribution("Sensitive access",
                        scenario.sensitiveAccess(), scenario.sensitiveAccess() * 0.13125),
                new FactorContribution("Observed exposure",
                        scenario.observedExposure(), scenario.observedExposure() * 0.13125),
                new FactorContribution("Resource criticality",
                        scenario.resourceCriticality(), scenario.resourceCriticality() * 0.175),
                new FactorContribution("Asset sensitivity",
                        scenario.assetSensitivity(), scenario.assetSensitivity() * 0.075)
        );

        List<FactorContribution> top = contributions.stream()
                .filter(c -> c.weightedPoints() > 0.0)
                .sorted(Comparator.comparingDouble(FactorContribution::weightedPoints)
                        .reversed())
                .limit(TOP_RISK_CONTRIBUTORS)
                .toList();

        if (top.isEmpty()) {
            return "None (all evidence values contribute 0 points)";
        }

        return top.stream()
                .map(c -> String.format(Locale.ROOT,
                        "%s (value %.0f; +%.2f points)",
                        c.factor(), c.evidenceValue(), c.weightedPoints()))
                .collect(Collectors.joining("; "));
    }

    /**
     * Operational actions are independent evaluation guidance and do not
     * affect the scoring engine.
     */
    private static String expectedOperationalAction(String scenarioId) {
        return switch (scenarioId) {
            case "S01" -> "Continue routine monitoring and lifecycle controls.";
            case "S02" -> "Maintain sandbox restrictions and routine monitoring.";
            case "S03" -> "Retain approved scoped access; review through normal access governance.";
            case "S04" -> "Follow the normal credential-expiry and rotation process.";
            case "S05" -> "Schedule credential rotation; prioritize by access scope and resource impact.";
            case "S06" -> "Confirm future need and owner; disable or remove if no longer required.";
            case "S07" -> "Establish purpose and ownership; validate broad access and remediate unjustified access.";
            case "S08" -> "Verify approved elevation controls and time limits; remediate deviations.";
            case "S09" -> "Confirm business need and retain approved read-only scope.";
            case "S10" -> "Conduct access review and remove permissions without business justification.";
            case "S11" -> "Review authentication weakness and apply proportionate control improvement.";
            case "S12" -> "Investigate shared credential use and replace with managed, scoped credentials.";
            case "S13" -> "Review development permissions and remove unnecessary access.";
            case "S14" -> "Validate endpoint protections and retain narrow workload permissions.";
            case "S15" -> "Review public exposure and sensitive-storage write access; reduce access as appropriate.";
            case "S16" -> "Validate cross-tenant business need and restrict unjustified access.";
            case "S17" -> "Investigate anomalous activity and validate token use and resource access.";
            case "S18" -> "Expedite security review and risk reduction; investigate without presuming compromise.";
            case "S19" -> "Maintain strong controls and periodically review critical-service access.";
            case "S20" -> "Expedite security review, validate risk-driving evidence, and reduce risk.";
            default -> throw new IllegalArgumentException(
                    "Unknown baseline scenario: " + scenarioId);
        };
    }
}
