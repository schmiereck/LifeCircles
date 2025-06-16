package de.lifecircles.model.neural;

import java.io.Serializable;

public interface NeuronInterface extends Serializable {
    double getBias(final int outputTypePos);
    void setBias(final int outputTypePos, final double bias);

    long activate(final NeuralNetwork neuralNetwork);
}
