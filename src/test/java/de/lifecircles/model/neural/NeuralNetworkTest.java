package de.lifecircles.model.neural;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.util.ArrayList;
import java.util.Arrays;
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
        final int hiddenCount = 4;
        //final int[] hiddenCountArr = new int[]{hiddenCount, hiddenCount, hiddenCount, hiddenCount};
        //final int[] hiddenCountArr = new int[]{hiddenCount, hiddenCount, hiddenCount};
        //final int[] hiddenCountArr = new int[]{hiddenCount, hiddenCount};
        final int[] hiddenCountArr = new int[]{hiddenCount};
        final int populationSize = 10_000;
        final double populationMutateSize = 0.25; // x %
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

        double previousBestError = 1.0D;
        double threshold = 0.000001D;//THRESHOLD;
        //double maxMutationRate = 0.15D; // populationSize = 10_000
        double maxMutationRate = 0.6D; // populationSize = 10_000
        double minMutationRate = 0.01D;
        double mutationRate = maxMutationRate;
        NeuralNetwork bestNetwork = null;
        // Run for N generations
        for (int generation = 0; generation < 500_000; generation++) {
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
            final int topCount = (int) (population.size() * populationMutateSize);
            List<NeuralNetwork> bestNetworks = new ArrayList<>();
            for (int i = 0; i < topCount; i++) {
                bestNetworks.add(scores.get(i).getNetwork());
            }

            // Create next generation
            population.clear();
            
            // Add the best networks directly (elitism)
            population.addAll(bestNetworks);

            NetworkScore bestNetworkScore = scores.get(0);
            bestNetwork = bestNetworkScore.getNetwork();
            double currentBestError = bestNetworkScore.getError();
            if (generation > 0 && Math.abs(previousBestError - currentBestError) < threshold) {
                mutationRate = Math.min(mutationRate * 1.1, maxMutationRate); // Erhöhe die Mutationsrate
            } else {
                mutationRate = Math.max(mutationRate * 0.9, minMutationRate); // Reduziere die Mutationsrate
            }
            previousBestError = currentBestError;

            // Add mutated copies of the best networks
            // Create 4 mutated copies of each best network (since we already added 1 unmutated)
            while (population.size() < populationSize) {
                //NeuralNetwork best = bestNetworks.get((int) (Math.random() * (bestNetworks.size() - (topCount * 0.1D))) + (int)(topCount * 0.1D));
                //NeuralNetwork best = bestNetworks.get((int) (Math.random() * (bestNetworks.size())));

                NetworkScore bestScore;
                if (Math.random() < 0.1D) {
                    bestScore = scores.get((int) (Math.random() * scores.size()));
                } else {
                    bestScore = scores.get((int) (Math.random() * topCount));
                }

                NeuralNetwork best = bestScore.getNetwork();

                //double mutationRate = 0.01 + Math.random() * 0.2D; // x % chance of mutation
                //double mutationStrength = 0.001D + Math.random() * 0.2D;
                double mutationStrength = 0.001D + Math.random() * mutationRate;

                //double initialMutationRate = 0.2;
                //double finalMutationRate = 0.01;
                //double mutationRate = initialMutationRate - (generation / (double) populationSize) * (initialMutationRate - finalMutationRate);

                NeuralNetwork mutated = best.mutate(mutationRate, mutationStrength);

                population.add(mutated);
            }

            // Print progress
            double bestScore = bestNetworkScore.getError();
            NeuralNetwork bestNeuralNetwork = bestNetworkScore.getNetwork();
            NeuralNetwork neuralNetwork = bestNetworks.get((int) (Math.random() * (bestNetworks.size() / 10)));
            System.out.printf("Generation %d: Best error = %.9f | \t%d\t%d\t%d | \t%d\t%d\t%d | \t%.4f %n", generation, bestScore,
                    bestNeuralNetwork.getSynapseCount(),
                    bestNeuralNetwork.getAllNeurons().size(),
                    bestNeuralNetwork.getHiddenLayers().size(),
                    neuralNetwork.getSynapseCount(),
                    neuralNetwork.getAllNeurons().size(),
                    neuralNetwork.getHiddenLayers().size(),
                    mutationRate);
        }

        // Use the best network from the final generation
        //NeuralNetwork bestNetwork = population.get(0);
        
        // Test the final network
        for (int i = 0; i < inputs.length; i++) {
            bestNetwork.setInputs(inputs[i]);
            double[] output = bestNetwork.process();
            
            // Convert output to binary (0 or 1)
            //double expected = outputs[i][0];
            //double actual = output[0] > 0.5 ? 1 : 0;

            System.out.printf("input %s: \t%s \t%s %n",
                    Arrays.toString(inputs[i]),
                    Arrays.toString(outputs[i]),
                    Arrays.toString(output));

            // Verify the output is correct
            assertArrayEquals(outputs[i], output, THRESHOLD);
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
