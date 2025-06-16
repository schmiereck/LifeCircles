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

        final int outputTypePos = 0;
        boolean cycleFound = false;

        if (obj instanceof Neuron) {
            Neuron neuron = (Neuron) obj;

            for (Synapse synapse : neuron.getOutputSynapseList(outputTypePos)) {
                if (hasCycle(synapse.getTargetNeuron())) {
                    cycleFound = true;
                    break;
                }
            }

            if (!cycleFound) {
                for (int i = 0; i < neuron.getInputSynapseCount(outputTypePos); i++) {
                    Synapse synapse = neuron.getInputSynapseArr(outputTypePos)[i];
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