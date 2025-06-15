package de.lifecircles.model.neural;

public class DefaultNeuronValueFunctionFactory implements NeuronValueFunctionFactory {
    @Override
    public NeuronValueFunction create() {
        return new DefaultNeuronValueFunction();
    }
}
