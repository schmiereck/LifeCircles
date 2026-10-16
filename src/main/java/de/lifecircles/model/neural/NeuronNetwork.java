package de.lifecircles.model.neural;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class NeuronNetwork implements NeuronInterface {
    private static final long serialVersionUID = 1L;

    private final int id;

    /**
     * Input-Synapes for every Input of the Network.
     */
    private List<Synapse[]> inputSynapsesList;
    /**
     * Output-Synapes for every Input of the Network.
     */
    private List<Synapse[]> outputSynapsesList;
    private boolean isOutputNeuron; // Flag für Output-Neuronen

    private final NeuronTypeInfoData neuronTypeInfoData;

    private final NeuralNetwork network;

    //private double[] deltaArr;
    //private double[] inputSumArr;
    private double[] biasArr;

    public NeuronNetwork(final int id, final NeuronTypeInfoData neuronTypeInfoData,
                         final NeuronValueFunctionFactory neuronValueFunctionFactory) {
        this.id = id;
        this.neuronTypeInfoData = neuronTypeInfoData;
        this.inputSynapsesList = new ArrayList<>();
        this.outputSynapsesList = new ArrayList<>();
        this.isOutputNeuron = false; // Standardmäßig kein Output-Neuron

        // Keep the definition/factory here. Actual execution state belongs to
        // the enclosing value store, separately for every instance and level.
        this.network = new NeuralNetwork(neuronTypeInfoData.getNeuralNet(), neuronValueFunctionFactory);

        for (int inputTypePos = 0; inputTypePos < this.neuronTypeInfoData.getInputCount(); inputTypePos++) {
            this.inputSynapsesList.add(new Synapse[0]);
        }
        for (int outputTypePos = 0; outputTypePos < this.neuronTypeInfoData.getOutputCount(); outputTypePos++) {
            this.outputSynapsesList.add(new Synapse[0]);
        }
        //this.deltaArr = new double[this.neuronTypeInfoData.getOutputCount()];
        //this.inputSumArr = new double[this.neuronTypeInfoData.getOutputCount()];
        this.biasArr = new double[this.neuronTypeInfoData.getInputCount()];
    }

    ModuleExecutionState createExecutionState() {
        return new ModuleExecutionState(this.neuronTypeInfoData.getNeuralNet(), this.network.getNeuronValueFunctionFactory());
    }

    @Override
    public int getId() {
        return this.id;
    }

    @Override
    public NeuronTypeInfoData getNeuronTypeInfoData() {
        return this.neuronTypeInfoData;
    }

    @Override
    public NeuronInterface cloneNeuron(final NeuralNet neuralNet, final NeuronValueFunction neuronValueFunction, final boolean isActiveLayer) {
        final int newId = neuronValueFunction.fetchNextFreeId(neuralNet);
        final NeuronNetwork newNeuron = new NeuronNetwork(newId, neuralNet.copyNeuronType(this.neuronTypeInfoData, this.network.getNeuronValueFunctionFactory()), this.network.getNeuronValueFunctionFactory());

        for (int inputNeuronPos = 0; inputNeuronPos < newNeuron.getNeuronTypeInfoData().getInputCount(); inputNeuronPos++) {
            newNeuron.setBias(inputNeuronPos, this.getBias(inputNeuronPos));
        }

        return newNeuron;
    }

    @Override
    public void addOutputSynapse(final int outputTypePos, final Synapse synapse) {
        final Synapse[] outputSynapseArr = this.outputSynapsesList.get(outputTypePos);

        Synapse[] newOutputSynapses = new Synapse[outputSynapseArr.length + 1];
        System.arraycopy(outputSynapseArr, 0, newOutputSynapses, 0, outputSynapseArr.length);
        newOutputSynapses[outputSynapseArr.length] = synapse;

        this.outputSynapsesList.set(outputTypePos, newOutputSynapses);
    }

    @Override
    public void addInputSynapse(final int inputTypePos, final Synapse synapse) {
        final Synapse[] inputSynapseArr = this.inputSynapsesList.get(inputTypePos);

        Synapse[] newInputSynapses = new Synapse[inputSynapseArr.length + 1];
        System.arraycopy(inputSynapseArr, 0, newInputSynapses, 0, inputSynapseArr.length);
        newInputSynapses[inputSynapseArr.length] = synapse;

        this.inputSynapsesList.set(inputTypePos, newInputSynapses);
    }

    @Override
    public void removeInputSynapse(final int inputTypePos, final Synapse synapse) {
        final Synapse[] inputSynapseArr = this.inputSynapsesList.get(inputTypePos);

        int foundInputSynapsePos = -1;
        for (int inputSynapsePos = 0; inputSynapsePos < inputSynapseArr.length; inputSynapsePos++) {
            if (inputSynapseArr[inputSynapsePos] == synapse) {
                foundInputSynapsePos = inputSynapsePos;
                break;
            }
        }
        if (foundInputSynapsePos != -1) {
            Synapse[] newInputSynapses = new Synapse[inputSynapseArr.length - 1];
            System.arraycopy(inputSynapseArr, 0, newInputSynapses, 0, foundInputSynapsePos);
            System.arraycopy(inputSynapseArr, foundInputSynapsePos + 1, newInputSynapses, foundInputSynapsePos, inputSynapseArr.length - foundInputSynapsePos - 1);

            this.inputSynapsesList.set(inputTypePos, newInputSynapses);
        }
    }

    @Override
    public void removeOutputSynapse(final int outputTypePos, final Synapse synapse) {
        final Synapse[] outputSynapseArr = this.outputSynapsesList.get(outputTypePos);

        int foundOutputSynapsePos = -1;
        for (int outputSynapsePos = 0; outputSynapsePos < outputSynapseArr.length; outputSynapsePos++) {
            if (outputSynapseArr[outputSynapsePos] == synapse) {
                foundOutputSynapsePos = outputSynapsePos;
                break;
            }
        }
        if (foundOutputSynapsePos != -1) {
            Synapse[] newOutputSynapses = new Synapse[outputSynapseArr.length - 1];
            System.arraycopy(outputSynapseArr, 0, newOutputSynapses, 0, foundOutputSynapsePos);
            System.arraycopy(outputSynapseArr, foundOutputSynapsePos + 1, newOutputSynapses, foundOutputSynapsePos, outputSynapseArr.length - foundOutputSynapsePos - 1);

            this.outputSynapsesList.set(outputTypePos, newOutputSynapses);
        }
    }

    @Override
    public long activate(final NeuronValueFunction neuronValueFunction, final NeuralNet neuralNet) {
        this.checkInOutSize();

        long incomingCount = 0;
        final ModuleExecutionState state = neuronValueFunction.getModuleState(this, true);
        final double[] inputArr = state.inputValues;
        for (int inputTypePos = 0; inputTypePos < this.neuronTypeInfoData.getInputCount(); inputTypePos++) {
            double sum = this.getBias(inputTypePos);
            final Synapse[] inputSynapseArr = this.inputSynapsesList.get(inputTypePos);
            incomingCount += inputSynapseArr.length;
            for (int inputSynapsePos = 0; inputSynapsePos < inputSynapseArr.length; inputSynapsePos++) {
                final Synapse synapse = inputSynapseArr[inputSynapsePos];
                final int sourceOutputTypePos = synapse.getSourceOutputTypePos();
                final double sourceOutputValue = neuronValueFunction.readValue(neuralNet, synapse.getSourceNeuron(), sourceOutputTypePos);
                sum += sourceOutputValue * synapse.getWeight();
            }
            inputArr[inputTypePos] = sum;
        }

        final NeuralNetwork execution = state.network;
        final NeuralNet calcNeuralNet = execution.getNeuralNet();
        final NeuronValueFunction calcNeuronValueFunction = execution.getNeuronValueFunction();

        calcNeuralNet.setInputs(calcNeuronValueFunction, inputArr);
        calcNeuralNet.process(calcNeuronValueFunction, neuralNet.isRecordingForward(), false,
                neuralNet.isUsingExecutionPlan());

        for (int outputTypePos = 0; outputTypePos < this.neuronTypeInfoData.getOutputCount(); outputTypePos++) {
            Neuron calcOutputNeuron = calcNeuralNet.getOutputNeuronArr()[outputTypePos];
            // Kopieren der Werte des internen NeuralNet in das NeuronNetwork:
            final double calcOutputNeuronInputSum = calcNeuronValueFunction.readInputSum(calcNeuralNet, calcOutputNeuron, 0);
            neuronValueFunction.writeInputSum(neuralNet, this, outputTypePos, calcOutputNeuronInputSum);

            final double calcOutputNeuronValue = calcNeuronValueFunction.readValue(calcNeuralNet, calcOutputNeuron, 0);
            neuronValueFunction.writeValue(neuralNet, this, outputTypePos, calcOutputNeuronValue);
        }

        return incomingCount + calcNeuralNet.getProccessedSynapses();
    }

    private void checkInOutSize() {
        if (this.neuronTypeInfoData.getInputCount() != this.inputSynapsesList.size()) {
            // TODO this.inputSynapsesList
            //this.inputSynapsesList

            //private double[] inputSumArr;

            //private double[] deltaArr;
            //private double[] biasArr;
        }
    }

    @Override
    public double getBias(final int inputTypePos) {
        return this.biasArr[inputTypePos];
        //return this.network.getNeuralNet().getOutputNeuronArr()[inputTypePos].getBias(inputTypePos);
    }

    @Override
    public void setBias(final int inputTypePos, final double bias) {
        this.biasArr[inputTypePos] = bias;
        //this.network.getNeuralNet().getOutputNeuronArr()[inputTypePos].setBias(inputTypePos, bias);
    }

    @Override
    public void mutateNeuron(final Random random, double mutationRate, double mutationStrength) {
        for (int port=0; port<neuronTypeInfoData.getInputCount(); port++)
            if (random.nextDouble() < mutationRate)
                setBias(port,getBias(port) + (random.nextDouble()*2-1)*mutationStrength);
        // Shared inner definitions are mutated once by the owning genome traversal.
    }

    @Override
    public void backpropagateDelta(final NeuralNet neuralNet, final NeuronValueFunction values) {
        final ModuleExecutionState state = values.getModuleState(this);
        final NeuralNet inner = state.network.getNeuralNet();
        final NeuronValueFunction innerValues = state.network.getNeuronValueFunction();
        for (int port = 0; port < this.neuronTypeInfoData.getOutputCount(); port++) {
            double outputGradient = 0;
            for (Synapse edge : this.getOutputSynapseList(port)) {
                if (!neuralNet.propagatesCurrentStep(edge)) continue;
                outputGradient += edge.getWeight() * edge.getTargetNeuron().getInputDelta(
                        neuralNet, values, edge.getTargetInputTypePos());
            }
            // The module exposes output values. Its nonlinear derivatives are
            // applied inside the module, once, at each scalar output neuron.
            values.writeDelta(neuralNet, this, port, outputGradient);
            Neuron output = inner.getOutputNeuronArr()[port];
            double sum = innerValues.readInputSum(inner, output, 0);
            innerValues.writeDelta(inner, output, 0,
                    outputGradient * output.getActivationFunction().derivative(sum));
        }
        inner.backpropagateDelta(innerValues);

        // Inputs and outputs need not have the same count or matching indices.
        // Finish all input derivatives before any outer or inner weight changes.
        final double[] inputDeltaArr = state.inputDeltas;
        Arrays.fill(inputDeltaArr, 0);
        for (int port = 0; port < inputDeltaArr.length; port++) {
            Neuron input = inner.getInputNeuronArr()[port];
            for (Synapse edge : input.getOutputSynapseList(0)) {
                if (!inner.propagatesCurrentStep(edge)) continue;
                inputDeltaArr[port] += edge.getWeight() * edge.getTargetNeuron().getInputDelta(
                        inner, innerValues, edge.getTargetInputTypePos());
            }
        }
    }

    @Override
    public double getInputDelta(final NeuralNet neuralNet, final NeuronValueFunction values, final int inputTypePos) {
        return values.getModuleState(this).inputDeltas[inputTypePos];
    }

    @Override
    public void backpropagateExtra(final NeuralNet neuralNet, final NeuronValueFunction values, final double learningRate) {
        final NeuralNetwork execution = values.getModuleState(this).network;
        final NeuralNet inner = execution.getNeuralNet();
        inner.updateBiasAndWeights(execution.getNeuronValueFunction(), learningRate);
        inner.backpropagateExtra(execution.getNeuronValueFunction(), learningRate);
    }

    @Override
    public List<Synapse> getOutputSynapseList(final int outputTypePos) {
        return Arrays.asList(this.outputSynapsesList.get(outputTypePos));
    }

    @Override
    public Synapse[] getInputSynapseArr(final int inputTypePos) {
        return this.inputSynapsesList.get(inputTypePos);
    }

    @Override
    public void setInputSynapseArr(final int inputTypePos, final Synapse[] inputSynapses) {
        this.inputSynapsesList.set(inputTypePos, inputSynapses);
    }
}

