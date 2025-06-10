package de.lifecircles.model.neural;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MemoryNeuralNetworkTest {
    private static final Logger logger = LoggerFactory.getLogger(MemoryNeuralNetworkTest.class);

    private static final class NNTrainResult {
        private final NeuralNetwork network;
        public double lossSum;
        public double proccesLoss;
        private double loss;

        public NNTrainResult(NeuralNetwork network, double loss) {
            this.network = network;
            this.loss = loss;
        }

        public NeuralNetwork getNetwork() {
            return network;
        }

        public double getLoss() {
            return loss;
        }

        public void setLoss(double loss) {
            this.loss = loss;
        }
    }

    @Test
    public void test() {
        final Random random = new Random();

        final int inputCount = 8;
        final int outputCount = 8;
        final int hiddenCount = inputCount * 2;
        final int[] architecture = {
                hiddenCount, hiddenCount,
                hiddenCount,
        };
        final double synapseConnectivity = 0.8D;

        List<NNTrainResult> trainResultList = new ArrayList<>();
        for (int networkPos = 0; networkPos < 60; networkPos++) {
            NeuralNetwork network = new NeuralNetwork(inputCount, architecture, outputCount, synapseConnectivity, 0);
            network.setDisableLayerDeactivation(true); // Layer-Deaktivierung für Test abschalten
            trainResultList.add(new NNTrainResult(network, 0.0D));
        }

        {
            String[] patterns = { "01.01.01.12." };
            int epochs = 5_000;
            int trainDataSize = 20;
            trainResultList = trainSmallLanguageModel(random, trainResultList, patterns, trainDataSize, epochs);
            final NeuralNetwork network = trainResultList.get(0).getNetwork();
            generateText(network);
        }
        {
            String[] patterns = { "12.12.12.01." };
            int epochs = 5_000;
            int trainDataSize = 20;
            trainResultList = trainSmallLanguageModel(random, trainResultList, patterns, trainDataSize, epochs);
            final NeuralNetwork network = trainResultList.get(0).getNetwork();
            generateText(network);
        }

        {
            String[] patterns = { "01.12.01.12." };
            int epochs = 8_000;
            int trainDataSize = 20;
            trainResultList = trainSmallLanguageModel(random, trainResultList, patterns, trainDataSize, epochs);
            final NeuralNetwork network = trainResultList.get(0).getNetwork();
            generateText(network);
        }
        {
            String[] patterns = { "01.12.01." };
            int epochs = 10_000;
            int trainDataSize = 20;
            trainResultList = trainSmallLanguageModel(random, trainResultList, patterns, trainDataSize, epochs);
            final NeuralNetwork network = trainResultList.get(0).getNetwork();
            generateText(network);
        }
        {
            String[] patterns = { "01.", "12." };
            int epochs = 15_000;
            int trainDataSize = 20;
            trainResultList = trainSmallLanguageModel(random, trainResultList, patterns, trainDataSize, epochs);
            final NeuralNetwork network = trainResultList.get(0).getNetwork();
            generateText(network);
        }
    }

    @Test
    public void testText() {
        final Random random = new Random();

        final int inputCount = 8;
        final int outputCount = 8;
        final int hiddenCount = inputCount * 2;
        final int[] architecture = {
                hiddenCount, hiddenCount,
                hiddenCount,
        };
        final double synapseConnectivity = 0.8D;

        List<NNTrainResult> trainResultList = new ArrayList<>();
        for (int networkPos = 0; networkPos < 60; networkPos++) {
            NeuralNetwork network = new NeuralNetwork(inputCount, architecture, outputCount, synapseConnectivity, 0);
            network.setDisableLayerDeactivation(true); // Layer-Deaktivierung für Test abschalten
            trainResultList.add(new NNTrainResult(network, 0.0D));
        }
        char[] startCharArr = new char[] { 'A', 'F', 'A', 'F' };
        {
            String[] patterns = {"Auto fährt. ", "Fahrad fährt. ", "Auto fährt schnell. ", "Fahrrad fährt langsam. "};
            int epochs = 5_000;
            int trainDataSize = 20;
            trainResultList = trainSmallLanguageModel(random, trainResultList, patterns, trainDataSize, epochs, startCharArr);
            final NeuralNetwork network = trainResultList.get(0).getNetwork();
            generateText(network, startCharArr);
        }
    }

    private static List<NNTrainResult> trainSmallLanguageModel(final Random random, final List<NNTrainResult> inTrainResultList, String[] patterns, int trainDataSize, int epochs) {
        return trainSmallLanguageModel(random,
                                        inTrainResultList,
                                        patterns,
                                        trainDataSize,
                                        epochs,
                                        new char[] { '0', '1', '2' });
    }

    private static List<NNTrainResult> trainSmallLanguageModel(final Random random,
                                                               final List<NNTrainResult> inTrainResultList,
                                                               String[] patterns,
                                                               int trainDataSize,
                                                               int epochs,
                                                               char[] startCharArr) {
        // Zufällige Reihenfolge der Muster generieren
        StringBuilder trainingData = new StringBuilder();
        for (int i = 0; i < trainDataSize; i++) {
            trainingData.append(patterns[random.nextInt(patterns.length)]);
        }

        String trainText = trainingData.toString();
        System.out.println("Trainingsdaten: " + trainText);

        // Sprachmodell initialisieren und trainieren
        System.out.println("Starte Training für " + epochs + " Epochen...");
        return train(random, inTrainResultList, trainText, epochs, startCharArr);
    }

    /**
     * Trainiert das Modell mit einer Textsequenz.
     *
     * @param text Der Trainingstext
     * @param epochs Anzahl der Trainingsepochen
     */
    public static List<NNTrainResult> train(final Random random, final List<NNTrainResult> inTrainResultList, String text, int epochs,
                                            char[] startCharArr) {
        NeuralNetwork bestNeuralNetwork = null;
        logger.info("Starte Training mit Text: '{}' für {} Epochen", text, epochs);

        double mutationRate = 0.1D;
        double mutationStrengt = 0.1D;

        final List<NNTrainResult> trainResultList = new ArrayList<>(inTrainResultList) ;

        char[] characters = text.toCharArray();

        // Länge der Sequenz für BPTT festlegen - z.B. 10 Zeichen
        int timeSeriesLength = characters.length - 1; //Math.min(9, characters.length - 1);

        for (int epoch = 0; epoch < epochs; epoch++) {
            trainResultList.parallelStream().forEach(trainResult -> {
            //for (final NNTrainResult trainResult : trainResultList) {
                final NeuralNetwork network = trainResult.getNetwork();

                // Netzwerk für die nächste Epoche zurücksetzen
                network.rnnClearPreviousState();

                trainResult.lossSum = 0;
                trainResult.proccesLoss = 0;
                final int trainCount = 30;

                for (int trainPos = 0; trainPos < trainCount; trainPos++) {
                    for (int charPos = 0; charPos < timeSeriesLength; charPos++) {
                        char currentChar = characters[charPos];
                        char nextChar = characters[charPos + 1];

                        double[] inputArr = CharEncoder.encode(currentChar);
                        double[] expectedOutputArr = CharEncoder.encode(nextChar);

                        // Training für die gesamte Sequenz
                        double[] outputArray = network.calcTrain(inputArr, expectedOutputArr);

                        long proccessedSynapses = network.getProccessedSynapses();

                        calculateLoss(trainResult, outputArray, expectedOutputArr, proccessedSynapses);
                    }
                }
                // 75% Verlust, 25% Proccessed Synapses
                //final double resultWeight = 1.0D; // 0.75D;
                final double resultWeight = 0.925D;
                //final double proccesWeight = 0.0D; // 0.25D;
                final double proccesWeight = 0.025D;
                trainResult.setLoss((trainResult.lossSum / (timeSeriesLength * trainCount)) * resultWeight +
                        (trainResult.proccesLoss / (timeSeriesLength * trainCount * maxProccessedSynapses)) * proccesWeight);
            });
            // Sortieren der Netzwerke nach Verlust
            trainResultList.sort((a, b) -> Double.compare(a.getLoss(), b.getLoss()));
            NNTrainResult bestTrainResult = trainResultList.get(0);
            bestNeuralNetwork = bestTrainResult.getNetwork();

            if (bestTrainResult.getLoss() < 0.01D) {
                logger.info("Bestes Netzwerk erreicht einen Verlust von {}. Training wird abgebrochen.", bestTrainResult.getLoss());
                generateText(bestNeuralNetwork, startCharArr);
                break; // Training abbrechen, wenn Verlust unter 0.01 liegt
            }
            if (epoch % 100 == 0) {
                generateText(bestNeuralNetwork, startCharArr);
            }

            // Durchschnittlicher Verlust über alle Sequenzen
            System.out.printf("\rEpoche %d abgeschlossen, Durchschnittsverlust: %f, Synapses: %d",
                    epoch, bestTrainResult.getLoss(), bestNeuralNetwork.getProccessedSynapses());
            System.out.flush();
            if ((epoch + 1) >= epochs) {
                break;
            }
            // 20% der besten Netze auswählen und hinzufügen zu den Gewinnern.
            final List<NeuralNetwork> winnerNetworkList = new ArrayList<>();
            for (int winnerPos = 0; winnerPos < trainResultList.size() / 5; winnerPos++) {
                final NNTrainResult trainResult = trainResultList.get(winnerPos);
                NeuralNetwork winnerNetwork = trainResult.getNetwork();
                winnerNetwork.rnnClearPreviousState();
                winnerNetworkList.add(winnerNetwork);
            }
            // Muttieren von 10% aller Netze und hinzufügen zu den Gewinnern.
            while (winnerNetworkList.size() < inTrainResultList.size()) {
                final NNTrainResult trainResult = trainResultList.get(random.nextInt(trainResultList.size()));
                NeuralNetwork winnerNetwork = trainResult.getNetwork();
                // Mutieren des Netzwerks
                NeuralNetwork childNetwork = winnerNetwork.mutate(mutationRate, mutationStrengt);
                winnerNetworkList.add(childNetwork);
            }
            // Muttieren der ausgewählten Netze und hinzufügen zu den Gewinnern.
            while (winnerNetworkList.size() < (inTrainResultList.size() - (trainResultList.size()) / 10)) {
                NeuralNetwork winnerNetwork = winnerNetworkList.get(random.nextInt(winnerNetworkList.size()));
                // Mutieren des Netzwerks
                NeuralNetwork childNetwork = winnerNetwork.mutate(mutationRate, mutationStrengt);
                winnerNetworkList.add(childNetwork);
            }
            // Alle Netzwerke für die nächste Epoche zurücksetzen
            trainResultList.clear();
            for (final NeuralNetwork network : winnerNetworkList) {
                trainResultList.add(new NNTrainResult(network, 0.0D));
            }
        }

        logger.info("Training abgeschlossen.");
        return trainResultList;
    }

    private static long maxProccessedSynapses = 0;
    /**
     * Berechnet den Verlust zwischen Vorhersage und Zielwert.
     */
    private static void calculateLoss(NNTrainResult trainResult, double[] outputArray, double[] expectedOutputArr, long proccessedSynapses) {
        double loss = 0.0D;
        for (int outputPos = 0; outputPos < outputArray.length; outputPos++) {
            double diff = expectedOutputArr[outputPos] - outputArray[outputPos];
            loss += diff * diff; // Quadratischer Verlust
        }
        if (maxProccessedSynapses < proccessedSynapses) {
            maxProccessedSynapses = proccessedSynapses;
        }
        double proccesLoss = (double) proccessedSynapses;

        trainResult.lossSum += loss;
        trainResult.proccesLoss += proccesLoss;

        //return loss * 0.75 + proccesLoss * 0.25; // 75% Verlust, 25% Proccessed Synapses
    }

    private static void generateText(NeuralNetwork network) {
        generateText(network, new char[] { '0', '1', '2' });
    }

    private static void generateText(NeuralNetwork network, char[] startCharArr) {
        System.out.println();
        for (Character startChar : startCharArr) {
            System.out.print("Generierter Text: ");
            String generatedText = generateText(network, startChar, 100);
            System.out.println(generatedText);
        }
    }

    /**
     * Generiert eine Textsequenz beginnend mit einem Startzeichen.
     *
     * @param startChar Das Startzeichen
     * @param length Die Länge der zu generierenden Sequenz
     * @return Die generierte Textsequenz
     */
    public static String generateText(NeuralNetwork network, char startChar, int length) {
        StringBuilder result = new StringBuilder();
        result.append(startChar);

        char currentChar = startChar;

        // Reset des Netzwerkzustands vor der Generierung
        //model.rnnClearPreviousState();

        for (int i = 0; i < length - 1; i++) {
            currentChar = predict(network, currentChar);
            result.append(currentChar);
        }

        return result.toString();
    }

    /**
     * Vorhersage des nächsten Zeichens bei gegebener Eingabe.
     *
     * @param input Das Eingabezeichen
     * @return Das vorhergesagte nächste Zeichen
     */
    public static char predict(NeuralNetwork network, char input) {
        // Eingabezeichen kodieren
        double[] inputArray = CharEncoder.encode(input);

        // Vorhersage durch das Netzwerk
        network.setInputs(inputArray);
        double[] outputArray = network.process();

        // Umwandeln der Ausgabe in ein Zeichen
        double[] binaryOutput = CharEncoder.argmax(outputArray);

        return CharEncoder.decode(binaryOutput);
    }
}
