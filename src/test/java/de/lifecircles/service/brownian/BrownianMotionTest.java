package de.lifecircles.service.brownian;

import de.lifecircles.model.*;
import de.lifecircles.model.neural.*;
import de.lifecircles.service.*;
import de.lifecircles.service.partitioningStrategy.SpatialGridPartitioningStrategy;
import de.lifecircles.service.trainStrategy.RaceTrainStrategy;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/** BROWNSCHE_BEWEGUNG.md: hits without a current, equal strength in both models, independent of the time step. */
class BrownianMotionTest {
    static final double DT = 1.0 / 60;
    final SimulationConfig config = SimulationConfig.getInstance();
    BrownianModel model;
    BrownianProfile profile;
    double density, strength, gravity, coldShare, kneeHeight, kneeShare;
    boolean adhesion;

    @BeforeEach void save() {
        model = config.getBrownianModel(); density = config.getBrownianDensity(); strength = config.getBrownianStrength();
        gravity = config.getGravity(); adhesion = config.isCellAdhesion();
        profile = config.getBrownianProfile(); coldShare = config.getBrownianColdShare();
        kneeHeight = config.getBrownianKneeHeight(); kneeShare = config.getBrownianKneeShare();
    }
    @AfterEach void restore() {
        config.setBrownianModel(model); config.setBrownianDensity(density); config.setBrownianStrength(strength);
        config.setGravity(gravity); config.setCellAdhesion(adhesion);
        config.setBrownianProfile(profile); config.setBrownianColdShare(coldShare);
        config.setBrownianKneeHeight(kneeHeight); config.setBrownianKneeShare(kneeShare);
    }

    /** Fixed outputs: radius 10, all actors red and at full attraction (attract) or neutral. */
    static final class FixedBrain implements CellBrainInterface {
        final double force;
        FixedBrain(double force) { this.force = force; }
        public int getSynapseCount() { return 0; }
        public long getProccessedSynapses() { return 0; }
        public boolean[] determineActiveHiddenLayers(int state) { return new boolean[3]; }
        public void setInputs(double[] inputs) {}
        public int getInputCount() { return CellFactory.calcInputCount(12); }
        public double getInputValue(int index) { return 0; }
        public double getOutputValue(int index) { return process()[index]; }
        public NeuralNetwork mutate(double rate, double strength) { throw new UnsupportedOperationException(); }
        public double[] process() {
            double[] outputs = new double[CellFactory.calcOutputCount(12)];
            outputs[GlobalOutputFeature.SIZE.ordinal()] = (10.0 - 6.0) / (20.0 - 6.0);
            for (int i = 0; i < 12; i++) {
                int base = GlobalOutputFeature.values().length + i * ActorOutputFeature.values().length;
                outputs[base + ActorOutputFeature.TYPE_RED.ordinal()] = 1;
                outputs[base + ActorOutputFeature.FORCE.ordinal()] = force;
            }
            return outputs;
        }
    }

    static Cell cell(double x, double y, double force) {
        Cell cell = new Cell(new Vector2D(x, y), 10, new FixedBrain(force));
        cell.setType(new CellType(.2, .6, .4));
        cell.setEnergy(1); cell.setEnergy(EnergyType.SECONDARY, 1);
        return cell;
    }

    /** Velocity changes of a resting cell from the model alone, step by step. */
    static double[][] kicks(BrownianMotion motion, double dt, int steps, long seed) {
        Cell cell = cell(100, 100, .5);
        Random random = new Random(seed);
        double[][] dv = new double[steps][3];
        for (int i = 0; i < steps; i++) {
            cell.setVelocity(new Vector2D(0, 0)); cell.setAngularVelocity(0);
            motion.agitate(cell, dt, 20, 10, random);
            cell.updateForce(dt);
            dv[i][0] = cell.getVelocity().getX(); dv[i][1] = cell.getVelocity().getY(); dv[i][2] = cell.getAngularVelocity();
        }
        return dv;
    }

    @Test void hitsHaveNoDirectionAndBothModelsTheSameStrength() {
        int steps = 200_000;
        for (BrownianModel m : new BrownianModel[]{BrownianModel.COLLISIONS, BrownianModel.LANGEVIN}) {
            double[][] dv = kicks(m.motion(), DT, steps, 7);
            double mx = 0, my = 0, sx = 0, sy = 0, sw = 0;
            for (double[] d : dv) { mx += d[0]; my += d[1]; sx += d[0] * d[0]; sy += d[1] * d[1]; sw += d[2] * d[2]; }
            double seconds = steps * DT;
            // Expected mean square velocity change per second: hit rate (20/s at radius 10) times strength² (100).
            double perSecond = (sx + sy) / seconds;
            System.out.printf(Locale.ROOT, "%s: mean dv (%.4f, %.4f) per step, mean square %.0f per s (expected 2000), x/y %.3f, spin rms %.4f per step%n",
                    m, mx / steps, my / steps, perSecond, sx / sy, Math.sqrt(sw / steps));
            assertEquals(2000, perSecond, 100);
            assertEquals(1, sx / sy, .05, "isotropic");
            double rms = Math.sqrt((sx + sy) / steps);
            assertEquals(0, mx / steps, rms * 5 / Math.sqrt(steps), "no current");
            assertEquals(0, my / steps, rms * 5 / Math.sqrt(steps), "no current");
            assertTrue(sw > 0, "hits off-centre turn the cell");
        }
    }

    @Test void theStrengthDoesNotDependOnTheTimeStep() {
        for (BrownianModel m : new BrownianModel[]{BrownianModel.COLLISIONS, BrownianModel.LANGEVIN}) {
            double[] perSecond = new double[2];
            double[] dts = {DT, DT / 2};
            for (int k = 0; k < 2; k++) {
                int steps = (int) Math.round(2000 / dts[k]);
                double sum = 0;
                for (double[] d : kicks(m.motion(), dts[k], steps, 11)) sum += d[0] * d[0] + d[1] * d[1];
                perSecond[k] = sum / (steps * dts[k]);
            }
            assertEquals(perSecond[0], perSecond[1], perSecond[0] * .06, m + " at 60 and 120 Hz");
        }
    }

    /** Height profile: two straight pieces meeting at the knee, warm at the ground or at the top. */
    @Test void theProfileBendsAtTheKnee() {
        double top = 1000;
        assertEquals(1, BrownianProfile.UNIFORM.factor(500, top, 0, 100, .8), 1e-12);
        // Warm ground, cold top: 80 % of the change within the lowest 100 px.
        assertEquals(1, BrownianProfile.WARM_BOTTOM.factor(0, top, 0, 100, .8), 1e-12);
        assertEquals(.6, BrownianProfile.WARM_BOTTOM.factor(50, top, 0, 100, .8), 1e-12);
        assertEquals(.2, BrownianProfile.WARM_BOTTOM.factor(100, top, 0, 100, .8), 1e-12);
        assertEquals(.1, BrownianProfile.WARM_BOTTOM.factor(550, top, 0, 100, .8), 1e-12);
        assertEquals(0, BrownianProfile.WARM_BOTTOM.factor(top, top, 0, 100, .8), 1e-12);
        assertEquals(0, BrownianProfile.WARM_BOTTOM.factor(top + 20, top, 0, 100, .8), 1e-12, "clamped");
        // Warm top: the mirror image of the change, the cold end keeps half the strength.
        assertEquals(.5, BrownianProfile.WARM_TOP.factor(-20, top, .5, 100, .8), 1e-12, "clamped");
        assertEquals(.9, BrownianProfile.WARM_TOP.factor(100, top, .5, 100, .8), 1e-12);
        assertEquals(1, BrownianProfile.WARM_TOP.factor(top, top, .5, 100, .8), 1e-12);
        // A knee at the ground puts the share there as a jump; share 0 leaves the lowest part warm.
        assertEquals(.2, BrownianProfile.WARM_BOTTOM.factor(0, top, 0, 0, .8), 1e-12);
        assertEquals(1, BrownianProfile.WARM_BOTTOM.factor(100, top, 0, 100, 0), 1e-12);
    }

    /** In a world the strength follows the cell's height above the ground surface. */
    @Test void cellsNearTheWarmGroundAreHitHarder() {
        config.setBrownianModel(BrownianModel.LANGEVIN);
        config.setBrownianDensity(20); config.setBrownianStrength(10);
        config.setBrownianColdShare(0); config.setBrownianKneeHeight(100); config.setBrownianKneeShare(.8);
        double height = 1050, ground = height - Environment.GroundBlockerHeight; // 1000 px above the ground
        for (BrownianProfile p : BrownianProfile.values()) {
            config.setBrownianProfile(p);
            BrownianMotionService service = new BrownianMotionService(3);
            Cell low = cell(100, ground - 10, .5), knee = cell(200, ground - 100, .5), high = cell(300, ground - 990, .5);
            double[] square = new double[3];
            for (int i = 0; i < 100_000; i++) {
                List<Cell> cells = List.of(low, knee, high);
                for (Cell cell : cells) { cell.setVelocity(new Vector2D(0, 0)); cell.setAngularVelocity(0); }
                service.agitate(cells, DT, height);
                for (int c = 0; c < 3; c++) {
                    cells.get(c).updateForce(DT);
                    double v = cells.get(c).getVelocity().length();
                    square[c] += v * v;
                }
            }
            double expected = 20 * 100 * 100_000 * DT; // full strength: rate 20/s · 10² over the run
            double[] factors = {p.factor(10, 1000, 0, 100, .8), p.factor(100, 1000, 0, 100, .8), p.factor(990, 1000, 0, 100, .8)};
            System.out.printf(Locale.ROOT, "%s: strength share low %.2f knee %.2f high %.2f (expected %.2f %.2f %.2f)%n", p,
                    Math.sqrt(square[0] / expected), Math.sqrt(square[1] / expected), Math.sqrt(square[2] / expected),
                    factors[0], factors[1], factors[2]);
            for (int c = 0; c < 3; c++)
                assertEquals(factors[c] * factors[c], square[c] / expected, .05, p + " cell " + c);
        }
    }

    @Test void offByDefaultAndOffInTheRace() {
        assertFalse(new RaceTrainStrategy().useBrownianMotion());
        assertTrue(SimulationStepPolicy.OPEN_WORLD.useBrownianMotion());
        config.setGravity(0);
        config.setBrownianModel(BrownianModel.COLLISIONS);
        Environment world = new Environment(300, 300);
        Cell cell = cell(150, 150, .5);
        world.addCell(cell);
        var grid = new SpatialGridPartitioningStrategy(300, 300, 60);
        grid.build(world.getCellList());
        world.update(DT, grid, new RaceTrainStrategy());
        assertEquals(0, cell.getVelocity().length(), 1e-12, "the race keeps the cells still");
        for (int i = 0; i < 60; i++) { grid.build(world.getCellList()); world.update(DT, grid, SimulationStepPolicy.OPEN_WORLD); }
        assertTrue(cell.getVelocity().length() > 0, "an open world pushes them");
    }

    /**
     * Calibration (printed): at density 20 the strength that moves and turns single cells clearly but keeps
     * attracting pairs of kin cells together. Without gravity, adhesion on, open world, 30 s; 16 single cells and
     * 16 pairs, 250 px apart, so they hardly meet.
     */
    @Test void calibration() {
        config.setGravity(0);
        config.setCellAdhesion(true);
        config.setBrownianModel(BrownianModel.COLLISIONS);
        config.setBrownianDensity(20);
        for (double s : new double[]{3, 4, 5, 6, 7, 8, 10, 15}) {
            config.setBrownianStrength(s);
            Environment world = new Environment(2000, 1000);
            List<Cell> singles = new ArrayList<>();
            List<Cell[]> pairs = new ArrayList<>();
            for (int i = 0; i < 16; i++) {
                double x = 125 + (i % 4) * 250, y = 125 + (i / 4) * 250;
                Cell single = cell(x, y, .5);
                singles.add(single); world.addCell(single);
                Cell a = cell(1000 + x - 10, y, 0), b = cell(1000 + x + 10, y, 0);
                pairs.add(new Cell[]{a, b}); world.addCell(a); world.addCell(b);
            }
            List<Vector2D> start = singles.stream().map(Cell::getPosition).toList();
            double[] lastRotation = singles.stream().mapToDouble(Cell::getRotation).toArray();
            double turned = 0;
            var grid = new SpatialGridPartitioningStrategy(2000, 1000, 100);
            int apart = 0;
            for (int step = 0; step < 1800; step++) {
                grid.build(world.getCellList());
                world.update(DT, grid, SimulationStepPolicy.OPEN_WORLD);
                for (int i = 0; i < singles.size(); i++) {
                    turned += Math.abs(Math.IEEEremainder(singles.get(i).getRotation() - lastRotation[i], 2 * Math.PI));
                    lastRotation[i] = singles.get(i).getRotation();
                }
            }
            double squares = 0;
            for (int i = 0; i < singles.size(); i++) squares += Math.pow(singles.get(i).getPosition().subtract(start.get(i)).length(), 2);
            for (Cell[] pair : pairs) if (pair[0].getPosition().subtract(pair[1].getPosition()).length() - 20 > 10) apart++;
            if (s == 5) {
                assertEquals(0, apart, "the default keeps kin pairs together");
                assertTrue(Math.sqrt(squares / singles.size()) > 20, "and moves single cells by more than a radius");
            }
            System.out.printf(Locale.ROOT, "strength %4.1f px/s: single cells moved %.0f px (rms) and turned %.1f rad each in 30 s; kin pairs apart after 30 s: %d of 16%n",
                    s, Math.sqrt(squares / singles.size()), turned / singles.size(), apart);
        }
    }
}
