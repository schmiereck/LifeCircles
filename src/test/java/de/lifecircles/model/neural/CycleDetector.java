package de.lifecircles.model.neural;

import de.lifecircles.model.Cell;
import de.lifecircles.model.SensorActor;

import java.util.HashSet;
import java.util.Set;

public class CycleDetector {
    private final Set<Object> visited = new HashSet<>();

    public boolean hasCycle(Object obj) {
        if (obj == null) {
            return false; // Null-Objekte ignorieren
        }

        if (visited.contains(obj)) {
            return true; // Zyklus gefunden
        }
        visited.add(obj);
        final int outputTypePos = 0;
        boolean cycleFound = false;

        if (obj instanceof Neuron) {
            Neuron neuron = (Neuron) obj;

            // Überprüfe Output-Synapsen
            for (Synapse synapse : neuron.getOutputSynapseList(outputTypePos)) {
                if (hasCycle(synapse.getTargetNeuron())) {
                    cycleFound = true;
                    break;
                }
            }

            // Überprüfe Input-Synapsen
            if (!cycleFound) {
                for (int i = 0; i < neuron.getInputSynapseCount(outputTypePos); i++) {
                    Synapse synapse = neuron.getInputSynapseArr(outputTypePos)[i];
                    if (hasCycle(synapse.getSourceNeuron())) {
                        cycleFound = true;
                        break;
                    }
                }
            }
        } else if (obj instanceof Cell) {
            Cell cell = (Cell) obj;

            // Überprüfe SensorActors
            for (SensorActor sensorActor : cell.getSensorActors()) {
                if (hasCycle(sensorActor)) {
                    cycleFound = true;
                    break;
                }
            }

            // Überprüfe CellBrain (falls vorhanden)
            if (!cycleFound && cell.getBrain() != null) {
                cycleFound = hasCycle(cell.getBrain());
            }
        }

        visited.remove(obj); // Objekt aus der Besuchsmenge entfernen
        return cycleFound;
    }
}
