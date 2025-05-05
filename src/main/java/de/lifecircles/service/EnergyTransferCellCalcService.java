package de.lifecircles.service;

import de.lifecircles.model.Cell;
import de.lifecircles.model.SensorActor;
import de.lifecircles.model.Vector2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for processing energy transfer between cells using sensors and actuators.
 */
public class EnergyTransferCellCalcService {
    // Maximum energy transfer per interaction
    private static final double MAX_ENERGY_TRANSFER = 0.01;
    // Minimum energy threshold for transfer
    private static final double MIN_ENERGY_FOR_TRANSFER = 0.1;

    /**
     * Processes energy transfer between cells based on sensor-actor interactions.
     * @param cells list of cells in the environment
     * @param deltaTime time step in seconds
     */
    public static void processEnergyTransfers(PartitioningStrategy partitioner, List<Cell> cells, double deltaTime) {
        for (Cell cell : cells) {
            // Skip cells with too little energy
            if (cell.getEnergy() < MIN_ENERGY_FOR_TRANSFER) continue;

            for (SensorActor sensor : cell.getSensorActors()) {
                if (!sensor.shouldFireEnergyBeam()) continue;

                // Find nearby cells that can receive energy
                for (Cell other : partitioner.getNeighbors(cell)) {
                    if (other == cell) continue;
                    
                    // Check if any of the other cell's actors are within range
                    boolean canTransfer = false;
                    for (SensorActor otherActor : other.getSensorActors()) {
                        double distance = sensor.getPosition().subtract(otherActor.getPosition()).length();
                        if (distance < sensor.getForceStrength() * Cell.getMaxSize()) {
                            canTransfer = true;
                            break;
                        }
                    }

                    if (canTransfer) {
                        // Calculate energy transfer amount
                        double transferAmount = Math.min(
                            MAX_ENERGY_TRANSFER,
                            cell.getEnergy() * deltaTime
                        );
                        
                        // Transfer energy
                        cell.setEnergy(cell.getEnergy() - transferAmount);
                        other.setEnergy(other.getEnergy() + transferAmount);
                        
                        // Break after first successful transfer
                        break;
                    }
                }
            }
        }
    }
}
