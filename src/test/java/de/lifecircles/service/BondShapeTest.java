package de.lifecircles.service;

import de.lifecircles.model.*;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Shape metric and snapshot of bonded bodies (ZoneSeasonStudyTest, ZELLVERBINDUNGEN.md). */
class BondShapeTest {
    static final CellType RED = CellAdhesionTest.RED, BODY = CellAdhesionTest.BODY;

    static Environment world() {
        Environment world = new Environment(400, 400); world.addGroundBlocker();
        return world;
    }
    static double ground() { return 400 - Environment.GroundBlockerHeight; }
    static Cell at(Environment world, double x, double y) {
        Cell c = CellAdhesionTest.cell(x, 20, BODY, RED, 0);
        c.setPosition(new Vector2D(x, y));
        world.addCell(c);
        return c;
    }
    /** Upward actor (index 9, 270°) of the lower cell to the downward actor (index 3, 90°) of the upper one. */
    static void stack(Cell lower, Cell upper) { SensorActor.bond(lower.getSensorActors().get(9), upper.getSensorActors().get(3), -90); }
    static Cell[] column(Environment world, double x, double base, int n) {
        Cell[] cells = new Cell[n];
        for (int i = 0; i < n; i++) {
            cells[i] = at(world, x, base - 20 - 40 * i);
            if (i > 0) stack(cells[i - 1], cells[i]);
        }
        return cells;
    }

    @Test void standingColumnRisesByItsHeightAlsoOnTopOfLooseCells() {
        Environment world = world();
        column(world, 100, ground(), 3);
        at(world, 160, ground() - 20); // loose neighbour beside the column, outside its extent
        List<List<Cell>> bodies = ZoneSeasonStudyTest.bondedBodies(world);
        assertEquals(1, bodies.size());
        double[] shape = ZoneSeasonStudyTest.bodyShape(world, bodies.get(0));
        assertEquals(120, shape[0], 1e-9); assertEquals(120, shape[1], 1e-9);

        Environment heap = world();
        at(heap, 100, ground() - 20); at(heap, 100, ground() - 60);
        column(heap, 100, ground() - 80, 3);
        assertArrayEquals(new double[]{120, 120}, ZoneSeasonStudyTest.bodyShape(heap, ZoneSeasonStudyTest.bondedBodies(heap).get(0)), 1e-9,
                "rise counts from the loose cells it stands on");
    }

    @Test void buriedBodyHasNegativeRiseAndCeilingsAreIgnored() {
        Environment world = world();
        Cell a = at(world, 100, ground() - 20), b = at(world, 140, ground() - 20);
        SensorActor.bond(a.getSensorActors().get(0), b.getSensorActors().get(6), -90);
        world.addSunBlocker(120, 100, 200); // platform high above the body
        assertArrayEquals(new double[]{40, 40}, ZoneSeasonStudyTest.bodyShape(world, ZoneSeasonStudyTest.bondedBodies(world).get(0)), 1e-9);
        at(world, 120, ground() - 20 - Math.sqrt(1200)); // loose cell resting in the dip on both
        assertEquals(-Math.sqrt(1200), ZoneSeasonStudyTest.bodyShape(world, ZoneSeasonStudyTest.bondedBodies(world).get(0))[1], 1e-9);
    }

    @Test void bodiesAcrossTheWorldBorderAreMeasuredAndDrawnWithoutWrapLines() {
        Environment world = world();
        Cell a = at(world, 390, ground() - 20), b = at(world, 34, ground() - 20); // 4 px gap across x = 0
        SensorActor.bond(a.getSensorActors().get(0), b.getSensorActors().get(6), -90);
        assertArrayEquals(new double[]{40, 40}, ZoneSeasonStudyTest.bodyShape(world, ZoneSeasonStudyTest.bondedBodies(world).get(0)), 1e-9);
        assertEquals(1, ZoneSeasonStudyTest.bondedBodies(world).size());

        BufferedImage image = BondSnapshotImage.render(world, new Vector2D(10, 200), 400, 400, 1, List.of(a, b), null);
        // The bond runs from x = 410 = 10 to 14, i.e. image x 200 to 204, at the cells' height.
        int y = (int) Math.round(ground() - 20);
        assertEquals(BondSnapshotImage.BOND.getRGB(), image.getRGB(202, y));
        // No line through the rest of the view, which a wrap-unaware drawing would produce.
        assertEquals(BondSnapshotImage.BACKGROUND.getRGB(), image.getRGB(300, y));
        assertEquals(BondSnapshotImage.BACKGROUND.getRGB(), image.getRGB(100, y));
    }
}
