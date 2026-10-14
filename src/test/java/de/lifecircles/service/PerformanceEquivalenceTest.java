package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.service.partitioningStrategy.SpatialGrid;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PerformanceEquivalenceTest {
    @Test void projectedSunIndexMatchesFullScanAcrossAnglesObstaclesAndTies() {
        SimulationConfig config = SimulationConfig.getInstance();
        double rate = config.getSunRayRate(), spacing = config.getSunRaySpacingPx();
        try {
            config.setSunRaySpacingPx(10);
            Random positions = new Random(781);
            List<Cell> cells = new ArrayList<>(); List<Blocker> blockers = new ArrayList<>();
            for (int i = 0; i < 600; i++) cells.add(new Cell(new Vector2D(positions.nextDouble() * 2200 - 100,
                    positions.nextDouble() * 1200 - 100), 3 + positions.nextDouble() * 17, new PhysicsRegressionTest.ConstantBrain()));
            cells.add(new Cell(new Vector2D(1000, 200), 20, new PhysicsRegressionTest.ConstantBrain()));
            cells.add(new Cell(new Vector2D(1000, 200), 20, new PhysicsRegressionTest.ConstantBrain()));
            for (int i = 0; i < 50; i++) blockers.add(new Blocker(new Vector2D(positions.nextDouble() * 2200 - 100,
                    positions.nextDouble() * 1200 - 100), 5 + positions.nextDouble() * 250,
                    5 + positions.nextDouble() * 60, Color.GRAY, Blocker.BlockerType.PLATFORM));
            blockers.add(new Blocker(new Vector2D(1000, 980), 2200, 40, Color.GRAY, Blocker.BlockerType.GROUND));
            EnergySunCalcService indexed = new EnergySunCalcService(new Random(79), true);
            EnergySunCalcService scanned = new EnergySunCalcService(new Random(79), false);
            for (double dt : new double[]{.1, 60, 90, 150, 180, 240, .01}) {
                config.setSunRayRate(2 / dt); // Enough rays to exercise the index at every angle.
                for (Cell cell : cells) cell.setEnergy(.1);
                var expected = scanned.calculateSunEnergy(cells, blockers, 2000, 1000, config, dt);
                double[] energies = cells.stream().mapToDouble(Cell::getEnergy).toArray();
                boolean[] hits = new boolean[cells.size()];
                for (int i = 0; i < hits.length; i++) { hits[i] = cells.get(i).isSunRayHit(); cells.get(i).setEnergy(.1); }
                var actual = indexed.calculateSunEnergy(cells, blockers, 2000, 1000, config, dt);
                assertEquals(expected.size(), actual.size()); assertFalse(actual.isEmpty());
                for (int i = 0; i < expected.size(); i++) {
                    assertEquals(expected.get(i).getStart().getX(), actual.get(i).getStart().getX());
                    assertEquals(expected.get(i).getEnd().getX(), actual.get(i).getEnd().getX());
                    assertEquals(expected.get(i).getEnd().getY(), actual.get(i).getEnd().getY());
                }
                for (int i = 0; i < cells.size(); i++) {
                    assertEquals(energies[i], cells.get(i).getEnergy()); assertEquals(hits[i], cells.get(i).isSunRayHit());
                }
            }
        } finally { config.setSunRayRate(rate); config.setSunRaySpacingPx(spacing); }
    }

    @Test void sharedBucketNeighborsTrackMigrationDeathAndPeriodicSeams() {
        SpatialGrid grid = new SpatialGrid(240, 240, 60);
        Cell a = new Cell(new Vector2D(1, 10), 10, new PhysicsRegressionTest.ConstantBrain());
        Cell b = new Cell(new Vector2D(2, 10), 10, new PhysicsRegressionTest.ConstantBrain());
        Cell seam = new Cell(new Vector2D(239, 10), 10, new PhysicsRegressionTest.ConstantBrain());
        grid.addCells(List.of(a, b, seam));
        assertSame(grid.getNeighbors(a), grid.getNeighbors(b));
        assertEquals(Set.of(a, b, seam), new HashSet<>(grid.getNeighbors(a)));
        b.setPosition(new Vector2D(125, 125)); grid.addCells(List.of(a, b));
        assertEquals(List.of(a), grid.getNeighbors(a));
        assertEquals(List.of(b), grid.getNeighbors(b)); assertTrue(grid.getNeighbors(seam).isEmpty());
        grid.addCells(List.of()); assertTrue(grid.getNeighbors(a).isEmpty());
    }
}
