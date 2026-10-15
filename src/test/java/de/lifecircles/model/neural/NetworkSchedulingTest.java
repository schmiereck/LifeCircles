package de.lifecircles.model.neural;

import de.lifecircles.model.*;
import de.lifecircles.service.ReproductionManagerService;
import org.junit.jupiter.api.*;

import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in observations of scheduling and state gating. */
class NetworkSchedulingTest {
    private final NeuronValueFunctionFactory factory = new ValuesNeuronValueFunctionFactory();
    private Random previous;
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

    @Test void sleepingDynamicLayerHoldsPayloadButStillEvaluatesControllerAndOutput() {
        var net = heldNetwork();
        double first = net.calcProcess(new double[]{1})[0];
        assertEquals(2, net.getProccessedSynapses());
        assertFalse(net.getNeuralNet().getHiddenLayerArr()[0].isActiveLayer());
        assertEquals(first, net.calcProcess(new double[]{5})[0]);
        assertEquals(1, net.getProccessedSynapses());
        assertTrue(net.getNeuralNet().getHiddenLayerArr()[0].isActiveLayer());
        assertTrue(net.calcProcess(new double[]{5})[0] > first);
    }

    @Test void silentControllerWakesWithinOneThousandCalls() {
        var net = heldNetwork();
        Neuron control = (Neuron) net.getNeuralNet().getHiddenLayerArr()[0].getNeuronsArr()[0];
        control.setActivationFunction(ActivationFunction.ReLU); control.setBias(0, -1);
        net.calcProcess(new double[]{1});
        boolean woke = false;
        for (int i = 0; i < 1000; i++) {
            net.calcProcess(new double[]{5});
            woke |= net.getNeuralNet().getHiddenLayerArr()[0].isActiveLayer();
        }
        var layer = net.getNeuralNet().getHiddenLayerArr()[0];
        assertTrue(woke);
    }

    @Test void resetRestoresSchedulingAndIdenticalSequenceResult() {
        var net = heldNetwork(); double first = net.calcProcess(new double[]{1})[0];
        net.rnnClearPreviousState();
        assertTrue(net.getNeuralNet().getHiddenLayerArr()[0].isActiveLayer());
        assertEquals(0,net.getHiddenLayerArr()[0].getActivationCounter());
        assertEquals(first, net.calcProcess(new double[]{1})[0]);
        assertEquals(first, new NeuralNetwork(net).calcProcess(new double[]{1})[0]);
    }

    @Test void factoryAndStateChangesApplyGatesImmediately() {
        Cell cell = CellFactory.createCell(new Vector2D(100, 100), 10, .05, 0, 0);
        Layer[] layers = ((CellBrain) cell.getBrain()).getNeuralNetwork().getNeuralNet().getHiddenLayerArr();
        assertEquals(0, cell.getCellState());
        for (int i = 0; i < 3; i++) assertFalse(layers[i].isActiveLayer());
        ReproductionManagerService.calcActiveLayersByState(cell);
        for (int i = 0; i < 3; i++) assertFalse(layers[i].isActiveLayer());
        cell.setCellState(7);
        for (int i = 0; i < 3; i++) assertTrue(layers[i].isActiveLayer());
    }

    @Test void eachStateExpertIndependentlyFeedsCommonTrunk() {
        CellBrain brain = new CellBrain(factory, 4, 1, .5, 1, 1);
        NeuralNetwork definition = brain.getNeuralNetwork();
        definition.getNeuralNet().applyToAllNeurons(n -> n.setBias(0, 0));
        for (Synapse edge : definition.getSynapseList()) edge.setWeight(.1);
        double[] outputs = new double[8];
        for (int state = 0; state < 8; state++) {
            var net = new NeuralNetwork(definition);
            for (int bit = 0; bit < 3; bit++) net.getNeuralNet().getHiddenLayerArr()[bit].setActiveLayer((state & 1 << bit) != 0);
            outputs[state] = net.calcProcess(new double[]{1, 0, 0, 0})[0];
        }
        for (int bit=0; bit<3; bit++) assertTrue(outputs[1 << bit] > outputs[0]);
        assertEquals(outputs[1],outputs[2],1e-15); assertEquals(outputs[2],outputs[4],1e-15);
        assertTrue(outputs[7]>outputs[3]);
    }

}
