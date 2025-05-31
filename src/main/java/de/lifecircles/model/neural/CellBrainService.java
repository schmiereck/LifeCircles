package de.lifecircles.model.neural;

import java.util.List;
import java.util.Objects;

import de.lifecircles.model.*;
import de.lifecircles.service.SimulationConfig;

public class CellBrainService {

    public static double[] generateInputs(final Cell cell) {
        final List<SensorActor> myActorList = cell.getSensorActors();
        final int actorCount = myActorList.size();
        final CellBrainInterface cellBrain = cell.getBrain();
        final int totalInputs = cellBrain.getInputCount();
        final double[] inputs = new double[totalInputs];

        // Global inputs (cell type and energy)
        CellType cellType = cell.getType();
        inputs[GlobalInputFeature.MY_CELL_TYPE_RED.ordinal()] = cellType.getRed();
        inputs[GlobalInputFeature.MY_CELL_TYPE_GREEN.ordinal()] = cellType.getGreen();
        inputs[GlobalInputFeature.MY_CELL_TYPE_BLUE.ordinal()] = cellType.getBlue();
        inputs[GlobalInputFeature.MY_CELL_ENERGY.ordinal()] = cell.getEnergy();
        inputs[GlobalInputFeature.MY_CELL_AGE.ordinal()] = cell.getAge();
        inputs[GlobalInputFeature.MY_CELL_SUNRAY_HIT.ordinal()] = cell.isSunRayHit() ? 1.0 : 0.0;

        // Add cell state as global input
        double[] normalizedCellState = cell.getNormalizedCellState();

        inputs[GlobalInputFeature.MY_CELL_STATE_0.ordinal()] = normalizedCellState[0];
        inputs[GlobalInputFeature.MY_CELL_STATE_1.ordinal()] = normalizedCellState[1];
        inputs[GlobalInputFeature.MY_CELL_STATE_2.ordinal()] = normalizedCellState[2];

        int baseGlobal = GlobalInputFeature.values().length;

        final int topSensorIndex = cell.getTopSensorIndex();

        // Sensor inputs per actor using enum ordinals
        for (int actorPos = 0; actorPos < actorCount; actorPos++) {
            final SensorActor maActor = myActorList.get(actorPos);
            final CellType myActorType = maActor.getType();
            inputs[baseGlobal + SensorInputFeature.MY_ACTOR_RED.ordinal()] = myActorType.getRed();
            inputs[baseGlobal + SensorInputFeature.MY_ACTOR_GREEN.ordinal()] = myActorType.getGreen();
            inputs[baseGlobal + SensorInputFeature.MY_ACTOR_BLUE.ordinal()] = myActorType.getBlue();
            inputs[baseGlobal + SensorInputFeature.MY_ACTOR_FORCE_STRENGTH.ordinal()] = maActor.getForceStrength();
            inputs[baseGlobal + SensorInputFeature.MY_ACTOR_TOP_POSITION.ordinal()] = (actorPos == topSensorIndex) ? 1.0 : 0.0;

            // Use cached sensing from ActorSensorCellCalcService
            final double sensedCellTypeR;
            final double sensedCellTypeG;
            final double sensedCellTypeB;
            final double sensedCellEnergy;
            final double sensedCellAge;
            final SensableCell sensedCell = maActor.getSensedCell();
            if (Objects.nonNull(sensedCell)) {
                final CellType sensedCellType = sensedCell.getType();

                // --- Entfernung berechnen (0.0 = fern, 1.0 = nah) ---
                double dist = 0.0;
                double maxDist = 1.0;
                if (maActor.getCachedPosition() != null && maActor.getSensedActor() != null && maActor.getSensedActor().getCachedPosition() != null) {
                    dist = maActor.getCachedPosition().distance(maActor.getSensedActor().getCachedPosition());
                    int totalSensors = maActor.getParentCell().getSensorActors().size();
                    maxDist = de.lifecircles.service.SensorActorForceCellCalcService.calcSensorRadius(maActor.getParentCell().getRadiusSize(), totalSensors);
                }
                double distanceFactor = 1.0 - Math.min(dist / maxDist, 1.0); // 1.0 = nah, 0.0 = fern
                // Werte mit Distanzfaktor multiplizieren
                sensedCellTypeR = sensedCellType.getRed() * distanceFactor;
                sensedCellTypeG = sensedCellType.getGreen() * distanceFactor;
                sensedCellTypeB = sensedCellType.getBlue() * distanceFactor;

                sensedCellEnergy = sensedCell.getEnergy();
                sensedCellAge = sensedCell.getAge();

                // Sensed cell state as sensor input:
                double[] normalizedSensedCellState = sensedCell.getNormalizedCellState();

                inputs[baseGlobal + SensorInputFeature.SENSED_CELL_STATE_0.ordinal()] = normalizedSensedCellState[0];
                inputs[baseGlobal + SensorInputFeature.SENSED_CELL_STATE_1.ordinal()] = normalizedSensedCellState[1];
                inputs[baseGlobal + SensorInputFeature.SENSED_CELL_STATE_2.ordinal()] = normalizedSensedCellState[2];
            } else {
                sensedCellTypeR = 0;
                sensedCellTypeG = 0;
                sensedCellTypeB = 0;
                sensedCellEnergy = 0;
                sensedCellAge = 0;

                inputs[baseGlobal + SensorInputFeature.SENSED_CELL_STATE_0.ordinal()] = 0;
                inputs[baseGlobal + SensorInputFeature.SENSED_CELL_STATE_1.ordinal()] = 0;
                inputs[baseGlobal + SensorInputFeature.SENSED_CELL_STATE_2.ordinal()] = 0;
            }

            inputs[baseGlobal + SensorInputFeature.SENSED_CELL_TYPE_RED.ordinal()] = sensedCellTypeR;
            inputs[baseGlobal + SensorInputFeature.SENSED_CELL_TYPE_GREEN.ordinal()] = sensedCellTypeG;
            inputs[baseGlobal + SensorInputFeature.SENSED_CELL_TYPE_BLUE.ordinal()] = sensedCellTypeB;

            inputs[baseGlobal + SensorInputFeature.SENSED_CELL_ENERGY.ordinal()] = sensedCellEnergy;
            inputs[baseGlobal + SensorInputFeature.SENSED_CELL_AGE.ordinal()] = sensedCellAge;

            final SensableActor sensedActor = maActor.getSensedActor();
            if (Objects.nonNull(sensedActor)) {
                final CellType sensedActorType = sensedActor.getType();
                inputs[baseGlobal + SensorInputFeature.SENSED_ACTOR_TYPE_RED.ordinal()] = sensedActorType.getRed();
                inputs[baseGlobal + SensorInputFeature.SENSED_ACTOR_TYPE_GREEN.ordinal()] = sensedActorType.getGreen();
                inputs[baseGlobal + SensorInputFeature.SENSED_ACTOR_TYPE_BLUE.ordinal()] = sensedActorType.getBlue();
                inputs[baseGlobal + SensorInputFeature.SENSED_ACTOR_FORCE_STRENGTH.ordinal()] = sensedActor.getForceStrength();
            } else {
                inputs[baseGlobal + SensorInputFeature.SENSED_ACTOR_TYPE_RED.ordinal()] = 0;
                inputs[baseGlobal + SensorInputFeature.SENSED_ACTOR_TYPE_GREEN.ordinal()] = 0;
                inputs[baseGlobal + SensorInputFeature.SENSED_ACTOR_TYPE_BLUE.ordinal()] = 0;
                inputs[baseGlobal + SensorInputFeature.SENSED_ACTOR_FORCE_STRENGTH.ordinal()] = 0;
            }

            baseGlobal += SensorInputFeature.values().length;
        }

        return inputs;
    }

    public static void applyOutputs(final Cell cell, final double[] outputs) {
        // Apply size output
        final SimulationConfig config = SimulationConfig.getInstance();
        cell.setRadiusSize((outputs[GlobalOutputFeature.SIZE.ordinal()] *
                (config.getCellMaxRadiusSize() - config.getCellMinRadiusSize())) +
                config.getCellMinRadiusSize());

        int index = GlobalOutputFeature.values().length;

        // Apply actor outputs
        final List<SensorActor> actorList = cell.getSensorActors();
        for (final SensorActor actor : actorList) {
            // Set actor type
            final CellType newType = new CellType(
                    outputs[index + ActorOutputFeature.TYPE_RED.ordinal()],
                    outputs[index + ActorOutputFeature.TYPE_GREEN.ordinal()],
                    outputs[index + ActorOutputFeature.TYPE_BLUE.ordinal()]
            );
            actor.setType(newType);

            // Set force strength (range -1 to 1)
            //actor.setForceStrength(outputs[index + ActorOutputFeature.FORCE.ordinal()] * 2.0D - 1.0D);
            //actor.setForceStrength((outputs[index + ActorOutputFeature.FORCE.ordinal()] *
            //        SimulationConfig.getInstance().getCellActorMaxForceStrength() * 2.0D) -
            //        SimulationConfig.getInstance().getCellActorMaxForceStrength());

            final double force = outputs[index + ActorOutputFeature.FORCE.ordinal()] * 2.0D - 1.0D;
            final double forceStrength;
            if (force < 0.0D) {
                // attractive
                forceStrength = force * config.getCellActorMaxAttractiveForceStrength();
            } else {
                // repulsive
                forceStrength = force * config.getCellActorMaxRepulsiveForceStrength();
            }
            actor.setForceStrength(forceStrength);

            actor.setEnergyAbsorption(outputs[index + ActorOutputFeature.ENERGY_ABSORPTION.ordinal()]);
            actor.setEnergyDelivery(outputs[index + ActorOutputFeature.ENERGY_DELIVERY.ordinal()]);
            actor.setReproductionEnergyShareOutput(outputs[index + ActorOutputFeature.REPRODUCTION_ENERGY_SHARE.ordinal()]);
            actor.setReproductionDesire(outputs[index + ActorOutputFeature.REPRODUCTION_DESIRE.ordinal()]);
            final double state0Output = (outputs[index + ActorOutputFeature.STATE_0.ordinal()]);
            final double state1Output = (outputs[index + ActorOutputFeature.STATE_1.ordinal()]);
            final double state2Output = (outputs[index + ActorOutputFeature.STATE_2.ordinal()]);
            // Berechnung des Zell-Zustands der Kind-Zelle basierend auf Thresholds
            int childState = 0;
            if (state0Output >= config.getCellStateOutputThreshold()) {
                childState |= 1; // Setze Bit 0
            }
            if (state1Output >= config.getCellStateOutputThreshold()) {
                childState |= 2; // Setze Bit 1
            }
            if (state2Output >= config.getCellStateOutputThreshold()) {
                childState |= 4; // Setze Bit 2
            }
            actor.setReproductionState(childState);

            index += ActorOutputFeature.values().length;
        }
    }

    /**
     * Updates the cell's behavior based on its current state and environment.
     */
    public static void think(final Cell cell) {
        final CellBrainInterface cellBrain = cell.getBrain();

        final double[] inputs = CellBrainService.generateInputs(cell);
        cellBrain.setInputs(inputs);

        final double[] outputs = cellBrain.process();

        CellBrainService.applyOutputs(cell, outputs);
    }

}
