package de.lifecircles.service.trainStrategy;

import de.lifecircles.model.*;
import de.lifecircles.model.neural.*;
import de.lifecircles.service.*;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.Duration;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

class TrainingGenerationTest {
    private final SimulationConfig config = SimulationConfig.getInstance();
    private double mutationRate, mutationStrength;
    private Random previousRandom;
    record Mode(Supplier<TrainStrategy> factory, int interval, int target, boolean sections) {}
    static Stream<Mode> modes() {
        return Stream.of(new Mode(HighEnergyTrainStrategy::new, 2500, 20, false),
                new Mode(HighPositionTrainStrategy::new, 10000, 16, true),
                new Mode(HighPosition2TrainStrategy::new, 3000, 16, true));
    }
    @BeforeEach void setup() {
        mutationRate = config.getMutationRate(); mutationStrength = config.getMutationStrength();
        config.setMutationRate(0); config.setMutationStrength(0);
        previousRandom = NeuralNetwork.getRandom(); NeuralNetwork.setRandom(new Random(148));
    }
    @AfterEach void restore() {
        config.setMutationRate(mutationRate); config.setMutationStrength(mutationStrength);
        NeuralNetwork.setRandom(previousRandom);
    }
    private Cell add(Environment world, double x, double y) {
        CellBrain brain = new CellBrain(new NeuralNetwork(new ValuesNeuronValueFunctionFactory(),
                CellFactory.calcInputCount(12), new int[]{1, 1, 1}, CellFactory.calcOutputCount(12), 0, 3));
        Cell cell = new Cell(new Vector2D(x, y), 10, brain);
        cell.setEnergy(0.2); cell.setCellState(5); cell.setGeneration(7);
        for (SensorActor actor : cell.getSensorActors()) actor.setReproductionDesire(0);
        world.addCell(cell); return cell;
    }
    private void generation(TrainStrategy strategy, Environment world, Mode mode) {
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            for (int step = 0; step < mode.interval; step++) strategy.selectAndMutate(world);
        });
    }

    @ParameterizedTest @MethodSource("modes")
    void fillsGenerationWithoutAnyBiologicalReproductionReadiness(Mode mode) {
        Environment world = new Environment(6400, 1200); world.addGroundBlocker();
        Cell parent = add(world, 200, 800);
        assertFalse(ReproductionManagerService.canReproduce(config, parent));
        assertNull(parent.getSensorActors().getFirst().getCachedPosition());
        NeuralNetwork originalBrain = ((CellBrain) parent.getBrain()).getNeuralNetwork();
        generation(mode.factory.get(), world, mode);
        assertEquals(mode.target, world.getCellList().size());
        assertEquals(mode.target, new HashSet<>(world.getCellList()).size());
        assertNotSame(parent, world.getCellList().getFirst());
        assertEquals(7,world.getCellList().getFirst().getGeneration());
        assertEquals(.2,parent.getEnergy());
        for (int i = 0; i < world.getCellList().size(); i++) {
            Cell cell = world.getCellList().get(i);
            assertEquals(1, cell.getEnergy(), 0);
            assertTrue(ReproductionManagerService.isFreePosition(world, cell.getPosition(), cell.getRadiusSize()));
            assertNotNull(cell.getSensorActors().getFirst().getCachedPosition());
            if (i > 0) {
                assertEquals(8, cell.getGeneration()); assertEquals(0, cell.getAge(), 0);
                assertNotSame(originalBrain, ((CellBrain) cell.getBrain()).getNeuralNetwork());
            }
            if (mode.sections) {
                assertEquals(0, cell.getCellState()); assertEquals(0, cell.getAge(), 0);
                assertEquals(world.getWidth() / mode.target * (i + 0.5), cell.getPosition().getX(), 1e-12);
            } else assertEquals(5, cell.getCellState());
        }
    }

    @ParameterizedTest @MethodSource("modes")
    void fullyBlockedWorldReturnsWithinAttemptBudgetAndKeepsPreviousPopulation(Mode mode) {
        Environment world = new Environment(6400, 1200);
        Cell first = add(world, 200, 800), second = add(world, 600, 700);
        second.setEnergy(0.3);
        class CountingBlocker extends Blocker {
            int checks;
            CountingBlocker() { super(new Vector2D(3200, 600), 6600, 1400, Color.GRAY, BlockerType.WALL); }
            @Override public boolean overlapsCircle(Vector2D position, double radius, double width, double height) {
                checks++;
                return super.overlapsCircle(position, radius, width, height);
            }
        }
        CountingBlocker blocker = new CountingBlocker(); world.addBlocker(blocker);
        TrainStrategy strategy = mode.factory.get();
        for (int round = 0; round < 2; round++) {
            generation(strategy, world, mode);
            assertEquals(List.of(first, second), world.getCellList());
            assertEquals(0.2, first.getEnergy(), 0); assertEquals(0.3, second.getEnergy(), 0);
            assertEquals(200, first.getPosition().getX(), 0); assertEquals(5, first.getCellState());
            assertEquals(TrainingPopulationBuilder.MAX_PLACEMENT_ATTEMPTS * (round + 1), blocker.checks);
        }
    }

    @ParameterizedTest @MethodSource("modes")
    void blockedPreferredPositionUsesAlternativeFreePlacement(Mode mode) {
        Environment world = new Environment(6400, 1200);
        double x = mode.sections ? 6400.0 / mode.target / 2 : 200;
        double y = mode.sections ? 1200 - Environment.GroundBlockerHeight - config.getCellMaxRadiusSize() : 800;
        add(world, x, y);
        world.addBlocker(new Blocker(new Vector2D(x, y), 10, 10, Color.GRAY, Blocker.BlockerType.WALL));
        generation(mode.factory.get(), world, mode);
        assertEquals(mode.target, world.getCellList().size());
        for (Cell cell : world.getCellList()) {
            assertTrue(ReproductionManagerService.isFreePosition(world, cell.getPosition(), cell.getRadiusSize()));
        }
    }

    @ParameterizedTest @MethodSource("modes")
    void emptyPopulationDoesNotThrowOrWaitForWinners(Mode mode) {
        Environment world = new Environment(6400, 1200);
        generation(mode.factory.get(), world, mode);
        assertTrue(world.getCellList().isEmpty());
    }

    @Test void highPositionFillsTheSixteenthSlotWhenOnlyFifteenCellsSurvive() {
        Environment world = new Environment(6400, 1200);
        for (int i = 0; i < 15; i++) add(world, i * 400 + 200, 800);
        Mode mode = new Mode(HighPositionTrainStrategy::new, 10000, 16, true);
        generation(mode.factory.get(), world, mode);
        assertEquals(16, world.getCellList().size());
    }

    @Test void highEnergyCapsAnOversizedWinningPopulationAtItsTarget() {
        Environment world = new Environment(6400, 1200);
        for (int i = 0; i < 110; i++) add(world, 200 + i * 40, 800);
        Mode mode = new Mode(HighEnergyTrainStrategy::new, 2500, 20, false);
        generation(mode.factory.get(), world, mode);
        assertEquals(20, world.getCellList().size());
    }

    @Test void noEligibleSectionWinnersDoesNotCauseRandomSelectionFromAnEmptyList() {
        Environment world = new Environment(6400, 1200);
        Cell parent = add(world, world.getWidth() * 2, 800);
        Mode mode = new Mode(HighPosition2TrainStrategy::new, 3000, 16, true);
        generation(mode.factory.get(), world, mode);
        assertEquals(List.of(parent), world.getCellList());
        assertEquals(0.2, parent.getEnergy(), 0);
    }

    @Test void trainingOffspringDoesNotConsumeParentEnergyOrEnableNaturalReproduction() {
        Environment world = new Environment(6400, 1200);
        Cell parent = add(world, 200, 800);
        assertNull(ReproductionManagerService.reproduce(config, world, parent));
        Cell child = ReproductionManagerService.createTrainingOffspring(config, world, parent, new Vector2D(300, 800), 0);
        assertNotNull(child); assertEquals(1, child.getEnergy(), 0); assertEquals(0.2, parent.getEnergy(), 0);
        assertNull(ReproductionManagerService.reproduce(config, world, parent));
    }

    @Test void latePlacementFailureDoesNotMutateParentsOrCreatePartialOffspring() {
        Environment world = new Environment(6400, 1200);
        java.util.concurrent.atomic.AtomicInteger mutations = new java.util.concurrent.atomic.AtomicInteger();
        CellBrain brain = new CellBrain(new NeuralNetwork(new ValuesNeuronValueFunctionFactory(),
                CellFactory.calcInputCount(12), new int[]{1, 1, 1}, CellFactory.calcOutputCount(12), 0, 3)) {
            @Override public NeuralNetwork mutate(double rate, double strength) {
                mutations.incrementAndGet(); return super.mutate(rate, strength);
            }
        };
        Cell parent = new Cell(new Vector2D(1000, 500), 10, brain);
        parent.setEnergy(0.2); parent.setCellState(5); world.addCell(parent);
        // Slot zero is free, but the entire second section is blocked.
        world.addBlocker(new Blocker(new Vector2D(600, 600), 400, 1400, Color.GRAY, Blocker.BlockerType.WALL));
        Optional<List<Cell>> result = assertTimeoutPreemptively(Duration.ofSeconds(5),
                () -> TrainingPopulationBuilder.build(world, List.of(parent), 16, true, new Random(11)));
        assertTrue(result.isEmpty()); assertEquals(0, mutations.get());
        assertEquals(List.of(parent), world.getCellList());
        assertEquals(1000, parent.getPosition().getX(), 0);
        assertEquals(500, parent.getPosition().getY(), 0);
        assertEquals(0.2, parent.getEnergy(), 0); assertEquals(5, parent.getCellState());
    }
}
