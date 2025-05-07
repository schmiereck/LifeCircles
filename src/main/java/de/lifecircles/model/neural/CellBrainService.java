package de.lifecircles.model.neural;

import java.util.List;

import de.lifecircles.model.Cell;
import de.lifecircles.model.CellType;
import de.lifecircles.model.SensorActor;

public class CellBrainService {
    /** Threshold for energy beam trigger output */
    public static final double ENERGY_BEAM_THRESHOLD = 0.9;

    public static double[] generateInputs(final Cell cell) {
        final List<SensorActor> myActorList = cell.getSensorActors();
        final int actorCount = myActorList.size();
        final CellBrain cellBrain = cell.getBrain();
        final NeuralNetwork network = cellBrain.getNetwork();
        final int totalInputs = network.getInputCount();
        final double[] inputs = new double[totalInputs];

        // Global inputs (cell type and energy)
        CellType cellType = cell.getType();
        inputs[GlobalInputFeature.MY_CELL_TYPE_RED.ordinal()] = cellType.getRed();
        inputs[GlobalInputFeature.MY_CELL_TYPE_GREEN.ordinal()] = cellType.getGreen();
        inputs[GlobalInputFeature.MY_CELL_TYPE_BLUE.ordinal()] = cellType.getBlue();
        inputs[GlobalInputFeature.ENERGY.ordinal()] = cell.getEnergy();
        inputs[GlobalInputFeature.AGE.ordinal()] = cell.getAge();
        inputs[GlobalInputFeature.SUNRAY_HIT.ordinal()] = cell.isSunRayHit() ? 1.0 : 0.0;

        int baseGlobal = GlobalInputFeature.values().length;

        final int topSensorIndex = cell.getTopSensorIndex();

        // Sensor inputs per actor using enum ordinals
        for (int actorPos = 0; actorPos < actorCount; actorPos++) {
            final SensorActor maActor = myActorList.get(actorPos);
            final CellType myActorType = maActor.getType();
            inputs[baseGlobal + SensorInputFeature.MY_ACTOR_RED.ordinal()] = myActorType.getRed();
            inputs[baseGlobal + SensorInputFeature.MY_ACTOR_GREEN.ordinal()] = myActorType.getGreen();
            inputs[baseGlobal + SensorInputFeature.MY_ACTOR_BLUE.ordinal()] = myActorType.getBlue();
            inputs[baseGlobal + SensorInputFeature.FORCE_STRENGTH.ordinal()] = maActor.getForceStrength();
            // Use cached sensing from ActorSensorCellCalcService
            final CellType sensedCellType;
            if (maActor.getSensedCell() != null) {
                sensedCellType = maActor.getSensedCell().getType();
            } else {
                sensedCellType = new CellType(0, 0, 0);
            }
            inputs[baseGlobal + SensorInputFeature.SENSED_CELL_TYPE_RED.ordinal()] = sensedCellType.getRed();
            inputs[baseGlobal + SensorInputFeature.SENSED_CELL_TYPE_GREEN.ordinal()] = sensedCellType.getGreen();
            inputs[baseGlobal + SensorInputFeature.SENSED_CELL_TYPE_BLUE.ordinal()] = sensedCellType.getBlue();
            final double sensedForce = (maActor.getSensedActor() != null)
                    ? maActor.getSensedActor().getForceStrength()
                    : 0.0;
            inputs[baseGlobal + SensorInputFeature.SENSED_FORCE_STRENGTH.ordinal()] = sensedForce;
            // Sensed actor color inputs
            final SensorActor sensedActor = maActor.getSensedActor();
            if (sensedActor != null) {
                final CellType sensedActorType = sensedActor.getType();
                inputs[baseGlobal + SensorInputFeature.SENSED_ACTOR_TYPE_RED.ordinal()] = sensedActorType.getRed();
                inputs[baseGlobal + SensorInputFeature.SENSED_ACTOR_TYPE_GREEN.ordinal()] = sensedActorType.getGreen();
                inputs[baseGlobal + SensorInputFeature.SENSED_ACTOR_TYPE_BLUE.ordinal()] = sensedActorType.getBlue();
            } else {
                inputs[baseGlobal + SensorInputFeature.SENSED_ACTOR_TYPE_RED.ordinal()] = 0.0;
                inputs[baseGlobal + SensorInputFeature.SENSED_ACTOR_TYPE_GREEN.ordinal()] = 0.0;
                inputs[baseGlobal + SensorInputFeature.SENSED_ACTOR_TYPE_BLUE.ordinal()] = 0.0;
            }
            // Add TOP_POSITION input based on actual sensor position
            inputs[baseGlobal + SensorInputFeature.TOP_POSITION.ordinal()] = (actorPos == topSensorIndex) ? 1.0 : 0.0;
            baseGlobal += SensorInputFeature.values().length;
        } 

        return inputs;
    }

    public static void applyOutputs(final Cell cell, double[] outputs) {
        // Apply size output
        cell.setRadiusSize(outputs[GlobalOutputFeature.SIZE.ordinal()] * 40 + 10); // Scale to 10-50 range
        // Set reproduction desire output
        cell.setReproductionDesire(outputs[GlobalOutputFeature.REPRODUCTION_DESIRE.ordinal()]);

        int index = GlobalOutputFeature.values().length;

        // Apply actor outputs
        List<SensorActor> actors = cell.getSensorActors();
        for (SensorActor actor : actors) {
            // Set actor type
            CellType newType = new CellType(
                outputs[index + ActorOutputFeature.TYPE_RED.ordinal()],
                outputs[index + ActorOutputFeature.TYPE_GREEN.ordinal()],
                outputs[index + ActorOutputFeature.TYPE_BLUE.ordinal()]  
            );
            actor.setType(newType);

            // Set force strength (range -1 to 1)
            actor.setForceStrength(outputs[index + ActorOutputFeature.FORCE.ordinal()] * 2 - 1);
            // Set energy beam trigger
            actor.setFireEnergyBeam(outputs[index + ActorOutputFeature.ENERGY_BEAM.ordinal()] > ENERGY_BEAM_THRESHOLD);

            index += ActorOutputFeature.values().length;
        }
    }

    /**
     * Updates the cell's behavior based on its current state and environment.
     */
    public static void think(final Cell cell) {
        final CellBrain cellBrain = cell.getBrain();
        final NeuralNetwork network = cellBrain.getNetwork();

        final double[] inputs = CellBrainService.generateInputs(cell);
        network.setInputs(inputs);
        final double[] outputs = network.process();
        CellBrainService.applyOutputs(cell, outputs);
    }
    
}
