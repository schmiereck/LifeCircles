package de.lifecircles.model.neural;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.io.*;

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

    final NeuronValueFunctionFactory neuronValueFunctionFactory = new ValuesNeuronValueFunctionFactory();


    @Test
    public void testSimple() {
        final Random random = new Random(2342);
        NeuralNetwork.setRandom(random);

        final int inputCount = 8;
        final int outputCount = 8;
        final int hiddenCount = inputCount * 2;
        final int[] architecture = {
                hiddenCount,
                hiddenCount,
                hiddenCount,
        };
        final double synapseConnectivity = 0.8D;
        final int networkCount = 60; // Anzahl der Netzwerke
        final boolean useBackpropagate = true;

        List<NNTrainResult> trainResultList = new ArrayList<>();
        for (int networkPos = 0; networkPos < networkCount; networkPos++) {
            NeuralNetwork network = new NeuralNetwork(neuronValueFunctionFactory,
                    inputCount, architecture, outputCount, synapseConnectivity, 0);
            network.setDisableLayerDeactivation(true); // Layer-Deaktivierung für Test abschalten
            network.setEnableNeuronType(true);
            trainResultList.add(new NNTrainResult(network, 0.0D));
        }

        {
            String[] patterns = { "01" };
            char[] startCharArr = new char[] { '0', '1' };
            int epochs = 15_000;
            int trainDataSize = 6;
            final int trainCount = 30;
            trainResultList = trainSmallLanguageModel(random, trainResultList, patterns, trainDataSize, epochs, trainCount, startCharArr, useBackpropagate);
            final NeuralNetwork network = trainResultList.getFirst().getNetwork();
            generateText(network, startCharArr);
        }
        {
            String[] patterns = { "012" };
            char[] startCharArr = new char[] { '0', '1', '2' };
            int epochs = 15_000;
            int trainDataSize = 6;
            final int trainCount = 30;
            trainResultList = trainSmallLanguageModel(random, trainResultList, patterns, trainDataSize, epochs, trainCount, startCharArr, useBackpropagate);
            final NeuralNetwork network = trainResultList.getFirst().getNetwork();
            generateText(network, startCharArr);
        }
        {
            String[] patterns = { "0123" };
            char[] startCharArr = new char[] { '0', '1', '2', '3' };
            int epochs = 15_000;
            int trainDataSize = 6;
            final int trainCount = 30;
            trainResultList = trainSmallLanguageModel(random, trainResultList, patterns, trainDataSize, epochs, trainCount, startCharArr, useBackpropagate);
            final NeuralNetwork network = trainResultList.getFirst().getNetwork();
            generateText(network, startCharArr);
        }
        {
            String[] patterns = { "01234" };
            char[] startCharArr = new char[] { '0', '1', '2', '3', '4' };
            int epochs = 15_000;
            int trainDataSize = 6;
            final int trainCount = 30;
            trainResultList = trainSmallLanguageModel(random, trainResultList, patterns, trainDataSize, epochs, trainCount, startCharArr, useBackpropagate);
            final NeuralNetwork network = trainResultList.getFirst().getNetwork();
            generateText(network, startCharArr);
        }
        {
            String[] patterns = { "012345" };
            char[] startCharArr = new char[] { '0', '1', '2', '3', '4', '5' };
            int epochs = 15_000;
            int trainDataSize = 6;
            final int trainCount = 30;
            trainResultList = trainSmallLanguageModel(random, trainResultList, patterns, trainDataSize, epochs, trainCount, startCharArr, useBackpropagate);
            final NeuralNetwork network = trainResultList.getFirst().getNetwork();
            generateText(network, startCharArr);
        }
        {
            String[] patterns = { "0123456" };
            char[] startCharArr = new char[] { '0', '1', '2', '3', '4', '5', '6' };
            int epochs = 15_000;
            int trainDataSize = 6;
            final int trainCount = 30;
            trainResultList = trainSmallLanguageModel(random, trainResultList, patterns, trainDataSize, epochs, trainCount, startCharArr, useBackpropagate);
            final NeuralNetwork network = trainResultList.getFirst().getNetwork();
            generateText(network, startCharArr);
        }
        {
            String[] patterns = { "01234567" };
            char[] startCharArr = new char[] { '0', '1', '2', '3', '4', '5', '6', '7' };
            int epochs = 15_000;
            int trainDataSize = 6;
            final int trainCount = 30;
            trainResultList = trainSmallLanguageModel(random, trainResultList, patterns, trainDataSize, epochs, trainCount, startCharArr, useBackpropagate);
            final NeuralNetwork network = trainResultList.getFirst().getNetwork();
            generateText(network, startCharArr);
        }
        {
            String[] patterns = { "012345678" };
            char[] startCharArr = new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8' };
            int epochs = 15_000;
            int trainDataSize = 6;
            final int trainCount = 30;
            trainResultList = trainSmallLanguageModel(random, trainResultList, patterns, trainDataSize, epochs, trainCount, startCharArr, useBackpropagate);
            final NeuralNetwork network = trainResultList.getFirst().getNetwork();
            generateText(network, startCharArr);
        }
        {
            String[] patterns = { "0123456789" };
            char[] startCharArr = new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' };
            int epochs = 15_000;
            int trainDataSize = 6;
            final int trainCount = 30;
            trainResultList = trainSmallLanguageModel(random, trainResultList, patterns, trainDataSize, epochs, trainCount, startCharArr, useBackpropagate);
            final NeuralNetwork network = trainResultList.getFirst().getNetwork();
            generateText(network, startCharArr);
        }
    }

    @Test
    public void testMemorySimple() {
        final Random random = new Random(2342);
        NeuralNetwork.setRandom(random);

        final int inputCount = 8;
        final int outputCount = 8;
        final int hiddenCount = inputCount * 2;
        final int[] architecture = {
                hiddenCount, hiddenCount,
                hiddenCount,
        };
        final double synapseConnectivity = 0.8D;
        final boolean useBackpropagate = true;
        final int networkCount = 60; // Anzahl der Netzwerke

        List<NNTrainResult> trainResultList = new ArrayList<>();
        for (int networkPos = 0; networkPos < networkCount; networkPos++) {
            NeuralNetwork network = new NeuralNetwork(neuronValueFunctionFactory,
                    inputCount, architecture, outputCount, synapseConnectivity, 0);
            network.setDisableLayerDeactivation(true); // Layer-Deaktivierung für Test abschalten
            network.setEnableNeuronType(true);
            trainResultList.add(new NNTrainResult(network, 0.0D));
        }

        {
            String[] patterns = { "0011" };
            char[] startCharArr = new char[] { '0', '1' };
            int epochs = 15_000;
            int trainDataSize = 6;
            final int trainCount = 20;
            trainResultList = trainSmallLanguageModel(random, trainResultList, patterns, trainDataSize, epochs, trainCount, startCharArr, useBackpropagate);
            final NeuralNetwork network = trainResultList.getFirst().getNetwork();
            generateText(network, startCharArr);
        }
        {
            String[] patterns = { "001122" };
            char[] startCharArr = new char[] { '0', '1', '2' };
            int epochs = 15_000;
            int trainDataSize = 6;
            final int trainCount = 20;
            trainResultList = trainSmallLanguageModel(random, trainResultList, patterns, trainDataSize, epochs, trainCount, startCharArr, useBackpropagate);
            final NeuralNetwork network = trainResultList.getFirst().getNetwork();
            generateText(network, startCharArr);
        }
        {
            String[] patterns = { "00112233" };
            char[] startCharArr = new char[] { '0', '1', '2', '3' };
            int epochs = 15_000;
            int trainDataSize = 6;
            final int trainCount = 20;
            trainResultList = trainSmallLanguageModel(random, trainResultList, patterns, trainDataSize, epochs, trainCount, startCharArr, useBackpropagate);
            final NeuralNetwork network = trainResultList.getFirst().getNetwork();
            generateText(network, startCharArr);
        }
    }

    @Test
    public void test() {
        final Random random = new Random(42);
        NeuralNetwork.setRandom(random);

        final int inputCount = 8;
        final int outputCount = 8;
        final int hiddenCount = inputCount * 2;
        final int[] architecture = {
                hiddenCount, hiddenCount,
                hiddenCount,
        };
        final double synapseConnectivity = 0.8D;
        final boolean useBackpropagate = true;

        List<NNTrainResult> trainResultList = new ArrayList<>();
        for (int networkPos = 0; networkPos < 60; networkPos++) {
            NeuralNetwork network = new NeuralNetwork(neuronValueFunctionFactory,
                    inputCount, architecture, outputCount, synapseConnectivity, 0);
            network.setDisableLayerDeactivation(true); // Layer-Deaktivierung für Test abschalten
            network.setEnableNeuronType(true);
            trainResultList.add(new NNTrainResult(network, 0.0D));
        }

        {
            String[] patterns = { "01.12." };
            char[] startCharArr = new char[] { '0', '1' };
            int epochs = 15_000;
            int trainDataSize = 6;
            final int trainCount = 30;
            trainResultList = trainSmallLanguageModel(random, trainResultList, patterns, trainDataSize, epochs, trainCount, startCharArr, useBackpropagate);
            final NeuralNetwork network = trainResultList.getFirst().getNetwork();
            generateText(network, startCharArr);
        }
        {
            String[] patterns = { "01.12.23." };
            char[] startCharArr = new char[] { '0', '1', '2', '3' };
            int epochs = 15_000;
            int trainDataSize = 6;
            final int trainCount = 30;
            trainResultList = trainSmallLanguageModel(random, trainResultList, patterns, trainDataSize, epochs, trainCount, startCharArr, useBackpropagate);
            final NeuralNetwork network = trainResultList.getFirst().getNetwork();
            generateText(network, startCharArr);
        }
    }

    @Test
    public void testText() {
        final Random random = new Random(32);
        NeuralNetwork.setRandom(random);

        final int inputCount = 8;
        final int outputCount = 8;
        final int hiddenCount = inputCount * 2;
        final int[] architecture = {
                hiddenCount,
                hiddenCount,
                hiddenCount * 2,
                hiddenCount,
        };
        final double synapseConnectivity = 0.9D;
        //final int networkCount = 60 * 3; // Anzahl der Netzwerke
        final int networkCount = 12 * 3; // Anzahl der Netzwerke
        final boolean useBackpropagate = true;

        List<NNTrainResult> trainResultList = new ArrayList<>();
        for (int networkPos = 0; networkPos < networkCount; networkPos++) {
            NeuralNetwork network = new NeuralNetwork(neuronValueFunctionFactory,
                    inputCount, architecture, outputCount, synapseConnectivity, 0);
            network.setDisableLayerDeactivation(true); // Layer-Deaktivierung für Test abschalten
            network.setEnableNeuronType(true);
            trainResultList.add(new NNTrainResult(network, 0.0D));
        }
        {
            //String[] patterns = { "Auto fährt. ", "Fahrad fährt. ", "Auto fährt schnell. ", "Fahrrad fährt langsam. " };
            String[] patterns = { "Auto. ", "Boat. ", "Car. ", "Door. ", "drive. ", "swim. " };
            char[] startCharArr = new char[] { 'A', 'B', 'C', 'D', 'd', 's' };
            int epochs = 10_000;
            int trainDataSize = 1;
            final int trainCount = 30;
            trainResultList = trainSmallLanguageModel(random, trainResultList, patterns, trainDataSize, epochs, trainCount, startCharArr, useBackpropagate);
            final NeuralNetwork network = trainResultList.getFirst().getNetwork();
            generateText(network, startCharArr);
        }
        {
            String[] patterns = { "Auto drive. ", "Boat swim. ", "Car drive. ", "Door swim. ", "drive. ", "swim. " };
            char[] startCharArr = new char[] { 'A', 'B', 'C', 'D', 'd', 's' };
            int epochs = 10_000;
            int trainDataSize = 1;
            final int trainCount = 30;
            trainResultList = trainSmallLanguageModel(random, trainResultList, patterns, trainDataSize, epochs, trainCount, startCharArr, useBackpropagate);
            final NeuralNetwork network = trainResultList.getFirst().getNetwork();
            generateText(network, startCharArr);
        }
    }

    @Test
    public void testBigText() {
        final Random random = new Random(42);
        NeuralNetwork.setRandom(random);

        final String networkFileName =
                "C:\\Users\\SCMJ178\\OneDrive\\Dokumente\\#Projekte\\LifeCircles\\" +
                "bigtext_network.vnn";

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
        final int networkCount = 12 * 3; // Anzahl der Netzwerke
        final boolean useBackpropagate = true;

        List<NNTrainResult> trainResultList = new ArrayList<>();

        // Versuche zuerst, ein vorhandenes Netzwerk zu laden
        NeuralNetwork loadedNetwork = loadNetwork(networkFileName);

        if (loadedNetwork != null) {
            logger.info("Verwende gespeichertes Netzwerk als Basis für die Population");
            // Erstelle die Population basierend auf dem geladenen Netzwerk
            trainResultList.add(new NNTrainResult(loadedNetwork, 0.0D)); // Original

            // Erstelle den Rest der Population durch Mutation des geladenen Netzwerks
            for (int networkPos = 1; networkPos < networkCount; networkPos++) {
                NeuralNetwork mutatedNetwork = loadedNetwork.mutate(0.1D, 0.1D);
                mutatedNetwork.setDisableLayerDeactivation(true);
                mutatedNetwork.setEnableNeuronType(true);
                trainResultList.add(new NNTrainResult(mutatedNetwork, 0.0D));
            }
        } else {
            logger.info("Kein gespeichertes Netzwerk gefunden, erstelle neue Population");
            // Erstelle eine neue Population, da kein Netzwerk geladen werden konnte
            for (int networkPos = 0; networkPos < networkCount; networkPos++) {
                NeuralNetwork network = new NeuralNetwork(neuronValueFunctionFactory,
                        inputCount, architecture, outputCount, synapseConnectivity, 0);
                network.setDisableLayerDeactivation(true); // Layer-Deaktivierung für Test abschalten
                network.setEnableNeuronType(true);
                trainResultList.add(new NNTrainResult(network, 0.0D));
            }
        }

        {
            //String[] patterns = { "Auto fährt. ", "Fahrad fährt. ", "Auto fährt schnell. ", "Fahrrad fährt langsam. " };
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
            int trainDataSize = 1;
            final int trainCount = 25;

            // Trainiere mit periodischem Speichern
            trainResultList = trainSmallLanguageModelWithSave(random, trainResultList, patterns, trainDataSize,
                                                           epochs, trainCount, startCharArr, networkFileName,
                    useBackpropagate);

            final NeuralNetwork network = trainResultList.getFirst().getNetwork();
            generateText(network, startCharArr);

            // Nach Abschluss des Trainings speichern
            saveNetwork(network, networkFileName);
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
            final int trainCount = 20;

            // Trainiere mit periodischem Speichern
            trainResultList = trainSmallLanguageModelWithSave(random, trainResultList, patterns, trainDataSize,
                                                           epochs, trainCount, startCharArr, networkFileName,
                    useBackpropagate);

            final NeuralNetwork network = trainResultList.getFirst().getNetwork();
            generateText(network, startCharArr);

            // Nach Abschluss des Trainings das beste Netzwerk speichern
            saveNetwork(network, networkFileName);
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
        final boolean useBackpropagate = true;

        List<NNTrainResult> trainResultList = new ArrayList<>();
        for (int networkPos = 0; networkPos < networkCount; networkPos++) {
            NeuralNetwork network = new NeuralNetwork(neuronValueFunctionFactory,
                    inputCount, architecture, outputCount, synapseConnectivity, 0);
            network.setDisableLayerDeactivation(true); // Layer-Deaktivierung für Test abschalten
            network.setEnableNeuronType(true);
            trainResultList.add(new NNTrainResult(network, 0.0D));
        }
        {
            String[] patterns = {"01"};
            char[] startCharArr = new char[]{'0', '1'};
            int epochs = 250_000;
            int trainDataSize = 12;
            final int trainCount = 8 * 3;
            trainResultList = trainSmallLanguageModel(random, trainResultList, patterns, trainDataSize, epochs, trainCount, startCharArr, useBackpropagate);
            final NeuralNetwork network = trainResultList.getFirst().getNetwork();
            generateText(network, startCharArr);
        }
        {
            String[] patterns = {"012"};
            char[] startCharArr = new char[]{'0', '1', '2'};
            int epochs = 250_000;
            int trainDataSize = 8;
            final int trainCount = 8 * 3;
            trainResultList = trainSmallLanguageModel(random, trainResultList, patterns, trainDataSize, epochs, trainCount, startCharArr, useBackpropagate);
            final NeuralNetwork network = trainResultList.getFirst().getNetwork();
            generateText(network, startCharArr);
        }
        {
            String[] patterns = {"0123"};
            char[] startCharArr = new char[]{'0', '1', '2', '3'};
            int epochs = 250_000;
            int trainDataSize = 4;
            final int trainCount = 8 * 3;
            trainResultList = trainSmallLanguageModel(random, trainResultList, patterns, trainDataSize, epochs, trainCount, startCharArr, useBackpropagate);
            final NeuralNetwork network = trainResultList.getFirst().getNetwork();
            generateText(network, startCharArr);
        }
        // With memory:
        {
            String[] patterns = {"011"};
            char[] startCharArr = new char[]{'0', '1', '1'};
            int epochs = 250_000;
            int trainDataSize = 8;
            final int trainCount = 8 * 3;
            trainResultList = trainSmallLanguageModel(random, trainResultList, patterns, trainDataSize, epochs, trainCount, startCharArr, useBackpropagate);
            final NeuralNetwork network = trainResultList.getFirst().getNetwork();
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
        final boolean useBackpropagate = true;

        List<NNTrainResult> trainResultList = new ArrayList<>();
        for (int networkPos = 0; networkPos < networkCount; networkPos++) {
            NeuralNetwork network = new NeuralNetwork(neuronValueFunctionFactory,
                    inputCount, architecture, outputCount, synapseConnectivity, 0);
            network.setDisableLayerDeactivation(true); // Layer-Deaktivierung für Test abschalten
            network.setEnableNeuronType(true);
            trainResultList.add(new NNTrainResult(network, 0.0D));
        }
        {
            //String[] patterns = {"Auto fährt. ", "Fahrad fährt. ", "Auto fährt schnell. ", "Fahrrad fährt langsam. "};
            String[] patterns = { "0123456789" };
            char[] startCharArr = new char[] { '0', '3', '6', '9' };
            int epochs = 250_000;
            int trainDataSize = 4;
            final int trainCount = 8;
            trainResultList = trainSmallLanguageModel(random, trainResultList, patterns, trainDataSize, epochs, trainCount, startCharArr, useBackpropagate);
            final NeuralNetwork network = trainResultList.getFirst().getNetwork();
            generateText(network, startCharArr);
        }
    }

    private static List<NNTrainResult> trainSmallLanguageModel(final Random random, final List<NNTrainResult> inTrainResultList,
                                                               String[] patterns, int trainDataSize, int epochs,
                                                               final int trainCount, final boolean useBackpropagate) {
        return trainSmallLanguageModel(random,
                                        inTrainResultList,
                                        patterns,
                                        trainDataSize,
                                        epochs, trainCount,
                                        new char[] { '0', '1', '2' },
                useBackpropagate);
    }

    private static List<NNTrainResult> trainSmallLanguageModel(final Random random,
                                                               final List<NNTrainResult> inTrainResultList,
                                                               String[] patterns,
                                                               int trainDataSize,
                                                               int epochs, final int trainCount,
                                                               char[] startCharArr,
                                                               final boolean useBackpropagate) {
        return train(random, inTrainResultList, patterns, trainDataSize, epochs, trainCount, startCharArr, useBackpropagate);
    }

    /**
     * Trainiert das Modell mit einer Textsequenz.
     *
     * @param patterns Der Trainingstext
     * @param epochs Anzahl der Trainingsepochen
     */
    private static List<NNTrainResult> train(final Random random, final List<NNTrainResult> inTrainResultList, String[] patterns,
                                             int trainPatternCount, int epochs, final int trainCount,
                                             char[] startCharArr, final boolean useBackpropagate) {
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

                if (useBackpropagate) {
                    for (int trainPos = 0; trainPos < trainCount; trainPos++) {

                        // Netzwerk für das nächste Training zurücksetzen
                        network.rnnClearPreviousState();

                        // Training für die gesamte Sequenz
                        for (int trainDataPos = 0; trainDataPos < trainPatternCount; trainDataPos++) {
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
                } else {
                    // Bewertung für alle Patterns.
                    for (int trainPatternPos = 0; trainPatternPos < patterns.length; trainPatternPos++) {
                        final String text = patterns[trainPatternPos];
                        char[] characters = text.toCharArray();

                        // Netzwerk für das nächste Training zurücksetzen
                        network.rnnClearPreviousState();

                        for (int charPos = 0; charPos < characters.length - 1; charPos++) {
                            final int currentCharPos = (charPos) % characters.length;
                            final int nextCharPos = (currentCharPos + 1) % characters.length;
                            char currentChar = characters[currentCharPos];
                            char nextChar = characters[nextCharPos];

                            double[] inputArr = CharEncoder.encode(currentChar);
                            double[] expectedOutputArr = CharEncoder.encode(nextChar);

                            // Prezessieren des den nächsten Buchstaben.
                            final double[] outputArray = network.calcProcess(inputArr);

                            long proccessedSynapses = network.getProccessedSynapses();

                            calculateLoss(trainResult, outputArray, expectedOutputArr, proccessedSynapses);
                            trainedCharacters++;
                        }
                    }
                }

                // 75% Verlust, 25% Proccessed Synapses
                //final double resultWeight = 1.0D; // 0.75D;
                final double resultWeight = 0.9995D;
                //final double proccesWeight = 0.0D; // 0.25D;
                final double proccesWeight = 0.0005D;
                trainResult.lossSum = (trainResult.lossSum / (trainedCharacters)) * resultWeight;
                trainResult.proccesLoss = (trainResult.proccesLoss / (trainedCharacters * maxProccessedSynapses)) * proccesWeight;
                trainResult.setLoss(trainResult.lossSum + trainResult.proccesLoss);
            });
            // Sortieren der Netzwerke nach Verlust
            trainResultList.sort(Comparator.comparingDouble(NNTrainResult::getLoss));
            NNTrainResult bestTrainResult = trainResultList.getFirst();
            final NeuralNetwork bestNeuralNetwork = bestTrainResult.getNetwork();

            if (bestTrainResult.getLoss() < 0.005D) {
                logger.info("Bestes Netzwerk erreicht einen Verlust von {}. Training wird abgebrochen.", bestTrainResult.getLoss());
                generateText(bestNeuralNetwork, startCharArr);
                break; // Training abbrechen, wenn Verlust unter 0.01 liegt
            }
            if (epoch % 100 == 0) {
                // Anzahl der neuronen.
                final long neuronCount = Arrays.stream(bestNeuralNetwork.getHiddenLayerArr())
                        .mapToInt(layer -> layer.getNeuronsArr().length).sum();

                System.out.printf("Epoche %d, Error: %f, Proc-Synapses: %d / %d, HiddenLayers: %d, neuronCount: %d, lossSum: %f, proccesLoss: %f, maxProccessedSynapses: %d%n",
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

            final List<NeuralNetwork> nextGenerationList = new ArrayList<>(winnerNetworkList);

            // Muttieren der Gewinner, um die nächste Generation aufzufüllen
            while (nextGenerationList.size() < inTrainResultList.size()) {
                NeuralNetwork winnerNetwork = winnerNetworkList.get(random.nextInt(winnerNetworkList.size()));
                // Mutieren des Netzwerks
                NeuralNetwork childNetwork = winnerNetwork.mutate(mutationRate, mutationStrengt);
                nextGenerationList.add(childNetwork);
            }

            // Alle Netzwerke für die nächste Epoche zurücksetzen
            trainResultList.clear();
            for (final NeuralNetwork network : nextGenerationList) {
                trainResultList.add(new NNTrainResult(network, 0.0D));
            }
        }

        logger.info("Training abgeschlossen.");
        return trainResultList;
    }

    /**
     * Version von trainSmallLanguageModel, die zusätzlich periodisch das beste Netzwerk speichert
     */
    private static List<NNTrainResult> trainSmallLanguageModelWithSave(final Random random,
                                                               final List<NNTrainResult> inTrainResultList,
                                                               String[] patterns,
                                                               int trainPatternCount,
                                                               int epochs, final int trainCount,
                                                               char[] startCharArr,
                                                               String networkFileName,
                                                                       final boolean useBackpropagate) {
        logger.info("Starte Training mit Text: '{}' für {} Epochen mit periodischem Speichern", patterns, epochs);

        double mutationRate = 0.1D;
        double mutationStrengt = 0.1D;

        final List<NNTrainResult> trainResultList = new ArrayList<>(inTrainResultList);

        for (int epoch = 0; epoch < epochs; epoch++) {
            trainResultList.parallelStream().forEach(trainResult -> {
                final NeuralNetwork network = trainResult.getNetwork();

                trainResult.lossSum = 0;
                trainResult.proccesLoss = 0;

                int trainedCharacters = 0;

                if (useBackpropagate) {
                    for (int trainPos = 0; trainPos < trainCount; trainPos++) {

                        // Netzwerk für das nächste Training zurücksetzen
                        network.rnnClearPreviousState();

                        // Training für verschiedene Patterns.
                        for (int trainPatternPos = 0; trainPatternPos < trainPatternCount; trainPatternPos++) {
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
                } else {
                    // Bewertung für alle Patterns.
                    for (int trainPatternPos = 0; trainPatternPos < patterns.length; trainPatternPos++) {
                        final String text = patterns[trainPatternPos];
                        char[] characters = text.toCharArray();

                        // Netzwerk für das nächste Training zurücksetzen
                        network.rnnClearPreviousState();

                        for (int charPos = 0; charPos < characters.length - 1; charPos++) {
                            final int currentCharPos = (charPos) % characters.length;
                            final int nextCharPos = (currentCharPos + 1) % characters.length;
                            char currentChar = characters[currentCharPos];
                            char nextChar = characters[nextCharPos];

                            double[] inputArr = CharEncoder.encode(currentChar);
                            double[] expectedOutputArr = CharEncoder.encode(nextChar);

                            // Prezessieren des den nächsten Buchstaben.
                            final double[] outputArray = network.calcProcess(inputArr);

                            long proccessedSynapses = network.getProccessedSynapses();

                            calculateLoss(trainResult, outputArray, expectedOutputArr, proccessedSynapses);
                            trainedCharacters++;
                        }
                    }
                }
                // 75% Verlust, 25% Proccessed Synapses
                final double resultWeight = 0.9995D;
                final double proccesWeight = 0.0005D;
                trainResult.lossSum = (trainResult.lossSum / (trainedCharacters)) * resultWeight;
                trainResult.proccesLoss = (trainResult.proccesLoss / (trainedCharacters * maxProccessedSynapses)) * proccesWeight;
                trainResult.setLoss(trainResult.lossSum + trainResult.proccesLoss);
            });

            // Sortieren der Netzwerke nach Verlust
            trainResultList.sort(Comparator.comparingDouble(NNTrainResult::getLoss));
            NNTrainResult bestTrainResult = trainResultList.getFirst();
            final NeuralNetwork bestNeuralNetwork = bestTrainResult.getNetwork();

            // Periodisches Speichern des besten Netzwerks alle 1000 Epochen
            if (epoch % 1000 == 0 && networkFileName != null) {
                // Speichere mit Epochen-Nummer im Dateinamen
                saveNetwork(bestNeuralNetwork, networkFileName);
                logger.info("Netzwerk nach Epoche {} gespeichert als {}", epoch, networkFileName);
            }

            if (bestTrainResult.getLoss() < 0.005D) {
                logger.info("Bestes Netzwerk erreicht einen Verlust von {}. Training wird abgebrochen.", bestTrainResult.getLoss());
                generateText(bestNeuralNetwork, startCharArr);

                // Speichere das finale Netzwerk, wenn es vorzeitig das Trainingsziel erreicht
                if (networkFileName != null) {
                    saveNetwork(bestNeuralNetwork, networkFileName);
                    logger.info("Finales Netzwerk gespeichert als {}", networkFileName);
                }

                break; // Training abbrechen, wenn Verlust unter 0.005 liegt
            }

            if (epoch % 100 == 0) {
                // Anzahl der neuronen
                final long neuronCount = Arrays.stream(bestNeuralNetwork.getHiddenLayerArr())
                        .mapToInt(layer -> layer.getNeuronsArr().length).sum();

                System.out.printf("Epoche %d, Error: %f, Proc-Synapses: %d / %d, HiddenLayers: %d, neuronCount: %d, lossSum: %f, proccesLoss: %f, maxProccessedSynapses: %d%n",
                        epoch, bestTrainResult.getLoss(),
                        bestNeuralNetwork.getProccessedSynapses(),
                        bestNeuralNetwork.getSynapseList().size(),
                        bestNeuralNetwork.getHiddenLayerArr().length,
                        neuronCount,
                        bestTrainResult.lossSum, bestTrainResult.proccesLoss, maxProccessedSynapses);

                generateText(bestNeuralNetwork, startCharArr);
            }

            if ((epoch + 1) >= epochs) {
                // Speichere das beste Netzwerk am Ende des Trainings
                if (networkFileName != null) {
                    saveNetwork(bestNeuralNetwork, networkFileName);
                    logger.info("Finales Netzwerk nach {} Epochen gespeichert als {}", epoch, networkFileName);
                }
                break;
            }

            // 20% der besten Netze auswählen und hinzufügen zu den Gewinnern.
            final int winnerCount = trainResultList.size() / 5;
            final List<NeuralNetwork> winnerNetworkList = new ArrayList<>();
            for (int winnerPos = 0; winnerPos < winnerCount; winnerPos++) {
                final NNTrainResult trainResult = trainResultList.get(winnerPos);
                NeuralNetwork winnerNetwork = trainResult.getNetwork();
                winnerNetwork.rnnClearPreviousState();
                winnerNetworkList.add(winnerNetwork);
            }

            // 10% zufällige Netze auswählen und hinzufügen zu den Gewinnern.
            final int luckyCount = trainResultList.size() / 10;
            for (int winnerPos = 0; winnerPos < luckyCount; winnerPos++) {
                final NNTrainResult trainResult = trainResultList.get(winnerCount + random.nextInt(trainResultList.size() - winnerCount));
                final NeuralNetwork winnerNetwork = trainResult.getNetwork();
                winnerNetwork.rnnClearPreviousState();
                winnerNetworkList.add(winnerNetwork);
            }

            final List<NeuralNetwork> nextGenerationList = new ArrayList<>(winnerNetworkList);

            // Mutieren der Gewinner, um die nächste Generation aufzufüllen
            while (nextGenerationList.size() < inTrainResultList.size()) {
                final NeuralNetwork winnerNetwork = winnerNetworkList.get(random.nextInt(winnerNetworkList.size()));
                // Mutieren des Netzwerks
                final NeuralNetwork childNetwork = winnerNetwork.mutate(mutationRate, mutationStrengt);
                nextGenerationList.add(childNetwork);
            }

            // Alle Netzwerke für die nächste Epoche zurücksetzen
            trainResultList.clear();
            for (final NeuralNetwork network : nextGenerationList) {
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

    /**
     * Speichert das neuronale Netzwerk in eine Datei
     *
     * @param network Das zu speichernde neuronale Netzwerk
     * @param fileName Der Dateiname
     * @return true, wenn das Speichern erfolgreich war, sonst false
     */
    private static boolean saveNetwork(NeuralNetwork network, String fileName) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fileName))) {
            oos.writeObject(network);
            logger.info("Netzwerk erfolgreich in Datei {} gespeichert", fileName);
            return true;
        } catch (IOException e) {
            logger.error("Fehler beim Speichern des Netzwerks: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Lädt ein neuronales Netzwerk aus einer Datei
     *
     * @param fileName Der Dateiname
     * @return Das geladene neuronale Netzwerk oder null, wenn ein Fehler aufgetreten ist
     */
    private static NeuralNetwork loadNetwork(String fileName) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fileName))) {
            NeuralNetwork network = (NeuralNetwork) ois.readObject();
            logger.info("Netzwerk erfolgreich aus Datei {} geladen", fileName);
            return network;
        } catch (IOException | ClassNotFoundException e) {
            logger.info("Netzwerk konnte nicht geladen werden: {}", e.getMessage());
            return null;
        }
    }
}
