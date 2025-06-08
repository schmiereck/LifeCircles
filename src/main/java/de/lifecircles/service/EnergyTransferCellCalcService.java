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
    private static final double MAX_ENERGY_DELIVERY_TRANSFER = 0.005D;
    private static final double MAX_ENERGY_ABSORBTION_TRANSFER = 0.01D;
    // Minimum energy threshold for transfer
    private static final double MIN_ENERGY_FOR_TRANSFER = 0.1D;

    /**
     * Processes energy transfer and absorption between cells based on sensor-actor interactions.
     * @param cells list of cells in the environment
     */
    public static void processEnergyTransfers(final List<Cell> cells) {
        for (final Cell calcCell : cells) {
            for (final SensorActor sensor : calcCell.getSensorActors()) {
                final SensableActor otherActor = sensor.getSensedActor();
                if (Objects.nonNull(otherActor)) {
                    final SensableCell otherCell = sensor.getSensedCell();
                    if (Objects.nonNull(otherCell)) {
                        // --- Distanzabhängigkeit berechnen ---
                        double distance = 0.0;
                        double maxSensorRadius = 1.0;
                        if (sensor.getCachedPosition() != null && otherActor.getCachedPosition() != null) {
                            distance = sensor.getCachedPosition().distance(otherActor.getCachedPosition());
                            int totalSensors = calcCell.getSensorActors().size();
                            maxSensorRadius = SensorActorForceCellCalcService.calcSensorRadius(calcCell.getRadiusSize(), totalSensors);
                        }
                        double distFactorDelivery = 1.0;
                        double distFactorAbsorption = 1.0;
                        if (maxSensorRadius > 0.0) {
                            double normDist = Math.min(distance / maxSensorRadius, 1.0);

                            final double calcCellTransferStrength = calcCell.getRadiusSize() / SimulationConfig.getInstance().getCellMaxRadiusSize();
                            //final double otherCellTransferStrength = otherCell.getRadiusSize() / SimulationConfig.getInstance().getCellMaxRadiusSize();

                            // Linear Interpolation: 1.0 (nah) -> 0.8 (fern) für Delivery
                            distFactorDelivery = 1.0 - (1.0 - SimulationConfig.CELL_ENERGY_DELIVERY_FACTOR_FAR) * normDist * calcCellTransferStrength;
                            // Linear Interpolation: 1.0 (nah) -> 0.7 (fern) für Absorption
                            distFactorAbsorption = 1.0 - (1.0 - SimulationConfig.CELL_ENERGY_ABSORPTION_FACTOR_FAR) * normDist * calcCellTransferStrength;
                        }
                        // --- Absorption ---
                        final double absorptionOutput = sensor.getEnergyAbsorption();
                        if ((absorptionOutput > 0.0D)) {
                            final double otherCellTransferAmountLimit =
                                    Math.min(
                                            otherCell.getEnergy(),
                                            Math.min(
                                                    MAX_ENERGY_ABSORBTION_TRANSFER,
                                                    absorptionOutput * distFactorAbsorption
                                            ));

                            final double cellTransferAmountLimited;
                            if ((calcCell.getEnergy() + otherCellTransferAmountLimit) > calcCell.getMaxEnergy()) {
                                cellTransferAmountLimited = calcCell.getMaxEnergy() - calcCell.getEnergy();
                            } else {
                                cellTransferAmountLimited = otherCellTransferAmountLimit;
                            }
                            calcCell.setEnergy(calcCell.getEnergy() + cellTransferAmountLimited);
                            otherCell.setEnergy(otherCell.getEnergy() - cellTransferAmountLimited);
                        }
                        // --- Delivery ---
                        final double deliveryOutput = sensor.getEnergyDelivery();
                        if ((deliveryOutput > 0.0D) && (calcCell.getEnergy() > MIN_ENERGY_FOR_TRANSFER)) {
                            final double cellTransferAmountLimit =
                                        Math.min(
                                                calcCell.getEnergy(),
                                                Math.min(
                                                        MAX_ENERGY_DELIVERY_TRANSFER,
                                                        deliveryOutput * distFactorDelivery
                                            ));
                            final double otherCellTransferAmountLimited;
                            if ((otherCell.getEnergy() + cellTransferAmountLimit) > otherCell.getMaxEnergy()) {
                                otherCellTransferAmountLimited = otherCell.getMaxEnergy() - otherCell.getEnergy();
                            } else {
                                otherCellTransferAmountLimited = cellTransferAmountLimit;
                            }
                            calcCell.setEnergy(calcCell.getEnergy() - otherCellTransferAmountLimited);
                            otherCell.setEnergy(otherCell.getEnergy() + otherCellTransferAmountLimited);
                        }
                    }
                }
            }
        }
    }
}
