package de.lifecircles.model.neural;

import de.lifecircles.model.*;
import de.lifecircles.service.*;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class CompactFounderBrainTest {
    Random previous;
    @BeforeEach void seed() { previous=NeuralNetwork.getRandom();NeuralNetwork.setRandom(new Random(417)); }
    @AfterEach void restore() { NeuralNetwork.setRandom(previous); }
    private int hidden(CellBrain brain) {
        return Arrays.stream(brain.getNeuralNetwork().getHiddenLayerArr()).mapToInt(l->l.getNeuronsArr().length).sum();
    }
    @Test void defaultHasFortyEightHiddenNeuronsWithAllEnergyAndActuatorChannels() {
        Cell cell=CellFactory.createCell(new Vector2D(100,100),10);
        CellBrain brain=(CellBrain)cell.getBrain();
        assertEquals(328,brain.getInputCount());assertEquals(134,brain.getNeuralNetwork().getNeuralNet().getOutputNeuronArr().length);
        assertEquals(48,hidden(brain));
        Layer[] layers=brain.getNeuralNetwork().getHiddenLayerArr();
        assertEquals(4,layers.length);
        assertTrue(layers[3].isActiveLayer());
        for(int state=0;state<8;state++) {
            cell.setCellState(state);
            for(int bit=0;bit<3;bit++)assertEquals((state&(1<<bit))!=0,layers[bit].isActiveLayer());
            // The common layer's own controller may change its gate while processing.
            brain.setInputs(new double[328]);for(double value:brain.process())assertTrue(Double.isFinite(value));
        }
    }
    @Test void compactBrainStillRespondsToSensorsAndHasLowerStorageFees() {
        CellBrain compact=(CellBrain)CellFactory.createCell(new Vector2D(100,100),10).getBrain();
        CellBrain historical=new CellBrain(new ValuesNeuronValueFunctionFactory(),316,133,1.4,.005,.025);
        assertEquals(590,hidden(historical));
        assertTrue(compact.getSynapseCount()<historical.getSynapseCount());
        assertTrue(BrainEnergyCosts.DEFAULT.cost(compact.getSynapseCount(),0,1)
                <BrainEnergyCosts.DEFAULT.cost(historical.getSynapseCount(),0,1));
        compact.setInputs(new double[328]);double[] zero=compact.process().clone();
        compact.getNeuralNetwork().rnnClearPreviousState();double[] inputs=new double[328];Arrays.fill(inputs,1);
        compact.setInputs(inputs);double[] one=compact.process();
        assertTrue(java.util.stream.IntStream.range(0,133).anyMatch(i->Math.abs(zero[i]-one[i])>1e-8));
        System.out.printf(Locale.ROOT,"Founder comparison: hidden %d -> %d, synapses %d -> %d%n",
                hidden(historical),hidden(compact),historical.getSynapseCount(),compact.getSynapseCount());
    }
    @Test void inheritedGenomeIsIndependentAndRetainsCompactTopology() {
        CellBrain parent=(CellBrain)CellFactory.createCell(new Vector2D(100,100),10).getBrain();
        CellBrain child=new CellBrain(parent.mutate(0,0));
        assertEquals(48,hidden(child));assertEquals(parent.getSynapseCount(),child.getSynapseCount());
        var a=parent.getNeuralNetwork().getNeuralNet().getOutputNeuronArr()[0];
        var b=child.getNeuralNetwork().getNeuralNet().getOutputNeuronArr()[0];
        double before=a.getBias(0);b.setBias(0,42);assertEquals(before,a.getBias(0));
    }
}
