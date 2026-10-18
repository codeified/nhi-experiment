package com.nhi.experiment.experiment;

import java.util.List;

import static com.nhi.experiment.experiment.BaselineScenario.ExpectedSeverity.*;

/**
 * Canonical complete-evidence baseline for scenarios S01-S20.
 *
 * Expected severities are evaluation labels and must not be passed
 * to the risk-scoring calculation.
 */
public final class BaselineScenarios {

    private BaselineScenarios() {
        // Utility class; do not instantiate.
    }


    public static List<BaselineScenario> all() {
        return List.of(
                s("S01", "Controlled managed identity",
                        0, 0, 0, 0, 0, 0, 0, 0, LOW),

                s("S02", "Restricted sandbox reporting identity",
                        0, 0, 0, 0, 0, 0, 0, 0, LOW),

                s("S03", "Legitimate write access to a low-impact business service",
                        0, 0, 0, 25, 25, 0, 0, 25, LOW),

                s("S04", "Credential approaching expiry but still compliant",
                        0, 0, 0, 25, 25, 0, 25, 25, LOW),

                s("S05", "Overdue credential rotation with limited access",
                        0, 0, 50, 25, 25, 0, 25, 25, MONITOR_REMEDIATE),

                s("S06", "Dormant six months with possible future use",
                        0, 0, 50, 25, 25, 0, 25, 25, MONITOR_REMEDIATE),

                s("S07", "Dormant over a year, unknown owner, broad permissions",
                        0, 0, 75, 75, 75, 0, 50, 50, INVESTIGATE),

                s("S08", "Powerful production permissions with approved controls",
                        0, 0, 0, 75, 25, 0, 50, 50, MONITOR_REMEDIATE),

                s("S09", "Approved read-only access to confidential data",
                        0, 0, 0, 25, 25, 0, 75, 25, MONITOR_REMEDIATE),

                s("S10", "Broad read/write access without justified need",
                        0, 0, 0, 75, 75, 0, 75, 50, INVESTIGATE),

                s("S11", "Weak authentication with narrow low-impact access",
                        75, 0, 25, 0, 0, 0, 25, 25, LOW),

                s("S12", "Shared, long-lived production credential",
                        25, 25, 75, 75, 75, 0, 50, 50, INVESTIGATE),

                s("S13", "Excessive development permissions with limited impact",
                        0, 0, 0, 50, 50, 0, 25, 25, MONITOR_REMEDIATE),

                s("S14", "Publicly reachable workload with narrow permissions",
                        25, 0, 0, 25, 50, 75, 25, 25, MONITOR_REMEDIATE),

                s("S15", "Public application with write access to sensitive production storage",
                        25, 0, 0, 75, 75, 75, 75, 75, INVESTIGATE),

                s("S16", "Broad cross-tenant access without clear justification",
                        25, 0, 0, 75, 75, 75, 75, 50, INVESTIGATE),

                s("S17", "Anomalous API activity and unexpected resource access",
                        75, 75, 0, 50, 50, 0, 50, 50, INVESTIGATE),

                s("S18", "Unauthorized destructive activity against critical assets",
                        75, 100, 0, 75, 75, 0, 100, 100, HIGH),

                s("S19", "Mission-critical service with sensitive assets and strong controls",
                        0, 0, 0, 50, 50, 0, 100, 50, MONITOR_REMEDIATE),

                s("S20", "Combined weaknesses and suspicious activity",
                        75, 75, 75, 75, 75, 75, 50, 75, HIGH)
        );
    }


    private static BaselineScenario s(
            String id,
            String description,
            double a,
            double b,
            double m,
            double p,
            double sensitiveAccess,
            double observedExposure,
            double resourceCriticality,
            double assetSensitivity,
            BaselineScenario.ExpectedSeverity expected
    ) {
        return new BaselineScenario(
                id, description,
                a, b, m, p,
                sensitiveAccess,
                observedExposure,
                resourceCriticality,
                assetSensitivity,
                expected
        );
    }
}