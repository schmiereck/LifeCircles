package de.lifecircles.model.neural;

import de.lifecircles.model.Cell;
import de.lifecircles.model.CellFactory;
import de.lifecircles.model.SensorActor;
import de.lifecircles.model.Vector2D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.*;

public class SerialTest {

    @Test
    void testCyclicReferences() throws IOException, ClassNotFoundException {
        Cell cell = CellFactory.createCell(new Vector2D(10, 20), 5.0);

        GenerelCycleDetector generelCycleDetector = new GenerelCycleDetector();
        System.out.println("Teste direkte Cell-SensorActor Zyklen:");
        boolean hasCellCycle = generelCycleDetector.testCellSensorActorCycle(cell);
        System.out.println(generelCycleDetector.getCycleDescription());
        
        System.out.println("Teste allgemeine Zyklen:");
        boolean hasGeneralCycle = generelCycleDetector.hasCycle(cell);
        System.out.println(generelCycleDetector.getCycleDescription());
        
        // Hier prüfen wir, ob der gefundene Zyklus problematisch ist (nicht-transient)
        boolean hasProblematicCycle = generelCycleDetector.isProblematicCycle();
        
        if (hasProblematicCycle) {
            Assertions.fail("Problematischer (nicht-transienter) Zyklus gefunden: " + 
                    generelCycleDetector.getCycleDescription());
        }
        
        OptimizedCycleDetector optimizedCycleDetector = new OptimizedCycleDetector();
        boolean hasOptimizedCycle = optimizedCycleDetector.hasCycle(cell);
        
        // Serialisiere und deserialisiere das Objekt
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(byteOut)) {
            out.writeObject(cell);
        }

        ByteArrayInputStream byteIn = new ByteArrayInputStream(byteOut.toByteArray());
        Cell deserializedCell;
        try (ObjectInputStream in = new ObjectInputStream(byteIn)) {
            deserializedCell = (Cell) in.readObject();
        }

        // Überprüfe, ob die zyklischen Referenzen korrekt gesetzt sind
        for (SensorActor sensorActor : deserializedCell.getSensorActors()) {
            Assertions.assertEquals(deserializedCell, sensorActor.getParentCell());
        }
        
        System.out.println("Serialisierung und Deserialisierung erfolgreich abgeschlossen.");
    }

    @Test
    void testNeuronSynapseConnectionsAfterDeserialization() throws IOException, ClassNotFoundException {
        final NeuronValueFunctionFactory neuronValueFunctionFactory = new NeuronValueFunctionFactory() {
            @Override
            public NeuronValueFunction create() {
                return new DefaultNeuronValueFunction();
            }
        };
        NeuralNetwork network = new NeuralNetwork(neuronValueFunctionFactory,
                3, 2, 1);
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(byteOut)) {
            out.writeObject(network);
        }

        ByteArrayInputStream byteIn = new ByteArrayInputStream(byteOut.toByteArray());
        NeuralNetwork deserializedNetwork;
        try (ObjectInputStream in = new ObjectInputStream(byteIn)) {
            deserializedNetwork = (NeuralNetwork) in.readObject();
        }

        // Überprüfe, ob die Verbindungen korrekt wiederhergestellt wurden
        for (Synapse synapse : deserializedNetwork.getSynapseList()) {
            Assertions.assertNotNull(synapse.getSourceNeuron());
            Assertions.assertNotNull(synapse.getTargetNeuron());
            Assertions.assertTrue(synapse.getSourceNeuron().getOutputSynapses().contains(synapse));
            Assertions.assertTrue(synapse.getTargetNeuron().getInputSynapses()[0] == synapse);
        }
    }

    @Test
    void testNoProblematicCycles() throws IOException, ClassNotFoundException {
        Cell cell = CellFactory.createCell(new Vector2D(10, 20), 5.0);
        GenerelCycleDetector generelCycleDetector = new GenerelCycleDetector();

        // Teste allgemeine Zyklen
        boolean hasGeneralCycle = generelCycleDetector.hasCycle(cell);
        System.out.println(generelCycleDetector.getCycleDescription());

        // Sicherstellen, dass keine problematischen Zyklen vorhanden sind
        Assertions.assertFalse(generelCycleDetector.isProblematicCycle(), 
            "Problematischer (nicht-transienter) Zyklus gefunden: " + generelCycleDetector.getCycleDescription());
    }
}
