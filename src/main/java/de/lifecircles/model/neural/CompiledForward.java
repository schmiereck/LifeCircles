package de.lifecircles.model.neural;

import java.util.Arrays;

/**
 * Flat copy of the scalar neurons of one network for the forward pass: source ids, weights, bias and activation in
 * plain arrays instead of neuron and synapse objects scattered over the heap. Arithmetic order is the one of
 * {@link Neuron#activate}, so values, input sums and work bills are bit-identical.
 * <p>
 * Valid while nothing changed. Changes through methods (weights, biases, activation, synapse arrays) raise the
 * neuron's gene stamp; replaced arrays are found by reference. Rule (NETZKOMPILIERUNG.md): arrays returned by getters
 * of networks, layers and neurons (neurons, layers, synapses) must not be written in place; use the methods, which
 * replace the arrays. With {@code -Dlife.verifyCompiled=true} (or {@link #setVerifyContents}) the contents are compared
 * as well, so tests still catch a violation.
 * Networks with module neurons, multi-port or foreign wiring are not compiled ({@link #eligible} false) and keep
 * the object path.
 */
final class CompiledForward {
    /** Also compare wiring contents against copies (detects forbidden in-place writes); costs most of the pass. */
    private static volatile boolean verifyContents = Boolean.getBoolean("life.verifyCompiled");
    static void setVerifyContents(boolean verify) { verifyContents = verify; }
    static boolean isVerifyContents() { return verifyContents; }

    private final Neuron[] inputs, outputs;
    private final Layer[] layers;
    private final NeuronInterface[][] layerNeurons;
    /** Scalar hidden and output neurons in execution order (layers, then outputs), with their stamps and wiring. */
    private final Neuron[] neurons;
    private final int[] stamps;
    private final Synapse[][] wiring, wiringArrays;
    /** Position of a layer neuron, or of output k at layers.length, in {@link #neurons}. */
    private final int[][] index;
    final boolean eligible;
    // Compiled form, only when eligible.
    final int[] inputIds;
    private final int[] ids, start, sources;
    private final double[] bias, weights;
    private final ActivationFunction[] functions;
    final int maxId;

    /** The live arrays at compile time, compared by reference. */
    private final Neuron[] inputArray, outputArray;
    private final Layer[] layerArray;
    private final NeuronInterface[][] layerNeuronArrays;

    CompiledForward(NeuralNet net) {
        inputArray = net.getInputNeuronArr();
        outputArray = net.getOutputNeuronArr();
        layerArray = net.getHiddenLayerArr();
        inputs = net.getInputNeuronArr().clone();
        outputs = net.getOutputNeuronArr().clone();
        layers = net.getHiddenLayerArr().clone();
        layerNeurons = new NeuronInterface[layers.length][];
        layerNeuronArrays = new NeuronInterface[layers.length][];
        int count = outputs.length;
        for (int l = 0; l < layers.length; l++) {
            layerNeuronArrays[l] = layers[l].getNeuronsArr();
            layerNeurons[l] = layerNeuronArrays[l].clone();
            count += layerNeurons[l].length;
        }
        neurons = new Neuron[count];
        stamps = new int[count];
        wiring = new Synapse[count][];
        wiringArrays = new Synapse[count][];
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
                    wiringArrays[n] = neuron.getInputSynapseArr(0);
                    wiring[n] = wiringArrays[n].clone();
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
    boolean inputsMatch(Neuron[] current) { return current == inputArray && (!verifyContents || Arrays.equals(inputs, current)); }

    /** True while the network still has exactly the compiled neurons, wiring and genes. No allocation. */
    boolean matches(NeuralNet net) {
        final boolean verify = verifyContents;
        if (net.getInputNeuronArr() != inputArray || net.getOutputNeuronArr() != outputArray || net.getHiddenLayerArr() != layerArray) return false;
        for (int l = 0; l < layers.length; l++) if (layers[l].getNeuronsArr() != layerNeuronArrays[l]) return false;
        if (verify && (!Arrays.equals(inputs, inputArray) || !Arrays.equals(outputs, outputArray) || !Arrays.equals(layers, layerArray))) return false;
        if (verify) for (int l = 0; l < layers.length; l++) if (!Arrays.equals(layerNeurons[l], layerNeuronArrays[l])) return false;
        for (int i = 0; i < neurons.length; i++) {
            Neuron neuron = neurons[i];
            if (neuron == null) continue;
            Synapse[] current = neuron.getInputSynapseArr(0);
            if (neuron.getGeneStamp() != stamps[i] || current != wiringArrays[i] || verify && !Arrays.equals(wiring[i], current)) return false;
        }
        return true;
    }

    /** Value of output k, read by id like the value function does. */
    double output(int k, double[] values) { return values[ids[index[layers.length][k]]]; }

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
