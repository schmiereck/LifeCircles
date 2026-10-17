package de.lifecircles.model;

import de.lifecircles.model.neural.*;
import de.lifecircles.service.SimulationConfig;
import de.lifecircles.service.EnergyAllocationService;

public abstract class CellFactory {

    public static Cell createCell(final Vector2D position, final double radiusSize) {
        return founder(new Cell(position, radiusSize, CellBrain.createDefault()));
    }

    public static Cell createCell(final Vector2D position, final double radiusSize, final double hiddenCountFactor,
                                  final double stateHiddenLayerSynapseConnectivity, final double hiddenLayerSynapseConnectivity) {
        final int sensorActorCount = SimulationConfig.CELL_SENSOR_ACTOR_COUNT;
        final NeuronValueFunctionFactory neuronValueFunctionFactory = new ValuesNeuronValueFunctionFactory();

        final CellBrain cellBrain = new CellBrain(neuronValueFunctionFactory,
                calcInputCount(sensorActorCount), calcOutputCount(sensorActorCount),
                hiddenCountFactor,
                stateHiddenLayerSynapseConnectivity, hiddenLayerSynapseConnectivity);

        return founder(new Cell(position, radiusSize, cellBrain));
    }

    public static Cell createCell(final Vector2D position, final double radiusSize, final CellBrainInterface cellBrain) {
        return founder(new Cell(position, radiusSize, cellBrain));
    }
    private static Cell founder(Cell cell) { EnergyAllocationService.initializeFounder(cell,1);return cell; }

    public static int calcInputCount(final int sensorActorCount) {
        return EnergyInputLayout.inputCount(sensorActorCount);
    }

    /** Construction size; Cell appends the enzyme throttle (OutputLayout) as a disconnected output. */
    public static int calcOutputCount(final int sensorActorCount) {
        return OutputLayout.legacyOutputCount(sensorActorCount);
    }
}
