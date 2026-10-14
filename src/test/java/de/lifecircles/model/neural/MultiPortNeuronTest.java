package de.lifecircles.model.neural;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Small fixed networks; numerical loss gradients are independent of backprop implementation. */
class MultiPortNeuronTest {
    private final NeuronValueFunctionFactory factory = new ValuesNeuronValueFunctionFactory();
    private Random previousRandom;

    @BeforeEach void setup() {
        previousRandom = NeuralNetwork.getRandom();
        NeuralNetwork.setRandom(new Random(731));
    }
    @AfterEach void restore() { NeuralNetwork.setRandom(previousRandom); }

    private NeuralNetwork network(boolean upstreamLayer) {
        return network(upstreamLayer, 2, 2, false);
    }

    private NeuralNetwork network(boolean upstreamLayer, int inputs, int outputs, boolean nested) {
        NeuralNetwork outer = new NeuralNetwork(factory, inputs, upstreamLayer ? new int[]{inputs, 1} : new int[]{1}, outputs, 1, 0);
        NeuralNetwork inner = nested ? network(false, inputs, outputs, false)
                : new NeuralNetwork(factory, inputs, new int[]{2}, outputs, 1, 0);
        outer.setDisableLayerDeactivation(true);
        inner.setDisableLayerDeactivation(true);
        Layer moduleLayer = outer.getHiddenLayerArr()[outer.getHiddenLayerArr().length - 1];
        NeuronInterface old = moduleLayer.getNeuron(0);
        NeuronNetwork module = new NeuronNetwork(old.getId(), new NeuronTypeInfoData(inputs, outputs, inner.getNeuralNet()), factory);
        moduleLayer.getNeuronsArr()[0] = module;
        Synapse[] edges = outer.getNeuralNet().getSynapseArr();
        NeuronInterface[] upstream = upstreamLayer ? outer.getHiddenLayerArr()[0].getNeuronsArr() : outer.getInputNeuronArr();
        for (int i = 0; i < edges.length; i++) {
            Synapse edge = edges[i];
            if (edge.getSourceNeuron() != old && edge.getTargetNeuron() != old) continue;
            edge.getSourceNeuron().removeOutputSynapse(edge.getSourceOutputTypePos(), edge);
            edge.getTargetNeuron().removeInputSynapse(edge.getTargetInputTypePos(), edge);
            if (edge.getTargetNeuron() == old) {
                int port = Arrays.asList(upstream).indexOf(edge.getSourceNeuron());
                edges[i] = new Synapse(edge.getSourceNeuron(), 0, module, port, edge.getWeight());
            } else {
                int port = Arrays.asList(outer.getOutputNeuronArr()).indexOf(edge.getTargetNeuron());
                edges[i] = new Synapse(module, port, edge.getTargetNeuron(), 0, edge.getWeight());
            }
        }
        return outer;
    }

    private record Parameter(java.util.function.DoubleSupplier read, java.util.function.DoubleConsumer write) {}

    private List<Parameter> parameters(NeuralNet net) {
        List<Parameter> parameters = new ArrayList<>();
        for (Synapse edge : net.getSynapseList()) parameters.add(new Parameter(edge::getWeight, edge::setWeight));
        List<NeuronInterface> neurons = new ArrayList<>();
        for (Layer layer : net.getHiddenLayerArr()) neurons.addAll(layer.getNeuronList());
        neurons.addAll(Arrays.asList(net.getOutputNeuronArr()));
        for (NeuronInterface neuron : neurons) {
            for (int port = 0; port < neuron.getNeuronTypeInfoData().getInputCount(); port++) {
                final int input = port;
                parameters.add(new Parameter(() -> neuron.getBias(input), value -> neuron.setBias(input, value)));
            }
            if (neuron instanceof NeuronNetwork) parameters.addAll(parameters(neuron.getNeuronTypeInfoData().getNeuralNet()));
        }
        return parameters;
    }

    private double loss(NeuralNetwork net, double[] inputs, double[] target) {
        double[] output = net.calcProcess(inputs);
        double loss = 0;
        for (int i = 0; i < output.length; i++) loss += 0.5 * Math.pow(output[i] - target[i], 2);
        return loss;
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void everyPortGradientMatchesNumericalLossDerivative(boolean upstreamLayer) {
        NeuralNetwork net = network(upstreamLayer);
        double[] inputs = {0.2, -0.7}, targets = {0.3, 0.8};
        assertNumericalGradients(net, inputs, targets);
    }

    @ParameterizedTest @CsvSource({"1,3", "3,1", "2,3"})
    void differentInputAndOutputCountsHaveCorrectGradients(int inputCount, int outputCount) {
        NeuralNetwork net = network(true, inputCount, outputCount, false);
        double[] inputs = new double[inputCount], targets = new double[outputCount];
        for (int i = 0; i < inputs.length; i++) inputs[i] = 0.2 - i * 0.3;
        for (int i = 0; i < targets.length; i++) targets[i] = 0.2 + i * 0.15;
        assertNumericalGradients(net, inputs, targets);
    }

    @Test void nestedModulePortsHaveCorrectGradients() {
        assertNumericalGradients(network(true, 2, 2, true), new double[]{0.4, -0.1}, new double[]{0.2, 0.8});
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void crossedPortConnectionsHaveCorrectGradients(boolean crossInputs) {
        NeuralNetwork net = network(true);
        NeuronInterface module = net.getHiddenLayerArr()[1].getNeuron(0);
        Synapse[] edges = net.getNeuralNet().getSynapseArr();
        for (int i = 0; i < edges.length; i++) {
            Synapse edge = edges[i];
            boolean crossesOutput = edge.getSourceNeuron() == module;
            boolean crossesInput = crossInputs && edge.getTargetNeuron() == module;
            if (!crossesOutput && !crossesInput) continue;
            edge.getSourceNeuron().removeOutputSynapse(edge.getSourceOutputTypePos(), edge);
            edge.getTargetNeuron().removeInputSynapse(edge.getTargetInputTypePos(), edge);
            edges[i] = new Synapse(edge.getSourceNeuron(), crossesOutput ? 1 - edge.getSourceOutputTypePos() : edge.getSourceOutputTypePos(),
                    edge.getTargetNeuron(), crossesInput ? 1 - edge.getTargetInputTypePos() : edge.getTargetInputTypePos(), edge.getWeight());
        }
        assertNumericalGradients(net, new double[]{0.3, -0.5}, new double[]{0.2, 0.8});
    }

    private void assertNumericalGradients(NeuralNetwork net, double[] inputs, double[] targets) {
        List<Parameter> parameters = parameters(net.getNeuralNet());
        double[] old = new double[parameters.size()], numerical = new double[parameters.size()];
        final double epsilon = 1e-6, rate = 0.001;
        for (int i = 0; i < parameters.size(); i++) {
            Parameter parameter = parameters.get(i);
            old[i] = parameter.read.getAsDouble();
            parameter.write.accept(old[i] + epsilon); double plus = loss(net, inputs, targets);
            parameter.write.accept(old[i] - epsilon); double minus = loss(net, inputs, targets);
            parameter.write.accept(old[i]);
            numerical[i] = (plus - minus) / (2 * epsilon);
        }
        net.setLearningRate(rate);
        net.calcProcess(inputs); net.backpropagate(targets);
        for (int i = 0; i < parameters.size(); i++) {
            double actual = (old[i] - parameters.get(i).read.getAsDouble()) / rate;
            assertEquals(numerical[i], actual, 1e-7, "Wrong gradient for parameter " + i);
        }
    }

    @Test void trainingOneOutputDoesNotUpdateUnconnectedModuleOutput() {
        NeuralNetwork net = network(false);
        NeuronInterface module = net.getHiddenLayerArr()[0].getNeuron(0);
        NeuralNet inner = module.getNeuronTypeInfoData().getNeuralNet();
        Neuron untouched = inner.getOutputNeuronArr()[1];
        double bias = untouched.getBias(0);
        double[] weights = Arrays.stream(untouched.getInputSynapseArr(0)).mapToDouble(Synapse::getWeight).toArray();
        double[] output = net.calcProcess(new double[]{0.4, -0.2});
        net.backpropagate(new double[]{output[0] + 0.1, output[1]});
        assertEquals(0, net.getNeuronValueFunction().readDelta(net.getNeuralNet(), module, 1), 0);
        assertNotEquals(0, net.getNeuronValueFunction().readDelta(net.getNeuralNet(), module, 0));
        assertEquals(bias, untouched.getBias(0), 0);
        assertArrayEquals(weights, Arrays.stream(untouched.getInputSynapseArr(0)).mapToDouble(Synapse::getWeight).toArray(), 0);
    }

    @Test void sevenOutputsRemainIndependentWhenStorageGrows() {
        NeuronValueFunction values = factory.create();
        NeuronInterface first = new NeuronNetwork(0, new NeuronTypeInfoData(1, 7,
                new NeuralNetwork(factory, 1, new int[]{}, 7, 0, 0).getNeuralNet()), factory);
        NeuronInterface largeId = new NeuronNetwork(1024, first.getNeuronTypeInfoData(), factory);
        for (int port = 0; port < 7; port++) {
            values.writeValue(null, first, port, port + 0.1);
            values.writeDelta(null, first, port, port + 0.2);
            values.writeInputSum(null, first, port, port + 0.3);
            values.writeValue(null, largeId, port, -port - 0.4);
        }
        for (int port = 0; port < 7; port++) {
            assertEquals(port + 0.1, values.readValue(null, first, port), 0);
            assertEquals(port + 0.2, values.readDelta(null, first, port), 0);
            assertEquals(port + 0.3, values.readInputSum(null, first, port), 0);
            assertEquals(-port - 0.4, values.readValue(null, largeId, port), 0);
        }
    }
}
