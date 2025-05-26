package de.lifecircles.model.neural;

public interface CellBrainInterface {
    int getSynapseCount();

    boolean[] determineActiveHiddenLayers(int cellState);

    void setInputs(double[] inputs);

    double[] process();

    int getInputCount();

    long getProccessedSynapses();

    NeuralNetwork mutate(double mutationRate, double mutationStrength);

    double getOutputValue(int outputNeuronPos);
}
