package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.service.partitioningStrategy.PartitioningStrategy;

import java.util.List;

/**
 * Service responsible for processing sensor-actor interactions between cells.
 */
public class SensorActorForceCellCalcService {

    /**
     * Optimized processing of sensor/actor interactions using a partitioning strategy.
     */
    public static void processInteractions(final List<Cell> cells, final PartitioningStrategy partitioner) {
        // cache positions for all sensorActors in this simulation step
        //for (final Cell calcCell : cells) {
        cells.parallelStream().forEach(calcCell -> {
            for (final SensorActor actor : calcCell.getSensorActors()) {
                actor.updateCachedPosition();
                actor.setSensedCell(null);
                actor.setSensedActor(null);
            }
        });
        
        // Prüfe Blocker-Kollisionen
        checkBlockerCollisions(cells);
        
        cells.parallelStream().forEach(calcCell -> {
            for (final Cell otherCell : partitioner.getNeighbors(calcCell)) {
                if (calcCell != otherCell) {
                    processInteraction(calcCell, otherCell);
                }
            }
        });
    }

    /**
     * Berechne die Kraft, die der otherCellActor auf calcCellActor ausübt.
     */
    private static void processInteraction(final Cell calcCell, final Cell otherCell) {
        //final Vector2D delta = calcCell.getPosition().subtract(otherCell.getPosition());
        //final double cellDistance = delta.length();
        //final double combinedRadius = Math.max(otherCell.getRadiusSize(), calcCell.getRadiusSize());
        // Nur ausführen, wenn die Zellen sich nicht inneinander befinden.
        //if (cellDistance > combinedRadius) {

            // Process interactions in one direction only
            for (final SensorActor calcCellActor : calcCell.getSensorActors()) {
                // Wenn der Sensor bereits etwas wahrnimmt (z.B. einen Blocker), überspringen
                //if (calcCellActor.getSensedCell() != null) {
                //    continue;
                //}

                for (final SensorActor otherCellActor : otherCell.getSensorActors()) {
                    // Berechne die Kraft, die der otherCellActor auf calcCellActor ausübt
                    final Vector2D direction = calcCellActor.getCachedPosition().subtract(otherCellActor.getCachedPosition());
                    final double distance = direction.length();

                    if (distance > 0.0D) {
                        // sense(SensorActor sensorActor, SensorActor otherSensorActor)
                        //double distance = otherCellActor.getCachedPosition().distance(calcCellActor.getCachedPosition());
                        final int totalSensors = otherCellActor.getParentCell().getSensorActors().size();
                        final double maxSensorRadius = calcSensorRadius(otherCellActor.getParentCell().getRadiusSize(), totalSensors);
                        if (distance <= maxSensorRadius) {
                            final double senseForceValue = (distance / maxSensorRadius);

                            // Kraft von otherCellActor auf calcCellActor
                            final double otherForceStrength = otherCellActor.getForceStrength(); // Kraftstärke (positiv/negativ)
                            final double totalForceStrength = senseForceValue * otherForceStrength; // Berücksichtige Richtung und Stärke

                            if ((senseForceValue != 0.0D)) {// && (Math.abs(totalForceStrength) > Math.abs(foundForceStrength))) {
                                calcCellActor.setSensedCell(otherCell);
                                calcCellActor.setSensedActor(otherCellActor);

                                final double calcCellForceStrength = calcCell.getRadiusSize() / SimulationConfig.getInstance().getCellMaxRadiusSize();
                                final double otherCellForceStrength = otherCell.getRadiusSize() / SimulationConfig.getInstance().getCellMaxRadiusSize();

                                final Vector2D forceOnCalcCell = direction.normalize().multiply(totalForceStrength *
                                        calcCellForceStrength * otherCellForceStrength);
                                final Vector2D forceOnOtherCell = direction.normalize().multiply(-totalForceStrength *
                                        otherCellForceStrength * calcCellForceStrength);

                                calcCell.applyForce(forceOnCalcCell, calcCellActor.getCachedPosition());
                                otherCell.applyForce(forceOnOtherCell, calcCellActor.getCachedPosition());
                            }
                        }
                    }
                }
            //}
//            if (Objects.nonNull(foundOtherCellActor)) {
//                foundCalcCellActor.setSensedActor(foundOtherCellActor);
//                foundCalcCellActor.setSensedCell(otherCell);
//
//                final double usedForceStrength;
//                //// Abstoßung?
//                //if (foundForceStrength > 0.0D) {
//                //    usedForceStrength = foundForceStrength * 1.75D;
//                //} else {
//                    usedForceStrength = foundForceStrength;
//                //}
//                Vector2D forceOnCalcCell = foundDirection.normalize().multiply(usedForceStrength);
//                calcCell.applyForce(forceOnCalcCell, foundCalcCellActor.getCachedPosition(), deltaTime);
//            }
        }
    }

    /**
     * Überprüft, ob Sensoren mit Blockern kollidieren und setzt entsprechende Sensable-Objekte
     */
    private static void checkBlockerCollisions(List<Cell> cells) {
        // Hole die Blocker aus der Environment-Instanz
        Environment environment = Environment.getInstance();
        if (environment == null) return;
        
        List<Blocker> blockers = environment.getBlockerList();
        if (blockers == null || blockers.isEmpty()) return;
        
        // Prüfe für jeden Sensor, ob er einen Blocker berührt
        cells.parallelStream().forEach(cell -> {
            for (final SensorActor sensor : cell.getSensorActors()) {
                final Vector2D sensorPos = sensor.getCachedPosition();
                if (sensorPos != null) {
                    for (Blocker blocker : blockers) {
                        if (blocker.containsPoint(sensorPos)) {
                            // Setze den Blocker als wahrgenommenes Objekt
                            // Der SensorActor nimmt Blocker als Zelle wahr
                            sensor.setSensedCell(blocker);
                            sensor.setSensedActor(blocker);
                            break;
                        }
                    }
                }
            }
        });
    }

    public static double calcSensorRadius(final double radiusSize, final int totalSensors) {
        // Berechnung der vollen Sehnenlänge zwischen zwei benachbarten Sensoren
        return 2.0D * radiusSize * Math.sin(Math.PI / totalSensors) * SimulationConfig.cellActorMaxFieldRadiusFactor;
    }
}
