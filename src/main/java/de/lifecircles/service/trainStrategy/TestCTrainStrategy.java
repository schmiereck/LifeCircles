package de.lifecircles.service.trainStrategy;


import de.lifecircles.model.Cell;
import de.lifecircles.model.Environment;
import de.lifecircles.model.Vector2D;
import de.lifecircles.model.neural.*;
import de.lifecircles.service.SimulationConfig;

public class TestCTrainStrategy implements TrainStrategy {
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
        /*
        {
            final double yTop = this.config.getHeight() - 38.0;
            this.createAndAddCell(environment, 40.0D, yTop, radiusSize, false, false);
        }
        {
            final double yTop = this.config.getHeight() - 30.0;
            this.createAndAddCell(environment, 100.0D, yTop, radiusSize, false, false);
        }
        {
            final double yTop = this.config.getHeight() - 22.0;
            this.createAndAddCell(environment, 160.0D, yTop, radiusSize, false, false);
        }
        {
            final double yTop = this.config.getHeight() - 15.0;
            this.createAndAddCell(environment, 220.0D, yTop, radiusSize, false, false);
        }
        {
            final double yTop = this.config.getHeight() - 13.0;
            this.createAndAddCell(environment, 280.0D, yTop, radiusSize, false, false);
        }
        {
            final double yTop = this.config.getHeight() - 8.0;
            this.createAndAddCell(environment, 340.0D, yTop, radiusSize, false, false);
        }
        */
        /*
        {
            final double x = 500.0D;
            environment.addWallBlocker(x, 600.0D, 30.0D);
            this.createAndAddCell(environment, x - 20, 400.0D, radiusSize, false, false);
        }
        {
            final double x = 600.0D;
            environment.addWallBlocker(x, 600.0D, 30.0D);
            this.createAndAddCell(environment, x - 15, 400.0D, radiusSize, false, false);
        }
        */

        {
            final double x = 700.0D;
            environment.addWallBlocker(x, 600.0D, 30.0D);
            this.createAndAddCell(environment, x, 400.0D, radiusSize, false, false);
        }
        /*
        {
            final double x = 800.0D;
            environment.addWallBlocker(x, 600.0D, 30.0D);
            this.createAndAddCell(environment, x + 5, 400.0D, radiusSize, false, false);
        }
        {
            final double x = 900.0D;
            environment.addWallBlocker(x, 600.0D, 30.0D);
            this.createAndAddCell(environment, x + 15, 400.0D, radiusSize, false, false);
        }
        {
            final double x = 1000.0D;
            environment.addWallBlocker(x, 600.0D, 30.0D);
            this.createAndAddCell(environment, x + 20, 400.0D, radiusSize, false, false);
        }
        {
            final double x = 1100.0D;
            environment.addWallBlocker(x, 600.0D, 30.0D);
            this.createAndAddCell(environment, x + 25, 400.0D, radiusSize, false, false);
        }
        */
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
