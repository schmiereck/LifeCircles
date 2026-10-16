package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.model.neural.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class FunctionSignalControlsTest {
    @Test void constantNetworkDoesNotReactToInputProbes() {
        Random previous=NeuralNetwork.getRandom();
        try {
            NeuralNetwork.setRandom(new Random(731));
            Cell genome=new FunctionComparisonControlsTest().constant();
            var reference=FunctionSignalStudyTest.probe(genome,0,FunctionSignalStudyTest.Context.BASE);
            for(var context:FunctionSignalStudyTest.Context.values()) {
                var result=FunctionSignalStudyTest.probe(genome,0,context);
                assertArrayEquals(reference.applied(),result.applied(),0);
            }
        } finally {NeuralNetwork.setRandom(previous);}
    }
    @Test void energySensorProbeChangesKnownReactiveSizeOutput() {
        Random previous=NeuralNetwork.getRandom();
        try {
            NeuralNetwork.setRandom(new Random(732));
            var n=new NeuralNetwork(new ValuesNeuronValueFunctionFactory(),CellFactory.calcInputCount(12),new int[0],FunctionComparisonLab.OUTPUTS,1,0);
            for(var edge:n.getNeuralNet().getSynapseArr())edge.setWeight(0);
            var input=n.getInputNeuronArr()[GlobalInputFeature.MY_CELL_ENERGY.ordinal()];var size=n.getOutputNeuronArr()[0];size.setBias(0,-10);
            for(var edge:n.getNeuralNet().getSynapseArr())if(edge.getSourceNeuron()==input && edge.getTargetNeuron()==size)edge.setWeight(20);
            Cell genome=new Cell(new Vector2D(100,100),10,new CellBrain(n));
            var low=FunctionSignalStudyTest.probe(genome,0,FunctionSignalStudyTest.Context.ENERGY_LOW);
            var high=FunctionSignalStudyTest.probe(genome,0,FunctionSignalStudyTest.Context.ENERGY_HIGH);
            assertTrue(high.applied()[0]>.99 && low.applied()[0]<.01);
        } finally {NeuralNetwork.setRandom(previous);}
    }
}
