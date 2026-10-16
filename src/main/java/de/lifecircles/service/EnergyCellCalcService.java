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
        decayEnergy(cell, deltaTime, useSynapseEnergyCost ? BrainEnergyCosts.DEFAULT : new BrainEnergyCosts(0, 0));
    }

    public static void decayEnergy(Cell cell, double deltaTime, BrainEnergyCosts costs) {
        final int synapseCount = cell.getBrain().getSynapseCount();
        final long proccessedSynapses = cell.getBrain().getProccessedSynapses();
        final double synapseEnergyCost =
                costs.cost(synapseCount, proccessedSynapses, deltaTime);
        final double newEnergy =
                Math.max(0.0D,
                        cell.getEnergy() - ((SimulationConfig.ENERGY_DECAY_RATE * deltaTime) + synapseEnergyCost));
        cell.setEnergy(newEnergy);
    }
}
