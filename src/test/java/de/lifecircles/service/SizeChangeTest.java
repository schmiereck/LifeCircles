package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.model.neural.*;
import de.lifecircles.service.partitioningStrategy.SpatialGridPartitioningStrategy;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

/** Deterministic size commands through the real brain/update pipeline, without wall-clock sleeps. */
class SizeChangeTest {
    private final SimulationConfig config = SimulationConfig.getInstance();
    private long delay;
    private double sunRate;
    private int initialCount;

    @BeforeEach void configure() {
        delay = config.getSizeChangeDelay();
        sunRate = config.getSunRayRate();
        initialCount = config.getInitialCellCount();
        config.setSizeChangeDelay(100);
        config.setSunRayRate(0);
        config.setInitialCellCount(0);
    }

    @AfterEach void restore() {
        config.setSizeChangeDelay(delay);
        config.setSunRayRate(sunRate);
        config.setInitialCellCount(initialCount);
    }

    static class SizeBrain extends PhysicsRegressionTest.ConstantBrain implements Serializable {
        private static final long serialVersionUID = 1L;
        double output = 1;
        @Override public double[] process() {
            double[] outputs = super.process();
            outputs[GlobalOutputFeature.SIZE.ordinal()] = output;
            return outputs;
        }
    }

    private Cell cell() {
        return new Cell(new Vector2D(150, 150), 10, new SizeBrain());
    }

    private void output(Cell cell, double output) {
        ((SizeBrain) cell.getBrain()).output = output;
    }

    // Independent analytic reference for a constant command, tau = 0.1 seconds. Growing is limited to 20 px/s
    // (TEILUNG.md): linear while the gap exceeds 20 · 0.1 = 2 px, then exponential. Shrinking is not limited.
    static double expected(double start, double target, double time) {
        double gap = target - start;
        if (gap > 2) {
            double linear = (gap - 2) / 20;
            if (time <= linear) return start + 20 * time;
            return target - 2 * Math.exp(-(time - linear) / 0.1);
        }
        return target + (start - target) * Math.exp(-time / 0.1);
    }

    @ParameterizedTest @ValueSource(ints = {1, 2, 6, 60, 600})
    void repeatedThinkingDoesNotPostponeSizeChange(int steps) {
        Cell cell = cell();
        for (int i = 0; i < steps; i++) {
            double before = cell.getRadiusSize();
            CellCalcService.updateCell(cell, 0.1 / steps);
            assertTrue(cell.getRadiusSize() > before);
            assertTrue(cell.getRadiusSize() < 20);
        }
        assertEquals(expected(10, 20, 0.1), cell.getRadiusSize(), 1e-11);
    }

    @Test void thinkingAloneDoesNotAdvanceSizeOrGrowth() {
        Cell adult = cell(), newborn = cell();
        newborn.startGrowthProcess();
        for (int i = 0; i < 1000; i++) {
            CellBrainService.think(adult);
            CellBrainService.think(newborn);
        }
        assertEquals(10, adult.getRadiusSize());
        assertEquals(20, adult.getTargetRadiusSize());
        assertEquals(config.getCellMinGrowRadiusSize(), newborn.getRadiusSize());
        assertEquals(0, newborn.getGrowthAge());
        assertTrue(newborn.isGrowing());
    }

    @Test void changingCommandsFollowContinuousCurrentRadius() {
        Cell cell = cell();
        double radius = 10;
        for (int i = 0; i < 100; i++) {
            double target = i % 2 == 0 ? 20 : 6;
            output(cell, i % 2 == 0 ? 1 : 0);
            CellCalcService.updateCell(cell, 0.01);
            radius = expected(radius, target, 0.01);
            assertEquals(radius, cell.getRadiusSize(), 1e-12);
        }
        output(cell, 0);
        for (int i = 0; i < 100; i++) CellCalcService.updateCell(cell, 0.01);
        assertEquals(expected(radius, 6, 1), cell.getRadiusSize(), 1e-12);
    }

    @ParameterizedTest @ValueSource(doubles = {-10, 10})
    void commandsOutsideOutputRangeAreClamped(double command) {
        Cell cell = cell();
        output(cell, command);
        CellCalcService.updateCell(cell, 0.1);
        double target = command < 0 ? config.getCellMinRadiusSize() : config.getCellMaxRadiusSize();
        assertEquals(target, cell.getTargetRadiusSize());
        assertEquals(expected(10, target, 0.1), cell.getRadiusSize(), 1e-12);
    }

    @ParameterizedTest @ValueSource(longs = {0, -1, 100})
    void zeroSimulationTimeDoesNotChangeRadius(long delay) {
        config.setSizeChangeDelay(delay);
        Cell cell = cell();
        CellCalcService.updateCell(cell, 0);
        assertEquals(10, cell.getRadiusSize());
        assertEquals(20, cell.getTargetRadiusSize());
    }

    @ParameterizedTest @ValueSource(longs = {0, -1})
    void disabledSmoothingAppliesCommandOnNextPositiveStep(long delay) {
        config.setSizeChangeDelay(delay);
        Cell cell = cell();
        CellBrainService.think(cell);
        assertEquals(10, cell.getRadiusSize());
        CellCalcService.updateCell(cell, 0.001);
        assertEquals(10.02, cell.getRadiusSize(), 1e-12, "growing only at 20 px/s (TEILUNG.md)");
        CellCalcService.updateCell(cell, 1);
        assertEquals(20, cell.getRadiusSize());
        output(cell, 0);
        CellCalcService.updateCell(cell, 0.001);
        assertEquals(6, cell.getRadiusSize(), "shrinking at once");
    }

    @Test void largeStepDoesNotOvershootAndTinyStepStillAdvances() {
        Cell cell = cell();
        CellCalcService.updateCell(cell, 1e-12);
        assertTrue(cell.getRadiusSize() > 10);
        CellCalcService.updateCell(cell, 100);
        assertEquals(20, cell.getRadiusSize());
        output(cell, 0);
        CellCalcService.updateCell(cell, 100);
        assertEquals(6, cell.getRadiusSize());
    }

    @ParameterizedTest @ValueSource(ints = {1, 4, 64})
    void newbornReachesTargetAndThenUsesAdultSmoothing(int steps) {
        Cell cell = cell();
        cell.startGrowthProcess();
        double minimum = config.getCellMinGrowRadiusSize();
        for (int i = 1; i <= steps; i++) {
            CellCalcService.updateCell(cell, SimulationConfig.CELL_GROWTH_DURATION / steps);
            assertEquals(minimum + (20 - minimum) * i / steps, cell.getRadiusSize(), 1e-12);
            assertEquals(i < steps, cell.isGrowing());
        }
        output(cell, 0);
        CellCalcService.updateCell(cell, 0.1);
        assertEquals(expected(20, 6, 0.1), cell.getRadiusSize(), 1e-12);
    }

    @Test void changingNewbornTargetDoesNotRestartGrowthClock() {
        Cell cell = cell();
        cell.startGrowthProcess();
        for (int i = 1; i <= 4; i++) {
            output(cell, i % 2);
            double before = cell.getRadiusSize();
            CellCalcService.updateCell(cell, 0.25);
            double target = i % 2 == 1 ? 20 : 6;
            double minimum = config.getCellMinGrowRadiusSize();
            // A raised target does not let the radius jump: at most 20 px/s · 0.25 s (TEILUNG.md).
            assertEquals(Math.min(minimum + (target - minimum) * i / 4, before + 5), cell.getRadiusSize(), 1e-12);
        }
        assertFalse(cell.isGrowing());
        assertEquals(6, cell.getRadiusSize());
    }

    @Test void savedCellContinuesFromCurrentRadius() throws Exception {
        Cell cell = cell();
        CellCalcService.updateCell(cell, 0.05);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) { out.writeObject(cell); }
        Cell loaded;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            loaded = (Cell) in.readObject();
        }
        CellCalcService.updateCell(loaded, 0.05);
        assertEquals(expected(10, 20, 0.1), loaded.getRadiusSize(), 1e-12);
    }

    @Test void worldUpdatesRadiusAndActorPositionsEveryStep() {
        Environment world = new Environment(300, 300);
        Cell cell = cell();
        world.addCell(cell);
        SpatialGridPartitioningStrategy grid = new SpatialGridPartitioningStrategy(300, 300, 60);
        for (int i = 1; i <= 60; i++) {
            grid.build(world.getCellList());
            world.update(1.0 / 60, grid);
            assertEquals(expected(10, 20, i / 60.0), cell.getRadiusSize(), 1e-11);
            assertEquals(1, world.getCellList().size());
            for (SensorActor actor : cell.getSensorActors()) {
                assertEquals(cell.getRadiusSize(), actor.getCachedPosition().distance(cell.getPosition()), 1e-11);
            }
        }
    }
}
