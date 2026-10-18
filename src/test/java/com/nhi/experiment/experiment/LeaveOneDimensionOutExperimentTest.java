package com.nhi.experiment.experiment;

import com.nhi.experiment.engine.BatchRankingEngine;
import com.nhi.experiment.model.RankedRiskAssessmentV2;
import com.nhi.experiment.model.RiskAssessmentV2;
import com.nhi.experiment.service.AssessmentServiceV2;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pass 1 sensitivity experiment: remove one evidence dimension at a time
 * across the 20 canonical scenarios. Does not change model weights, thresholds,
 * scenarios, or expected labels. Missing evidence is evaluated using risk and
 * rank bounds when no single point score or rank is available.
 */
class LeaveOneDimensionOutExperimentTest {
    private final AssessmentServiceV2 service = new AssessmentServiceV2();
    private final EvidencePerturbation perturbation = new EvidencePerturbation();
    private final BatchRankingEngine rankingEngine = new BatchRankingEngine();

    private record Baseline(RiskAssessmentV2 assessment, Integer rank) { }
    private record Result(
            String id, String description, EvidencePerturbation.Dimension dimension,
            Double baselineScore, Double perturbedScore, double riskMin, double riskMax,
            double baselineConfidence, double perturbedConfidence,
            String baselineCategory, String decisionCategory, String priorityCategory,
            String expectedCategory, Integer baselineRank, Integer rank,
            int bestRank, int worstRank, double minDelta, double maxDelta,
            boolean categoryChanged, boolean labelMatch, boolean uncertain,
            boolean requiresInvestigation, boolean unsafeDowngrade) { }

    @Test
    void shouldReportLeaveOneDimensionOutSensitivityForAllScenarios() {
        List<BaselineScenario> scenarios = BaselineScenarios.all();
        assertEquals(20, scenarios.size(), "Pass 1 must use 20 canonical scenarios");
        Map<String, Baseline> baselines = buildBaselines(scenarios);
        List<Result> results = new ArrayList<>();

        for (EvidencePerturbation.Dimension dimension : EvidencePerturbation.Dimension.values()) {
            List<RiskAssessmentV2> assessments = new ArrayList<>();
            for (BaselineScenario scenario : scenarios) {
                var evidence = perturbation.apply(scenario.toEvidence(), dimension,
                        EvidencePerturbation.Type.MISSING);
                assessments.add(service.assess(scenario.id(), evidence));
            }
            Map<String, RankedRiskAssessmentV2> ranked = rankingEngine.rank(assessments).stream()
                    .collect(Collectors.toMap(x -> x.assessment().nhiId(), Function.identity()));

            for (BaselineScenario scenario : scenarios) {
                Baseline baseline = baselines.get(scenario.id());
                RankedRiskAssessmentV2 rankedResult = ranked.get(scenario.id());
                assertNotNull(rankedResult, "Missing perturbed assessment for " + scenario.id());
                RiskAssessmentV2 base = baseline.assessment();
                RiskAssessmentV2 changed = rankedResult.assessment();
                String expected = scenario.expectedSeverity().name();
                String category = changed.decisionCategory();
                String priority = changed.priorityCategory();
                // Use the provisional upper-bound priority category, not RISK_UNCERTAIN,
                // to test whether a high-priority scenario was actually downgraded.
                boolean unsafe = isHigh(expected) && severityOrder(priority) < severityOrder(expected);
                results.add(new Result(scenario.id(), scenario.description(), dimension,
                        base.riskScore(), changed.riskScore(), changed.riskMin(), changed.riskMax(),
                        base.confidence(), changed.confidence(), base.decisionCategory(), category,
                        priority, expected, baseline.rank(), rankedResult.priorityRank(),
                        rankedResult.bestPossibleRank(), rankedResult.worstPossibleRank(),
                        changed.riskMin() - base.riskScore(), changed.riskMax() - base.riskScore(),
                        !base.decisionCategory().equals(category), expected.equals(category),
                        "RISK_UNCERTAIN".equals(category) || "LOW_CONFIDENCE".equals(category),
                        changed.requiresInvestigation(), unsafe));
            }
        }

        // Validate experiment coverage before publishing any output files.
        assertEquals(20, scenarios.size(),
                "Pass 1 must use 20 canonical scenarios");

        assertEquals(20, baselines.size(),
                "Expected one complete-evidence baseline per scenario");

        assertEquals(8, EvidencePerturbation.Dimension.values().length,
                "Pass 1 expects eight evidence dimensions");

        assertEquals(160, results.size(),
                "Expected 20 scenarios x 8 missing-dimension conditions");

// Confirm that every scenario/dimension combination appears exactly once.
        Set<String> runKeys = new HashSet<>();
        for (Result result : results) {
            String key = result.id() + "|" + result.dimension().name();
            assertTrue(runKeys.add(key),
                    "Duplicate Pass 1 run: " + key);
        }
        assertEquals(160, runKeys.size(),
                "Every scenario/dimension combination must be represented");

// Publish results only after the structural checks pass.
        writeCsv(results);
        printReport(results);
    }


    /** Writes a machine-readable detail file for reproducible analysis. */
    private static void writeCsv(List<Result> rows) {
        Path directory = Path.of("target", "experiment-results");
        Path file = directory.resolve("pass1_leave_one_dimension_out.csv");
        List<String> lines = new ArrayList<>();
        lines.add("scenarioId,description,removedDimension,baselineScore,perturbedScore,riskMin,riskMax,minDeltaFromBaseline,maxDeltaFromBaseline,baselineConfidence,perturbedConfidence,baselineCategory,decisionCategory,priorityCategory,expectedCategory,baselineRank,perturbedRank,bestRank,worstRank,categoryChanged,labelMatch,riskUncertain,requiresInvestigation,unsafePriorityDowngrade");
        for (Result r : rows) {
            lines.add(String.join(",", csv(r.id()), csv(r.description()), csv(r.dimension().name()),
                    csv(f(r.baselineScore())), csv(f(r.perturbedScore())), csv(f(r.riskMin())), csv(f(r.riskMax())),
                    csv(f(r.minDelta())), csv(f(r.maxDelta())), csv(f(r.baselineConfidence())), csv(f(r.perturbedConfidence())),
                    csv(r.baselineCategory()), csv(r.decisionCategory()), csv(r.priorityCategory()), csv(r.expectedCategory()),
                    csv(f(r.baselineRank())), csv(f(r.rank())), csv(Integer.toString(r.bestRank())), csv(Integer.toString(r.worstRank())),
                    csv(Boolean.toString(r.categoryChanged())), csv(Boolean.toString(r.labelMatch())),
                    csv(Boolean.toString(r.uncertain())), csv(Boolean.toString(r.requiresInvestigation())),
                    csv(Boolean.toString(r.unsafeDowngrade()))));
        }
        try {
            Files.createDirectories(directory);
            Files.write(file, lines);
            System.out.println("Pass 1 detail CSV: " + file.toAbsolutePath());
        } catch (IOException e) {
            throw new IllegalStateException("Could not write Pass 1 CSV to " + file, e);
        }
    }

    private static String csv(String value) {
        String v = value == null ? "" : value;
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }

    private Map<String, Baseline> buildBaselines(List<BaselineScenario> scenarios) {
        List<RiskAssessmentV2> assessments = new ArrayList<>();
        for (BaselineScenario scenario : scenarios) {
            RiskAssessmentV2 a = service.assess(scenario.id(), scenario.toEvidence());
            assertNotNull(a.riskScore(), scenario.id() + " needs a complete-evidence score");
            assertTrue(a.missingDimensions().isEmpty(), scenario.id() + " should have complete evidence");
            assertEquals(a.riskScore(), a.riskMin(), 0.000001);
            assertEquals(a.riskScore(), a.riskMax(), 0.000001);
            assessments.add(a);
        }
        Map<String, RankedRiskAssessmentV2> ranked = rankingEngine.rank(assessments).stream()
                .collect(Collectors.toMap(x -> x.assessment().nhiId(), Function.identity()));
        Map<String, Baseline> out = new HashMap<>();
        for (BaselineScenario s : scenarios) {
            RankedRiskAssessmentV2 r = ranked.get(s.id());
            assertNotNull(r, "Missing baseline rank for " + s.id());
            out.put(s.id(), new Baseline(r.assessment(), r.priorityRank()));
        }
        return out;
    }

    private static boolean isHigh(String s) { return "HIGH".equals(s) || "CRITICAL".equals(s); }
    private static int severityOrder(String s) {
        return switch (s) {
            case "LOW" -> 0; case "MONITOR_REMEDIATE" -> 1; case "INVESTIGATE" -> 2;
            case "HIGH" -> 3; case "CRITICAL" -> 4; default -> -1;
        };
    }
    private static String f(Double d) {
        return d == null || !Double.isFinite(d) ? "NA" : String.format(Locale.ROOT, "%.2f", d);
    }
    private static String f(double d) {
        return Double.isFinite(d) ? String.format(Locale.ROOT, "%.2f", d) : "NA";
    }
    private static String f(Integer i) { return i == null ? "NA" : i.toString(); }
    private static String clean(String s) { return s == null ? "" : s.replace('\t', ' ').replace('\n', ' '); }

    private static void printReport(List<Result> rows) {
        System.out.println("\nNHI PASS 1: LEAVE-ONE-DIMENSION-OUT SENSITIVITY REPORT");
        System.out.println("Scenarios: 20 | Dimensions: 8 | Perturbation: MISSING | Runs: " + rows.size());
        System.out.println("NA point scores are expected when evidence is missing; risk bounds are fixed-weight bounds, not confidence intervals.");
        System.out.println("Rank bounds are best/worst possible ranks. Unsafe downgrade uses priorityCategory; uncertainty is reported separately.\n");
        System.out.println("Scenario\tDescription\tRemovedDimension\tBaselineScore\tPerturbedScore\tRiskMin\tRiskMax\tMinDeltaFromBaseline\tMaxDeltaFromBaseline\tBaselineConfidence\tPerturbedConfidence\tBaselineCategory\tDecisionCategory\tPriorityCategory\tExpectedCategory\tBaselineRank\tRank\tBestRank\tWorstRank\tCategoryChanged\tLabelMatch\tRiskUncertain\tRequiresInvestigation\tUnsafePriorityDowngrade");
        for (Result r : rows) {
            System.out.println(String.join("\t", r.id(), clean(r.description()), r.dimension().name(),
                    f(r.baselineScore()), f(r.perturbedScore()), f(r.riskMin()), f(r.riskMax()),
                    f(r.minDelta()), f(r.maxDelta()), f(r.baselineConfidence()), f(r.perturbedConfidence()),
                    r.baselineCategory(), r.decisionCategory(), r.priorityCategory(), r.expectedCategory(),
                    f(r.baselineRank()), f(r.rank()), Integer.toString(r.bestRank()), Integer.toString(r.worstRank()),
                    Boolean.toString(r.categoryChanged()), Boolean.toString(r.labelMatch()),
                    Boolean.toString(r.uncertain()), Boolean.toString(r.requiresInvestigation()),
                    Boolean.toString(r.unsafeDowngrade())));
        }
        System.out.println("\nSUMMARY BY REMOVED DIMENSION");
        System.out.println("Dimension\tRuns\tCategoryChanges\tExpectedLabelMatches\tRiskUncertain\tRequiresInvestigation\tUnsafePriorityDowngrades\tMeanRiskMinDelta\tMeanRiskMaxDelta\tMeanRiskBoundWidth\tMeanAbsConfidenceDrop\tRankUncertain");
        for (EvidencePerturbation.Dimension d : EvidencePerturbation.Dimension.values()) {
            List<Result> g = rows.stream().filter(r -> r.dimension() == d).toList();
            long changes = g.stream().filter(Result::categoryChanged).count();
            long matches = g.stream().filter(Result::labelMatch).count();
            long uncertain = g.stream().filter(Result::uncertain).count();
            long investigate = g.stream().filter(Result::requiresInvestigation).count();
            long unsafe = g.stream().filter(Result::unsafeDowngrade).count();
            long rankUncertain = g.stream().filter(r -> r.bestRank() != r.worstRank()).count();
            double minDelta = g.stream().mapToDouble(Result::minDelta).average().orElse(Double.NaN);
            double maxDelta = g.stream().mapToDouble(Result::maxDelta).average().orElse(Double.NaN);
            double width = g.stream().mapToDouble(r -> r.riskMax() - r.riskMin()).average().orElse(Double.NaN);
            double confidenceDrop = g.stream().mapToDouble(r -> Math.abs(r.baselineConfidence() - r.perturbedConfidence())).average().orElse(Double.NaN);
            System.out.println(String.join("\t", d.name(), Integer.toString(g.size()), Long.toString(changes),
                    matches + "/" + g.size(), Long.toString(uncertain), Long.toString(investigate),
                    Long.toString(unsafe), f(minDelta), f(maxDelta), f(width), f(confidenceDrop),
                    Long.toString(rankUncertain)));
        }
        System.out.println();
    }
}
