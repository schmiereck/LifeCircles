package de.lifecircles.model.neural;

import java.io.Serializable;

/** Per-instance state. Shared module definitions contain genes, not activations. */
public final class ModuleExecutionState implements Serializable {
    private static final long serialVersionUID = 1L;
    final NeuralNetwork network;
    final double[] inputDeltas;

    ModuleExecutionState(NeuralNet genotype, NeuronValueFunctionFactory factory) {
        network = new NeuralNetwork(genotype, factory);
        inputDeltas = new double[genotype.getInputCount()];
    }
}
