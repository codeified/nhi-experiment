package com.nhi.experiment.engine;

import com.nhi.experiment.model.Evidence;
import com.nhi.experiment.model.RiskAssessment;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RiskEngineTest {

    private final RiskEngine riskEngine = new RiskEngine();

    @Test
    void allEvidenceAt100ShouldProduceRisk100() {

        Evidence evidence = createEvidence(
                100.0,
                100.0,
                100.0,
                100.0,
                100.0,
                100.0,
                100.0,
                100.0
        );

        RiskAssessment result =
                riskEngine.calculate("NHI-001", evidence);

        assertEquals(100.0, result.getThreatScore());
        assertEquals(100.0, result.getExposureScore());
        assertEquals(100.0, result.getImpactScore());
        assertEquals(100.0, result.getRiskScore());
    }

    @Test
    void allEvidenceAtZeroShouldProduceRiskZero() {

        Evidence evidence = createEvidence(
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0
        );

        RiskAssessment result =
                riskEngine.calculate("NHI-002", evidence);

        assertEquals(0.0, result.getThreatScore());
        assertEquals(0.0, result.getExposureScore());
        assertEquals(0.0, result.getImpactScore());
        assertEquals(0.0, result.getRiskScore());
    }

    @Test
    void mixedEvidenceShouldCalculateExpectedRisk() {

        Evidence evidence = createEvidence(
                90.0, // Privilege
                75.0, // Authentication
                55.0, // Behavior
                70.0, // Sensitive Access
                40.0, // Observed Exposure
                10.0, // Misconfiguration
                80.0, // Resource Criticality
                60.0  // Asset Sensitivity
        );

        RiskAssessment result =
                riskEngine.calculate("NHI-003", evidence);

        /*
         * Threat
         *
         * T = .25P + .25A + .50B
         *
         * = .25(90) + .25(75) + .50(55)
         * = 22.5 + 18.75 + 27.5
         * = 68.75
         */

        assertEquals(68.75, result.getThreatScore());

        /*
         * Exposure
         *
         * E = .40S + .40O + .20M
         *
         * = .40(70) + .40(40) + .20(10)
         * = 28 + 16 + 2
         * = 46
         */

        assertEquals(46.0, result.getExposureScore());

        /*
         * Impact
         *
         * I = .70R + .30As
         *
         * = .70(80) + .30(60)
         * = 56 + 18
         * = 74
         */

        assertEquals(74.0, result.getImpactScore());

        /*
         * Risk
         *
         * = .40T + .35E + .25I
         *
         * = .40(68.75)
         * + .35(46)
         * + .25(74)
         *
         * = 27.5 + 16.1 + 18.5
         * = 62.1
         */

        assertEquals(62.1, result.getRiskScore());
    }

    @Test
    void evidenceAbove100ShouldBeRejected() {

        Evidence evidence = createEvidence(
                101.0,
                75.0,
                55.0,
                70.0,
                40.0,
                10.0,
                80.0,
                60.0
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> riskEngine.calculate("NHI-004", evidence)
        );
    }

    @Test
    void evidenceBelowZeroShouldBeRejected() {

        Evidence evidence = createEvidence(
                -1.0,
                75.0,
                55.0,
                70.0,
                40.0,
                10.0,
                80.0,
                60.0
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> riskEngine.calculate("NHI-005", evidence)
        );
    }

    @Test
    void missingRequiredEvidenceShouldBeRejected() {

        Evidence evidence = createEvidence(
                null,
                75.0,
                55.0,
                70.0,
                40.0,
                10.0,
                80.0,
                60.0
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> riskEngine.calculate("NHI-006", evidence)
        );
    }

    private Evidence createEvidence(
            Double privilege,
            Double authentication,
            Double behavior,
            Double sensitiveAccess,
            Double observedExposure,
            Double misconfiguration,
            Double resourceCriticality,
            Double assetSensitivity) {

        Evidence evidence = new Evidence();

        evidence.setPrivilegeLevel(privilege);
        evidence.setAuthentication(authentication);
        evidence.setBehavior(behavior);
        evidence.setSensitiveAccess(sensitiveAccess);
        evidence.setObservedExposure(observedExposure);
        evidence.setMisconfiguration(misconfiguration);
        evidence.setResourceCriticality(resourceCriticality);
        evidence.setAssetSensitivity(assetSensitivity);

        return evidence;
    }
}