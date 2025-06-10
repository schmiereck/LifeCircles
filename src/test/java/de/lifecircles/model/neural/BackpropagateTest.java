package de.lifecircles.model.neural;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class BackpropagateTest {
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
    private static final double[][] XXX_INPUTS = {
            {0, 0, 0},
            {0, 0, 1},
            {0, 1, 0},
            {0, 1, 1},
            {1, 0, 0},
            {1, 0, 1},
            {1, 1, 0},
            {1, 1, 1}
    };
    private static final double[][] XXX_OUTPUTS = {
            {0, 0},
            {0, 1},
            {0, 1},
            {1, 0},
            {0, 1},
            {1, 0},
            {1, 0},
            {1, 1}
    };

    @Test
    public void testXORProblem() {
        this.testProblem(XOR_INPUTS, XOR_OUTPUTS);
    }

    @Test
    public void testANDProblem() {
        this.testProblem(AND_INPUTS, AND_OUTPUTS);
    }

    @Test
    public void testBitcountProblem() {
        this.testProblem(XXX_INPUTS, XXX_OUTPUTS);
    }

    private static double synapseConnectivity = 0.6D;

    public void testProblem(final double[][] inputs, final double[][] outputs) {
        // Verschiedene Netzwerkarchitekturen für mehr Diversität
        final int inputCount = inputs[0].length;
        final int outputCount = outputs[0].length;
        final int hiddenCount = inputCount * 2;
        final int[] architecture = {
                hiddenCount, hiddenCount
        };

        final int populationSize = 1_000;
        final double eliteRate = 0.1; // Top 10% direkt übernehmen
        final Random random = new Random();

        NeuralNetwork nn = new NeuralNetwork(inputCount, architecture, outputCount,
                synapseConnectivity, 0);
        nn.setDisableLayerDeactivation(true); // Layer-Deaktivierung für Test abschalten

        nn.train(inputs, outputs, 60_000);


        // Teste das finale Netzwerk
        for (int i = 0; i < inputs.length; i++) {
            nn.setInputs(inputs[i]);
            double[] output = nn.process();

            System.out.printf("Input %s: \tErwartet: %s \tAusgabe: %s%n",
                    NeuralNetworkTest.formatArrayToString(inputs[i]),
                    NeuralNetworkTest.formatArrayToString(outputs[i]),
                    NeuralNetworkTest.formatArrayToString(output));

            // Überprüfe Ergebnis
            assertArrayEquals(outputs[i], output, THRESHOLD);
        }
    }
}
