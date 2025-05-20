package de.lifecircles.service;

import de.lifecircles.model.Cell;

/**
 * Service responsible for handling cell energy calculations.
 */
public class EnergyCellCalcService {

    /**
     * Applies energy decay to the cell.
     *
     * @param cell      the cell whose energy to decay
     * @param deltaTime time step in seconds
     */
    public static void decayEnergy(final Cell cell, final double deltaTime, final boolean useSynapseEnergyCost) {
        int synapseCount = cell.getBrain().getSynapseCount();
        double synapseEnergyCost = useSynapseEnergyCost ? synapseCount * SimulationConfig.ENERGY_COST_PER_SYNAPSE * deltaTime : 0;
        double newEnergy =
                Math.max(0.0D,
                        cell.getEnergy() - ((SimulationConfig.ENERGY_DECAY_RATE * deltaTime) + synapseEnergyCost));
        cell.setEnergy(newEnergy);
        // Mark cell death if energy below threshold
        if (cell.getEnergy() < 0.0D) {
            cell.setEnergy(0.0D);
        }
    }
}
