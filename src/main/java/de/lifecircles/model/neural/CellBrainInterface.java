package de.lifecircles.model.neural;

public interface CellBrainInterface {
    int getSynapseCount();

    boolean[] determineActiveHiddenLayers(int cellState);

    void setInputs(double[] inputs);

    double[] process();

    int getInputCount();

    long getProccessedSynapses();
    /** Conservative work bound for reserving a full decision before executing it. Custom brains may override. */
    default long getMaximumProcessedSynapses() { return Math.max(getSynapseCount(),getProccessedSynapses()); }

    NeuralNetwork mutate(double mutationRate, double mutationStrength);

    double getOutputValue(int outputNeuronPos);

    double getInputValue(int inputNeuronPos);
}
