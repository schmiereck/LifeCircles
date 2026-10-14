package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.model.neural.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class TopSensorTest {
    private Cell cell() {
        return new Cell(new Vector2D(100, 100), 10, new PhysicsRegressionTest.ConstantBrain());
    }

    private void assertTopInput(Cell cell, int expected) {
        assertEquals(expected, cell.getTopSensorIndex());
        double[] inputs = CellBrainService.generateInputs(cell);
        int selected = 0;
        for (int i = 0; i < 12; i++) {
            double value = inputs[GlobalInputFeature.values().length + i * SensorInputFeature.values().length
                    + SensorInputFeature.MY_ACTOR_TOP_POSITION.ordinal()];
            assertEquals(i == expected ? 1 : 0, value);
            if (value == 1) selected++;
        }
        assertEquals(1, selected);
    }

    @Test void freshCellSelectsUpperSensorBeforeFirstCacheUpdate() {
        Cell cell = cell();
        assertTopInput(cell, 9); // Sensor 3 points down; sensor 9 points up.
        for (SensorActor actor : cell.getSensorActors()) assertNull(actor.getCachedPosition());
    }

    @ParameterizedTest @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11})
    void rotationUsesCurrentOrientationEvenWithOldOrCorruptedCache(int rotationStep) {
        Cell cell = cell();
        for (SensorActor actor : cell.getSensorActors()) actor.updateCachedPosition();
        // An unrelated cache value must not redefine the meaning of 'up'.
        cell.getSensorActors().getFirst().getCachedPosition().setY(-999);
        cell.setRotation(rotationStep * Math.PI / 6);
        assertTopInput(cell, Math.floorMod(9 - rotationStep, 12));
        assertEquals(-999, cell.getSensorActors().getFirst().getCachedPosition().getY());
    }

    @ParameterizedTest @ValueSource(doubles = {-1000, -1, 0, 1, 299, 300, 1000, 1e16})
    void upperDirectionIsIndependentOfPositionAndSize(double y) {
        Cell cell = cell(); cell.setPosition(new Vector2D(295, y));
        cell.startGrowthProcess();
        assertTopInput(cell, 9);
        cell.setRealRadiusSize(20);
        assertTopInput(cell, 9);
    }

    @Test void eitherSideOfSensorBoundaryChangesTheSelectedSensor() {
        Cell cell = cell();
        cell.setRotation(Math.PI / 12 - 1e-6); assertTopInput(cell, 9);
        cell.setRotation(Math.PI / 12 + 1e-6); assertTopInput(cell, 8);
    }

    @Test void exactTieUsesLowestIndex() {
        Cell cell = cell();
        cell.setRotation(Math.PI / 12); assertTopInput(cell, 8);
        cell.setRotation(-5 * Math.PI / 12); assertTopInput(cell, 0); // 0 and 11 tie.
    }

    @Test void unwrappedNegativeRotationsRetainUpperDirection() {
        Cell cell = cell();
        for (int turns = -4; turns <= 4; turns++) {
            cell.setRotation(turns * 2 * Math.PI - Math.PI / 2);
            assertTopInput(cell, 0);
        }
    }
}
