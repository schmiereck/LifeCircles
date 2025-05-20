package de.lifecircles.model.neural;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

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
        List<NeuralNetwork> population = new ArrayList<>();
        for (int i = 0; i < populationSize; i++) {
            int[] architecture = architectureVariants[i % architectureVariants.length];
            population.add(new NeuralNetwork(inputCount, architecture, outputCount,
                    synapseConnectivity, 0));
        }

        // Initialisiere Synapse-Gewichte für die erste Generation
        for (NeuralNetwork network : population) {
            for (Synapse synapse : network.getSynapsesynapseList()) {
                // Verbesserte Xavier/Glorot-Initialisierung
                double limit = Math.sqrt(6.0 / (network.getInputLayerSize() + network.getOutputLayerSize()));
                synapse.setWeight(random.nextDouble() * 2 * limit - limit);
            }
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
        NeuralNetwork globalBestNetwork = null;
        double globalBestError = Double.MAX_VALUE;

        // Training über mehrere Generationen
        for (int generation = 0; generation < 500_000; generation++) {
            // Evaluiere alle Netzwerke
            List<NetworkScore> scores = new ArrayList<>();
            for (NeuralNetwork network : population) {
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
                scores.add(new NetworkScore(network, totalError));
            }

            // Sortiere nach Fehler (niedriger ist besser)
            scores.sort(Comparator.comparingDouble(NetworkScore::getError));
            
            // Beste Netzwerke identifizieren
            NetworkScore bestNetworkScore = scores.get(0);
            NeuralNetwork bestNetwork = bestNetworkScore.getNetwork();
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
            List<NeuralNetwork> nextGeneration = new ArrayList<>();
            
            // Elitismus: Behalte beste Netzwerke unverändert
            int eliteCount = (int) (populationSize * eliteRate);
            for (int i = 0; i < eliteCount; i++) {
                nextGeneration.add(cloneNetwork(scores.get(i).getNetwork()));
            }
            
            // Fülle die restliche Population
            while (nextGeneration.size() < populationSize) {
                double operationChance = random.nextDouble();
                
                if (operationChance < 0.7) {  // 70% Crossover + Mutation
                    // Eltern durch Tournament Selection auswählen
                    NeuralNetwork parent1 = tournamentSelect(scores, 5, random);
                    NeuralNetwork parent2 = tournamentSelect(scores, 5, random);
                    
                    // Crossover durchführen
                    NeuralNetwork child = crossover(parent1, parent2, random);
                    
                    // Mutationsstärke bestimmen
                    double mutationStrength = isCatastrophe ? 
                        0.1 + random.nextDouble() * 0.3 :  // Stärkere Mutation bei Katastrophe
                        0.01 + random.nextDouble() * 0.1;  // Normale Mutation
                    
                    // Kind mutieren
                    child = mutateNetwork(child, mutationRate, mutationStrength, random);
                    nextGeneration.add(child);
                } 
                else if (operationChance < 0.9) { // 20% Nur Mutation
                    // Wähle ein Netzwerk durch Tournament Selection
                    NeuralNetwork parent = tournamentSelect(scores, 3, random);
                    NeuralNetwork child = cloneNetwork(parent);
                    
                    // Bestimme Mutationsstärke
                    double mutationStrength = isCatastrophe ? 
                        0.1 + random.nextDouble() * 0.4 :  
                        0.01 + random.nextDouble() * 0.2;
                    
                    // Mutiere Kind
                    child = mutateNetwork(child, mutationRate, mutationStrength, random);
                    nextGeneration.add(child);
                }
                else { // 10% Kompletter Neustart (neue zufällige Netzwerke)
                    int[] architecture = architectureVariants[random.nextInt(architectureVariants.length)];
                    NeuralNetwork freshNetwork = new NeuralNetwork(inputCount, architecture, outputCount,
                            synapseConnectivity, 0);
                    
                    // Gewichte initialisieren
                    for (Synapse synapse : freshNetwork.getSynapsesynapseList()) {
                        double limit = Math.sqrt(6.0 / (freshNetwork.getInputLayerSize() + freshNetwork.getOutputLayerSize()));
                        synapse.setWeight(random.nextDouble() * 2 * limit - limit);
                    }
                    
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
                    NeuralNetwork freshNetwork = new NeuralNetwork(inputCount, architecture, outputCount,
                            synapseConnectivity, 0);
                    
                    // Gewichte initialisieren
                    for (Synapse synapse : freshNetwork.getSynapsesynapseList()) {
                        double limit = Math.sqrt(6.0 / (freshNetwork.getInputLayerSize() + freshNetwork.getOutputLayerSize()));
                        synapse.setWeight(random.nextDouble() * 2 * limit - limit);
                    }
                    
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
                NeuralNetwork genBestNetwork = scores.get(0).getNetwork();
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
        NeuralNetwork finalNetwork = globalBestNetwork != null ? globalBestNetwork : population.get(0);
        
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
    private NeuralNetwork tournamentSelect(List<NetworkScore> scores, int tournamentSize, Random random) {
        NetworkScore best = null;
        for (int i = 0; i < tournamentSize; i++) {
            int idx = random.nextInt(scores.size());
            NetworkScore candidate = scores.get(idx);
            if (best == null || candidate.getError() < best.getError()) {
                best = candidate;
            }
        }
        return best.getNetwork();
    }
    
    // Crossover: Kombiniert zwei Netzwerke zu einem neuen
    private NeuralNetwork crossover(NeuralNetwork parent1, NeuralNetwork parent2, Random random) {
        NeuralNetwork child = cloneNetwork(parent1);
        List<Synapse> childSynapses = child.getSynapsesynapseList();
        List<Synapse> parent2Synapses = parent2.getSynapsesynapseList();
        
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
    private NeuralNetwork mutateNetwork(NeuralNetwork network, double rate, double strength, Random random) {
        // Einfacher Wrapper um die vorhandene mutate-Methode
        return network.mutate(rate, strength);
    }
    
    // Hilfsmethode: Klont ein Netzwerk (falls nicht verfügbar)
    private NeuralNetwork cloneNetwork(NeuralNetwork network) {
        // Falls die Klasse keine clone-Methode hat, diese implementieren
        // Hier wird angenommen, dass network.mutate mit Rate 0 ein Klon erzeugt
        return network.mutate(0, 0);
    }
    
    // Hilfsmethode: Gibt die Architektur eines Netzwerks zurück
    private int[] getArchitecture(NeuralNetwork network) {
        List<Layer> hiddenLayers = network.getHiddenLayerList();
        int[] architecture = new int[hiddenLayers.size()];
        for (int i = 0; i < hiddenLayers.size(); i++) {
            final Layer layer = hiddenLayers.get(i);
            architecture[i] = layer.getNeurons().size();
        }
        return architecture;
    }
    
    // Hilfsmethode: Zeigt Zwischenergebnisse
    private void showResults(double[][] inputs, double[][] outputs, NeuralNetwork network) {
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
