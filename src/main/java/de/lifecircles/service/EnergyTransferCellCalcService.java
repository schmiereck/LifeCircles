package de.lifecircles.service;

import de.lifecircles.model.Cell;
import de.lifecircles.model.SensableActor;
import de.lifecircles.model.SensableCell;
import de.lifecircles.model.SensorActor;
import de.lifecircles.service.partitioningStrategy.PartitioningStrategy;

import java.util.List;
import java.util.Objects;

/**
 * Service for processing energy transfer between cells using sensors and actuators.
 */
public class EnergyTransferCellCalcService {
    // Maximum energy transfer per interaction
    private static final double MAX_ENERGY_DELIVERY_TRANSFER = 0.075D;
    private static final double MAX_ENERGY_ABSORBTION_TRANSFER = 0.09D;
    // Minimum energy threshold for transfer
    private static final double MIN_ENERGY_FOR_TRANSFER = 0.1D;
    // Threshold for energy absorption (output value)

    /**
     * Processes energy transfer and absorption between cells based on sensor-actor interactions.
     * @param cells list of cells in the environment
     * @param deltaTime time step in seconds
     */
    public static void processEnergyTransfers(final List<Cell> cells, final double deltaTime) {
        for (final Cell cell : cells) {
            for (final SensorActor sensor : cell.getSensorActors()) {
                // Find nearby cells that can deliver/receive energy
                //List<Cell> nearbyCells = partitioner.getNeighbors(cell);
                //for (Cell otherCell : nearbyCells) {
                //    if (otherCell == cell) continue;

                    // Check if any of other's actors should receive energy
                    final SensableActor otherActor = sensor.getSensedActor();
                    if (Objects.nonNull(otherActor)) {
                        final SensableCell otherCell = sensor.getSensedCell();
                        if (Objects.nonNull(otherCell)) {
                            //double intensity = ActorSensorCellCalcService.sense(sensor, otherActor);
                            //if (intensity != 0) {
                            // Check if this is an absorption attempt
                            //double absorptionOutput = cell.getBrain().getNetwork().getOutputValue(SensorInputFeature.ENERGY_ABSORPTION.ordinal());
                            final double absorptionOutput = sensor.getEnergyAbsorption();
                            if ((absorptionOutput > 0.0D)) { // && (otherCell.getEnergy() > MIN_ENERGY_FOR_TRANSFER))
                                // Calculate energy absorption amount
                                final double absorptionAmount =
                                        Math.min(otherCell.getEnergy(),
                                            Math.min(
                                            MAX_ENERGY_ABSORBTION_TRANSFER,
                                            otherCell.getEnergy() * deltaTime * absorptionOutput
                                        ));

                                // Absorb energy from other cell
                                cell.setEnergy(cell.getEnergy() + absorptionAmount);
                                otherCell.setEnergy(otherCell.getEnergy() - absorptionAmount);
                            }
                            // Check if this is a delivery attempt
                            final double deliveryOutput = sensor.getEnergyDelivery();
                            if ((deliveryOutput > 0.0D) && (cell.getEnergy() > MIN_ENERGY_FOR_TRANSFER)) {
                                // Regular energy transfer
                                final double transferAmount = Math.min(
                                        MAX_ENERGY_DELIVERY_TRANSFER,
                                        cell.getEnergy() * deltaTime * deliveryOutput
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
