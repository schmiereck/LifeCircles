package de.lifecircles.service.trainStrategy;

import de.lifecircles.model.Cell;
import de.lifecircles.model.Environment;
import de.lifecircles.model.Vector2D;
import de.lifecircles.model.neural.*;
import de.lifecircles.service.SimulationConfig;

public abstract class TestTrainStrategyUtils {

    public static void createAndAddCell(Environment environment, double x, double y, final double radiusSize,
                                         final boolean isAttraction) {
        final boolean isRepulsion = !isAttraction;
        final boolean isDelivery = false;
        final boolean isAbsorbtion = false;
        createAndAddCell(environment, x, y, radiusSize,
                isAttraction, isRepulsion, isDelivery, isAbsorbtion);
    }

    public static void createAndAddCell(Environment environment, double x, double y, final double radiusSize,
                                         final boolean isAttraction, final boolean isDelivery) {
        final boolean isRepulsion = !isAttraction;
        final boolean isAbsorbtion = false;
        createAndAddCell(environment, x, y, radiusSize,
                isAttraction, isRepulsion, isDelivery, isAbsorbtion);
    }

    public static void createAndAddCell(Environment environment, double x, double y, final double radiusSize,
                                         final boolean isAttraction, final boolean isDelivery, final boolean isAbsorbtion) {
        final boolean isRepulsion = !isAttraction;
        createAndAddCell(environment, x, y, radiusSize,
                isAttraction, isRepulsion, isDelivery, isAbsorbtion);
    }

    public static void createAndAddCell(Environment environment, double x, double y, final double radiusSize,
                                         final boolean isAttraction, final boolean isRepulsion, final boolean isDelivery, final boolean isAbsorbtion) {
        final CellBrainInterface cellBrain = new CellBrainInterface() {

            final double[] output;
            final double[] inputs;

            {
                int inputCount = GlobalInputFeature.values().length +
                        (SensorInputFeature.values().length * SimulationConfig.CELL_SENSOR_ACTOR_COUNT);
                this.inputs = new double[inputCount];

                // Initialize the output array with the desired size
                int outputCount = GlobalOutputFeature.values().length +
                        (ActorOutputFeature.values().length * SimulationConfig.CELL_SENSOR_ACTOR_COUNT);

                this.output = new double[outputCount];

                this.output[GlobalOutputFeature.SIZE.ordinal()] = radiusSize;

                final int offset = GlobalOutputFeature.values().length;

                for (int actorPos = 0; actorPos < SimulationConfig.CELL_SENSOR_ACTOR_COUNT; actorPos++) {
                    final int actorOffset = offset + (actorPos * ActorOutputFeature.values().length);

                    this.output[actorOffset + ActorOutputFeature.TYPE_RED.ordinal()] = 0.0D;
                    this.output[actorOffset + ActorOutputFeature.TYPE_GREEN.ordinal()] = 1.0D;
                    this.output[actorOffset + ActorOutputFeature.TYPE_BLUE.ordinal()] = 0.0D;

                    this.output[actorOffset + ActorOutputFeature.REPRODUCTION_DESIRE.ordinal()] = 0.0D;

                    if (isDelivery) {
                        this.output[actorOffset + ActorOutputFeature.ENERGY_DELIVERY.ordinal()] = 0.75D;
                    } else {
                        this.output[actorOffset + ActorOutputFeature.ENERGY_DELIVERY.ordinal()] = 0.0D;
                    }

                    if (isAbsorbtion) {
                        this.output[actorOffset + ActorOutputFeature.ENERGY_ABSORPTION.ordinal()] = 0.75D;
                    } else {
                        this.output[actorOffset + ActorOutputFeature.ENERGY_ABSORPTION.ordinal()] = 0.0D;
                    }


                    if (isAttraction) {
                        this.output[actorOffset + ActorOutputFeature.FORCE.ordinal()] = 0.0D;
                    } else {
                        if (isRepulsion) {
                            this.output[actorOffset + ActorOutputFeature.FORCE.ordinal()] = 1.0D;
                        } else {
                            this.output[actorOffset + ActorOutputFeature.FORCE.ordinal()] = 0.5D;
                        }

                    }
                }
            }

            @Override
            public double getOutputValue(int outputNeuronPos) {
                return this.output[outputNeuronPos];
            }

            @Override
            public int getInputCount() {
                return this.inputs.length;
            }

            @Override
            public int getSynapseCount() {
                return 0;
            }

            @Override
            public boolean[] determineActiveHiddenLayers(int cellState) {
                return new boolean[0];
            }

            @Override
            public void setInputs(double[] inputs) {

            }

            @Override
            public double[] process() {
                return this.output;
            }

            @Override
            public NeuralNetwork mutate(double mutationRate, double mutationStrength) {
                return null;
            }
        };

        final Cell cell = new Cell(new Vector2D(x, y), radiusSize, cellBrain);

        environment.addCell(cell);
    }
}
