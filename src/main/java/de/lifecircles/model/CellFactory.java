package de.lifecircles.model;

import de.lifecircles.model.neural.*;
import de.lifecircles.service.SimulationConfig;

public abstract class CellFactory {

    public static Cell createCell(final Vector2D position, final double radiusSize) {
        return createCell(position, radiusSize, SimulationConfig.hiddenCountFactorDefault,
                SimulationConfig.stateHiddenLayerSynapseConnectivityDefault,
                SimulationConfig.brainSynapseConnectivityDefault);
    }

    public static Cell createCell(final Vector2D position, final double radiusSize, final double hiddenCountFactor,
                                  final double stateHiddenLayerSynapseConnectivity, final double synapseConnectivity) {
        final int sensorActorCount = SimulationConfig.CELL_SENSOR_ACTOR_COUNT;

        final CellBrain cellBrain = new CellBrain(calcInputCount(sensorActorCount), calcOutputCount(sensorActorCount),
                hiddenCountFactor,
                stateHiddenLayerSynapseConnectivity, synapseConnectivity);

        return new Cell(position, radiusSize, cellBrain);
    }

    public static Cell createCell(final Vector2D position, final double radiusSize, final CellBrainInterface cellBrain) {
        return new Cell(position, radiusSize, cellBrain);
    }

    public static int calcInputCount(final int sensorActorCount) {
        return GlobalInputFeature.values().length +
                (SensorInputFeature.values().length * sensorActorCount);
    }

    public static int calcOutputCount(final int sensorActorCount) {
        return GlobalOutputFeature.values().length +
                (ActorOutputFeature.values().length * sensorActorCount);
    }
}
