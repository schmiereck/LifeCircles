package de.lifecircles.service.trainStrategy;

import de.lifecircles.model.Environment;
import de.lifecircles.model.SimulationStepPolicy;
import de.lifecircles.model.Cell;
import java.util.List;

/**
 * Interface for training mode strategies.
 */
public interface TrainStrategy {
    /**
     * Initialize environment for training mode.
     */
    void initialize(Environment environment);

    /**
     * Selection and mutation logic after a generation.
     */
    void selectAndMutate(Environment environment);

    Environment initializeEnvironment();

    /** Called before building the spatial index. */
    default void beforeStep(Environment environment, double deltaTime) {}
    default SimulationStepPolicy stepPolicy() { return SimulationStepPolicy.TRAINING; }
    default void selectAndMutate(Environment environment, double deltaTime) { selectAndMutate(environment); }
    /** Import may restart an experimental episode using the imported genomes. */
    default void onPopulationImported(Environment environment, List<Cell> imported, boolean merge) {}
    default String getStatus() { return ""; }
}
