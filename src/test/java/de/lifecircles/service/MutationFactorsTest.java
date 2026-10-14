package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.model.neural.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class MutationFactorsTest {
    private final SimulationConfig config = SimulationConfig.getInstance();
    private double rate, strength;
    private Random previousRandom;

    @BeforeEach void configure() {
        rate = config.getMutationRate(); strength = config.getMutationStrength();
        previousRandom = NeuralNetwork.getRandom(); NeuralNetwork.setRandom(new Random(573));
        config.setMutationRate(0.1); config.setMutationStrength(0.2);
    }
    @AfterEach void restore() {
        config.setMutationRate(rate); config.setMutationStrength(strength); NeuralNetwork.setRandom(previousRandom);
    }

    private NeuralNetwork network() {
        NeuralNetwork network = new NeuralNetwork(new ValuesNeuronValueFunctionFactory(), CellFactory.calcInputCount(12),
                new int[]{1, 1, 1}, CellFactory.calcOutputCount(12), 0, 3);
        network.getNeuralNet().getOutputNeuronArr()[0].setBias(0, 0);
        return network;
    }
    private class RecordingBrain extends CellBrain {
        double actualRate, actualStrength;
        RecordingBrain() { super(network()); }
        @Override public NeuralNetwork mutate(double rate, double strength) {
            actualRate = rate; actualStrength = strength;
            return getNeuralNetwork().mutate(0, 0);
        }
    }
    private Cell parent(CellBrain brain) {
        Cell cell = new Cell(new Vector2D(100, 100), 10, brain);
        SensorActor actor = cell.getSensorActors().getFirst();
        actor.setReproductionDesire(1); actor.setReproductionEnergyShareOutput(0.25);
        return cell;
    }
    private Cell child(Cell parent, boolean training) {
        Environment world = new Environment(300, 300);
        Cell child = training ? ReproductionManagerService.createTrainingOffspring(config, world, parent, new Vector2D(150, 100), 0)
                : ReproductionManagerService.reproduce(config, world, parent);
        assertNotNull(child); return child;
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void bothBirthPathsApplyParentFactors(boolean training) {
        RecordingBrain brain = new RecordingBrain(); Cell parent = parent(brain);
        parent.setMutationRateFactor(1.5); parent.setMutationStrengthFactor(0.5);
        child(parent, training);
        assertEquals(0.15, brain.actualRate, 1e-14); assertEquals(0.1, brain.actualStrength, 1e-14);
        assertEquals(1.5, parent.getMutationRateFactor()); assertEquals(0.5, parent.getMutationStrengthFactor());
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void rateIsCappedButStrengthStillUsesItsFactor(boolean training) {
        config.setMutationRate(0.8);
        RecordingBrain brain = new RecordingBrain(); Cell parent = parent(brain);
        parent.setMutationRateFactor(2); parent.setMutationStrengthFactor(2);
        child(parent, training);
        assertEquals(1, brain.actualRate); assertEquals(0.4, brain.actualStrength);
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void zeroGlobalRatePreservesInheritedFactors(boolean training) {
        config.setMutationRate(0);
        RecordingBrain brain = new RecordingBrain(); Cell parent = parent(brain);
        parent.setMutationRateFactor(0.3); parent.setMutationStrengthFactor(1.7);
        Cell child = child(parent, training);
        assertEquals(0, brain.actualRate);
        assertEquals(0.3, child.getMutationRateFactor()); assertEquals(1.7, child.getMutationStrengthFactor());
        assertEquals(0.3, parent.getMutationRateFactor()); assertEquals(1.7, parent.getMutationStrengthFactor());
    }

    private void fixedMutationDraw() {
        // Mutation is selected iff rate > .75; a selected bias moves by .5*strength.
        // Structural mutation probabilities remain below .75 and do not alter topology.
        NeuralNetwork.setRandom(new Random(1) {
            @Override public double nextDouble() { return 0.75; }
        });
    }
    private double firstBias(Cell cell) {
        return ((CellBrain) cell.getBrain()).getNeuralNetwork().getNeuralNet().getOutputNeuronArr()[0].getBias(0);
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void rateFactorChangesActualMutationDecisionInChildGenes(boolean training) {
        config.setMutationRate(0.5);
        Cell parent = parent(new CellBrain(network()));
        parent.setMutationRateFactor(0.5);
        fixedMutationDraw(); Cell lowRate = child(parent, training);
        parent.setMutationRateFactor(2);
        fixedMutationDraw(); Cell highRate = child(parent, training);
        assertEquals(0, firstBias(lowRate)); assertEquals(0.1, firstBias(highRate), 1e-14);
        assertEquals(0, firstBias(parent));
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void strengthFactorScalesActualBiasChangesInChildGenes(boolean training) {
        config.setMutationRate(1);
        Cell parent = parent(new CellBrain(network()));
        parent.setMutationStrengthFactor(0.5);
        fixedMutationDraw(); Cell lowStrength = child(parent, training);
        parent.setMutationStrengthFactor(2);
        fixedMutationDraw(); Cell highStrength = child(parent, training);
        assertEquals(0.05, firstBias(lowStrength), 1e-14); assertEquals(0.2, firstBias(highStrength), 1e-14);
        assertEquals(0, firstBias(parent));
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void factorBoundsAreAppliedBeforeMutation(boolean training) {
        RecordingBrain brain = new RecordingBrain(); Cell parent = parent(brain);
        parent.setMutationRateFactor(-10); parent.setMutationStrengthFactor(20);
        child(parent, training);
        assertEquals(0.01, brain.actualRate, 1e-14); assertEquals(0.4, brain.actualStrength, 1e-14);
        assertEquals(0.1, parent.getMutationRateFactor()); assertEquals(2, parent.getMutationStrengthFactor());
    }
}
