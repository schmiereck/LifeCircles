package de.lifecircles.model.neural;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class NeuralNetworkValuesTest {
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

    private static double synapseConnectivity = 0.4D;

    public void testProblem(final double[][] inputs, final double[][] outputs) {
        // Verschiedene Netzwerkarchitekturen für mehr Diversität
        final int inputCount = inputs[0].length;
        final int outputCount = outputs[0].length;
        final int hiddenCount = inputCount * 2;
        final int[][] architectureVariants = {
                {hiddenCount},
                {hiddenCount, hiddenCount},
                {hiddenCount * 2},
                {hiddenCount, hiddenCount / 2}
        };

        final int populationSize = 1_000;
        final double eliteRate = 0.1; // Top 10% direkt übernehmen
        final Random random = new Random();

        // Erstelle diversere initiale Population mit unterschiedlichen Architekturen
        List<NeuralNetworkValues> population = new ArrayList<>();
        for (int i = 0; i < populationSize; i++) {
            int[] architecture = architectureVariants[i % architectureVariants.length];
            NeuralNetworkValues nn = new NeuralNetworkValues(inputCount, architecture, outputCount,
                    synapseConnectivity, 0);
            nn.setDisableLayerDeactivation(true); // Layer-Deaktivierung für Test abschalten
            population.add(nn);
        }

        // Initialisiere Synapse-Gewichte für die erste Generation
        for (NeuralNetworkValues network : population) {
            for (Synapse synapse : network.getSynapseArr()) {
                // Verbesserte Xavier/Glorot-Initialisierung
                double limit = Math.sqrt(6.0 / (network.getInputLayerSize() + network.getOutputLayerSize()));
                synapse.setWeight(random.nextDouble() * 2 * limit - limit);
            }
            network.setDisableLayerDeactivation(true); // Layer-Deaktivierung für Test abschalten
        }

        // Parameter für adaptives Training
        double baseMutationRate = 0.3D;
        double maxMutationRate = 0.7D;
        double minMutationRate = 0.05D;
        double mutationRate = baseMutationRate;
        double previousBestError = Double.MAX_VALUE;
        double improvementThreshold = 0.0000001D;
        int stagnationCounter = 0;
        int catastropheInterval = 50;

        // Tracking für das beste Netzwerk global
        NeuralNetworkValues globalBestNetwork = null;
        double globalBestError = Double.MAX_VALUE;

        // Training über mehrere Generationen
        for (int generation = 0; generation < 500_000; generation++) {
            // Evaluiere alle Netzwerke
            List<NeuralNetworkValuesTest.NetworkScore> scores = new ArrayList<>();
            for (NeuralNetworkValues network : population) {
                double totalError = 0;
                for (int trainPos = 0; trainPos < inputs.length; trainPos++) {
                    network.setInputs(inputs[trainPos]);
                    double[] output = network.process();
                    for (int outPos = 0; outPos < output.length; outPos++) {
                        // Berechne den Fehler für jedes Ausgabe-Neuron
                        double error = outputs[trainPos][outPos] - output[outPos];
                        totalError += error * error;
                    }
                }
                scores.add(new NeuralNetworkValuesTest.NetworkScore(network, totalError));
            }

            // Sortiere nach Fehler (niedriger ist besser)
            scores.sort(Comparator.comparingDouble(NeuralNetworkValuesTest.NetworkScore::getError));

            // Beste Netzwerke identifizieren
            NeuralNetworkValuesTest.NetworkScore bestNetworkScore = scores.get(0);
            NeuralNetworkValues bestNetwork = bestNetworkScore.getNetwork();
            double currentBestError = bestNetworkScore.getError();

            // Globales Optimum aktualisieren
            if (currentBestError < globalBestError) {
                globalBestError = currentBestError;
                globalBestNetwork = cloneNetwork(bestNetwork);
            }

            // Stagnationserkennung
            boolean isImproving = Math.abs(previousBestError - currentBestError) > improvementThreshold;
            if (!isImproving) {
                stagnationCounter++;
                // Erhöhe Mutationsrate bei Stagnation
                mutationRate = Math.min(mutationRate * 1.05, maxMutationRate);
            } else {
                stagnationCounter = 0;
                mutationRate = Math.max(baseMutationRate, mutationRate * 0.95);
            }
            previousBestError = currentBestError;

            // Prüfe auf Katastrophen-Bedingung (periodisch oder bei langer Stagnation)
            boolean isCatastrophe = generation % catastropheInterval == 0 && generation > 0;
            if (stagnationCounter > 100) {
                isCatastrophe = true;
                System.out.println("Katastrophe ausgelöst wegen Stagnation in Generation " + generation);
            }

            // Erstelle neue Population
            List<NeuralNetworkValues> nextGeneration = new ArrayList<>();

            // Elitismus: Behalte beste Netzwerke unverändert
            int eliteCount = (int) (populationSize * eliteRate);
            for (int i = 0; i < eliteCount; i++) {
                NeuralNetworkValues elite = cloneNetwork(scores.get(i).getNetwork());
                elite.setDisableLayerDeactivation(true); // Layer-Deaktivierung für Test abschalten
                nextGeneration.add(elite);
            }

            // Fülle die restliche Population
            while (nextGeneration.size() < populationSize) {
                double operationChance = random.nextDouble();

                if (operationChance < 0.7) {  // 70% Crossover + Mutation
                    // Eltern durch Tournament Selection auswählen
                    NeuralNetworkValues parent1 = tournamentSelect(scores, 5, random);
                    NeuralNetworkValues parent2 = tournamentSelect(scores, 5, random);

                    // Crossover durchführen
                    NeuralNetworkValues child = crossover(parent1, parent2, random);

                    // Mutationsstärke bestimmen
                    double mutationStrength = isCatastrophe ?
                            0.1 + random.nextDouble() * 0.3 :  // Stärkere Mutation bei Katastrophe
                            0.01 + random.nextDouble() * 0.1;  // Normale Mutation

                    // Kind mutieren
                    child = mutateNetwork(child, mutationRate, mutationStrength, random);
                    child.setDisableLayerDeactivation(true); // Layer-Deaktivierung für Test abschalten
                    nextGeneration.add(child);
                }
                else if (operationChance < 0.9) { // 20% Nur Mutation
                    // Wähle ein Netzwerk durch Tournament Selection
                    NeuralNetworkValues parent = tournamentSelect(scores, 3, random);
                    NeuralNetworkValues child = cloneNetwork(parent);

                    // Bestimme Mutationsstärke
                    double mutationStrength = isCatastrophe ?
                            0.1 + random.nextDouble() * 0.4 :
                            0.01 + random.nextDouble() * 0.2;

                    // Mutiere Kind
                    child = mutateNetwork(child, mutationRate, mutationStrength, random);
                    child.setDisableLayerDeactivation(true); // Layer-Deaktivierung für Test abschalten
                    nextGeneration.add(child);
                }
                else { // 10% Kompletter Neustart (neue zufällige Netzwerke)
                    int[] architecture = architectureVariants[random.nextInt(architectureVariants.length)];
                    NeuralNetworkValues freshNetwork = new NeuralNetworkValues(inputCount, architecture, outputCount,
                            synapseConnectivity, 0);

                    // Gewichte initialisieren
                    for (Synapse synapse : freshNetwork.getSynapseList()) {
                        double limit = Math.sqrt(6.0 / (freshNetwork.getInputLayerSize() + freshNetwork.getOutputLayerSize()));
                        synapse.setWeight(random.nextDouble() * 2 * limit - limit);
                    }
                    freshNetwork.setDisableLayerDeactivation(true); // Layer-Deaktivierung für Test abschalten
                    nextGeneration.add(freshNetwork);
                }
            }

            // Aktualisiere Population
            population = nextGeneration;

            // Kompletter Neustart bei extremer Stagnation
            if (stagnationCounter > 200) {
                System.out.println("Kompletter Neustart wegen extremer Stagnation in Generation " + generation);

                // Behalte 10% der besten Netzwerke
                int keepCount = populationSize / 10;

                // Ersetze den Rest mit neuen zufälligen Netzwerken
                for (int i = keepCount; i < populationSize; i++) {
                    int[] architecture = architectureVariants[random.nextInt(architectureVariants.length)];
                    NeuralNetworkValues freshNetwork = new NeuralNetworkValues(inputCount, architecture, outputCount,
                            synapseConnectivity, 0);

                    // Gewichte initialisieren
                    for (Synapse synapse : freshNetwork.getSynapseList()) {
                        double limit = Math.sqrt(6.0 / (freshNetwork.getInputLayerSize() + freshNetwork.getOutputLayerSize()));
                        synapse.setWeight(random.nextDouble() * 2 * limit - limit);
                    }
                    freshNetwork.setDisableLayerDeactivation(true); // Layer-Deaktivierung für Test abschalten
                    population.set(i, freshNetwork);
                }

                // Stagnationszähler zurücksetzen
                stagnationCounter = 0;

                // Auch globales Optimum in die Population einfügen
                if (globalBestNetwork != null) {
                    population.set(random.nextInt(keepCount), cloneNetwork(globalBestNetwork));
                }
            }

            // Fortschritt ausgeben
            if (generation % 50 == 0 || generation < 10) {
                NeuralNetworkValues genBestNetwork = scores.get(0).getNetwork();
                System.out.printf("Gen %d: Fehler = %.9f | Global = %.9f | Stagnation: %d | Mut.Rate: %.3f | Arch: %s%n",
                        generation, currentBestError, globalBestError, stagnationCounter, mutationRate,
                        Arrays.toString(getArchitecture(genBestNetwork)));

                // Bei Meilensteinen Zwischenergebnisse anzeigen
                if (generation % 1000 == 0 || currentBestError < 0.01) {
                    showResults(inputs, outputs, genBestNetwork);
                }
            }

            // Frühzeitiger Abbruch bei sehr gutem Ergebnis
            if (currentBestError < 0.0001) {
                System.out.println("Erfolg: Sehr gutes Ergebnis erreicht in Generation " + generation);
                break;
            }
        }

        // Verwende das global beste Netzwerk für Tests
        NeuralNetworkValues finalNetwork = globalBestNetwork != null ? globalBestNetwork : population.get(0);
        finalNetwork.setDisableLayerDeactivation(true); // Layer-Deaktivierung für Test abschalten

        // Teste das finale Netzwerk
        for (int i = 0; i < inputs.length; i++) {
            finalNetwork.setInputs(inputs[i]);
            double[] output = finalNetwork.process();

            System.out.printf("Input %s: \tErwartet: %s \tAusgabe: %s%n",
                    formatArrayToString(inputs[i]),
                    formatArrayToString(outputs[i]),
                    formatArrayToString(output));

            // Überprüfe Ergebnis
            assertArrayEquals(outputs[i], output, THRESHOLD);
        }
    }

    public static String formatArrayToString(double[] arr) {
        if (arr == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(String.format("%.4f", arr[i]));
        }
        sb.append("]");
        return sb.toString();
    }

    // Tournament Selection: Wählt das beste Netzwerk aus einer zufälligen Gruppe
    private NeuralNetworkValues tournamentSelect(List<NeuralNetworkValuesTest.NetworkScore> scores, int tournamentSize, Random random) {
        NeuralNetworkValuesTest.NetworkScore best = null;
        for (int i = 0; i < tournamentSize; i++) {
            int idx = random.nextInt(scores.size());
            NeuralNetworkValuesTest.NetworkScore candidate = scores.get(idx);
            if (best == null || candidate.getError() < best.getError()) {
                best = candidate;
            }
        }
        return best.getNetwork();
    }

    // Crossover: Kombiniert zwei Netzwerke zu einem neuen
    private NeuralNetworkValues crossover(NeuralNetworkValues parent1, NeuralNetworkValues parent2, Random random) {
        NeuralNetworkValues child = cloneNetwork(parent1);
        List<Synapse> childSynapses = child.getSynapseList();
        List<Synapse> parent2Synapses = parent2.getSynapseList();

        int minSize = Math.min(childSynapses.size(), parent2Synapses.size());

        // Überprüfe, ob Crossover möglich ist (minSize muss > 0 sein)
        if (minSize <= 0) {
            // Kann keinen Crossover durchführen, gib einfach den Klon zurück
            return child;
        }

        if (random.nextDouble() < 0.5) {
            // One-Point Crossover
            int crossPoint = random.nextInt(minSize);
            for (int i = crossPoint; i < minSize; i++) {
                childSynapses.get(i).setWeight(parent2Synapses.get(i).getWeight());
            }
        } else {
            // Uniform Crossover
            for (int i = 0; i < minSize; i++) {
                if (random.nextDouble() < 0.5) {
                    childSynapses.get(i).setWeight(parent2Synapses.get(i).getWeight());
                }
            }
        }

        return child;
    }

    // Mutation: Ändert zufällig Gewichte im Netzwerk
    private NeuralNetworkValues mutateNetwork(NeuralNetworkValues network, double rate, double strength, Random random) {
        // Einfacher Wrapper um die vorhandene mutate-Methode
        return network.mutate(rate, strength);
    }

    // Hilfsmethode: Klont ein Netzwerk (falls nicht verfügbar)
    private NeuralNetworkValues cloneNetwork(NeuralNetworkValues network) {
        // Falls die Klasse keine clone-Methode hat, diese implementieren
        // Hier wird angenommen, dass network.mutate mit Rate 0 ein Klon erzeugt
        return network.mutate(0, 0);
    }

    // Hilfsmethode: Gibt die Architektur eines Netzwerks zurück
    private int[] getArchitecture(NeuralNetworkValues network) {
        Layer[] hiddenLayers = network.getHiddenLayerArr();
        int[] architecture = new int[hiddenLayers.length];
        for (int i = 0; i < hiddenLayers.length; i++) {
            final Layer layer = hiddenLayers[i];
            architecture[i] = layer.getNeurons().size();
        }
        return architecture;
    }

    // Hilfsmethode: Zeigt Zwischenergebnisse
    private void showResults(double[][] inputs, double[][] outputs, NeuralNetworkValues network) {
        System.out.println("\nZwischenergebnisse:");
        double totalError = 0;
        for (int i = 0; i < inputs.length; i++) {
            network.setInputs(inputs[i]);
            double[] output = network.process();
            double error = Math.abs(outputs[i][0] - output[0]);
            totalError += error * error;
            System.out.printf("  Input: %s | Erwartet: %s | Ausgabe: %s | Fehler: %.4f%n",
                    formatArrayToString(inputs[i]), formatArrayToString(outputs[i]),
                    formatArrayToString(output), error);
        }
        System.out.printf("  Gesamt-MSE: %.6f%n\n", totalError / inputs.length);
    }

    private static class NetworkScore {
        private final NeuralNetworkValues network;
        private final double error;

        public NetworkScore(NeuralNetworkValues network, double error) {
            this.network = network;
            this.error = error;
        }

        public NeuralNetworkValues getNetwork() {
            return network;
        }

        public double getError() {
            return error;
        }
    }
}
