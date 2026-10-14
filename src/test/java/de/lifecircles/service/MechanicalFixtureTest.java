package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.model.neural.*;
import de.lifecircles.service.trainStrategy.TestTrainStrategyUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class MechanicalFixtureTest {
    @ParameterizedTest @ValueSource(doubles = {6, 10, 12, 16, 20})
    void prescribedRadiusSurvivesThinkingAndSizeSmoothing(double radius) {
        Environment world = new Environment(300, 300);
        TestTrainStrategyUtils.createAndAddCell(world, 100, 100, radius, true);
        Cell cell = world.getCellList().getFirst();
        assertEquals(radius, cell.getTargetRadiusSize(), 1e-12);
        assertTrue(cell.getBrain().getOutputValue(GlobalOutputFeature.SIZE.ordinal()) >= 0);
        assertTrue(cell.getBrain().getOutputValue(GlobalOutputFeature.SIZE.ordinal()) <= 1);
        for (int i = 0; i < 120; i++) CellCalcService.updateCell(cell, 1.0 / 60);
        assertEquals(radius, cell.getRadiusSize(), 1e-12);
    }

    @Test void forceCommandsAreActiveBeforeFirstPhysicsStepAndInputsAreCurrent() {
        Environment world = new Environment(300, 300);
        TestTrainStrategyUtils.createAndAddCell(world, 100, 100, 12, true);
        Cell cell = world.getCellList().getFirst();
        assertTrue(cell.getSensorActors().stream().allMatch(actor -> actor.getForceStrength() < 0));
        cell.setEnergy(0.23); CellBrainService.think(cell);
        assertEquals(0.23, cell.getBrain().getInputValue(GlobalInputFeature.MY_CELL_ENERGY.ordinal()));
    }
}
