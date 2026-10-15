package de.lifecircles.service;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class StandingStructureTest {
    @ParameterizedTest @ValueSource(doubles={.016666666666666666,.008333333333333333})
    void fullGravityAllowsTwelveRowsWithSufficientCrossSection(double dt) {
        try (var saved = new MechanicsLab.Settings()) {
            var result = StandingStructureLab.run(new MechanicsLab.Parameters("current",90,125,.8,.8,.25),5,12,20,dt);
            assertEquals(1,result.component());
            assertTrue(result.heightRatio()>.95); assertTrue(result.widthRatio()<1.02);
            assertTrue(result.speed()<.001); assertTrue(result.overlap()<.1);
        }
    }

    @ParameterizedTest @ValueSource(doubles={.016666666666666666,.008333333333333333})
    void samePhysicsDoesNotKeepNarrowColumnUpright(double dt) {
        try (var saved = new MechanicsLab.Settings()) {
            var result = StandingStructureLab.run(new MechanicsLab.Parameters("current",90,125,.8,.8,.25),1,8,12,dt);
            assertEquals(1,result.component());
            assertTrue(result.heightRatio()<.6); assertTrue(result.widthRatio()>1.8);
        }
    }
}
