package de.lifecircles.service.trainStrategy;

import de.lifecircles.model.*;
import de.lifecircles.model.neural.*;
import de.lifecircles.service.SimulationConfig;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Current-behavior evidence for review; these assertions are not the desired training contract. */
@EnabledIfSystemProperty(named = "network.audit", matches = "true")
class TrainingLogicAuditTest {
    private final SimulationConfig config = SimulationConfig.getInstance();
    private double rate, strength;
    private Random previous;
    @BeforeEach void save() {
        rate = config.getMutationRate(); strength = config.getMutationStrength();
        config.setMutationRate(0); config.setMutationStrength(0);
        previous = NeuralNetwork.getRandom(); NeuralNetwork.setRandom(new Random(97));
    }
    @AfterEach void restore() {
        config.setMutationRate(rate); config.setMutationStrength(strength); NeuralNetwork.setRandom(previous);
    }
    private Cell add(Environment world, double x, double y) {
        var brain = new CellBrain(new NeuralNetwork(new ValuesNeuronValueFunctionFactory(),
                CellFactory.calcInputCount(12), new int[]{1, 1, 1}, CellFactory.calcOutputCount(12), 0, 3));
        Cell cell = new Cell(new Vector2D(x, y), 10, brain); cell.setGeneration(7); world.addCell(cell); return cell;
    }
    private void select(TrainStrategy strategy, Environment world, int steps) {
        for (int i = 0; i < steps; i++) strategy.selectAndMutate(world);
    }
    @Test void bothHeightStrategiesKeepAllSixteenWinnersAndGenerateNoTrainingOffspring() {
        for (TrainStrategy strategy : new TrainStrategy[]{new HighPositionTrainStrategy(), new HighPosition2TrainStrategy()}) {
            Environment world = new Environment(6400, 1200);
            for (int i = 0; i < 16; i++) add(world, 200 + 400 * i, 800);
            var original = new HashSet<>(world.getCellList());
            select(strategy, world, strategy instanceof HighPositionTrainStrategy ? 10000 : 3000);
            assertEquals(original, new HashSet<>(world.getCellList()));
            assertTrue(world.getCellList().stream().allMatch(c -> c.getGeneration() == 7));
        }
    }
    @Test void energyStrategyStopsGeneratingTrainingOffspringAtOneHundredSurvivors() {
        Environment world = new Environment(6400, 1200);
        for (int i = 0; i < 100; i++) add(world, 100 + i * 40, 800);
        var original = new HashSet<>(world.getCellList());
        select(new HighEnergyTrainStrategy(), world, 2500);
        assertEquals(20, world.getCellList().size()); assertTrue(original.containsAll(world.getCellList()));
    }
    @Test void energyStrategyDoesGenerateOffspringFromTwentySurvivors() {
        Environment world = new Environment(6400, 1200);
        for (int i = 0; i < 20; i++) add(world, 100 + i * 40, 800);
        var original = new HashSet<>(world.getCellList());
        select(new HighEnergyTrainStrategy(), world, 2500);
        assertEquals(4, world.getCellList().stream().filter(original::contains).count());
        assertEquals(16, world.getCellList().stream().filter(c -> c.getGeneration() == 8).count());
    }
    @Test void heightTwoCenteringScoreActuallySelectsEdgesOverCenters() {
        Environment world = new Environment(6400, 1200); List<Cell> centers = new ArrayList<>();
        for (int section = 0; section < 16; section++) {
            centers.add(add(world, 400 * section + 200, 800));
            add(world, 400 * section + 30, 800); add(world, 400 * section + 31, 800);
        }
        select(new HighPosition2TrainStrategy(), world, 3000);
        assertEquals(16, world.getCellList().size());
        assertTrue(centers.stream().noneMatch(world.getCellList()::contains));
    }
}
