package de.lifecircles.model.neural;

import java.util.Arrays;

/**
 * Flat copy of the scalar neurons of one network for the forward pass: source ids, weights, bias and activation in
 * plain arrays instead of neuron and synapse objects scattered over the heap. Arithmetic order is the one of
 * {@link Neuron#activate}, so values, input sums and work bills are bit-identical.
 * <p>
 * Valid while nothing changed. Changes through methods (weights, biases, activation, synapse arrays) raise the
 * neuron's gene stamp; arrays written in place (public getters return the live arrays) are found by comparing their
 * contents with copies. Networks with module neurons, multi-port or foreign wiring are not compiled
 * ({@link #eligible} false) and keep the object path.
 */
final class CompiledForward {
    private final Neuron[] inputs, outputs;
    private final Layer[] layers;
    private final NeuronInterface[][] layerNeurons;
    /** Scalar hidden and output neurons in execution order (layers, then outputs), with their stamps and wiring. */
    private final Neuron[] neurons;
    private final int[] stamps;
    private final Synapse[][] wiring;
    /** Position of a layer neuron, or of output k at layers.length, in {@link #neurons}. */
    private final int[][] index;
    final boolean eligible;
    // Compiled form, only when eligible.
    final int[] inputIds;
    private final int[] ids, start, sources;
    private final double[] bias, weights;
    private final ActivationFunction[] functions;
    final int maxId;

    CompiledForward(NeuralNet net) {
        inputs = net.getInputNeuronArr().clone();
        outputs = net.getOutputNeuronArr().clone();
        layers = net.getHiddenLayerArr().clone();
        layerNeurons = new NeuronInterface[layers.length][];
        int count = outputs.length;
        for (int l = 0; l < layers.length; l++) {
            layerNeurons[l] = layers[l].getNeuronsArr().clone();
            count += layerNeurons[l].length;
        }
        neurons = new Neuron[count];
        stamps = new int[count];
        wiring = new Synapse[count][];
        index = new int[layers.length + 1][];
        boolean ok = true;
        int n = 0;
        for (int l = 0; l <= layers.length; l++) {
            NeuronInterface[] row = l < layers.length ? layerNeurons[l] : outputs;
            index[l] = new int[row.length];
            for (int j = 0; j < row.length; j++) {
                index[l][j] = n;
                if (row[j] instanceof Neuron neuron) {
                    neurons[n] = neuron;
                    stamps[n] = neuron.getGeneStamp();
                    wiring[n] = neuron.getInputSynapseArr(0).clone();
                    ok &= compilable(neuron, wiring[n]);
                } else ok = false;
                n++;
            }
        }
        eligible = ok;
        if (!ok) {
            inputIds = ids = start = sources = null; bias = weights = null; functions = null; maxId = -1;
            return;
        }
        int max = -1;
        inputIds = new int[inputs.length];
        for (int i = 0; i < inputs.length; i++) max = Math.max(max, inputIds[i] = inputs[i].getId());
        ids = new int[count]; start = new int[count + 1]; bias = new double[count]; functions = new ActivationFunction[count];
        int edges = 0;
        for (int i = 0; i < count; i++) { start[i] = edges; edges += wiring[i].length; }
        start[count] = edges;
        sources = new int[edges]; weights = new double[edges];
        for (int i = 0; i < count; i++) {
            Neuron neuron = neurons[i];
            max = Math.max(max, ids[i] = neuron.getId());
            bias[i] = neuron.getBias(0);
            functions[i] = neuron.getActivationFunction();
            for (int k = 0, e = start[i]; k < wiring[i].length; k++, e++) {
                Synapse synapse = wiring[i][k];
                max = Math.max(max, sources[e] = synapse.getSourceNeuron().getId());
                weights[e] = synapse.getWeight();
            }
        }
        maxId = max;
    }

    /** One scalar port, own edges only (setWeight stamps the target), all sources read on port 0. */
    private static boolean compilable(Neuron neuron, Synapse[] edges) {
        if (neuron.getNeuronTypeInfoData().getInputCount() != 1 || neuron.getNeuronTypeInfoData().getOutputCount() != 1) return false;
        for (Synapse edge : edges)
            if (edge.getTargetNeuron() != neuron || edge.getSourceOutputTypePos() != 0) return false;
        return true;
    }

    /** Input neurons unchanged (their ids are final). */
    boolean inputsMatch(Neuron[] current) { return Arrays.equals(inputs, current); }

    /** True while the network still has exactly the compiled neurons, wiring and genes. No allocation. */
    boolean matches(NeuralNet net) {
        if (!Arrays.equals(inputs, net.getInputNeuronArr()) || !Arrays.equals(outputs, net.getOutputNeuronArr())
                || !Arrays.equals(layers, net.getHiddenLayerArr())) return false;
        for (int l = 0; l < layers.length; l++) if (!Arrays.equals(layerNeurons[l], layers[l].getNeuronsArr())) return false;
        for (int i = 0; i < neurons.length; i++) {
            Neuron neuron = neurons[i];
            if (neuron != null && (neuron.getGeneStamp() != stamps[i] || !Arrays.equals(wiring[i], neuron.getInputSynapseArr(0)))) return false;
        }
        return true;
    }

    /** Neuron j of hidden layer l (l = layer count: output j), exactly as {@link Neuron#activate}. Returns its work. */
    long activate(int l, int j, double[] values, double[] inputSums) {
        final int i = index[l][j];
        double sum = bias[i];
        final int end = start[i + 1];
        for (int e = start[i]; e < end; e++) sum += values[sources[e]] * weights[e];
        inputSums[ids[i]] = sum;
        values[ids[i]] = functions[i].apply(sum);
        return end - start[i];
    }
}
