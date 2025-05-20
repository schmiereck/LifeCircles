package de.lifecircles.service.trainStrategy;

import de.lifecircles.model.Cell;
import de.lifecircles.model.Environment;
import de.lifecircles.model.Vector2D;
import de.lifecircles.model.neural.*;
import de.lifecircles.service.SimulationConfig;

public class TestBTrainStrategy implements TrainStrategy {
    private final SimulationConfig config = SimulationConfig.getInstance();

    @Override
    public Environment initializeEnvironment() {
        //config.setWidth(1600 * 3);
        this.config.setWidth(1200);
        this.config.setHeight(800);

        //config.setScaleSimulation(5.7D);
        this.config.setScaleSimulation(1.2D);

        config.setEnergyPerRay(0.01D); // 0.005; //0.015; // 0.025;

        this.config.setInitialCellCount(1);

        return new Environment(config.getWidth(), config.getHeight());
    }

    @Override
    public void initialize(Environment environment) {
        // Add ground blocker by default
        environment.addGroundBlocker();

        final double radiusSize = this.config.getCellMaxRadiusSize();

        final double yTop = this.config.getHeight() - this.config.getHeight() / 4.0D;

        // Anziehung:
        {
            final double x = this.config.getWidth() / 2.0D - radiusSize * 14.0D;
            final double y = yTop;

            this.createAndAddCell(environment, x, y, radiusSize, true, true);
            this.createAndAddCell(environment, x + radiusSize * 2.5D, y, radiusSize, true, true);
            this.createAndAddCell(environment, x + radiusSize * 1.25D, y - radiusSize * 1.25D, radiusSize, true, true);
            //this.createAndAddCell(environment, x - radiusSize * 2.25D, y, radiusSize);
        }
        {
            final double x = this.config.getWidth() / 2.0D - radiusSize * 21.0D;
            final double y = yTop;

            this.createAndAddCell(environment, x, y - radiusSize * 2.5D, radiusSize, true, true);

            this.createAndAddCell(environment, x - radiusSize * 1.25D, y - radiusSize * 1.25D, radiusSize, true, true);
            this.createAndAddCell(environment, x + radiusSize * 1.25D, y - radiusSize * 1.25D, radiusSize, true, true);

            this.createAndAddCell(environment, x - radiusSize * 2.4D, y, radiusSize, true, false);
            this.createAndAddCell(environment, x, y, radiusSize, true, false);
            this.createAndAddCell(environment, x + radiusSize * 2.4D, y, radiusSize, true, false);

            //this.createAndAddCell(environment, x - radiusSize * 2.25D, y, radiusSize);
        }
        {
            final double x = this.config.getWidth() / 2.0D - radiusSize * 7.0D;
            final double y = yTop;

            this.createAndAddCell(environment, x, y, radiusSize, true, true);
            this.createAndAddCell(environment, x + radiusSize * 2.25D, y, radiusSize, true, true);
            //this.createAndAddCell(environment, x - radiusSize * 2.25D, y, radiusSize);
        }
        {
            final double x = this.config.getWidth() / 2.0D; //  + radiusSize geht
            final double y = yTop;

            this.createAndAddCell(environment, x, y, radiusSize, true);
            this.createAndAddCell(environment, x + radiusSize * 2.25D, y, radiusSize, true, true);
            //this.createAndAddCell(environment, x - radiusSize * 2.25D, y, radiusSize);
        }
        {
            final double x = this.config.getWidth() / 2.0D + radiusSize * 7.0D;
            final double y = yTop;

            this.createAndAddCell(environment, x, y, radiusSize, true, true);
            this.createAndAddCell(environment, x + radiusSize * 2.25D, y, radiusSize, true, true);
            //this.createAndAddCell(environment, x - radiusSize * 2.25D, y, radiusSize);
        }
        {
            final double x = this.config.getWidth() / 2.0D + radiusSize * 15.0D;
            final double y = yTop;

            this.createAndAddCell(environment, x, y, radiusSize, true, true);
            this.createAndAddCell(environment, x + radiusSize * 2.25D, y, radiusSize, true, true);
            this.createAndAddCell(environment, x + radiusSize * 1.125D, y - radiusSize * 2.25D, radiusSize, true, true);
            //this.createAndAddCell(environment, x - radiusSize * 2.25D, y, radiusSize);
        }
        {
            final double x = this.config.getWidth() / 2.0D + radiusSize * 21.0D;
            final double y = yTop;

            this.createAndAddCell(environment, x, y, radiusSize, true);
            this.createAndAddCell(environment, x + radiusSize * 2.25D, y, radiusSize, true, true);
            this.createAndAddCell(environment, x + radiusSize * 1.125D, y - radiusSize * 2.25D, radiusSize, false, true);
            //this.createAndAddCell(environment, x - radiusSize * 2.25D, y, radiusSize);
        }
    }

    private void createAndAddCell(Environment environment, double x, double y, final double radiusSize,
                                  final boolean isAttraction) {
        final boolean isDelivery = false;
        createAndAddCell(environment, x, y, radiusSize,
                isAttraction, isDelivery);
    }

    private void createAndAddCell(Environment environment, double x, double y, final double radiusSize,
                                  final boolean isAttraction, final boolean isDelivery) {
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

                    if (isAttraction) {
                        this.output[actorOffset + ActorOutputFeature.FORCE.ordinal()] = -1.0D;
                    } else {
                        this.output[actorOffset + ActorOutputFeature.FORCE.ordinal()] = 1.0D;
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

    @Override
    public void selectAndMutate(Environment environment) {
        // No-op
    }
}
