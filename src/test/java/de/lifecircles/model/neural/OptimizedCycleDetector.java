package de.lifecircles.model.neural;

import java.util.IdentityHashMap;
import java.util.Map;

public class OptimizedCycleDetector {
    private final Map<Object, Boolean> visited = new IdentityHashMap<>();

    public boolean hasCycle(Object obj) {
        if (obj == null) {
            return false;
        }

        if (visited.containsKey(obj)) {
            return true; // Zyklus gefunden
        }
        visited.put(obj, true);

        boolean cycleFound = false;

        if (obj instanceof Neuron) {
            Neuron neuron = (Neuron) obj;

            for (Synapse synapse : neuron.getOutputSynapses()) {
                if (hasCycle(synapse.getTargetNeuron())) {
                    cycleFound = true;
                    break;
                }
            }

            if (!cycleFound) {
                for (int i = 0; i < neuron.getInputSynapseCount(); i++) {
                    Synapse synapse = neuron.getInputSynapses()[i];
                    if (hasCycle(synapse.getSourceNeuron())) {
                        cycleFound = true;
                        break;
                    }
                }
            }
        }

        visited.remove(obj);
        return cycleFound;
    }
}