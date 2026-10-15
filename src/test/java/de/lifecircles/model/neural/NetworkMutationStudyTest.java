package de.lifecircles.model.neural;

import de.lifecircles.model.*;
import de.lifecircles.service.ReproductionManagerService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in mutation-distribution observation; correctness regressions live in dedicated tests. */
@EnabledIfSystemProperty(named = "network.audit", matches = "true")
class NetworkMutationStudyTest {
    private final NeuronValueFunctionFactory factory = new ValuesNeuronValueFunctionFactory();
    private Random previous;
    @BeforeEach void seed() { previous = NeuralNetwork.getRandom(); NeuralNetwork.setRandom(new Random(81)); }
    @AfterEach void restore() { NeuralNetwork.setRandom(previous); }

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
