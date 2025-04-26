package de.lifecircles.service;

import de.lifecircles.model.Cell;

/**
 * Service responsible for handling cell energy calculations.
 */
public class EnergyCellCalcService {

    private static final double MAX_ENERGY = 1.0;
    private static final double ENERGY_DECAY_RATE = 0.01;

    /**
     * Applies energy decay to the cell.
     *
     * @param cell      the cell whose energy to decay
     * @param deltaTime time step in seconds
     */
    public static void decayEnergy(Cell cell, double deltaTime) {
        double newEnergy = Math.max(0.0, cell.getEnergy() - ENERGY_DECAY_RATE * deltaTime);
        cell.setEnergy(newEnergy);
    }
}
