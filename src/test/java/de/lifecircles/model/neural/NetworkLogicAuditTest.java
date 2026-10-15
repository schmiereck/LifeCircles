package de.lifecircles.model.neural;

import de.lifecircles.model.*;
import de.lifecircles.service.ReproductionManagerService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in observations of CURRENT behavior, including defects. Rewrite expectations when fixing them. */
@EnabledIfSystemProperty(named = "network.audit", matches = "true")
class NetworkLogicAuditTest {
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

    @Test void defaultMutationPerturbsManyWeightsEvenWithoutTopologyChange() {
        var cell = CellFactory.createCell(new Vector2D(100, 100), 10);
        var parent = ((CellBrain) cell.getBrain()).getNeuralNetwork();
        NeuralNetwork.setRandom(new Random(42));
        var child = parent.mutate(.1, .2);
        assertArrayEquals(parent.getLayerSizes(), child.getLayerSizes());
        assertEquals(parent.getSynapseCount(), child.getSynapseCount());
        int changed = 0;
        for (int i = 0; i < parent.getSynapseCount(); i++)
            if (parent.getSynapseList().get(i).getWeight() != child.getSynapseList().get(i).getWeight()) changed++;
        assertTrue(changed > 100);
        System.out.printf(Locale.ROOT, "Default genome: layers=%s neurons=%d synapses=%d mutated_weights=%d%n",
                Arrays.toString(parent.getLayerSizes()), parent.getNeuralNet().getAllNeuronsSize(), parent.getSynapseCount(), changed);
    }

}
