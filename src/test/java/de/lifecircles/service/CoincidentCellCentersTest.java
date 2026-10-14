package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.service.partitioningStrategy.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/** Exercises every repulsion entry point with degenerate, finite geometry. */
class CoincidentCellCentersTest {
    enum Variant {
        LINEAR, LEGACY_LINEAR, NON_LINEAR;

        void apply(Environment world) {
            PartitioningStrategy grid = new SpatialGridPartitioningStrategy(300, 300, 60);
            grid.build(world.getCellList());
            switch (this) {
                case LINEAR -> RepulsionCellCalcService.processRepulsiveForces(world.getCellList(), grid);
                case LEGACY_LINEAR -> RepulsionCellCalcService.processRepulsiveForces_x(world.getCellList(), 1.0 / 60, grid);
                case NON_LINEAR -> RepulsionCellCalcService.processRepulsiveForces_nonLin(world.getCellList(), 1.0 / 60, grid);
            }
            world.getCellList().forEach(Cell::updateForce);
        }
    }

    private Environment world(double... xs) {
        Environment world = new Environment(300, 300);
        for (double x : xs) world.addCell(new Cell(new Vector2D(x, 100), 10, null));
        return world;
    }

    private void assertFiniteAndBalanced(Environment world) {
        double vx = 0, vy = 0;
        for (Cell cell : world.getCellList()) {
            assertTrue(Double.isFinite(cell.getPosition().getX()));
            assertTrue(Double.isFinite(cell.getPosition().getY()));
            assertTrue(Double.isFinite(cell.getVelocity().getX()));
            assertTrue(Double.isFinite(cell.getVelocity().getY()));
            assertTrue(Double.isFinite(cell.getAngularVelocity()));
            vx += cell.getVelocity().getX(); vy += cell.getVelocity().getY();
        }
        assertEquals(0, vx, 1e-9);
        assertEquals(0, vy, 1e-9);
    }

    @ParameterizedTest @EnumSource(Variant.class)
    void exactCoincidenceProducesOppositeNonzeroForces(Variant variant) {
        Environment world = world(100, 100);
        variant.apply(world);
        assertFiniteAndBalanced(world);
        assertTrue(world.getCellList().get(0).getVelocity().getX() < 0);
        assertTrue(world.getCellList().get(1).getVelocity().getX() > 0);
        double expected = SimulationConfig.getInstance().getCellRepulsionStrength() * 20 * 0.08;
        if (variant == Variant.NON_LINEAR) expected *= 4;
        assertEquals(expected, world.getCellList().get(1).getVelocity().getX(), 1e-10,
                "Each neighbor contributes once, without double counting the reverse pair");
    }

    @ParameterizedTest @EnumSource(Variant.class)
    void tinyDistancesIncludingSubnormalValuesRemainFinite(Variant variant) {
        for (double distance : new double[]{Double.MIN_VALUE, 1e-200, 1e-14, 1e-9}) {
            Environment world = world(0, distance);
            variant.apply(world);
            assertFiniteAndBalanced(world);
            assertTrue(world.getCellList().get(0).getVelocity().getX() < 0);
            assertTrue(world.getCellList().get(1).getVelocity().getX() > 0);
        }
    }

    @ParameterizedTest @EnumSource(Variant.class)
    void coincidentPeriodicImagesAlsoSeparate(Variant variant) {
        Environment world = world(0, 300);
        variant.apply(world);
        assertFiniteAndBalanced(world);
        assertTrue(world.getCellList().get(0).getVelocity().getX() < 0);
        assertTrue(world.getCellList().get(1).getVelocity().getX() > 0);
    }

    @ParameterizedTest @EnumSource(Variant.class)
    void severalCoincidentCellsSeparateOverRepeatedSteps(Variant variant) {
        Environment world = world(100, 100, 100, 100, 100);
        for (int step = 0; step < 60; step++) {
            variant.apply(world);
            assertFiniteAndBalanced(world);
            for (Cell cell : world.getCellList()) {
                cell.setPosition(cell.getPosition().add(cell.getVelocity().multiply(1.0 / 60)));
            }
        }
        List<Cell> cells = world.getCellList();
        for (int i = 0; i < cells.size(); i++) for (int j = i + 1; j < cells.size(); j++) {
            assertTrue(cells.get(i).getPosition().getShortestDirectionWithWrapAround(
                    cells.get(j).getPosition(), 300, 300).length() > 1e-6);
        }
    }
}
