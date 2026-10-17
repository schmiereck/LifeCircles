package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.model.neural.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConversionInheritanceTest {
    @Test void inactiveGeneCanAcquireCapacityAndAllMutationsStayValid() {
        EnergyConversion gene=EnergyConversion.NONE;
        Random random=new Random(212);boolean enabled=false;Set<EnergyType> sources=new HashSet<>(),targets=new HashSet<>();
        for(int i=0;i<10000;i++) {
            gene=gene.mutate(random,1,.2);enabled|=gene.active();
            assertNotEquals(gene.source(),gene.target());assertTrue(gene.capacity()>=0 && gene.capacity()<=1);
            sources.add(gene.source());targets.add(gene.target());
        }
        assertTrue(enabled);assertEquals(8,sources.size());assertEquals(8,targets.size());
        Random mustNotDraw=new Random(){public double nextDouble(){throw new AssertionError("Clones must not draw");}};
        assertSame(gene,gene.mutate(mustNotDraw,0,1));assertSame(gene,gene.mutate(mustNotDraw,1,0));
    }
    @Test void biologicalClonesAndTrainingElitesInheritEnzymeWithoutCopyingEnergy() {
        var config=SimulationConfig.getInstance();var previous=NeuralNetwork.getRandom();NeuralNetwork.setRandom(new Random(311));
        try {
            Environment world=new Environment(300,300);Cell parent=CellFactory.createCell(new Vector2D(100,100),10);
            var gene=new EnergyConversion(EnergyType.RGB_111,EnergyType.PRIMARY,.7);parent.setEnergyConversion(gene);
            parent.setEnergy(.8);parent.setEnergy(EnergyType.RGB_111,.6);
            var actor=parent.getSensorActors().getFirst();actor.setReproductionDesire(1);actor.setReproductionEnergyShareOutput(.25);
            world.addCell(parent);Cell child=ReproductionManagerService.reproduceClonally(config,world,parent);assertNotNull(child);
            assertEquals(gene,child.getEnergyConversion());assertEquals(.8,parent.getEnergy()+child.getEnergy(),1e-14);
            assertEquals(.6,parent.getEnergy(EnergyType.RGB_111)+child.getEnergy(EnergyType.RGB_111),1e-14);
            Cell founder=ReproductionManagerService.createTrainingFounder(config,world,parent,new Vector2D(200,100),0,false);
            assertEquals(gene,founder.getEnergyConversion());assertEquals(.8,founder.getEnergy());assertEquals(.2,founder.getEnergy(EnergyType.SECONDARY));assertEquals(0,founder.getEnergy(EnergyType.RGB_111));
            child.setEnergyConversion(EnergyConversion.NONE);assertEquals(gene,parent.getEnergyConversion());
        } finally {NeuralNetwork.setRandom(previous);}
    }
    @Test void biologicalEnzymeMutationUsesParentalFactorsAndCloneMultiplierDisablesIt() {
        var config=SimulationConfig.getInstance();double rate=config.getMutationRate(),strength=config.getMutationStrength(),division=config.getDivisionMutationRateMultiplier();
        var previous=NeuralNetwork.getRandom();NeuralNetwork.setRandom(new Random(612));
        try {
            config.setMutationRate(.5);config.setMutationStrength(.1);config.setDivisionMutationRateMultiplier(1);
            Environment world=new Environment(300,300);Cell parent=CellFactory.createCell(new Vector2D(100,100),10);
            parent.setMutationRateFactor(2);parent.setMutationStrengthFactor(2);
            parent.getSensorActors().getFirst().setReproductionDesire(1);world.addCell(parent);
            Random fixed=new Random(){
                public double nextDouble(){return .75;}
                public double nextDouble(double origin,double bound){return .5;}
                public int nextInt(int bound){return 0;}
            };
            Cell child=ReproductionManagerService.reproduce(config,world,parent,fixed);assertNotNull(child);
            assertEquals(.1,child.getEnergyConversion().capacity(),1e-14);
            assertEquals(EnergyConversion.NONE,parent.getEnergyConversion());
            config.setDivisionMutationRateMultiplier(0);
            Cell clone=ReproductionManagerService.reproduce(config,world,parent,fixed);assertNotNull(clone);
            assertEquals(EnergyConversion.NONE,clone.getEnergyConversion());
        } finally {
            config.setMutationRate(rate);config.setMutationStrength(strength);config.setDivisionMutationRateMultiplier(division);NeuralNetwork.setRandom(previous);
        }
    }
    @Test void selectedTrainingOffspringCanAcquireAnEnzymeWhileTheirParentStaysInactive() {
        var config=SimulationConfig.getInstance();double rate=config.getMutationRate(),strength=config.getMutationStrength();
        var previous=NeuralNetwork.getRandom();NeuralNetwork.setRandom(new Random(619));
        try {
            config.setMutationRate(1);config.setMutationStrength(.1);
            Environment world=new Environment(300,300);Cell parent=CellFactory.createCell(new Vector2D(100,100),10);
            boolean acquired=false;
            for(int i=0;i<20;i++) {
                Cell offspring=ReproductionManagerService.createTrainingFounder(config,world,parent,new Vector2D(200,100),0,true);
                acquired|=offspring.getEnergyConversion().active();
                assertTrue(offspring.getEnergyConversion().capacity()<=.1);
                assertEquals(EnergyConversion.NONE,parent.getEnergyConversion());
            }
            assertTrue(acquired);
        } finally {config.setMutationRate(rate);config.setMutationStrength(strength);NeuralNetwork.setRandom(previous);}
    }
}
