package de.lifecircles.model.neural;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
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

    final NeuronValueFunctionFactory neuronValueFunctionFactory = new DefaultNeuronValueFunctionFactory();


    @Test
    public void test() {
        final Random random = new Random(23);
        NeuralNetwork.setRandom(random);

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
            NeuralNetwork network = new NeuralNetwork(neuronValueFunctionFactory,
                    inputCount, architecture, outputCount, synapseConnectivity, 0);
            network.setDisableLayerDeactivation(true); // Layer-Deaktivierung für Test abschalten
            trainResultList.add(new NNTrainResult(network, 0.0D));
        }

        {
            String[] patterns = { "01.12." };
            char[] startCharArr = new char[] { '0', '1' };
            int epochs = 15_000;
            int trainDataSize = 6;
            final int trainCount = 30;
            trainResultList = trainSmallLanguageModel(random, trainResultList, patterns, trainDataSize, epochs, trainCount, startCharArr);
            final NeuralNetwork network = trainResultList.get(0).getNetwork();
            generateText(network);
        }
        {
            String[] patterns = { "01.12.23." };
            char[] startCharArr = new char[] { '0', '1', '2', '3' };
            int epochs = 15_000;
            int trainDataSize = 6;
            final int trainCount = 30;
            trainResultList = trainSmallLanguageModel(random, trainResultList, patterns, trainDataSize, epochs, trainCount, startCharArr);
            final NeuralNetwork network = trainResultList.get(0).getNetwork();
            generateText(network);
        }
    }

    //@Test
    public void testText() {
        final Random random = new Random(32);
        NeuralNetwork.setRandom(random);

        final int inputCount = 8;
        final int outputCount = 8;
        final int hiddenCount = inputCount * 2;
        final int[] architecture = {
                hiddenCount, hiddenCount,
                hiddenCount,
                hiddenCount,
        };
        final double synapseConnectivity = 0.9D;
        //final int networkCount = 60 * 3; // Anzahl der Netzwerke
        final int networkCount = 12 * 3; // Anzahl der Netzwerke

        List<NNTrainResult> trainResultList = new ArrayList<>();
        for (int networkPos = 0; networkPos < networkCount; networkPos++) {
            NeuralNetwork network = new NeuralNetwork(neuronValueFunctionFactory,
                    inputCount, architecture, outputCount, synapseConnectivity, 0);
            network.setDisableLayerDeactivation(true); // Layer-Deaktivierung für Test abschalten
            trainResultList.add(new NNTrainResult(network, 0.0D));
        }
        {
            //String[] patterns = { "Auto fährt. ", "Fahrad fährt. ", "Auto fährt schnell. ", "Fahrrad fährt langsam. " };
            String[] patterns = { "Auto. ", "Boat. ", "Car. ", "Door. ", "drive. ", "swim. " };
            char[] startCharArr = new char[] { 'A', 'B', 'C', 'D', 'd', 's' };
            int epochs = 10_000;
            int trainDataSize = 1;
            final int trainCount = 30;
            trainResultList = trainSmallLanguageModel(random, trainResultList, patterns, trainDataSize, epochs, trainCount, startCharArr);
            final NeuralNetwork network = trainResultList.get(0).getNetwork();
            generateText(network, startCharArr);
        }
        {
            String[] patterns = { "Auto drive. ", "Boat swim. ", "Car drive. ", "Door swim. ", "drive. ", "swim. " };
            char[] startCharArr = new char[] { 'A', 'B', 'C', 'D', 'd', 's' };
            int epochs = 250_000;
            int trainDataSize = 1;
            final int trainCount = 30;
            trainResultList = trainSmallLanguageModel(random, trainResultList, patterns, trainDataSize, epochs, trainCount, startCharArr);
            final NeuralNetwork network = trainResultList.get(0).getNetwork();
            generateText(network, startCharArr);
        }
    }

    //@Test
    public void testBigText() {
        final Random random = new Random(42);
        NeuralNetwork.setRandom(random);

        final int inputCount = 8;
        final int outputCount = 8;
        final int hiddenCount = inputCount * 2;
        final int[] architecture = {
                hiddenCount,
                hiddenCount,
                hiddenCount,
                hiddenCount,
        };
        final double synapseConnectivity = 0.9D;
        //final int networkCount = 60 * 3; // Anzahl der Netzwerke
        final int networkCount = 12 * 3; // Anzahl der Netzwerke

        List<NNTrainResult> trainResultList = new ArrayList<>();
        for (int networkPos = 0; networkPos < networkCount; networkPos++) {
            NeuralNetwork network = new NeuralNetwork(neuronValueFunctionFactory,
                    inputCount, architecture, outputCount, synapseConnectivity, 0);
            network.setDisableLayerDeactivation(true); // Layer-Deaktivierung für Test abschalten
            trainResultList.add(new NNTrainResult(network, 0.0D));
        }
        {
            String[] patterns = {
                    //Substantive (10):
                    "Bank ",
                    "Licht ",
                    "Spiel ",
                    "Schloss ",
                    "Blatt ",
                    "Zug ",
                    "Vogel ",
                    "Ton ",
                    "Ball ",
                    "Stern ",
                    //Verben (8):
                    "sitzt ",
                    "fliegt ",
                    "öffnet ",
                    "fällt ",
                    "leuchtet ",
                    "steht ",
                    "ist ",
                    "spielt ",
                    //Adjektive (6):
                    "hell ",
                    "alt ",
                    "leer ",
                    "laut ",
                    "groß ",
                    "schnell ",
                    //Funktionswörter (6):
                    "der ", "Der ",
                    "die ", "Die ",
                    "das ", "Das ",
                    "ein ", "Ein ",
                    "im ", "Im ",
                    "auf ", "Auf ",
                    //Satzzeichen (3):
                    ".",
                    " "
            };
            char[] startCharArr = new char[] { 'B', 'D', 'L', 'D', 'Z', 'a', 'd', 'l', 's' };
            int epochs = 20_000;
            int trainDataSize = 4;
            final int trainCount = 35;
            trainResultList = trainSmallLanguageModel(random, trainResultList, patterns, trainDataSize, epochs, trainCount, startCharArr);
            final NeuralNetwork network = trainResultList.get(0).getNetwork();
            generateText(network, startCharArr);
        }
        {
            String[] patterns = { 
                    "Der Vogel sitzt auf der Bank. ",
                    "Die Bank ist leer. ",
                    "Das Licht ist hell. ",
                    "Ein Ball fällt auf das Blatt. ",
                    "Der Stern leuchtet. ",
                    "Der Ton ist laut. ",
                    "Das Spiel ist alt. ",
                    "Der Zug ist schnell. ",
                    "Das Schloss ist groß. ",
                    "Der Ball fliegt im Licht. ",
                    "Ein Vogel fliegt schnell. ",
                    "Das Blatt fällt. ",
                    "Das Schloss öffnet. ",
                    "Der Ton fällt auf die Bank. ",
                    "Das Spiel steht im Schloss. ",
                    "Das Licht steht auf der Bank. ",
                    "Ein Spiel spielt im Schloss. ",
                    "Der Zug steht leer. ",
                    "Der Stern fällt. ",
                    "Der Vogel sitzt im Spiel. ",
                    "Die Bank ist alt. ",
                    "Der Ball sitzt auf dem Zug. ",
                    "Das Licht ist laut. ",
                    "Das Blatt spielt im Licht. ",
                    "Der Stern steht im Schloss. ",
                    "Ein Spiel leuchtet hell. ",
                    "Die Bank fällt schnell. ",
                    "Das Schloss ist leer. ",
                    "Der Vogel fliegt auf den Stern. ",
                    "Ein Blatt fällt auf das Spiel. ",

            //Substantive (10):
                    "Bank ",
                    "Licht ",
                    "Spiel ",
                    "Schloss ",
                    "Blatt ",
                    "Zug ",
                    "Vogel ",
                    "Ton ",
                    "Ball ",
                    "Stern ",
            //Verben (8):
                    "sitzt ",
                    "fliegt ",
                    "öffnet ",
                    "fällt ",
                    "leuchtet ",
                    "steht ",
                    "ist ",
                    "spielt ",
            //Adjektive (6):
                    "hell ",
                    "alt ",
                    "leer ",
                    "laut ",
                    "groß ",
                    "schnell ",
            //Funktionswörter (6):
                    "der ",
                    "die ",
                    "das ",
                    "ein ",
                    "im ",
                    "auf ",
            //Satzzeichen (3):
                    ".",
                    " "
            };
            char[] startCharArr = new char[] { 'D', 'E', 'D', 'E' };
            int epochs = 250_000;
            int trainDataSize = 1;
            final int trainCount = 30;
            trainResultList = trainSmallLanguageModel(random, trainResultList, patterns, trainDataSize, epochs, trainCount, startCharArr);
            final NeuralNetwork network = trainResultList.get(0).getNetwork();
            generateText(network, startCharArr);
        }
    }

    //@Test
    public void testSmallCount() {
        final Random random = new Random(42);
        NeuralNetwork.setRandom(random);

        final int inputCount = 8;
        final int outputCount = 8;
        final int hiddenCount = inputCount * 2;
        final int[] architecture = {
                hiddenCount,
                hiddenCount * 2,
                hiddenCount,
                hiddenCount,
        };
        final double synapseConnectivity = 0.9D;
        //final int networkCount = 60 * 3; // Anzahl der Netzwerke
        final int networkCount = 12 * 3; // Anzahl der Netzwerke

        List<NNTrainResult> trainResultList = new ArrayList<>();
        for (int networkPos = 0; networkPos < networkCount; networkPos++) {
            NeuralNetwork network = new NeuralNetwork(neuronValueFunctionFactory,
                    inputCount, architecture, outputCount, synapseConnectivity, 0);
            network.setDisableLayerDeactivation(true); // Layer-Deaktivierung für Test abschalten
            trainResultList.add(new NNTrainResult(network, 0.0D));
        }
        {
            String[] patterns = {"01"};
            char[] startCharArr = new char[]{'0', '1'};
            int epochs = 250_000;
            int trainDataSize = 12;
            final int trainCount = 8 * 3;
            trainResultList = trainSmallLanguageModel(random, trainResultList, patterns, trainDataSize, epochs, trainCount, startCharArr);
            final NeuralNetwork network = trainResultList.get(0).getNetwork();
            generateText(network, startCharArr);
        }
        {
            String[] patterns = {"012"};
            char[] startCharArr = new char[]{'0', '1', '2'};
            int epochs = 250_000;
            int trainDataSize = 8;
            final int trainCount = 8 * 3;
            trainResultList = trainSmallLanguageModel(random, trainResultList, patterns, trainDataSize, epochs, trainCount, startCharArr);
            final NeuralNetwork network = trainResultList.get(0).getNetwork();
            generateText(network, startCharArr);
        }
        {
            String[] patterns = {"0123"};
            char[] startCharArr = new char[]{'0', '1', '2', '3'};
            int epochs = 250_000;
            int trainDataSize = 4;
            final int trainCount = 8 * 3;
            trainResultList = trainSmallLanguageModel(random, trainResultList, patterns, trainDataSize, epochs, trainCount, startCharArr);
            final NeuralNetwork network = trainResultList.get(0).getNetwork();
            generateText(network, startCharArr);
        }
        // With memory:
        {
            String[] patterns = {"011"};
            char[] startCharArr = new char[]{'0', '1', '1'};
            int epochs = 250_000;
            int trainDataSize = 8;
            final int trainCount = 8 * 3;
            trainResultList = trainSmallLanguageModel(random, trainResultList, patterns, trainDataSize, epochs, trainCount, startCharArr);
            final NeuralNetwork network = trainResultList.get(0).getNetwork();
            generateText(network, startCharArr);
        }
    }

    //@Test
    public void testCount() {
        final Random random = new Random(42);
        NeuralNetwork.setRandom(random);

        final int inputCount = 8;
        final int outputCount = 8;
        final int hiddenCount = inputCount * 2;
        final int[] architecture = {
                hiddenCount, hiddenCount,
                hiddenCount,
                hiddenCount,
        };
        final double synapseConnectivity = 0.9D;
        //final int networkCount = 60 * 3; // Anzahl der Netzwerke
        final int networkCount = 12 * 2; // Anzahl der Netzwerke

        List<NNTrainResult> trainResultList = new ArrayList<>();
        for (int networkPos = 0; networkPos < networkCount; networkPos++) {
            NeuralNetwork network = new NeuralNetwork(neuronValueFunctionFactory,
                    inputCount, architecture, outputCount, synapseConnectivity, 0);
            network.setDisableLayerDeactivation(true); // Layer-Deaktivierung für Test abschalten
            trainResultList.add(new NNTrainResult(network, 0.0D));
        }
        {
            //String[] patterns = {"Auto fährt. ", "Fahrad fährt. ", "Auto fährt schnell. ", "Fahrrad fährt langsam. "};
            String[] patterns = { "0123456789" };
            char[] startCharArr = new char[] { '0', '3', '6', '9' };
            int epochs = 250_000;
            int trainDataSize = 4;
            final int trainCount = 8;
            trainResultList = trainSmallLanguageModel(random, trainResultList, patterns, trainDataSize, epochs, trainCount, startCharArr);
            final NeuralNetwork network = trainResultList.get(0).getNetwork();
            generateText(network, startCharArr);
        }
    }

    private static List<NNTrainResult> trainSmallLanguageModel(final Random random, final List<NNTrainResult> inTrainResultList, String[] patterns, int trainDataSize, int epochs, final int trainCount) {
        return trainSmallLanguageModel(random,
                                        inTrainResultList,
                                        patterns,
                                        trainDataSize,
                                        epochs, trainCount,
                                        new char[] { '0', '1', '2' });
    }

    private static List<NNTrainResult> trainSmallLanguageModel(final Random random,
                                                               final List<NNTrainResult> inTrainResultList,
                                                               String[] patterns,
                                                               int trainDataSize,
                                                               int epochs, final int trainCount,
                                                               char[] startCharArr) {
        return train(random, inTrainResultList, patterns, trainDataSize, epochs, trainCount, startCharArr);
    }

    /**
     * Trainiert das Modell mit einer Textsequenz.
     *
     * @param patterns Der Trainingstext
     * @param epochs Anzahl der Trainingsepochen
     */
    public static List<NNTrainResult> train(final Random random, final List<NNTrainResult> inTrainResultList, String[] patterns, int trainDataSize, int epochs, final int trainCount,
                                            char[] startCharArr) {
        logger.info("Starte Training mit Text: '{}' für {} Epochen", patterns, epochs);

        double mutationRate = 0.1D;
        double mutationStrengt = 0.1D;

        final List<NNTrainResult> trainResultList = new ArrayList<>(inTrainResultList) ;

        for (int epoch = 0; epoch < epochs; epoch++) {
            trainResultList.parallelStream().forEach(trainResult -> {
            //for (final NNTrainResult trainResult : trainResultList) {
                final NeuralNetwork network = trainResult.getNetwork();

                trainResult.lossSum = 0;
                trainResult.proccesLoss = 0;
                //final int trainCount = 30;

                int trainedCharacters = 0;

                for (int trainPos = 0; trainPos < trainCount; trainPos++) {
                    // Netzwerk für das nächste Training zurücksetzen
                    network.rnnClearPreviousState();

                    // Training für die gesamte Sequenz
                    for (int trainDataPos = 0; trainDataPos < trainDataSize; trainDataPos++) {
                        final String text = patterns[random.nextInt(patterns.length)];
                        char[] characters = text.toCharArray();

                        for (int charPos = 0; charPos < characters.length - 1; charPos++) {
                            final int currentCharPos = (charPos) % characters.length;
                            final int nextCharPos = (currentCharPos + 1) % characters.length;
                            char currentChar = characters[currentCharPos];
                            char nextChar = characters[nextCharPos];

                            double[] inputArr = CharEncoder.encode(currentChar);
                            double[] expectedOutputArr = CharEncoder.encode(nextChar);

                            // Training für den nächsten Buchstaben.
                            double[] outputArray = network.calcTrain(inputArr, expectedOutputArr);

                            long proccessedSynapses = network.getProccessedSynapses();

                            calculateLoss(trainResult, outputArray, expectedOutputArr, proccessedSynapses);
                            trainedCharacters++;
                        }
                    }
                }
                // 75% Verlust, 25% Proccessed Synapses
                //final double resultWeight = 1.0D; // 0.75D;
                final double resultWeight = 0.995D;
                //final double proccesWeight = 0.0D; // 0.25D;
                final double proccesWeight = 0.005D;
                trainResult.lossSum = (trainResult.lossSum / (trainedCharacters)) * resultWeight;
                trainResult.proccesLoss = (trainResult.proccesLoss / (trainedCharacters * maxProccessedSynapses)) * proccesWeight;
                trainResult.setLoss(trainResult.lossSum + trainResult.proccesLoss);
            });
            // Sortieren der Netzwerke nach Verlust
            trainResultList.sort((a, b) -> Double.compare(a.getLoss(), b.getLoss()));
            NNTrainResult bestTrainResult = trainResultList.get(0);
            NeuralNetwork bestNeuralNetwork = bestTrainResult.getNetwork();

            if (bestTrainResult.getLoss() < 0.005D) {
                logger.info("Bestes Netzwerk erreicht einen Verlust von {}. Training wird abgebrochen.", bestTrainResult.getLoss());
                generateText(bestNeuralNetwork, startCharArr);
                break; // Training abbrechen, wenn Verlust unter 0.01 liegt
            }
            if (epoch % 100 == 0) {
                // Anzahl der neuronen.
                final long neuronCount = Arrays.stream(bestNeuralNetwork.getHiddenLayerArr())
                        .mapToInt(layer -> layer.getNeuronsArr().length).sum();

                System.out.printf("Epoche %d, Error: %f, Proc-Synapses: %d / %d, HiddenLayers: %d, neronCount: %d, lossSum: %f, proccesLoss: %f, maxProccessedSynapses: %d%n",
                        epoch, bestTrainResult.getLoss(),
                        bestNeuralNetwork.getProccessedSynapses(),
                        bestNeuralNetwork.getSynapseList().size(),
                        bestNeuralNetwork.getHiddenLayerArr().length,
                        neuronCount,
                        bestTrainResult.lossSum, bestTrainResult.proccesLoss, maxProccessedSynapses);

                generateText(bestNeuralNetwork, startCharArr);
            }

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
            final int winnerCout = winnerNetworkList.size();
            // Muttieren der ausgewählten Netze und hinzufügen zu den Gewinnern.
            while (winnerNetworkList.size() < inTrainResultList.size()) {
                NeuralNetwork winnerNetwork = winnerNetworkList.get(random.nextInt(winnerCout));
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
        for (Character startChar : startCharArr) {
            network.rnnClearPreviousState();
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
