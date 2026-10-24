package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.service.partitioningStrategy.SpatialGridPartitioningStrategy;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/** Gravity and friction from the GUI act in the running world. */
class MechanicsSettingsTest {
    final SimulationConfig config = SimulationConfig.getInstance();
    double gravity, viscosity;
    @BeforeEach void save() { gravity = config.getGravity(); viscosity = config.getViscosity(); }
    @AfterEach void restore() { config.setGravity(gravity); config.setViscosity(viscosity); }

    /** Downward speed of a free cell after one simulated second. */
    double fall(double gravity, double viscosity) {
        config.setGravity(gravity); config.setViscosity(viscosity);
        Environment world = new Environment(800, 800);
        Cell cell = new Cell(new Vector2D(400, 100), 10, new PhysicsRegressionTest.ConstantBrain());
        cell.setEnergy(1);
        world.addCell(cell);
        SimulationStepPolicy policy = new SimulationStepPolicy() {
            @Override public boolean useSunlight() { return false; }
            @Override public boolean allowReproduction(Cell c) { return false; }
        };
        var grid = new SpatialGridPartitioningStrategy(800, 800, 80);
        for (int step = 0; step < 60; step++) { grid.build(world.getCellList()); world.update(1.0 / 60, grid, policy); }
        return cell.getVelocity().getY();
    }

    @Test void gravityAndFrictionChangeTheFall() {
        double standard = fall(SimulationConfig.GRAVITY, .8);
        assertTrue(standard > 0, "falls down (positive y)");
        assertEquals(0, fall(0, .8), 1e-9, "no gravity, no fall");
        assertTrue(fall(2 * SimulationConfig.GRAVITY, .8) > 1.9 * standard, "double gravity, about double speed");
        assertTrue(fall(SimulationConfig.GRAVITY, 3) < standard, "more friction, slower");
        assertThrows(IllegalArgumentException.class, () -> config.setGravity(-1));
        assertThrows(IllegalArgumentException.class, () -> config.setViscosity(Double.NaN));
    }
}
