package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.model.neural.*;
import de.lifecircles.service.partitioningStrategy.SpatialGridPartitioningStrategy;
import java.util.Locale;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/** TEILUNG.md: does a division push mother and child apart, and how far? */
class DivisionMechanicsTest {
    static final double DT = 1.0 / 60;
    final SimulationConfig config = SimulationConfig.getInstance();
    double gravity, rate;
    boolean adhesion;

    @BeforeEach void save() { gravity = config.getGravity(); adhesion = config.isCellAdhesion(); rate = config.getMutationRate(); }
    @AfterEach void restore() { config.setGravity(gravity); config.setCellAdhesion(adhesion); config.setMutationRate(rate); }

    /** Fixed outputs: one adult radius, neutral actors, no wish to divide. */
    static final class SizeBrain implements CellBrainInterface {
        final double radius, force;
        SizeBrain(double radius) { this(radius, .5); }
        SizeBrain(double radius, double force) { this.radius = radius; this.force = force; }
        public int getSynapseCount() { return 0; }
        public long getProccessedSynapses() { return 0; }
        public boolean[] determineActiveHiddenLayers(int state) { return new boolean[3]; }
        public void setInputs(double[] inputs) {}
        public int getInputCount() { return CellFactory.calcInputCount(12); }
        public double getInputValue(int index) { return 0; }
        public double getOutputValue(int index) { return process()[index]; }
        public NeuralNetwork mutate(double rate, double strength) {
            return ((CellBrain) CellFactory.createCell(new Vector2D(0, 0), 10).getBrain()).getNeuralNetwork();
        }
        public double[] process() {
            double[] outputs = new double[CellFactory.calcOutputCount(12)];
            double min = SimulationConfig.getInstance().getCellMinRadiusSize(), max = SimulationConfig.getInstance().getCellMaxRadiusSize();
            outputs[GlobalOutputFeature.SIZE.ordinal()] = (radius - min) / (max - min);
            for (int i = 0; i < 12; i++)
                outputs[GlobalOutputFeature.values().length + i * ActorOutputFeature.values().length + ActorOutputFeature.FORCE.ordinal()] = force;
            return outputs;
        }
    }

    static Cell cell(double x, double y, double radius) {
        Cell cell = new Cell(new Vector2D(x, y), radius, new SizeBrain(radius));
        cell.setRadiusSize(radius);
        cell.setType(new CellType(.2, .6, .4));
        EnergyAllocationService.initializeFounder(cell, 1);
        return cell;
    }

    /** Runs a pair for some seconds; returns {peak relative speed px/s, final gap px, peak overlap px}. */
    static double[] run(Environment world, Cell a, Cell b, double seconds) {
        var grid = new SpatialGridPartitioningStrategy(world.getWidth(), world.getHeight(), 100);
        double speed = 0, overlap = 0;
        for (int i = 0; i < Math.round(seconds / DT); i++) {
            grid.build(world.getCellList());
            world.update(DT, grid, SimulationStepPolicy.OPEN_WORLD);
            speed = Math.max(speed, a.getVelocity().subtract(b.getVelocity()).length());
            overlap = Math.max(overlap, a.getRadiusSize() + b.getRadiusSize() - distance(a, b));
        }
        return new double[]{speed, distance(a, b) - a.getRadiusSize() - b.getRadiusSize(), overlap};
    }

    static void setBrain(Cell cell, CellBrainInterface brain) {
        try {
            var field = Cell.class.getDeclaredField("brain");
            field.setAccessible(true);
            field.set(cell, brain);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

    /** Two touching cells of half the area each that swell back to the full radius; and full repulsive actors. */
    @Test void swellingAndRepulsiveActors() {
        config.setGravity(0);
        for (double radius : new double[]{8, 15, 20}) {
            for (int variant = 0; variant < 3; variant++) {
                config.setCellAdhesion(variant > 0);
                double half = radius / Math.sqrt(2);
                double target = variant == 0 ? radius : half;
                double force = variant == 2 ? 1 : .5;
                Environment world = new Environment(1000, 1000);
                Cell a = cell(500, 500, half), b = cell(500 + 2 * half, 500, half);
                setBrain(a, new SizeBrain(target, force)); setBrain(b, new SizeBrain(target, force));
                world.addCell(a); world.addCell(b);
                double[] r = run(world, a, b, 5);
                System.out.printf(Locale.ROOT, "%s r=%.0f: peak overlap %.1f px, peak relative speed %.0f px/s, gap after 5 s %.1f px%n",
                        new String[]{"Swelling halves back", "Halves, neutral actors, adhesion", "Halves, repelling actors, adhesion"}[variant],
                        radius, r[2], r[0], r[1]);
            }
        }
    }

    static double distance(Cell a, Cell b) { return a.getPosition().subtract(b.getPosition()).length(); }

    @Test void aDivisionLeavesMotherAndChildTouching() {
        config.setGravity(0);
        config.setCellAdhesion(false);
        config.setMutationRate(0);
        for (double radius : new double[]{8, 15, 20}) {
            Environment world = new Environment(1000, 1000);
            Cell mother = cell(500, 500, radius);
            world.addCell(mother);
            for (SensorActor actor : mother.getSensorActors()) actor.setReproductionDesire(1);
            double area = radius * radius;
            Cell child = ReproductionManagerService.reproduceClonally(config, world, mother);
            assertNotNull(child);
            setBrain(child, new SizeBrain(radius));
            EnergyAllocationService.initializeFounder(child, 1); // reserves do not matter here, only the paid growth
            world.addCell(child);
            double start = distance(mother, child);
            double[] r = run(world, mother, child, 5);
            System.out.printf(Locale.ROOT, "Division r=%.0f: start distance %.1f, radii %.1f+%.1f (area %.0f of %.0f), peak overlap %.1f px, peak relative speed %.0f px/s, gap after 5 s %.1f px%n",
                    radius, start, mother.getRadiusSize(), child.getRadiusSize(),
                    mother.getRadiusSize() * mother.getRadiusSize() + child.getRadiusSize() * child.getRadiusSize(), area, r[2], r[0], r[1]);
        }
    }
}
