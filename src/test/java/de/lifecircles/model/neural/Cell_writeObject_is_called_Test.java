package de.lifecircles.model.neural;

import de.lifecircles.model.Cell;
import de.lifecircles.model.Vector2D;
import org.junit.jupiter.api.Test;

import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

public class Cell_writeObject_is_called_Test {
    @Test
    public void testCellSerializationAndDeserialization() throws Exception {
        // Cell mit einfachem NN erzeugen
        int inputCount = 3;
        int outputCount = 2;
        double hiddenCountFactor = 1.0;
        double stateHiddenLayerSynapseConnectivity = 1.0;
        double synapseConnectivity = 1.0;
        final NeuronValueFunctionFactory neuronValueFunctionFactory = new DefaultNeuronValueFunctionFactory();
        CellBrain brain = new CellBrain(neuronValueFunctionFactory,
                inputCount, outputCount, hiddenCountFactor, stateHiddenLayerSynapseConnectivity, synapseConnectivity);
        Cell cell = new Cell(new Vector2D(1.0, 2.0), 5.0, brain);
        cell.setEnergy(42.0);
        cell.setAge(7.0);
        cell.setCellState(3);
        cell.setMutationRateFactor(1.23);
        cell.setMutationStrengthFactor(0.77);
        // NN mit festen Werten initialisieren
        double[] testInput = {0.5, 0.2, 0.8};
        brain.setInputs(testInput);
        brain.getNeuralNetwork().setDisableLayerDeactivation(true); // Layer-Deaktivierung für Test abschalten
        double[] expectedOutput = brain.process();

        // Serialisierung in den Speicher
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(cell);
        oos.close();
        byte[] data = baos.toByteArray();

        // Deserialisierung aus dem Speicher
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        ObjectInputStream ois = new ObjectInputStream(bais);
        Cell deserializedCell = (Cell) ois.readObject();
        ois.close();

        // Vergleiche Felder
        assertEquals(cell.getPosition().getX(), deserializedCell.getPosition().getX(), 1e-9);
        assertEquals(cell.getPosition().getY(), deserializedCell.getPosition().getY(), 1e-9);
        assertEquals(cell.getRadiusSize(), deserializedCell.getRadiusSize(), 1e-9);
        assertEquals(cell.getEnergy(), deserializedCell.getEnergy(), 1e-9);
        assertEquals(cell.getAge(), deserializedCell.getAge(), 1e-9);
        assertEquals(cell.getCellState(), deserializedCell.getCellState());
        assertEquals(cell.getMutationRateFactor(), deserializedCell.getMutationRateFactor(), 1e-9);
        assertEquals(cell.getMutationStrengthFactor(), deserializedCell.getMutationStrengthFactor(), 1e-9);

        CellBrainInterface deserializedBrain = deserializedCell.getBrain();
        CellBrain deserializedCellBrain = (CellBrain) deserializedBrain;

        // Vergleiche Gewichte und Biases des NN
        NeuralNetwork origNN = brain.getNeuralNetwork();
        NeuralNetwork deserNN = deserializedCellBrain.getNeuralNetwork();
        // Vergleiche Layergrößen
        assertArrayEquals(origNN.getLayerSizes(), deserNN.getLayerSizes(), "Layergrößen unterschiedlich");
        // Vergleiche Biases der Neuronen
        for (int i = 0; i < origNN.getInputNeuronArr().length; i++) {
            final Neuron origInputNeuron = origNN.getInputNeuronArr()[i];
            for (int outputTypePos = 0; outputTypePos < origInputNeuron.getNeuronTypeInfoData().getOutputCount(); outputTypePos++) {
                assertEquals(origInputNeuron.getBias(outputTypePos), deserNN.getInputNeuronArr()[i].getBias(outputTypePos), 1e-9, "Input-Bias unterschiedlich");
            }
        }
        for (int l = 0; l < origNN.getHiddenLayerArr().length; l++) {
            NeuronInterface[] origLayer = origNN.getHiddenLayerArr()[l].getNeuronsArr();
            NeuronInterface[] deserLayer = deserNN.getHiddenLayerArr()[l].getNeuronsArr();
            for (int n = 0; n < origLayer.length; n++) {
                final NeuronInterface origNeuron = origLayer[n];
                for (int outputTypePos = 0; outputTypePos < origNeuron.getNeuronTypeInfoData().getOutputCount(); outputTypePos++) {
                    assertEquals(origNeuron.getBias(outputTypePos), deserLayer[n].getBias(outputTypePos), 1e-9, "Hidden-Bias unterschiedlich");
                }
            }
        }
        for (int i = 0; i < origNN.getOutputNeuronArr().length; i++) {
            final NeuronInterface origOutputNeuron = origNN.getOutputNeuronArr()[i];
            for (int outputTypePos = 0; outputTypePos < origOutputNeuron.getNeuronTypeInfoData().getOutputCount(); outputTypePos++) {
                assertEquals(origOutputNeuron.getBias(outputTypePos), deserNN.getOutputNeuronArr()[i].getBias(outputTypePos), 1e-9, "Output-Bias unterschiedlich");
            }
        }
        // Vergleiche Synapsen-Gewichte
        assertEquals(origNN.getSynapseCount(), deserNN.getSynapseCount(), "Synapsenanzahl unterschiedlich");
        for (int i = 0; i < origNN.getSynapseCount(); i++) {
            assertEquals(origNN.getSynapseList().get(i).getWeight(), deserNN.getSynapseList().get(i).getWeight(), 1e-9, "Synapsen-Gewicht unterschiedlich an Index " + i);
        }

        // Vergleiche NN-Ausgabe mit Toleranz
        deserializedCellBrain.getNeuralNetwork().setDisableLayerDeactivation(true); // Layer-Deaktivierung für Test abschalten
        deserializedCellBrain.setInputs(testInput);
        double[] deserializedOutput = deserializedCellBrain.process();
        // Erlaube kleine Abweichung wegen Floating-Point und möglicher Initialisierungsunterschiede
        for (int i = 0; i < expectedOutput.length; i++) {
            assertEquals(expectedOutput[i], deserializedOutput[i], 1e-6, "NN-Ausgabe unterscheidet sich an Index " + i);
        }
        final NeuronValueFunctionFactory origNeuronValueFunctionFactory = origNN.getNeuronValueFunctionFactory();
        final NeuronValueFunctionFactory deserNeuronValueFunctionFactory = deserNN.getNeuronValueFunctionFactory();
        assertEquals(origNeuronValueFunctionFactory.getClass(), deserNeuronValueFunctionFactory.getClass());
    }
}
