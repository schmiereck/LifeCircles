package de.lifecircles.service.trainStrategy;

import de.lifecircles.model.*;
import de.lifecircles.model.neural.*;
import de.lifecircles.service.SimulationConfig;
import org.junit.jupiter.api.*;

import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Selection must produce mutants and fair, independent episodes. */
class TrainingSelectionTest {
    private final SimulationConfig config = SimulationConfig.getInstance();
    private double rate, strength;
    private Random previous;

    @Test void eliteAndMutantsStartSamePhysicalAndNeuralEpisode() {
        Environment world = new Environment(6400,1200);
        Cell parent = add(world,200,800);
        parent.setAge(500); parent.setVelocity(new Vector2D(80,-20));
        parent.setRotation(2); parent.setAngularVelocity(3); parent.setRadiusSize(30);
        CellBrainService.think(parent); CellBrainService.think(parent);
        var result = TrainingPopulationBuilder.build(world,List.of(parent),16,true,new Random(9)).orElseThrow();
        Cell first = result.getFirst();
        for (Cell cell : result) {
            assertNotSame(parent,cell); assertEquals(0,cell.getAge());
            assertEquals(0,cell.getVelocity().length()); assertEquals(0,cell.getAngularVelocity());
            assertEquals(0,cell.getRotation()); assertTrue(cell.isGrowing());
            assertEquals(first.getRadiusSize(),cell.getRadiusSize());
            assertEquals(first.getTargetRadiusSize(),cell.getTargetRadiusSize());
            assertEquals(first.getBrain().getOutputValue(0),cell.getBrain().getOutputValue(0));
            assertNotSame(first.getBrain(),cell==first ? parent.getBrain() : cell.getBrain());
        }
        assertEquals(500,parent.getAge()); assertEquals(80,parent.getVelocity().getX());
        assertEquals(3,parent.getAngularVelocity());
    }
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
    @Test void bothHeightStrategiesReserveThirteenMutationCandidates() {
        for (TrainStrategy strategy : new TrainStrategy[]{new HighPositionTrainStrategy(), new HighPosition2TrainStrategy()}) {
            Environment world = new Environment(6400, 1200);
            for (int i = 0; i < 16; i++) add(world, 200 + 400 * i, 800);
            var original = new HashSet<>(world.getCellList());
            select(strategy, world, strategy instanceof HighPositionTrainStrategy ? 10000 : 3000);
            assertTrue(world.getCellList().stream().noneMatch(original::contains));
            assertEquals(3,world.getCellList().stream().filter(c -> c.getGeneration()==7).count());
            assertEquals(13,world.getCellList().stream().filter(c -> c.getGeneration()==8).count());
        }
    }
    @Test void energyStrategyGeneratesMutantsWithOneHundredSurvivors() {
        Environment world = new Environment(6400, 1200);
        for (int i = 0; i < 100; i++) add(world, 100 + i * 40, 800);
        var original = new HashSet<>(world.getCellList());
        select(new HighEnergyTrainStrategy(), world, 2500);
        assertEquals(20, world.getCellList().size()); assertTrue(world.getCellList().stream().noneMatch(original::contains));
        assertEquals(16,world.getCellList().stream().filter(c -> c.getGeneration()==8).count());
    }
    @Test void energyStrategyDoesGenerateOffspringFromTwentySurvivors() {
        Environment world = new Environment(6400, 1200);
        for (int i = 0; i < 20; i++) add(world, 100 + i * 40, 800);
        var original = new HashSet<>(world.getCellList());
        select(new HighEnergyTrainStrategy(), world, 2500);
        assertEquals(0, world.getCellList().stream().filter(original::contains).count());
        assertEquals(4, world.getCellList().stream().filter(c -> c.getGeneration()==7).count());
        assertEquals(16, world.getCellList().stream().filter(c -> c.getGeneration() == 8).count());
    }
    @Test void heightTwoCenteringScoreSelectsCenters() {
        Environment world = new Environment(6400, 1200); List<Cell> centers = new ArrayList<>();
        for (int section = 0; section < 16; section++) {
            Cell center = add(world, 400 * section + 200, 800);
            center.setType(new CellType(.123,.234,.345)); centers.add(center);
            add(world, 400 * section + 30, 800); add(world, 400 * section + 31, 800);
        }
        select(new HighPosition2TrainStrategy(), world, 3000);
        assertEquals(16, world.getCellList().size());
        assertTrue(world.getCellList().stream().allMatch(c -> c.getType().getRed()==.123));
    }

    @Test void heightTwoBreedsGroundedDevelopmentProgramInsteadOfDetachedTip() {
        Environment world = new Environment(6400,1200); world.addGroundBlocker();
        double floor = world.getBlockerList().getFirst().getPosition().getY()
                - world.getBlockerList().getFirst().getHeight()/2;
        for(int row=0;row<6;row++) {
            Cell body = add(world,200,floor-10-20*row); body.setType(new CellType(.123,.234,.345));
        }
        Cell detached = add(world,200,100); detached.setType(new CellType(.9,.8,.7));
        select(new HighPosition2TrainStrategy(),world,3000);
        assertEquals(16,world.getCellList().size());
        assertTrue(world.getCellList().stream().allMatch(cell->cell.getType().getRed()==.123));
    }
}
