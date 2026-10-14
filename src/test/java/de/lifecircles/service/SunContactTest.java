package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.model.neural.*;
import de.lifecircles.service.partitioningStrategy.SpatialGridPartitioningStrategy;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SunContactTest {
    private final SimulationConfig config = SimulationConfig.getInstance();
    private double rate, spacing;
    private int initialCount;
    private EnergySunCalcService sun;

    @BeforeEach void configure() {
        rate = config.getSunRayRate(); spacing = config.getSunRaySpacingPx(); initialCount = config.getInitialCellCount();
        config.setSunRayRate(10); config.setSunRaySpacingPx(10); config.setInitialCellCount(0);
        // All emitted rays follow the middle path; no statistical hit assertions.
        sun = new EnergySunCalcService(new Random(1) {
            @Override public double nextDouble() { return 0.5; }
        });
    }
    @AfterEach void restore() {
        config.setSunRayRate(rate); config.setSunRaySpacingPx(spacing); config.setInitialCellCount(initialCount);
    }

    private Cell illuminatedCell() {
        double x = 150 - 100 * Math.tan(Math.toRadians(35));
        Cell cell = new Cell(new Vector2D(x, 200), 10, new PhysicsRegressionTest.ConstantBrain());
        cell.setEnergy(0.2);
        return cell;
    }
    private List<SunRay> calculate(List<Cell> cells, List<Blocker> blockers) {
        return sun.calculateSunEnergy(cells, blockers, 300, 300, config, 0.01);
    }
    private double brainSignal(Cell cell) {
        CellBrainService.think(cell);
        return CellBrainService.generateInputs(cell)[GlobalInputFeature.MY_CELL_SUNRAY_HIT.ordinal()];
    }

    @Test void actualHitsThenDarknessThenHitsUpdateFlagAndBrainInput() {
        Cell cell = illuminatedCell();
        assertFalse(calculate(List.of(cell), List.of()).isEmpty());
        assertTrue(cell.isSunRayHit()); assertEquals(1, brainSignal(cell));
        assertTrue(cell.getEnergy() > 0.2);
        double energyAfterHit = cell.getEnergy();
        config.setSunRayRate(0);
        for (int i = 0; i < 3; i++) {
            assertTrue(calculate(List.of(cell), List.of()).isEmpty());
            assertFalse(cell.isSunRayHit()); assertEquals(0, brainSignal(cell));
            assertEquals(energyAfterHit, cell.getEnergy());
        }
        config.setSunRayRate(10);
        calculate(List.of(cell), List.of());
        assertTrue(cell.isSunRayHit()); assertEquals(1, brainSignal(cell));
    }

    @Test void obstacleClearsPreviousContactDespiteEmittedRays() {
        Cell cell = illuminatedCell();
        calculate(List.of(cell), List.of()); assertTrue(cell.isSunRayHit());
        double energy = cell.getEnergy();
        Blocker shade = new Blocker(new Vector2D(150 - 200 * Math.tan(Math.toRadians(35)), 100),
                20, 20, Color.GRAY, Blocker.BlockerType.PLATFORM);
        assertFalse(calculate(List.of(cell), List.of(shade)).isEmpty());
        assertFalse(cell.isSunRayHit()); assertEquals(0, brainSignal(cell));
        assertEquals(energy, cell.getEnergy());
    }

    @Test void onlyCellsActuallyHitInThisStepKeepContact() {
        Cell hit = illuminatedCell();
        Cell missed = new Cell(new Vector2D(250, 100), 10, new PhysicsRegressionTest.ConstantBrain());
        hit.notifySunRayHit(); missed.notifySunRayHit();
        calculate(List.of(hit, missed), List.of());
        assertTrue(hit.isSunRayHit()); assertFalse(missed.isSunRayHit());
        assertEquals(1, brainSignal(hit)); assertEquals(0, brainSignal(missed));
    }

    @Test void fractionalRayAccumulatorDoesNotPreserveOldContact() {
        config.setSunRayRate(0.001);
        Cell cell = illuminatedCell(); cell.notifySunRayHit();
        assertTrue(calculate(List.of(cell), List.of()).isEmpty());
        assertFalse(cell.isSunRayHit());
    }

    @Test void worldBrainSeesClearedContactInSameUpdate() {
        config.setSunRayRate(0);
        class RecordingBrain extends PhysicsRegressionTest.ConstantBrain {
            double sunlight = -1;
            @Override public void setInputs(double[] inputs) { sunlight = inputs[GlobalInputFeature.MY_CELL_SUNRAY_HIT.ordinal()]; }
        }
        RecordingBrain brain = new RecordingBrain();
        Environment world = new Environment(300, 300);
        Cell cell = new Cell(new Vector2D(150, 100), 10, brain); world.addCell(cell);
        cell.notifySunRayHit();
        SpatialGridPartitioningStrategy grid = new SpatialGridPartitioningStrategy(300, 300, 60);
        grid.build(world.getCellList()); world.update(1.0 / 60, grid);
        assertFalse(cell.isSunRayHit()); assertEquals(0, brain.sunlight);
    }
}
