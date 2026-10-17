package de.lifecircles.service;

import de.lifecircles.model.Cell;
import de.lifecircles.model.EnergyType;

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

    /** Post-decision meter for isolated callers. Worlds pre-fund brains separately and pass FREE here. */
    public static void decayEnergy(Cell cell, double deltaTime, BrainEnergyCosts costs) {
        if (cell.isRemains()) return;
        final double synapseEnergyCost = costs.storage()==0 && costs.work()==0?0:
                costs.cost(cell.getBrain().getSynapseCount(),cell.getBrain().getProccessedSynapses(),deltaTime);
        cell.setEnergy(Math.max(0,cell.getEnergy()-SimulationConfig.ENERGY_DECAY_RATE*deltaTime));
        cell.setEnergy(EnergyType.SECONDARY,Math.max(0,cell.getEnergy(EnergyType.SECONDARY)-synapseEnergyCost));
    }
}
