package de.lifecircles.service;

import de.lifecircles.model.Cell;
import de.lifecircles.model.Environment;
import de.lifecircles.model.Vector2D;
import de.lifecircles.model.reproduction.ReproductionManager;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Training-Strategie: HighPosition.
 * Start mit 60 Zellen, selektiert alle 3000 Schritte die Top-20% nach Höhe über dem Boden,
 * erzeugt mutierte Nachkommen und ersetzt die Population.
 */
public class HighPositionTrainStrategy implements TrainStrategy {
    private static final int INITIAL_COUNT = 20;
    private static final int GENERATION_STEP = 500;
    private static final double SELECTION_PERCENT = 0.2;
    private final SimulationConfig config = SimulationConfig.getInstance();
    private long stepCounter = 0;

    @Override
    public void initialize(Environment environment) {
        // Add ground blocker by default
        environment.addGroundBlocker();
        //environment.addSunBlocker();

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
        if (cells.isEmpty()) {
            return;
        }
        int winnersCount = Math.max(1, (int) (cells.size() * SELECTION_PERCENT));
        // Sortiere nach Y-Koordinate aufsteigend (höhere Zellen oben)
        cells.sort(Comparator.comparingDouble(c -> c.getPosition().getY()));
        List<Cell> winners = new ArrayList<>(cells.subList(0, winnersCount));
        List<Cell> nextGen = new ArrayList<>();
        nextGen.addAll(winners);
        Random random = new Random();
        // Fülle bis INITIAL_COUNT mit mutierten Nachkommen auf
        while (nextGen.size() < INITIAL_COUNT) {
            Cell parent = winners.get(random.nextInt(winnersCount));
            nextGen.add(ReproductionManager.reproduce(config, parent));
        }
        environment.resetCells(nextGen);
    }
}
