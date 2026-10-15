package de.lifecircles.model.neural;

import de.lifecircles.model.*;
import de.lifecircles.service.SimulationConfig;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class BrainInputScalingTest {
    private final SimulationConfig config = SimulationConfig.getInstance();
    private boolean previous;
    @BeforeEach void save() { previous = config.isNormalizeBrainInputs(); }
    @AfterEach void restore() { config.setNormalizeBrainInputs(previous); }

    @Test void legacyInputsKeepPhysicalUnits() {
        config.setNormalizeBrainInputs(false);
        assertEquals(120,CellBrainService.ageInput(120));
        assertEquals(-90,CellBrainService.forceInput(-90));
    }

    @Test void normalizedInputsBoundAgeAndBothForceSigns() {
        config.setNormalizeBrainInputs(true);
        assertEquals(0,CellBrainService.ageInput(0));
        assertEquals(.5,CellBrainService.ageInput(60));
        assertTrue(CellBrainService.ageInput(10000)<1);
        assertEquals(1,CellBrainService.forceInput(config.getCellActorMaxRepulsiveForceStrength()));
        assertEquals(-1,CellBrainService.forceInput(-config.getCellActorMaxAttractiveForceStrength()));
        assertEquals(0,CellBrainService.forceInput(0));
    }

    @Test void generatedInputsUseScalingForOwnAndSensedAgeAndForce() {
        config.setNormalizeBrainInputs(true);
        Cell own = CellFactory.createCell(new Vector2D(100,100),10,.01,0,0);
        Cell sensed = CellFactory.createCell(new Vector2D(101,100),10,.01,0,0);
        own.setAge(60); sensed.setAge(120);
        var actor = own.getSensorActors().getFirst(); var other = sensed.getSensorActors().getFirst();
        actor.setForceStrength(config.getCellActorMaxRepulsiveForceStrength());
        other.setForceStrength(-config.getCellActorMaxAttractiveForceStrength());
        actor.setSensedCell(sensed); actor.setSensedActor(other);
        double[] inputs = CellBrainService.generateInputs(own,new Environment(300,300));
        int base = GlobalInputFeature.values().length;
        assertEquals(.5,inputs[GlobalInputFeature.MY_CELL_AGE.ordinal()]);
        assertEquals(2.0/3,inputs[base+SensorInputFeature.SENSED_CELL_AGE.ordinal()],1e-15);
        assertEquals(1,inputs[base+SensorInputFeature.MY_ACTOR_FORCE_STRENGTH.ordinal()]);
        assertEquals(-1,inputs[base+SensorInputFeature.SENSED_ACTOR_FORCE_STRENGTH.ordinal()]);
    }
}
