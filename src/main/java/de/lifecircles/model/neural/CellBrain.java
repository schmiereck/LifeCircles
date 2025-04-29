package de.lifecircles.model.neural;

import de.lifecircles.model.Cell;
import de.lifecircles.model.CellType;
import de.lifecircles.model.SensorActor;
import de.lifecircles.model.reproduction.ReproductionManager;
import de.lifecircles.service.ActorSensorCellCalcService;
import de.lifecircles.model.neural.SensorInputFeature;
import de.lifecircles.model.neural.GlobalInputFeature;

import java.util.List;
import java.util.Random;

/**
 * Manages the neural network that controls cell behavior.
 */
public class CellBrain {
    // Neural network input counts
    private static final int SENSOR_INPUTS_PER_ACTOR = SensorInputFeature.values().length; 
    private static final int CELL_TYPE_INPUTS = 3; // R,G,B
    private static final int ENERGY_INPUTS = 1; // cell energy

    // Neural network output counts
    private static final int SIZE_OUTPUT = 1;
    private static final int OUTPUTS_PER_ACTOR = 5; // type(R,G,B) + force + beam trigger
    private static final int REPRODUCTION_OUTPUT = 1; // reproduction desire

    /** Threshold for energy beam trigger output */
    private static final double ENERGY_BEAM_THRESHOLD = 0.9;

    private final Cell cell;
    private final NeuralNetwork network;

    public CellBrain(Cell cell) {
        this.cell = cell;
        
        int inputCount = (SENSOR_INPUTS_PER_ACTOR * cell.getSensorActors().size()) +
                        CELL_TYPE_INPUTS +
                        ENERGY_INPUTS;
        
        int outputCount = SIZE_OUTPUT +
                         (OUTPUTS_PER_ACTOR * cell.getSensorActors().size()) +
                         REPRODUCTION_OUTPUT;
        
        int hiddenCount = (inputCount + outputCount) * 2; // Arbitrary hidden layer size
        
        this.network = new NeuralNetwork(inputCount, hiddenCount, outputCount);
    }

    /**
     * Updates the cell's behavior based on its current state and environment.
     * @param neighbors List of neighboring cells
     */
    public void think(List<Cell> neighbors) {
        double[] inputs = generateInputs(neighbors);
        network.setInputs(inputs);
        double[] outputs = network.process();
        applyOutputs(outputs);
    }

    /**
     * Creates a mutated copy of this brain for a new cell.
     */
    public CellBrain mutate(Cell newCell) {
        CellBrain childBrain = new CellBrain(newCell);
        
        // Copy and mutate network weights and biases
        childBrain.network.copyFrom(this.network);
        childBrain.network.mutate(
            ReproductionManager.getMutationRate(),
            ReproductionManager.getMutationStrength()
        );
        
        return childBrain;
    }


    private double[] generateInputs(List<Cell> neighbors) {
        List<SensorActor> myActorList = cell.getSensorActors();
        int actorCount = myActorList.size();
        int totalInputs = network.getInputCount();
        double[] inputs = new double[totalInputs];

        // Sensor inputs per actor using enum ordinals
        for (int i = 0; i < actorCount; i++) {
            SensorActor maActor = myActorList.get(i);
            int base = i * SENSOR_INPUTS_PER_ACTOR;
            CellType myActorType = maActor.getType();
            inputs[base + SensorInputFeature.ACTOR_RED.ordinal()] = myActorType.getRed();
            inputs[base + SensorInputFeature.ACTOR_GREEN.ordinal()] = myActorType.getGreen();
            inputs[base + SensorInputFeature.ACTOR_BLUE.ordinal()] = myActorType.getBlue();
            inputs[base + SensorInputFeature.FORCE_STRENGTH.ordinal()] = maActor.getForceStrength();
            // Use cached sensing from ActorSensorCellCalcService
            CellType sensedCellType;
            if (maActor.getSensedCell() != null) {
                sensedCellType = maActor.getSensedCell().getType();
            } else {
                sensedCellType = new CellType(0, 0, 0);
            }
            inputs[base + SensorInputFeature.SENSED_RED.ordinal()] = sensedCellType.getRed();
            inputs[base + SensorInputFeature.SENSED_GREEN.ordinal()] = sensedCellType.getGreen();
            inputs[base + SensorInputFeature.SENSED_BLUE.ordinal()] = sensedCellType.getBlue();
            double sensedForce = (maActor.getSensedActor() != null)
                    ? maActor.getSensedActor().getForceStrength()
                    : 0.0;
            inputs[base + SensorInputFeature.SENSED_FORCE_STRENGTH.ordinal()] = sensedForce;
            // Sensed actor color inputs
            SensorActor sensedActor = maActor.getSensedActor();
            if (sensedActor != null) {
                CellType sensedActorType = sensedActor.getType();
                inputs[base + SensorInputFeature.SENSED_ACTOR_RED.ordinal()] = sensedActorType.getRed();
                inputs[base + SensorInputFeature.SENSED_ACTOR_GREEN.ordinal()] = sensedActorType.getGreen();
                inputs[base + SensorInputFeature.SENSED_ACTOR_BLUE.ordinal()] = sensedActorType.getBlue();
            } else {
                inputs[base + SensorInputFeature.SENSED_ACTOR_RED.ordinal()] = 0.0;
                inputs[base + SensorInputFeature.SENSED_ACTOR_GREEN.ordinal()] = 0.0;
                inputs[base + SensorInputFeature.SENSED_ACTOR_BLUE.ordinal()] = 0.0;
            }
        } 

        // Global inputs (cell type and energy)
        int baseGlobal = actorCount * SENSOR_INPUTS_PER_ACTOR;
        CellType cellType = cell.getType();
        inputs[baseGlobal + GlobalInputFeature.CELL_RED.ordinal()] = cellType.getRed();
        inputs[baseGlobal + GlobalInputFeature.CELL_GREEN.ordinal()] = cellType.getGreen();
        inputs[baseGlobal + GlobalInputFeature.CELL_BLUE.ordinal()] = cellType.getBlue();

        inputs[baseGlobal + GlobalInputFeature.ENERGY.ordinal()] = cell.getEnergy();

        return inputs;
    }

    private void applyOutputs(double[] outputs) {
        int index = 0;

        // Apply size output
        cell.setSize(outputs[index++] * 40 + 10); // Scale to 10-50 range

        // Apply actor outputs
        List<SensorActor> actors = cell.getSensorActors();
        for (SensorActor actor : actors) {
            // Set actor type
            CellType newType = new CellType(
                outputs[index++],
                outputs[index++],
                outputs[index++]  
            );
            actor.setType(newType);

            // Set force strength (range -1 to 1)
            actor.setForceStrength(outputs[index++] * 2 - 1);
            // Set energy beam trigger
            actor.setFireEnergyBeam(outputs[index++] > ENERGY_BEAM_THRESHOLD);
        }
        // Set reproduction desire output
        cell.setReproductionDesire(outputs[index++]);
    }

    private int getInputCount() {
        return (SENSOR_INPUTS_PER_ACTOR * cell.getSensorActors().size()) +
               CELL_TYPE_INPUTS +
               ENERGY_INPUTS;
    }

    public int getSynapseCount() {
        return this.network.getSynapseCount();
    }

    private CellType findFirstSensedCell(SensorActor actor, List<Cell> neighbors) {
        actor.setSensedCell(null);
        actor.setSensedActor(null);
        for (Cell otherCell : neighbors) {
            for (SensorActor otherActor : otherCell.getSensorActors()) {
                double intensity = ActorSensorCellCalcService.sense(actor, otherActor);
                if (intensity != 0) {
                    actor.setSensedCell(otherCell);
                    actor.setSensedActor(otherActor);
                    return otherCell.getType();
                }
            }
        }
        // No sensed cell: return default (e.g., black)
        return new CellType(0, 0, 0);
    }

    /**
     * Returns the force strength of the first sensed actor, or 0 if none.
     */
    private double findFirstSensedActorForce(SensorActor actor, List<Cell> neighbors) {
        for (Cell otherCell : neighbors) {
            for (SensorActor otherActor : otherCell.getSensorActors()) {
                double intensity = ActorSensorCellCalcService.sense(actor, otherActor);
                if (intensity != 0) {
                    return otherActor.getForceStrength();
                }
            }
        }
        return 0.0;
    }

    /**
     * Returns the first sensed SensorActor from neighbors, or null if none.
     */
    private SensorActor findFirstSensedActor(SensorActor actor, List<Cell> neighbors) {
        actor.setSensedActor(null);
        actor.setSensedCell(null);
        for (Cell otherCell : neighbors) {
            for (SensorActor otherActor : otherCell.getSensorActors()) {
                double intensity = ActorSensorCellCalcService.sense(actor, otherActor);
                if (intensity != 0) {
                    actor.setSensedActor(otherActor);
                    actor.setSensedCell(otherCell);
                    return otherActor;
                }
            }
        }
        return null;
    }
}
