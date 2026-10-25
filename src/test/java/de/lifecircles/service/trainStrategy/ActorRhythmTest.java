package de.lifecircles.service.trainStrategy;

import de.lifecircles.model.*;
import de.lifecircles.model.neural.*;
import de.lifecircles.service.*;
import de.lifecircles.service.partitioningStrategy.SpatialGridPartitioningStrategy;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/** TAKTGEBER.md: does a clock reach the actor force, through the step, funding and tonus, and in random brains? */
class ActorRhythmTest {
    final SimulationConfig config = SimulationConfig.getInstance();
    static final double DT = 1.0 / 60;
    boolean adhesion;
    double rate, strength;
    Random previous;

    @BeforeEach void save() {
        adhesion = config.isCellAdhesion(); rate = config.getMutationRate(); strength = config.getMutationStrength();
        previous = NeuralNetwork.getRandom(); NeuralNetwork.setRandom(new Random(4711));
    }
    @AfterEach void restore() {
        config.setCellAdhesion(adhesion); config.setMutationRate(rate); config.setMutationStrength(strength);
        NeuralNetwork.setRandom(previous);
    }

    /** A network without synapses whose force outputs follow one clock's sine input, as a large weight would. */
    static final class ClockDrivenBrain implements CellBrainInterface {
        final CellBrainInterface base = RaceTrainStrategyTest.genome(false).getBrain();
        final int clock;
        double[] inputs;
        ClockDrivenBrain(int clock) { this.clock = clock; }
        @Override public void setInputs(double[] inputs) { this.inputs = inputs; base.setInputs(inputs); }
        @Override public double[] process() {
            double[] outputs = base.process().clone();
            double sine = inputs[EnergyInputLayout.clockSine(12, clock)];
            for (int a = 0; a < 12; a++)
                outputs[GlobalOutputFeature.values().length + a * ActorOutputFeature.values().length + ActorOutputFeature.FORCE.ordinal()] = .5 + .5 * sine;
            return outputs;
        }
        @Override public int getSynapseCount() { return base.getSynapseCount(); }
        @Override public boolean[] determineActiveHiddenLayers(int cellState) { return base.determineActiveHiddenLayers(cellState); }
        @Override public int getInputCount() { return base.getInputCount(); }
        @Override public long getProccessedSynapses() { return base.getProccessedSynapses(); }
        @Override public NeuralNetwork mutate(double mutationRate, double mutationStrength) { return base.mutate(mutationRate, mutationStrength); }
        @Override public double getOutputValue(int outputNeuronPos) { return base.getOutputValue(outputNeuronPos); }
        @Override public double getInputValue(int inputNeuronPos) { return inputs[inputNeuronPos]; }
    }

    static int flips(double[] forces, int every) {
        int flips = 0;
        double sign = 0;
        for (int i = 0; i < forces.length; i += every) {
            if (Math.abs(forces[i]) < .05) continue;
            if (sign != 0 && Math.signum(forces[i]) != sign) flips++;
            sign = Math.signum(forces[i]);
        }
        return flips;
    }

    /**
     * Full step with adhesion (tonus 0.5 s): the tonus is a first-order lag, so the swing shrinks for fast clocks.
     * From 1 s on every octave flips the actor twice per period, also when sampled every 0.5 s like the race study.
     * The 0.5 s clock never changes the sign.
     */
    @Test void aClockDrivenForcePassesTheStepFundingAndTonus() {
        config.setCellAdhesion(true);
        double tau = config.getActorForceTonusSeconds(), seconds = 16, from = 4;
        System.out.printf(Locale.ROOT, "Tonus %.2f s%n", tau);
        for (int k = 0; k < 5; k++) {
            double period = Cell.DEFAULT_CLOCK_PERIODS[k];
            Environment world = new Environment(1000, 1000);
            Cell cell = new Cell(new Vector2D(500, 500), 20, new ClockDrivenBrain(k));
            EnergyAllocationService.initializeFounder(cell, 1);
            world.addCell(cell);
            var grid = new SpatialGridPartitioningStrategy(world.getWidth(), world.getHeight(), 100);
            int steps = (int) Math.round(seconds / DT), skip = (int) Math.round(from / DT);
            double[] forces = new double[steps - skip];
            for (int i = 0; i < steps; i++) {
                grid.build(world.getCellList());
                world.update(DT, grid, SimulationStepPolicy.OPEN_WORLD);
                if (i >= skip) forces[i - skip] = cell.getSensorActors().getFirst().getForceStrength();
            }
            assertFalse(cell.isBrainPaused());
            double max = Arrays.stream(forces).max().orElseThrow(), min = Arrays.stream(forces).min().orElseThrow();
            double expected = 1 / Math.sqrt(1 + Math.pow(2 * Math.PI * tau / period, 2));
            double swing = (max - min) / (config.getCellActorMaxAttractiveForceStrength() + config.getCellActorMaxRepulsiveForceStrength());
            int everyStep = flips(forces, 1), halfSecond = flips(forces, 30), cycles = (int) Math.round(2 * (seconds - from) / period);
            System.out.printf(Locale.ROOT, "Clock %.1f s: force %.1f..%.1f, swing %.2f (first-order lag %.2f), flips %d of %d, sampled every 0.5 s %d%n",
                    period, min, max, swing, expected, everyStep, cycles, halfSecond);
            assertEquals(expected, swing, .1);
            // Attraction (90) and repulsion (150) differ, so the command pushes by 19 on average; the lag keeps that
            // and lets the 0.5 s swing pass only around it.
            if (period >= 1) assertEquals(cycles, everyStep, 1, "two sign changes per period");
            else assertTrue(min > 0, "fast clock stays on the repulsive side");
        }
    }

    /**
     * How far the clocks move the force of race founders (random brains as RaceTrainStrategy builds them) and of
     * lineages drifting by mutation without selection. Other inputs stay at the founder's first real inputs; only the
     * clocks run through 16 s of age. Before the direct clock synapses the median swing was 0.002 and no founder swung.
     */
    @Test void randomBrainsSwingWithTheClocks() {
        Environment world = new Environment(1000, 1000);
        int founders = 16;
        int[] drift = {0, 30, 100, 300};
        for (int generations : drift) {
            List<Double> swings = new ArrayList<>();
            int oscillating = 0;
            for (int f = 0; f < founders; f++) {
                Cell cell = CellFactory.createCell(new Vector2D(500, 500), config.getCellMaxRadiusSize() / 2, .5, .01, .025);
                CellBrain brain = (CellBrain) cell.getBrain();
                for (int g = 0; g < generations; g++) brain = new CellBrain(brain.mutate(.1, .2));
                world.addCell(cell);
                double[] inputs = CellBrainService.generateInputs(cell, world);
                world.getCellList().remove(cell);
                int steps = (int) Math.round(16 / DT);
                double[][] forces = new double[12][steps];
                for (int i = 0; i < steps; i++) {
                    double age = i * DT;
                    for (int k = 0; k < Cell.DEFAULT_CLOCK_PERIODS.length; k++) {
                        double angle = 2 * Math.PI * age / Cell.DEFAULT_CLOCK_PERIODS[k];
                        inputs[EnergyInputLayout.clockSine(12, k)] = Math.sin(angle);
                        inputs[EnergyInputLayout.clockCosine(12, k)] = Math.cos(angle);
                    }
                    brain.setInputs(inputs);
                    double[] outputs = brain.process();
                    for (int a = 0; a < 12; a++) {
                        double o = outputs[GlobalOutputFeature.values().length + a * ActorOutputFeature.values().length + ActorOutputFeature.FORCE.ordinal()];
                        assertTrue(Double.isFinite(o));
                        double force = o * 2 - 1;
                        forces[a][i] = force * (force < 0 ? config.getCellActorMaxAttractiveForceStrength() : config.getCellActorMaxRepulsiveForceStrength());
                    }
                }
                boolean swings4 = false;
                for (double[] actor : forces) {
                    // Skip the first second: hidden state layers settle.
                    double[] tail = Arrays.copyOfRange(actor, 60, actor.length);
                    swings.add(Arrays.stream(tail).max().orElseThrow() - Arrays.stream(tail).min().orElseThrow());
                    if (flips(tail, 1) >= 4) swings4 = true;
                }
                if (swings4) oscillating++;
            }
            if (generations == 0) assertEquals(founders, oscillating, "every new brain swings");
            Collections.sort(swings);
            System.out.printf(Locale.ROOT, "Mutations %3d: force swing from the clocks median %.4f, 90%% %.4f, max %.4f (range -90..150); oscillating founders %d of %d%n",
                    generations, swings.get(swings.size() / 2), swings.get(swings.size() * 9 / 10), swings.getLast(), oscillating, founders);
        }
    }
}
