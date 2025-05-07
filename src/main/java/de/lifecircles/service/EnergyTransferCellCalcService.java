package de.lifecircles.service;

import de.lifecircles.model.Cell;
import de.lifecircles.model.SensorActor;
import de.lifecircles.service.partitioningStrategy.PartitioningStrategy;

import java.util.List;

/**
 * Service for processing energy transfer between cells using sensors and actuators.
 */
public class EnergyTransferCellCalcService {
    // Maximum energy transfer per interaction
    private static final double MAX_ENERGY_TRANSFER = 0.01;
    // Minimum energy threshold for transfer
    private static final double MIN_ENERGY_FOR_TRANSFER = 0.1;
    // Threshold for energy absorption (output value)
    private static final double ENERGY_ABSORPTION_THRESHOLD = 0.9;

    /**
     * Processes energy transfer and absorption between cells based on sensor-actor interactions.
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
                List<Cell> nearbyCells = partitioner.getNeighbors(cell);
                for (Cell other : nearbyCells) {
                    if (other == cell) continue;
                    if (other.getEnergy() <= 0) continue; // Skip cells with no energy

                    // Check if any of other's actors should receive energy
                    for (SensorActor otherActor : other.getSensorActors()) {
                        double intensity = ActorSensorCellCalcService.sense(sensor, otherActor);
                        if (intensity != 0) {
                            // Check if this is an absorption attempt
                            //double absorptionOutput = cell.getBrain().getNetwork().getOutputValue(SensorInputFeature.ENERGY_ABSORPTION.ordinal());
                            double absorptionOutput = sensor.getEnergyAbsorption();
                            if (absorptionOutput >= ENERGY_ABSORPTION_THRESHOLD) {
                                // Calculate energy absorption amount
                                double absorptionAmount = Math.min(
                                    MAX_ENERGY_TRANSFER,
                                    other.getEnergy() * deltaTime
                                );
                                
                                // Absorb energy from other cell
                                cell.setEnergy(cell.getEnergy() + absorptionAmount);
                                other.setEnergy(other.getEnergy() - absorptionAmount);
                            } else {
                                // Regular energy transfer
                                double transferAmount = Math.min(
                                    MAX_ENERGY_TRANSFER,
                                    cell.getEnergy() * deltaTime
                                );
                                
                                // Transfer energy
                                cell.setEnergy(cell.getEnergy() - transferAmount);
                                other.setEnergy(other.getEnergy() + transferAmount);
                            }
                            
                            // Break after first successful transfer/absorption
                            break;
                        }
                    }
                }
            }
        }
    }
}
