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
        final int synapseCount = cell.getBrain().getSynapseCount();
        final long proccessedSynapses = cell.getBrain().getProccessedSynapses();
        final double synapseEnergyCost =
                useSynapseEnergyCost
                            ?
                                synapseCount * SimulationConfig.ENERGY_COST_PER_SYNAPSE * deltaTime +
                                proccessedSynapses * SimulationConfig.ENERGY_COST_PER_PROCESSED_SYNAPSE
                            :
                                0.0D;
        final double newEnergy =
                Math.max(0.0D,
                        cell.getEnergy() - ((SimulationConfig.ENERGY_DECAY_RATE * deltaTime) + synapseEnergyCost));
        cell.setEnergy(newEnergy);
    }
}
