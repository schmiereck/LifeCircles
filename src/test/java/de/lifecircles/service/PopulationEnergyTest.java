package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.model.neural.*;
import de.lifecircles.service.partitioningStrategy.SpatialGridPartitioningStrategy;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/** Energy budgets in worlds deliberately below the former automatic repopulation threshold. */
class PopulationEnergyTest {
    private final SimulationConfig config = SimulationConfig.getInstance();
    private double sunRate, mutationRate, mutationStrength, energyThreshold, ageThreshold, desireThreshold, deathAge;
    private int initialCount;

    @BeforeEach void configure() {
        sunRate = config.getSunRayRate(); initialCount = config.getInitialCellCount();
        mutationRate = config.getMutationRate(); mutationStrength = config.getMutationStrength();
        energyThreshold = config.getReproductionEnergyThreshold(); ageThreshold = config.getReproductionAgeThreshold();
        desireThreshold = config.getReproductionDesireThreshold(); deathAge = config.getCellDeathAge();
        config.setSunRayRate(0); config.setInitialCellCount(20);
        config.setMutationRate(0); config.setMutationStrength(0);
        config.setReproductionEnergyThreshold(0.4); config.setReproductionAgeThreshold(8);
        config.setReproductionDesireThreshold(0.5); config.setCellDeathAge(10);
    }
    @AfterEach void restore() {
        config.setSunRayRate(sunRate); config.setInitialCellCount(initialCount);
        config.setMutationRate(mutationRate); config.setMutationStrength(mutationStrength);
        config.setReproductionEnergyThreshold(energyThreshold); config.setReproductionAgeThreshold(ageThreshold);
        config.setReproductionDesireThreshold(desireThreshold); config.setCellDeathAge(deathAge);
    }

    private static class ControlledBrain extends PhysicsRegressionTest.ConstantBrain {
        double desire = 1, share = 0.5;
        @Override public double[] process() {
            double[] outputs = super.process();
            int base = GlobalOutputFeature.values().length;
            outputs[base + ActorOutputFeature.REPRODUCTION_DESIRE.ordinal()] = desire;
            outputs[base + ActorOutputFeature.REPRODUCTION_ENERGY_SHARE.ordinal()] = share;
            return outputs;
        }
        @Override public NeuralNetwork mutate(double rate, double strength) {
            NeuralNetwork network = new NeuralNetwork(new ValuesNeuronValueFunctionFactory(), CellFactory.calcInputCount(12),
                    new int[]{1, 1, 1}, CellFactory.calcOutputCount(12), 0, 3);
            // Isolate the division budget from energy exchange with newborns.
            for (int i = 0; i < 12; i++) {
                int base = GlobalOutputFeature.values().length + i * ActorOutputFeature.values().length;
                network.getNeuralNet().getOutputNeuronArr()[base + ActorOutputFeature.ENERGY_ABSORPTION.ordinal()].setBias(0, -1000);
                network.getNeuralNet().getOutputNeuronArr()[base + ActorOutputFeature.ENERGY_DELIVERY.ordinal()].setBias(0, -1000);
            }
            return network;
        }
    }

    private Cell parent(Environment world, double energy, double age) {
        Cell cell = new Cell(new Vector2D(100, 100), 10, new ControlledBrain());
        cell.setEnergy(energy); cell.setAge(age); world.addCell(cell); return cell;
    }
    private void update(Environment world, double time) {
        SpatialGridPartitioningStrategy grid = new SpatialGridPartitioningStrategy(300, 300, 60);
        grid.build(world.getCellList()); world.update(time, grid);
    }
    private double totalEnergy(Environment world) {
        return world.getCellList().stream().mapToDouble(Cell::getEnergy).sum();
    }

    @ParameterizedTest @ValueSource(doubles = {0.1, 0.25, 0.5})
    void naturalBirthOnlySplitsAvailableEnergyEvenAtVeryLowPopulation(double share) {
        Environment world = new Environment(300, 300); Cell parent = parent(world, 0.8, 9);
        ((ControlledBrain) parent.getBrain()).share = share;
        update(world, 0);
        assertEquals(2, world.getCellList().size());
        assertEquals(0.8 * (1 - share), parent.getEnergy(), 1e-14);
        assertEquals(0.8 * share, world.getCellList().getLast().getEnergy(), 1e-14);
        assertEquals(0.8, totalEnergy(world), 1e-14);
        assertTrue(world.getCellList().getLast().isGrowing());
    }

    @Test void worldEnergyAfterBirthAccountsForMetabolicLoss() {
        Environment world = new Environment(300, 300); parent(world, 0.8, 9);
        update(world, 0.1);
        assertEquals(2, world.getCellList().size());
        assertEquals(0.8 - SimulationConfig.ENERGY_DECAY_RATE * 0.1, totalEnergy(world), 1e-14);
    }

    @Test void repeatedBirthsStopAtEnergyThresholdInsteadOfRestoringTargetPopulation() {
        Environment world = new Environment(300, 300); Cell parent = parent(world, 0.8, 9);
        ((ControlledBrain) parent.getBrain()).share = 0.1;
        for (int i = 0; i < 10; i++) {
            update(world, 0);
            assertEquals(0.8, totalEnergy(world), 1e-14);
        }
        assertEquals(8, world.getCellList().size());
        assertEquals(0.8 * Math.pow(0.9, 7), parent.getEnergy(), 1e-14);
        assertFalse(ReproductionManagerService.canReproduce(config, parent));
        for (int i = 0; i < 10; i++) update(world, 0);
        assertEquals(8, world.getCellList().size()); assertEquals(0.8, totalEnergy(world), 1e-14);
    }

    @Test void insufficientEnergyIsNotBypassedByLowPopulation() {
        Environment world = new Environment(300, 300); Cell parent = parent(world, 0.2, 9);
        for (int i = 0; i < 5; i++) update(world, 0);
        assertEquals(1, world.getCellList().size()); assertSame(parent, world.getCellList().getFirst());
        assertEquals(0.2, totalEnergy(world));
    }

    @Test void youngCellsAreNotForcedToReproduce() {
        Environment world = new Environment(300, 300); parent(world, 0.8, 0);
        update(world, 0);
        assertEquals(1, world.getCellList().size()); assertEquals(0.8, totalEnergy(world));
    }

    @Test void cellsWithoutReproductionDesireAreNotForcedToReproduce() {
        Environment world = new Environment(300, 300); Cell parent = parent(world, 0.8, 9);
        ((ControlledBrain) parent.getBrain()).desire = 0;
        update(world, 0);
        assertEquals(1, world.getCellList().size()); assertEquals(0.8, totalEnergy(world));
    }

    @ParameterizedTest @ValueSource(doubles = {0, 0.001})
    void exhaustedPopulationStaysExtinctWithoutRewardOrReseeding(double energy) {
        Environment world = new Environment(300, 300); parent(world, energy, 9);
        update(world, 1); // Exhaust any residual energy before reproduction.
        assertTrue(world.getCellList().isEmpty());
        for (int i = 0; i < 3; i++) update(world, 0);
        assertTrue(world.getCellList().isEmpty()); assertEquals(0, totalEnergy(world));
    }

    @Test void ageExpiredParentCannotProduceChildrenOrRestartPopulation() {
        Environment world = new Environment(300, 300); parent(world, 0.8, 10);
        update(world, 0);
        assertTrue(world.getCellList().isEmpty());
        update(world, 0); assertTrue(world.getCellList().isEmpty());
    }

    @Test void initiallyEmptyWorldDoesNotCreateFreeEnergy() {
        Environment world = new Environment(300, 300);
        update(world, 0);
        assertTrue(world.getCellList().isEmpty()); assertEquals(0, totalEnergy(world));
    }

    @Test void blockedBirthLeavesParentEnergyUntouched() {
        Environment world = new Environment(300, 300); Cell parent = parent(world, 0.8, 9);
        world.addBlocker(new Blocker(new Vector2D(111, 100), 4, 100, Color.GRAY, Blocker.BlockerType.WALL));
        update(world, 0);
        assertEquals(1, world.getCellList().size()); assertEquals(0.8, parent.getEnergy());
    }
}
