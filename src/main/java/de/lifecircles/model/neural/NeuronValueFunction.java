package de.lifecircles.model.neural;

import java.io.Serializable;

public interface NeuronValueFunction extends Serializable {
    double readValue(final NeuralNet neuralNet, final NeuronInterface neuron, final int outputTypePos);
    void writeValue(final NeuralNet neuralNet, final NeuronInterface neuron, int outputTypePos, final double value);

    double readDelta(final NeuralNet neuralNet, final NeuronInterface neuron, final int outputTypePos);
    void writeDelta(final NeuralNet neuralNet, final NeuronInterface neuron, final int outputTypePos, final double delta);

   double readInputSum(final NeuralNet neuralNet, final NeuronInterface neuron, final int outputTypePos);
    void writeInputSum(final NeuralNet neuralNet, final NeuronInterface neuron, final int outputTypePos, final double inputSum);

    int fetchNextFreeId(final NeuralNet neuralNet);

    void releaseNeuron(final NeuralNet neuralNet, final NeuronInterface neuron);

    default ModuleExecutionState getModuleState(final NeuronNetwork module) {
        return getModuleState(module, false);
    }

    ModuleExecutionState getModuleState(final NeuronNetwork module, final boolean checkTopology);

    void clearModuleStates();

    /** Initialize an execution store attached to a graph with pre-existing IDs. */
    void reserveExistingIds(final NeuralNet neuralNet);

}
