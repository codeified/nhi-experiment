package com.nhi.experiment.experiment;

import com.nhi.experiment.model.Evidence;
import com.nhi.experiment.model.RiskAssessmentV2;
import com.nhi.experiment.service.AssessmentServiceV2;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pass 2: Minimum Sufficient Evidence (MSE) class will exhaustively evaluate
 * every subset of the eight canonical evidence
 * dimensions for each of the 20 fixed scenarios. A subset is sufficient only
 * when: a. the risk bounds resolve to one category,
 * b. decision category equals the independently assigned expected label,
 * and c. a HIGH/CRITICAL expected case is not assigned a lower priority category.
 * The expected label is used only for evaluation, never as scoring input.
 * No weights, thresholds, scenarios, or expected labels are changed here.
 */
class MinimumSufficientEvidenceExperimentTest {
    private static final double MIN_CONFIDENCE = 0.0;

    private final AssessmentServiceV2 service = new AssessmentServiceV2();
    private final EvidencePerturbation perturbation = new EvidencePerturbation();
    private final List<EvidencePerturbation.Dimension> dimensions =
            List.of(EvidencePerturbation.Dimension.values());

    private record Candidate(
            String scenarioId,
            String description,
            String retainedDimensions,
            int retainedCount,
            double confidence,
            double baselineScore,
            double riskMin,
            double riskMax,
            String decisionCategory,
            String priorityCategory,
            String expectedCategory,
            boolean boundsResolveToOneCategory,
            boolean expectedCategoryMatch,
            boolean confidenceSufficient,
            boolean unsafePriorityDowngrade,
            boolean sufficient) { }

    private record ScenarioMse(
            String scenarioId,
            String description,
            String expectedCategory,
            String baselineCategory,
            double baselineScore,
            int candidateSubsetsEvaluated,
            String status,
            int minimumEvidenceCount,
            String minimumSufficientSubset,
            double confidence,
            double riskMin,
            double riskMax,
            String resultingCategory) { }

    @Test
    void shouldFindMinimumSufficientEvidenceForAllCanonicalScenarios() {
        var scenarios = BaselineScenarios.all();
        assertEquals(20, scenarios.size(), "Pass 2 must use the same 20 canonical scenarios");
        assertEquals(8, dimensions.size(), "Pass 2 expects exactly eight evidence dimensions");

        List<Candidate> candidates = new ArrayList<>();
        List<ScenarioMse> summaries = new ArrayList<>();
        int totalSubsetsPerScenario = 1 << dimensions.size();

        for (BaselineScenario scenario : scenarios) {
            RiskAssessmentV2 baseline = service.assess(scenario.id(), scenario.toEvidence());
            assertNotNull(baseline.riskScore(), "Complete baseline score missing for " + scenario.id());
            String expected = scenario.expectedSeverity().name();
            List<Candidate> scenarioCandidates = new ArrayList<>();

            // Enumerate all 2^8 subsets, including the empty subset.
            for (int mask = 0; mask < totalSubsetsPerScenario; mask++) {
                Evidence evidence = scenario.toEvidence();
                List<EvidencePerturbation.Dimension> retained = new ArrayList<>();
                for (int i = 0; i < dimensions.size(); i++) {
                    EvidencePerturbation.Dimension dimension = dimensions.get(i);
                    if ((mask & (1 << i)) != 0) {
                        retained.add(dimension);
                    } else {
                        evidence = perturbation.apply(evidence, dimension,
                                EvidencePerturbation.Type.MISSING);
                    }
                }

                RiskAssessmentV2 assessment = service.assess(scenario.id(), evidence);
                String lowerCategory = categoryFor(assessment.riskMin());
                String upperCategory = categoryFor(assessment.riskMax());
                boolean boundsStable = lowerCategory.equals(upperCategory);
                boolean expectedMatch = expected.equals(assessment.decisionCategory());
                boolean confidenceSufficient = assessment.confidence() >= MIN_CONFIDENCE;
                boolean unsafeDowngrade = isHigh(expected)
                        && severityOrder(assessment.priorityCategory()) < severityOrder(expected);
                boolean sufficient = confidenceSufficient && boundsStable && expectedMatch
                        && !unsafeDowngrade;

                Candidate candidate = new Candidate(
                        scenario.id(), scenario.description(),
                        retained.stream().map(Enum::name).collect(Collectors.joining("|")),
                        retained.size(), assessment.confidence(), baseline.riskScore(),
                        assessment.riskMin(), assessment.riskMax(),
                        assessment.decisionCategory(), assessment.priorityCategory(), expected,
                        boundsStable, expectedMatch, confidenceSufficient,
                        unsafeDowngrade, sufficient);
                scenarioCandidates.add(candidate);
                candidates.add(candidate);
            }

            // Minimum number of dimensions first; lexical subset order is only a
            // deterministic tie-break for reporting, not a scoring/model rule.
            Candidate best = scenarioCandidates.stream()
                    .filter(Candidate::sufficient)
                    .min(Comparator.comparingInt(Candidate::retainedCount)
                            .thenComparing(Candidate::retainedDimensions))
                    .orElse(null);
            if (best != null) {
                assertTrue(best.sufficient(),
                        "Reported MSE must satisfy all criteria for " + scenario.id());

                int minimumCount = best.retainedCount();
                assertTrue(scenarioCandidates.stream()
                                .noneMatch(c -> c.sufficient()
                                        && c.retainedCount() < minimumCount),
                        "A smaller sufficient subset exists for " + scenario.id());
            }
            summaries.add(new ScenarioMse(
                    scenario.id(), scenario.description(), expected,
                    baseline.decisionCategory(), baseline.riskScore(), scenarioCandidates.size(),
                    best == null ? "NO_SUFFICIENT_SUBSET" : "MSE_FOUND",
                    best == null ? -1 : best.retainedCount(),
                    best == null ? "" : best.retainedDimensions(),
                    best == null ? Double.NaN : best.confidence(),
                    best == null ? Double.NaN : best.riskMin(),
                    best == null ? Double.NaN : best.riskMax(),
                    best == null ? "" : best.decisionCategory()));
        }

// Validate experiment coverage before publishing outputs.
        assertEquals(20, scenarios.size(),
                "Pass 2 must use 20 canonical scenarios");
        assertEquals(8, dimensions.size(),
                "Pass 2 expects eight evidence dimensions");
        assertEquals(5120, candidates.size(),
                "Expected 20 scenarios x 256 evidence subsets");
        assertEquals(20, summaries.size(),
                "Expected one summary per scenario");

// Every scenario must evaluate all 256 subsets.
        for (ScenarioMse summary : summaries) {
            assertEquals(256, summary.candidateSubsetsEvaluated(),
                    "Incorrect subset count for " + summary.scenarioId());
        }

// Publish outputs only after structural validation passes.
        writeCandidateCsv(candidates);
        writeSummaryCsv(summaries);
        printSummary(summaries, candidates);
    }

    private static String categoryFor(double risk) {
        if (risk >= 80.0) return "CRITICAL";
        if (risk >= 60.0) return "HIGH";
        if (risk >= 40.0) return "INVESTIGATE";
        if (risk >= 20.0) return "MONITOR_REMEDIATE";
        return "LOW";
    }

    private static boolean isHigh(String category) {
        return "HIGH".equals(category) || "CRITICAL".equals(category);
    }

    private static int severityOrder(String category) {
        return switch (category) {
            case "LOW" -> 0;
            case "MONITOR_REMEDIATE" -> 1;
            case "INVESTIGATE" -> 2;
            case "HIGH" -> 3;
            case "CRITICAL" -> 4;
            default -> -1;
        };
    }

    private static void writeCandidateCsv(List<Candidate> rows) {
        List<String> lines = new ArrayList<>();
        lines.add("scenarioId,description,retainedDimensions,retainedCount,confidence,baselineScore,riskMin,riskMax,decisionCategory,priorityCategory,expectedCategory,boundsResolveToOneCategory,expectedCategoryMatch,confidenceSufficient,unsafePriorityDowngrade,sufficient");
        for (Candidate r : rows) {
            lines.add(String.join(",", csv(r.scenarioId()), csv(r.description()), csv(r.retainedDimensions()),
                    Integer.toString(r.retainedCount()), f(r.confidence()), f(r.baselineScore()),
                    f(r.riskMin()), f(r.riskMax()), csv(r.decisionCategory()), csv(r.priorityCategory()),
                    csv(r.expectedCategory()), Boolean.toString(r.boundsResolveToOneCategory()),
                    Boolean.toString(r.expectedCategoryMatch()), Boolean.toString(r.confidenceSufficient()),
                    Boolean.toString(r.unsafePriorityDowngrade()), Boolean.toString(r.sufficient())));
        }
        writeLines("pass2_all_evidence_subsets.csv", lines);
    }

    private static void writeSummaryCsv(List<ScenarioMse> rows) {
        List<String> lines = new ArrayList<>();
        lines.add("scenarioId,description,expectedCategory,baselineCategory,baselineScore,candidateSubsetsEvaluated,status,minimumEvidenceCount,minimumSufficientSubset,confidence,riskMin,riskMax,resultingCategory");
        for (ScenarioMse r : rows) {
            lines.add(String.join(",", csv(r.scenarioId()), csv(r.description()), csv(r.expectedCategory()),
                    csv(r.baselineCategory()), f(r.baselineScore()), Integer.toString(r.candidateSubsetsEvaluated()),
                    csv(r.status()), r.minimumEvidenceCount() < 0 ? "NA" : Integer.toString(r.minimumEvidenceCount()),
                    csv(r.minimumSufficientSubset()), f(r.confidence()), f(r.riskMin()), f(r.riskMax()),
                    csv(r.resultingCategory())));
        }
        writeLines("pass2_mse_by_scenario.csv", lines);
    }

    private static void writeLines(String filename, List<String> lines) {
        Path directory = Path.of("target", "experiment-results");
        Path file = directory.resolve(filename);
        try {
            Files.createDirectories(directory);
            Files.write(file, lines);
            System.out.println("Experiment CSV: " + file.toAbsolutePath());
        } catch (IOException e) {
            throw new IllegalStateException("Could not write experiment CSV to " + file, e);
        }
    }

    private static String csv(String value) {
        String v = value == null ? "" : value;
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }

    private static String f(double value) {
        return Double.isFinite(value) ? String.format(Locale.ROOT, "%.2f", value) : "NA";
    }

    private static void printSummary(List<ScenarioMse> summaries, List<Candidate> candidates) {
        long found = summaries.stream().filter(s -> "MSE_FOUND".equals(s.status())).count();
        long baselineMismatch = summaries.stream()
                .filter(s -> !s.expectedCategory().equals(s.baselineCategory())).count();
        System.out.println("\nNHI PASS 2: MINIMUM SUFFICIENT EVIDENCE");
        System.out.println("Scenarios: " + summaries.size() + " | Dimensions: " + dimensionsLabel()
                + " | Subsets evaluated: " + candidates.size());
        System.out.println("MSE found: " + found + "/" + summaries.size()
                + " | Baseline category mismatches: " + baselineMismatch);
        System.out.println("Criteria: expected category match, confidence >= 0%, stable risk category across bounds, and no HIGH/CRITICAL priority downgrade.");
        System.out.println("Scenario\tExpected\tBaseline\tStatus\tMinEvidenceCount\tMSE\tConfidence\tRiskMin\tRiskMax\tResultCategory");
        for (ScenarioMse r : summaries) {
            System.out.println(String.join("\t", r.scenarioId(), r.expectedCategory(), r.baselineCategory(),
                    r.status(), r.minimumEvidenceCount() < 0 ? "NA" : Integer.toString(r.minimumEvidenceCount()),
                    r.minimumSufficientSubset().isBlank() ? "NA" : r.minimumSufficientSubset(),
                    f(r.confidence()), f(r.riskMin()), f(r.riskMax()),
                    r.resultingCategory().isBlank() ? "NA" : r.resultingCategory()));
        }
    }

    private static String dimensionsLabel() { return Integer.toString(EvidencePerturbation.Dimension.values().length); }
}
