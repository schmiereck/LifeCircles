package de.lifecircles.model.neural;

public class ValuesNeuronValueFunctionFactory implements NeuronValueFunctionFactory {
    @Override
    public NeuronValueFunction create() {
        return new ValuesNeuronValueFunction();
    }
}
