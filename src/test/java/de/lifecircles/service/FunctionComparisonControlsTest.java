package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.model.neural.*;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class FunctionComparisonControlsTest {
    Random previous;
    @BeforeEach void seed(){previous=NeuralNetwork.getRandom();NeuralNetwork.setRandom(new Random(71));}
    @AfterEach void restore(){NeuralNetwork.setRandom(previous);}
    Cell constant() {
        var n=new NeuralNetwork(new ValuesNeuronValueFunctionFactory(),CellFactory.calcInputCount(12),new int[0],FunctionComparisonLab.OUTPUTS,0,0);
        for(var output:n.getOutputNeuronArr())output.setBias(0,0);
        for(int i=0;i<12;i++)for(var feature:new ActorOutputFeature[]{ActorOutputFeature.REPRODUCTION_DESIRE,ActorOutputFeature.ENERGY_DELIVERY,ActorOutputFeature.ENERGY_ABSORPTION})
            n.getOutputNeuronArr()[1+i*ActorOutputFeature.values().length+feature.ordinal()].setBias(0,-20);
        return new Cell(new Vector2D(100,100),10,new CellBrain(n));
    }
    @Test void constantControlHasNoArtificialLiveFrozenAdvantage() {
        Cell genome=constant();var free=new BrainEnergyCosts(0,0);
        var profiles=FunctionComparisonLab.calibrate(genome,free,60);
        var live=FunctionComparisonLab.run(genome,free,null,992000,120,null);
        var frozen=FunctionComparisonLab.run(genome,free,profiles.mean(),992000,120,null);
        assertEquals(live.cellSeconds(),frozen.cellSeconds(),1e-12);
        assertEquals(live.energySeconds(),frozen.energySeconds(),1e-9);
        assertEquals(live.births(),frozen.births());assertEquals(live.cells(),frozen.cells());
        assertEquals(0,frozen.commandDeviation(),1e-12);
        for(double[] values:profiles.mean())for(double value:values)assertTrue(Double.isFinite(value));
        assertTrue(profiles.counts()[0]>0);
    }
    @Test void frozenCommandsDoNotDisableBrainProcessingAndRespectBirthState() {
        Cell cell=constant();var n=((CellBrain)cell.getBrain()).getNeuralNetwork();
        // Direct edge is sufficient for this processing control; it is not used as an inheritance fixture.
        new Synapse(n.getInputNeuronArr()[GlobalInputFeature.MY_CELL_ENERGY.ordinal()],0,n.getOutputNeuronArr()[0],0,20);
        n.getOutputNeuronArr()[0].setBias(0,-10);
        double[][] fixed=new double[8][FunctionComparisonLab.OUTPUTS];
        for(int state=0;state<8;state++){Arrays.fill(fixed[state],.5);fixed[state][0]=state==1?.75:.25;}
        var policy=new FunctionComparisonLab.Policy(BrainEnergyCosts.DEFAULT,fixed);
        var world=new Environment(600,400,new Random(1));world.addCell(cell);
        cell.setEnergy(1);CellBrainService.think(cell);double high=cell.getBrain().getOutputValue(0);policy.afterThinking(cell);
        assertEquals(9.5,cell.getTargetRadiusSize(),1e-12);
        cell.setEnergy(0);CellBrainService.think(cell);double low=cell.getBrain().getOutputValue(0);policy.afterThinking(cell);
        assertTrue(high>.99 && low<.01);assertTrue(cell.getBrain().getProccessedSynapses()>0);
        assertEquals(9.5,cell.getTargetRadiusSize(),1e-12);
        Cell child=constant();child.setCellState(1);policy.onBirth(cell,child);
        assertEquals(16.5,child.getTargetRadiusSize(),1e-12);assertEquals(1,policy.births);
    }
    @Test void freshFoundersShareGenesButNotNeuralStateAndHaveEqualEnergy() {
        Cell genome=constant();genome.setAge(500);genome.setEnergy(.01);genome.setCellState(3);
        var world=FunctionComparisonLab.world(genome,991000);
        assertEquals(8,world.getCellList().size());
        for(Cell cell:world.getCellList()) {
            assertEquals(.75,cell.getEnergy());assertEquals(9,cell.getAge());assertEquals(3,cell.getCellState());
            assertNotSame(genome.getBrain(),cell.getBrain());
        }
        assertNotSame(world.getCellList().get(0).getBrain(),world.getCellList().get(1).getBrain());
        for(int i=0;i<8;i++)assertEquals(i*Math.PI/4,world.getCellList().get(i).getRotation(),1e-12);
        assertEquals(.01,genome.getEnergy());assertEquals(500,genome.getAge());
    }
}
