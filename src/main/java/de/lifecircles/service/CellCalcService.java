package de.lifecircles.service;

import de.lifecircles.model.Cell;
import de.lifecircles.model.neural.CellBrainService;

/**
 * Service for cell calculation operations.
 */
public class CellCalcService {
    
    /**
     * Updates the cell's position, rotation, and behavior based on its current state.
     * @param cell The cell to update
     * @param deltaTime Time step in seconds
     */
    public static void updateCell(final Cell cell, final double deltaTime) {
        // Update neural network
        final boolean useSynapseEnergyCost;
        if (cell.getTempThinkHackCounter() >= SimulationConfig.CELL_TEMP_THINK_HACK_COUNTER_MAX) {
            CellBrainService.think(cell);
            useSynapseEnergyCost = true;
            cell.setTempThinkHackCounter(0);
        } else {
            useSynapseEnergyCost = false;
            cell.incTempThinkHackCounter();
        }

        // Update size if cell is growing
        if (cell.isGrowing()) {
            cell.incGrowthAge(deltaTime);
            if (cell.getGrowthAge() >= SimulationConfig.CELL_GROWTH_DURATION) {
                // Wachstumsprozess abgeschlossen
                cell.setRealRadiusSize(cell.getTargetRadiusSize());
                cell.setIsGrowing(false);
            } else {
                // Lineare Interpolation zwischen Startgröße und Zielgröße
                double growthProgress = cell.getGrowthAge() / SimulationConfig.CELL_GROWTH_DURATION;
                double minSize = SimulationConfig.getInstance().getCellMinRadiusSize();
                cell.setRealRadiusSize(minSize + (cell.getTargetRadiusSize() - minSize) * growthProgress);
            }
        }

        // Verzögerte Größenänderung nur anwenden, wenn die Zelle nicht wächst
        if (!cell.isGrowing()) {
            cell.applyDelayedSizeChange();
        }

        // Update physics
        cell.setPosition(cell.getPosition().add(cell.getVelocity().multiply(deltaTime)));
        cell.setRotation(cell.getRotation() + cell.getAngularVelocity() * deltaTime);
        // Apply rotational friction
        cell.setAngularVelocity(cell.getAngularVelocity() * 
                (1.0D - SimulationConfig.getInstance().getRotationalFriction()) * deltaTime);
        
        // Normalize rotation to [0, 2π)
        double rotation = cell.getRotation() % (2 * Math.PI);
        if (rotation < 0) {
            rotation += 2 * Math.PI;
        }
        cell.setRotation(rotation);

        // Update energy and age
        EnergyCellCalcService.decayEnergy(cell, deltaTime, useSynapseEnergyCost);

        cell.incAge(deltaTime);
    }

    /**
     * velocityForce to velocity.
     * angularVelocityForce to angularVelocity
     */
    public static void updateForces(final Cell cell) {
        cell.updateForce();
    }
}
