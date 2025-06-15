package de.lifecircles.model.neural;

import java.io.Serializable;

public interface NeuronValueFunctionFactory extends Serializable {
    NeuronValueFunction create();
}
