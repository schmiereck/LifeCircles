package de.lifecircles.service;

import de.lifecircles.model.Cell;
import de.lifecircles.model.Environment;
import de.lifecircles.model.Vector2D;
import de.lifecircles.service.SimulationConfig;
import de.lifecircles.model.reproduction.ReproductionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Training-Strategie: HighEnergy.
 * Startet mit 20 Zellen, selektiert alle 500 Schritte die Top-20% nach Energie,
 * erzeugt mutierte Nachkommen und ersetzt die Population.
 */
public class HighEnergyTrainStrategy implements TrainStrategy {
    private static final int INITIAL_COUNT = 20;
    private static final int GENERATION_STEP = 800;
    private static final double SELECTION_PERCENT = 0.2;
    private final SimulationConfig config = SimulationConfig.getInstance();
    private long stepCounter = 0;

    @Override
    public Environment initializeEnvironment() {
        return new Environment(config.getWidth(), config.getHeight());
    }

    @Override
    public void initialize(Environment environment) {
        // Add ground blocker by default
        environment.addGroundBlocker();
        environment.addSunBlocker();

        // Initiale Population
        Random random = new Random();
        for (int i = 0; i < INITIAL_COUNT; i++) {
            double x = random.nextDouble() * config.getWidth();
            double y = random.nextDouble() * config.getHeight();
            environment.addCell(new Cell(new Vector2D(x, y), config.getCellMaxRadius() / 2.0));
        }
    }

    @Override
    public void selectAndMutate(Environment environment) {
        stepCounter++;
        if (stepCounter % GENERATION_STEP != 0) {
            return;
        }
        List<Cell> cells = environment.getCells();
        if (cells.isEmpty()) return;
        int winnersCount = Math.max(1, (int) (cells.size() * SELECTION_PERCENT));
        cells.sort((c1, c2) -> Double.compare(c2.getEnergy(), c1.getEnergy()));
        List<Cell> winners = new ArrayList<>(cells.subList(0, winnersCount));
        winners.forEach(cell -> cell.setEnergy(Cell.MAX_ENERGY));
        // Elites unverändert übernehmen
        List<Cell> nextGen = new ArrayList<>();
        nextGen.addAll(winners);
        Random random = new Random();
        // Fülle Population bis INITIAL_COUNT mit mutierten Nachkommen auf
        while (nextGen.size() < INITIAL_COUNT) {
            Cell parent = winners.get(random.nextInt(winnersCount));
            Cell child = ReproductionManager.reproduce(config, parent);
            child.setEnergy(Cell.MAX_ENERGY);
            nextGen.add(child);
        }
        environment.resetCells(nextGen);
    }
}
