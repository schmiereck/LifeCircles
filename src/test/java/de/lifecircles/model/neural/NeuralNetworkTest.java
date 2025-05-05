package de.lifecircles.model.neural;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class NeuralNetworkTest {
    private static final double THRESHOLD = 0.1;
    private static final double[][] XOR_INPUTS = {
        {0, 0},
        {0, 1},
        {1, 0},
        {1, 1}
    };
    private static final double[][] XOR_OUTPUTS = {
        {0},
        {1},
        {1},
        {0}
    };
    private static final double[][] AND_INPUTS = {
        {0, 0},
        {0, 1},
        {1, 0},
        {1, 1}
    };
    private static final double[][] AND_OUTPUTS = {
        {0},
        {0},
        {0},
        {1}
    };

    @Test
    public void testXORProblem() {
        this.testProblem(XOR_INPUTS, XOR_OUTPUTS);
    }

    @Test
    public void testANDProblem() {
        this.testProblem(AND_INPUTS, AND_OUTPUTS);
    }

    public void testProblem(final double[][] inputs, final double[][] outputs) {
        final int hiddenCount = 8;
        final int[] hiddenCountArr = new int[]{hiddenCount, hiddenCount, hiddenCount};
        final int populationSize = 1_000;
        final double populationMutateSize = 0.4; // x %
        // Create initial population of 100 networks
        List<NeuralNetwork> population = new ArrayList<>();
        for (int i = 0; i < populationSize; i++) {
            //population.add(new NeuralNetwork(2, hiddenCount, 1));
            population.add(new NeuralNetwork(2, hiddenCountArr, 1));
        }

        // Initialize synapse weights for the first generation
        for (NeuralNetwork network : population) {
            for (Synapse synapse : network.getSynapses()) {
                synapse.setWeight(Math.random() * 2 - 1); // Initialize with random values between -1 and 1
            }
        }

        // Run for N generations
        for (int generation = 0; generation < 1_000_000; generation++) {
            // Evaluate all networks
            List<NetworkScore> scores = new ArrayList<>();
            for (NeuralNetwork network : population) {
                double totalError = 0;
                for (int trainPos = 0; trainPos < inputs.length; trainPos++) {
                    network.setInputs(inputs[trainPos]);
                    double[] output = network.process();
                    double error = outputs[trainPos][0] - output[0];
                    totalError += error * error; // Using squared error instead of absolute error
                }
                scores.add(new NetworkScore(network, totalError));
            }

            // Sort by error (lower is better)
            scores.sort(Comparator.comparingDouble(NetworkScore::getError));

            // Select top x %.
            int topCount = (int) (population.size() * populationMutateSize);
            List<NeuralNetwork> bestNetworks = new ArrayList<>();
            for (int i = 0; i < topCount; i++) {
                bestNetworks.add(scores.get(i).getNetwork());
            }

            // Create next generation
            population.clear();
            
            // Add the best networks directly (elitism)
            population.addAll(bestNetworks);
            
            // Add mutated copies of the best networks
            // Create 4 mutated copies of each best network (since we already added 1 unmutated)
            while (population.size() < populationSize) {
                //NeuralNetwork best = bestNetworks.get((int) (Math.random() * (bestNetworks.size() - (topCount * 0.1D))) + (int)(topCount * 0.1D));
                NeuralNetwork best = bestNetworks.get((int) (Math.random() * (bestNetworks.size())));
                double mutationRate = 0.01 + Math.random() * 0.2D; // x % chance of mutation
                double mutationStrength = 0.05 + Math.random() * 0.2D;

                NeuralNetwork mutated = best.mutate(mutationRate, mutationStrength);

                population.add(mutated);
            }

            // Print progress
            double bestScore = scores.get(0).getError();
            NeuralNetwork neuralNetwork2 = bestNetworks.get(0);
            NeuralNetwork neuralNetwork = bestNetworks.get((int) (Math.random() * 19));
            System.out.printf("Generation %d: Best error = %.4f | \t%d\t%d\t%d | \t%d\t%d\t%d%n", generation, bestScore,
                    neuralNetwork2.getSynapseCount(),
                    neuralNetwork2.getAllNeurons().size(),
                    neuralNetwork2.getHiddenLayers().size(),
                    neuralNetwork.getSynapseCount(),
                    neuralNetwork.getAllNeurons().size(),
                    neuralNetwork.getHiddenLayers().size());
        }

        // Use the best network from the final generation
        NeuralNetwork bestNetwork = population.get(0);
        
        // Test the final network
        for (int i = 0; i < inputs.length; i++) {
            bestNetwork.setInputs(inputs[i]);
            double[] output = bestNetwork.process();
            
            // Convert output to binary (0 or 1)
            double expected = outputs[i][0];
            double actual = output[0] > 0.5 ? 1 : 0;
            
            // Verify the output is correct
            assertArrayEquals(new double[]{expected}, new double[]{actual}, THRESHOLD);
        }
    }

    private static class NetworkScore {
        private final NeuralNetwork network;
        private final double error;

        public NetworkScore(NeuralNetwork network, double error) {
            this.network = network;
            this.error = error;
        }

        public NeuralNetwork getNetwork() {
            return network;
        }

        public double getError() {
            return error;
        }
    }
}
