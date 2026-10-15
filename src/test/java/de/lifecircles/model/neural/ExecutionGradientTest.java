package de.lifecircles.model.neural;

import de.lifecircles.model.*;
import de.lifecircles.service.ReproductionManagerService;
import org.junit.jupiter.api.*;

import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in observations of CURRENT behavior, including defects. Rewrite expectations when fixing them. */
class ExecutionGradientTest {
    private final NeuronValueFunctionFactory factory = new ValuesNeuronValueFunctionFactory();
    private Random previous;

    @Test void feedbackAndOutputConnectionsMatchDetachedNumericalGradients() {
        var definition = new NeuralNetwork(factory, 1, new int[]{2}, 2, 0, 1);
        definition.getNeuralNet().applyToAllNeurons(n -> n.setBias(0,.1));
        var h = definition.getHiddenLayerArr()[0].getNeuronsArr();
        var o = definition.getOutputNeuronArr();
        connect(definition,new Synapse(definition.getInputNeuronArr()[0],0,h[0],0,.4));
        connect(definition,new Synapse(h[0],0,h[1],0,.3));
        connect(definition,new Synapse(h[1],0,h[0],0,.2));
        connect(definition,new Synapse(h[1],0,o[0],0,.5));
        connect(definition,new Synapse(o[0],0,o[1],0,.6));
        connect(definition,new Synapse(o[1],0,h[0],0,.7));
        for (int index=0; index<definition.getSynapseCount(); index++) {
            var high = new NeuralNetwork(definition); var low = new NeuralNetwork(definition);
            var trained = new NeuralNetwork(definition); trained.setLearningRate(.1);
            // Warm-up is identical. Only current-step parameters are perturbed.
            for (var net : List.of(high,low,trained)) net.calcProcess(new double[]{.2});
            double weight = definition.getSynapseList().get(index).getWeight();
            high.getSynapseList().get(index).setWeight(weight+1e-6);
            low.getSynapseList().get(index).setWeight(weight-1e-6);
            double highLoss = Arrays.stream(high.calcProcess(new double[]{.8})).map(x->x*x/2).sum();
            double lowLoss = Arrays.stream(low.calcProcess(new double[]{.8})).map(x->x*x/2).sum();
            trained.calcTrain(new double[]{.8},new double[]{0,0});
            assertEquals((highLoss-lowLoss)/2e-6,
                    (weight-trained.getSynapseList().get(index).getWeight())/.1,1e-8,"edge "+index);
        }
    }

    @Test void sleepingPayloadIsNotTrainedWhenItsNextStepWakes() {
        var net = heldNetwork(); net.setLearningRate(.1);
        net.calcProcess(new double[]{1});
        double weight = net.getSynapseList().getFirst().getWeight();
        net.calcTrain(new double[]{5},new double[]{0});
        assertTrue(net.getHiddenLayerArr()[0].isActiveLayer());
        assertEquals(weight,net.getSynapseList().getFirst().getWeight());
    }

    @Test void resetInvalidatesOldTrainingTrace() {
        var net = heldNetwork(); net.calcProcess(new double[]{1}); net.rnnClearPreviousState();
        assertThrows(IllegalStateException.class,()->net.backpropagate(new double[]{0}));
    }
    @BeforeEach void seed() { previous = NeuralNetwork.getRandom(); NeuralNetwork.setRandom(new Random(81)); }
    @AfterEach void restore() { NeuralNetwork.setRandom(previous); }

    private void connect(NeuralNetwork net, Synapse edge) {
        try {
            var method = NeuralNet.class.getDeclaredMethod("addSynapse", Synapse.class);
            method.setAccessible(true); method.invoke(net.getNeuralNet(), edge);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

    private NeuralNetwork heldNetwork() {
        NeuralNetwork net = new NeuralNetwork(factory, 1, new int[]{2}, 1, 0, 0);
        net.getNeuralNet().applyToAllNeurons(n -> n.setBias(0, 0));
        var hidden = net.getNeuralNet().getHiddenLayerArr()[0].getNeuronsArr();
        ((Neuron) hidden[1]).setActivationFunction(ActivationFunction.ReLU);
        connect(net, new Synapse(net.getNeuralNet().getInputNeuronArr()[0], 0, hidden[1], 0, 1));
        connect(net, new Synapse(hidden[1], 0, net.getNeuralNet().getOutputNeuronArr()[0], 0, 1));
        return net;
    }

    @Test void backpropTrainsExecutedPayloadDespiteNextStepSleep() {
        var net = heldNetwork(); net.setLearningRate(.1);
        var inputWeight = net.getSynapseList().get(0);
        var numerical = new NeuralNetwork(net); numerical.setDisableLayerDeactivation(true);
        double epsilon = 1e-6;
        numerical.getSynapseList().get(0).setWeight(1 + epsilon);
        double high = Math.pow(numerical.calcProcess(new double[]{1})[0], 2) / 2;
        numerical.getSynapseList().get(0).setWeight(1 - epsilon);
        double low = Math.pow(numerical.calcProcess(new double[]{1})[0], 2) / 2;
        assertTrue((high - low) / (2 * epsilon) > .1);
        net.calcTrain(new double[]{1}, new double[]{0});
        assertEquals((high-low)/(2*epsilon),(1-inputWeight.getWeight())/.1,1e-7);
        assertNotEquals(1, net.getSynapseList().get(1).getWeight());
    }

    @Test void feedbackGradientUsesPreviousSourceValue() {
        var net = new NeuralNetwork(factory, 1, new int[]{1}, 1, 0, 1);
        net.getNeuralNet().applyToAllNeurons(n -> n.setBias(0, 0)); net.setLearningRate(.1);
        NeuronInterface h = net.getHiddenLayerArr()[0].getNeuron(0);
        connect(net, new Synapse(net.getInputNeuronArr()[0], 0, h, 0, 1));
        connect(net, new Synapse(h, 0, net.getOutputNeuronArr()[0], 0, 1));
        Synapse feedback = new Synapse(net.getOutputNeuronArr()[0], 0, h, 0, .5); connect(net, feedback);
        var high = new NeuralNetwork(net); high.getSynapseList().get(2).setWeight(.500001);
        var low = new NeuralNetwork(net); low.getSynapseList().get(2).setWeight(.499999);
        // The source value was zero at the first forward pass, so this weight cannot affect its loss.
        assertEquals(high.calcProcess(new double[]{1})[0], low.calcProcess(new double[]{1})[0]);
        net.calcTrain(new double[]{1}, new double[]{0});
        assertEquals(.5, feedback.getWeight());
    }

}
