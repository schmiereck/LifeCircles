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

        // Dass NN direkt verwenden, nicht als Kopie.
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
        final NeuronNetwork newNeuron = new NeuronNetwork(newId, this.neuronTypeInfoData, this.network.getNeuronValueFunctionFactory());

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

        final double[] inputArr = new double[this.neuronTypeInfoData.getInputCount()];
        for (int inputTypePos = 0; inputTypePos < this.neuronTypeInfoData.getInputCount(); inputTypePos++) {
            double sum = this.getBias(inputTypePos);
            final Synapse[] inputSynapseArr = this.inputSynapsesList.get(inputTypePos);
            for (int inputSynapsePos = 0; inputSynapsePos < inputSynapseArr.length; inputSynapsePos++) {
                final Synapse synapse = inputSynapseArr[inputSynapsePos];
                final int sourceOutputTypePos = synapse.getSourceOutputTypePos();
                final double sourceOutputValue = neuronValueFunction.readValue(neuralNet, synapse.getSourceNeuron(), sourceOutputTypePos);
                sum += sourceOutputValue * synapse.getWeight();
            }
            inputArr[inputTypePos] = sum;
        }

        //final NeuralNet calcNeuralNet = neuralNet;
        final NeuralNet calcNeuralNet = this.network.getNeuralNet();
        //final NeuronValueFunction calcNeuronValueFunction = neuronValueFunction;
        final NeuronValueFunction calcNeuronValueFunction = this.network.getNeuronValueFunction();

        calcNeuralNet.setInputs(calcNeuronValueFunction, inputArr);
        calcNeuralNet.process(calcNeuronValueFunction);

        for (int outputTypePos = 0; outputTypePos < this.neuronTypeInfoData.getOutputCount(); outputTypePos++) {
            Neuron calcOutputNeuron = calcNeuralNet.getOutputNeuronArr()[outputTypePos];
            // Kopieren der Werte des internen NeuralNet in das NeuronNetwork:
            final double calcOutputNeuronInputSum = calcNeuronValueFunction.readInputSum(calcNeuralNet, calcOutputNeuron, outputTypePos);
            neuronValueFunction.writeInputSum(neuralNet, this, outputTypePos, calcOutputNeuronInputSum);

            final double calcOutputNeuronValue = calcNeuronValueFunction.readValue(calcNeuralNet, calcOutputNeuron, outputTypePos);
            neuronValueFunction.writeValue(neuralNet, this, outputTypePos, calcOutputNeuronValue);
        }

        return calcNeuralNet.getProccessedSynapses();
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
        // Anpassung der Bias-Werte.
        for (int inputTypePos = 0; inputTypePos < this.neuronTypeInfoData.getInputCount(); inputTypePos++) {
            if (random.nextDouble() < mutationRate) {
                double mutationFactor = random.nextDouble() * 0.1 - 0.05; // Zufällige Mutation zwischen -0.05 und +0.05
                double newBias = this.getBias(inputTypePos) + mutationFactor;
                this.setBias(inputTypePos, newBias);
            }
        }

        if (random.nextDouble() < mutationRate) {
            final NeuralNet neuralNet = this.network.getNeuralNet();
            neuralNet.mutateNeuronTypeInfoDataList(this.network.getNeuronValueFunctionFactory(), this.network.getNeuronValueFunction(),
                    random, mutationRate, mutationStrength);
        }

        //if (random.nextDouble() < mutationRate) {
        //    final NeuralNet neuralNet = this.network.getNeuralNet();
        //    neuralNet.mutate(this.network.getNeuronValueFunction(), mutationRate, mutationStrength);
        //}
    }

    @Override
    public void backpropagateDelta(final NeuralNet neuralNet, final NeuronValueFunction neuronValueFunction) {
        for (int outputTypePos = 0; outputTypePos < this.getNeuronTypeInfoData().getOutputCount(); outputTypePos++) {
            double errorSum = 0.0;

            // Sammle Fehler von allen ausgehenden Verbindungen
            for (final Synapse synapse : this.getOutputSynapseList(outputTypePos)) {
                final NeuronInterface targetNeuron = synapse.getTargetNeuron();
                for (int targetOutputTypePos = 0; targetOutputTypePos < targetNeuron.getNeuronTypeInfoData().getOutputCount(); targetOutputTypePos++) {
                    //final double targetDelta = targetNeuron.getDelta(targetOutputTypePos);
                    final double targetDelta = neuronValueFunction.readDelta(neuralNet, targetNeuron, outputTypePos);
                    errorSum += targetDelta * synapse.getWeight();
                }
            }

            // Berechne Delta für dieses Neuron
            //final double inputSum = this.getInputSum(outputTypePos);
            final double inputSum = neuronValueFunction.readInputSum(neuralNet, this, outputTypePos);
            //final double delta = errorSum * this.getActivationFunction().derivative(inputSum);
            final double delta = errorSum * inputSum;
            //this.setDelta(outputTypePos, delta);
            neuronValueFunction.writeDelta(neuralNet, this, outputTypePos, delta);
        }
    }

    @Override
    public void backpropagateExtra(final double learningRate) {
        final NeuronValueFunction neuronValueFunction = this.network.getNeuronValueFunction();
        final NeuralNet neuralNet = this.network.getNeuralNet();

        for (int outputTypePos = 0; outputTypePos < this.getNeuronTypeInfoData().getOutputCount(); outputTypePos++) {
            // Transfer the delta-values of this Neuron to the output neurons of the inner network.
            final double delta = neuronValueFunction.readDelta(neuralNet, this, outputTypePos);
            final Neuron outputNeuron = neuralNet.getOutputNeuronArr()[outputTypePos];
            //outputNeuron.setDelta(outputTypePos, delta);
            neuronValueFunction.writeDelta(neuralNet, outputNeuron, outputTypePos, delta);
            //neuronValueFunction.writeDelta(neuralNet, this, outputTypePos, delta);

            // Nicht nötig, da von activate schon berechnet???
            //final double inputSum = neuronValueFunction.readInputSum(neuralNet, this, outputTypePos);
            ////outputNeuron.setInputSum(outputTypePos, inputSum);
            //neuronValueFunction.writeInputSum(neuralNet, outputNeuron, outputTypePos, inputSum);
            ////neuronValueFunction.writeInputSum(neuralNet, this, outputTypePos, inputSum);
        }

        // 2. Backpropagiere den Fehler durch alle versteckten Schichten (von hinten nach vorne)
        neuralNet.backpropagateDelta(neuronValueFunction);

        // 3. Aktualisiere Gewichte und Bias-Werte
        neuralNet.updateBiasAndWeights(neuronValueFunction, learningRate);

        // 4. Backpropagiere die speziellen Neuronen (NeuronNetwork).
        neuralNet.backpropagateExtra(learningRate);
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

