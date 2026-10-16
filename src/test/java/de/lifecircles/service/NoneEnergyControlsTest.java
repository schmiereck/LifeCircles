package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.model.neural.*;
import de.lifecircles.service.partitioningStrategy.SpatialGridPartitioningStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.io.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class NoneEnergyControlsTest {
    @ParameterizedTest @CsvSource({"1,1","1,.1","100,.1","0,0"})
    void worldActuallyChargesPriceBeforeDeath(double storage,double work) {
        var price=new BrainEnergyCosts(storage,work);
        var brain=new PhysicsRegressionTest.ConstantBrain(){
            public int getSynapseCount(){return 750;}
            public long getProccessedSynapses(){return 431;}
        };
        var world=new Environment(300,300,new Random(1));
        var cell=new Cell(new Vector2D(150,100),10,brain);cell.setEnergy(.75);world.addCell(cell);
        var grid=new SpatialGridPartitioningStrategy(300,300,60);grid.build(world.getCellList());
        var policy=new SimulationStepPolicy(){
            public boolean useSunlight(){return false;}
            public BrainEnergyCosts brainEnergyCosts(){return price;}
        };
        world.update(1./60,grid,policy);
        double expected=.75-SimulationConfig.ENERGY_DECAY_RATE/60-price.cost(750,431,1./60);
        assertEquals(expected,cell.getEnergy(),1e-15);
        cell.setEnergy(price.cost(750,431,1./60)/2);
        grid.build(world.getCellList());world.update(1./60,grid,policy);
        assertTrue(world.getCellList().isEmpty(),"Cannot recover energy after death/clamping");
    }

    @Test void rejectsInvalidCosts() {
        for(double invalid:new double[]{-1,Double.NaN,Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class,()->new BrainEnergyCosts(invalid,1));
            assertThrows(IllegalArgumentException.class,()->new BrainEnergyCosts(1,invalid));
        }
    }

    @Test void bottleneckKeepsRepresentativesOfEverySectionWithoutEditingThem() {
        List<Cell> cells=new ArrayList<>();
        for(int section=0;section<8;section++) for(int i=0;i<6;i++)
            cells.add(new Cell(new Vector2D(100+section*1400+i,100),10,new PhysicsRegressionTest.ConstantBrain()));
        var selected=NoneEnergyStudyTest.selectFounders(cells,2);
        assertEquals(16,selected.size());
        for(int section=0;section<8;section++) {
            assertSame(cells.get(section*6),selected.get(section*2));
            assertSame(cells.get(section*6+3),selected.get(section*2+1));
        }
        assertSame(cells,NoneEnergyStudyTest.selectFounders(cells,0));
        assertThrows(IllegalArgumentException.class,()->NoneEnergyStudyTest.selectFounders(cells,-1));
    }

    @Test void sunlightBirthMutationAndContinuationReplayExactly() throws Exception {
        Random previous=NeuralNetwork.getRandom();
        try {
            NeuralNetwork.setRandom(new Random(901));
            var founder=CellFactory.createCell(new Vector2D(150,100),10,.1,.01,.01);
            founder.setType(new CellType(.3,.7,.4));
            var bytes=new ByteArrayOutputStream();
            try(var out=new ObjectOutputStream(bytes)){out.writeObject(new ArrayList<>(List.of(founder)));}
            assertEquals(replay(bytes.toByteArray(),BrainEnergyCosts.DEFAULT),replay(bytes.toByteArray(),BrainEnergyCosts.DEFAULT));
            assertNotEquals(replay(bytes.toByteArray(),BrainEnergyCosts.DEFAULT),replay(bytes.toByteArray(),new BrainEnergyCosts(100,.1)));
        } finally {NeuralNetwork.setRandom(previous);}
    }

    private String replay(byte[] source,BrainEnergyCosts price) throws Exception {
        NeuralNetwork.setRandom(new Random(91));
        var world=new Environment(300,300,new Random(71));world.resetCells(NoneEnergyStudyTest.load(source));
        var policy=new NoneEnergyStudyTest.Policy(price,81,world.getCellList());
        var parent=world.getCellList().getFirst();var actor=parent.getSensorActors().getFirst();
        actor.setReproductionDesire(1);actor.setReproductionEnergyShareOutput(.5);
        var child=policy.reproduce(world,parent);assertNotNull(child);policy.onBirth(parent,child);world.addCell(child);
        var grid=new SpatialGridPartitioningStrategy(300,300,60);
        for(int step=0;step<100;step++){grid.build(world.getCellList());world.update(1./60,grid,policy);}
        return NoneEnergyStudyTest.row(0,price,100,world,policy,0,"test");
    }
}
