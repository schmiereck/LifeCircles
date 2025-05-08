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
    private static final double ENERGY_ABSORPTION_THRESHOLD = 0.75;
    private static final double ENERGY_DELIVERY_THRESHOLD = 0.75;

    /**
     * Processes energy transfer and absorption between cells based on sensor-actor interactions.
     * @param cells list of cells in the environment
     * @param deltaTime time step in seconds
     */
    public static void processEnergyTransfers(PartitioningStrategy partitioner, List<Cell> cells, double deltaTime) {
        for (Cell cell : cells) {
            for (SensorActor sensor : cell.getSensorActors()) {
                // Find nearby cells that can deliver/receive energy
                List<Cell> nearbyCells = partitioner.getNeighbors(cell);
                for (Cell otherCell : nearbyCells) {
                    if (otherCell == cell) continue;

                    // Check if any of other's actors should receive energy
                    for (SensorActor otherActor : otherCell.getSensorActors()) {
                        double intensity = ActorSensorCellCalcService.sense(sensor, otherActor);
                        if (intensity != 0) {
                            // Check if this is an absorption attempt
                            //double absorptionOutput = cell.getBrain().getNetwork().getOutputValue(SensorInputFeature.ENERGY_ABSORPTION.ordinal());
                            double absorptionOutput = sensor.getEnergyAbsorption();
                            if ((absorptionOutput >= ENERGY_ABSORPTION_THRESHOLD) && (otherCell.getEnergy() > MIN_ENERGY_FOR_TRANSFER)) {
                                // Calculate energy absorption amount
                                double absorptionAmount = Math.min(
                                    MAX_ENERGY_TRANSFER,
                                    otherCell.getEnergy() * deltaTime
                                );
                                
                                // Absorb energy from other cell
                                cell.setEnergy(cell.getEnergy() + absorptionAmount);
                                otherCell.setEnergy(otherCell.getEnergy() - absorptionAmount);
                            }
                            // Check if this is a delivery attempt
                            double deliveryOutput = sensor.getEnergyDelivery();
                            if ((deliveryOutput >= ENERGY_DELIVERY_THRESHOLD) && (cell.getEnergy() > MIN_ENERGY_FOR_TRANSFER)) {
                                // Regular energy transfer
                                double transferAmount = Math.min(
                                    MAX_ENERGY_TRANSFER,
                                    cell.getEnergy() * deltaTime
                                );

                                // Transfer energy
                                cell.setEnergy(cell.getEnergy() - transferAmount);
                                otherCell.setEnergy(otherCell.getEnergy() + transferAmount);
                            }
                        }
                    }
                }
            }
        }
    }
}
