package de.lifecircles.service;

import de.lifecircles.model.*;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SunIndexRegressionTest {
    private final SimulationConfig config = SimulationConfig.getInstance();
    private double rate, spacing;
    @BeforeEach void save() { rate = config.getSunRayRate(); spacing = config.getSunRaySpacingPx(); }
    @AfterEach void restore() { config.setSunRayRate(rate); config.setSunRaySpacingPx(spacing); }

    private Cell cell(double x, double y, double r) {
        Cell cell = new Cell(new Vector2D(x, y), r, new PhysicsRegressionTest.ConstantBrain()); cell.setEnergy(.1); return cell;
    }
    private void compare(EnergySunCalcService scan, EnergySunCalcService index, List<Cell> cells,
                         List<Blocker> blockers, double width, double height, double dt) {
        for (Cell c : cells) c.setEnergy(.1);
        List<SunRay> expected = scan.calculateSunEnergy(cells, blockers, width, height, config, dt);
        double[] energy = cells.stream().mapToDouble(Cell::getEnergy).toArray();
        boolean[] hits = new boolean[cells.size()];
        for (int i = 0; i < hits.length; i++) { hits[i] = cells.get(i).isSunRayHit(); cells.get(i).setEnergy(.1); }
        List<SunRay> actual = index.calculateSunEnergy(cells, blockers, width, height, config, dt);
        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i).getStart().getX(), actual.get(i).getStart().getX());
            assertEquals(expected.get(i).getEnd().getX(), actual.get(i).getEnd().getX());
            assertEquals(expected.get(i).getEnd().getY(), actual.get(i).getEnd().getY());
        }
        for (int i = 0; i < cells.size(); i++) {
            assertEquals(energy[i], cells.get(i).getEnergy()); assertEquals(hits[i], cells.get(i).isSunRayHit());
        }
    }

    @Test void reusedIndexMatchesScanAfterMovementResizeRemovalDarknessAndFallback() {
        config.setSunRayRate(120); config.setSunRaySpacingPx(20);
        var scan = new EnergySunCalcService(new Random(68), false);
        var index = new EnergySunCalcService(new Random(68), true);
        Random positions = new Random(87); List<Cell> cells = new ArrayList<>();
        for (int i = 0; i < 900; i++) cells.add(cell(positions.nextDouble() * 2200 - 100,
                positions.nextDouble() * 1800 - 100, 6 + positions.nextDouble() * 14));
        List<Blocker> blockers = new ArrayList<>();
        blockers.add(new Blocker(new Vector2D(1000, 500), 1e8, 20, Color.GRAY, Blocker.BlockerType.PLATFORM));
        compare(scan, index, cells, blockers, 2000, 1600, .1);
        cells.subList(0, 350).clear(); blockers.clear();
        for (Cell c : cells) {
            c.setPosition(new Vector2D(positions.nextDouble() * 2200 - 100, positions.nextDouble() * 1800 - 100));
            c.setRealRadiusSize(6 + positions.nextDouble() * 14);
        }
        compare(scan, index, cells, blockers, 2000, 1600, .2);
        config.setSunRayRate(0);
        compare(scan, index, cells, blockers, 2000, 1600, .01);
        config.setSunRayRate(120);
        compare(scan, index, cells.subList(0, 2), blockers, 2000, 1600, .1);
        compare(scan, index, cells, blockers, 2000, 1600, .1);
        // Rebuild head/depth dimensions as both world size and sun angle change.
        compare(scan, index, cells, blockers, 3000, 2400, 29.49);
    }

    @Test void equalHitsAcrossDepthRowsKeepFullScanPriorityIncludingTangency() {
        config.setSunRayRate(10); config.setSunRaySpacingPx(300);
        double dx = Math.sin(Math.toRadians(360)), dy = Math.cos(Math.toRadians(360));
        double minX = -600 * dx / dy, maxX = 300 - 600 * dx / dy;
        double startX = minX + .5 * (maxX - minX);
        Cell deeperBound = cell(startX, 276, 12); // Front hit t=264, lower bound in row 2.
        Cell tangent = cell(startX + 20, 264, 20); // Same hit t=264, lower bound in row 1.
        List<Cell> cells = new ArrayList<>(List.of(deeperBound, tangent));
        for (int i = 0; i < 300; i++) cells.add(cell(20, 550, 6));
        for (boolean reverse : new boolean[]{false, true}) {
            if (reverse) Collections.swap(cells, 0, 1);
            Random fixedScan = new Random(1) { @Override public double nextDouble() { return .5; } };
            Random fixedIndex = new Random(1) { @Override public double nextDouble() { return .5; } };
            compare(new EnergySunCalcService(fixedScan, false), new EnergySunCalcService(fixedIndex, true),
                    cells, List.of(), 300, 600, 30);
            assertTrue(cells.get(0).isSunRayHit()); assertFalse(cells.get(1).isSunRayHit());
        }
    }

    @Test void shadeBoundsPreserveCellPriorityAtAnEqualHitAndIgnoreBlockerContainingOrigin() {
        config.setSunRayRate(10); config.setSunRaySpacingPx(300);
        double dx = Math.sin(Math.toRadians(360)), dy = Math.cos(Math.toRadians(360));
        double minX = -600 * dx / dy, maxX = 300 - 600 * dx / dy;
        double startX = minX + .5 * (maxX - minX);
        List<Cell> cells = new ArrayList<>(); cells.add(cell(startX, 276, 12));
        for (int i = 0; i < 300; i++) cells.add(cell(20, 550, 6));
        for (double blockerY : new double[]{274, 273, 275, 0}) {
            var blocker = new Blocker(new Vector2D(startX, blockerY), 40, 20, Color.GRAY, Blocker.BlockerType.PLATFORM);
            Random fixedScan = new Random(1) { @Override public double nextDouble() { return .5; } };
            Random fixedIndex = new Random(1) { @Override public double nextDouble() { return .5; } };
            compare(new EnergySunCalcService(fixedScan, false), new EnergySunCalcService(fixedIndex, true),
                    cells, List.of(blocker), 300, 600, 30);
            assertEquals(blockerY != 273, cells.getFirst().isSunRayHit());
        }
    }

    @Test void startingInsideCircleAndWorldExitAreIdenticalToFullScan() {
        config.setSunRayRate(10); config.setSunRaySpacingPx(300);
        double dx = Math.sin(Math.toRadians(360)), dy = Math.cos(Math.toRadians(360));
        double minX = -600 * dx / dy, maxX = 300 - 600 * dx / dy;
        double startX = minX + .5 * (maxX - minX);
        List<Cell> cells = new ArrayList<>(); cells.add(cell(startX, 0, 20));
        cells.add(cell(startX, 610, 10)); // Entry exactly at tMax must not be illuminated.
        for (int i = 0; i < 300; i++) cells.add(cell(20, 550, 6));
        for (int step = 0; step < 2; step++) {
            Random fixedScan = new Random(1) { @Override public double nextDouble() { return .5; } };
            Random fixedIndex = new Random(1) { @Override public double nextDouble() { return .5; } };
            compare(new EnergySunCalcService(fixedScan, false), new EnergySunCalcService(fixedIndex, true),
                    cells, List.of(), 300, 600, 30);
            if (step == 0) { assertTrue(cells.getFirst().isSunRayHit()); cells.removeFirst(); }
            else assertFalse(cells.getFirst().isSunRayHit());
        }
    }
}
