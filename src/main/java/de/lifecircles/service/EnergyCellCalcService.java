package de.lifecircles.service;

import de.lifecircles.model.Cell;

/**
 * Service responsible for handling cell energy calculations.
 */
public class EnergyCellCalcService {

    private static final double ENERGY_DECAY_RATE = 0.005D;
    private static final double ENERGY_COST_PER_SYNAPSE = 0.00000001D;

    /**
     * Applies energy decay to the cell.
     *
     * @param cell      the cell whose energy to decay
     * @param deltaTime time step in seconds
     */
    public static void decayEnergy(final Cell cell, final double deltaTime, final boolean useSynapseEnergyCost) {
        int synapseCount = cell.getBrain().getSynapseCount();
        double synapseEnergyCost = useSynapseEnergyCost ? synapseCount * ENERGY_COST_PER_SYNAPSE * deltaTime : 0;
        double newEnergy = Math.max(0.0,
                cell.getEnergy() -
                ((ENERGY_DECAY_RATE * deltaTime) + synapseEnergyCost));
        cell.setEnergy(newEnergy);
    }
}
