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
    private final Cell cell;
    private final NeuralNetwork network;

    public CellBrain(Cell cell) {
        this.cell = cell;
        
        int inputCount = GlobalInputFeature.values().length +
                         (SensorInputFeature.values().length * cell.getSensorActors().size());
        
        int outputCount = GlobalInputFeature.values().length +
                         (ActorOutputFeature.values().length * cell.getSensorActors().size());
        
        int hiddenCount = (inputCount + outputCount) * 2; // Arbitrary hidden layer size
        
        this.network = new NeuralNetwork(inputCount, hiddenCount, outputCount);
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

    public NeuralNetwork getNetwork() {
        return this.network;
    }
}
