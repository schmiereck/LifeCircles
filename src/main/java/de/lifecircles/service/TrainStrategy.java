package de.lifecircles.service;

import de.lifecircles.model.Environment;

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
}
