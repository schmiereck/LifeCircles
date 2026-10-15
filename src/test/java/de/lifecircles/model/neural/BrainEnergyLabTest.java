package de.lifecircles.model.neural;

import de.lifecircles.model.*;
import de.lifecircles.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class BrainEnergyLabTest {
    Random previous;
    @BeforeEach void save(){previous=NeuralNetwork.getRandom();}
    @AfterEach void restore(){NeuralNetwork.setRandom(previous);}
    @ParameterizedTest @EnumSource(BrainEnergyLab.Task.class)
    void positiveControlsReallySolveUnseenSequences(BrainEnergyLab.Task task) {
        for(int seed=0;seed<3;seed++) {
            var metrics=BrainEnergyLab.evaluate(BrainEnergyControls.known(task,false),BrainEnergyLab.data(task,seed,true),task);
            assertTrue(metrics.solved(task),task+" "+metrics);
        }
    }
    @ParameterizedTest @EnumSource(BrainEnergyLab.Task.class)
    void addedBloatPreservesFunctionButAddsChargedConnections(BrainEnergyLab.Task task) {
        var data=BrainEnergyLab.data(task,95,true);
        var reference=BrainEnergyLab.evaluate(BrainEnergyControls.known(task,false),data,task);
        var bloated=BrainEnergyLab.evaluate(BrainEnergyControls.known(task,true),data,task);
        assertEquals(reference.loss(),bloated.loss(),1e-12);assertEquals(reference.accuracy(),bloated.accuracy());
        assertEquals(reference.stored()+24,bloated.stored());assertEquals(reference.neurons()+12,bloated.neurons());
    }
    @Test void memoryTasksCannotBeSolvedByConstantOutput() {
        for(var task:List.of(BrainEnergyLab.Task.DELAY,BrainEnergyLab.Task.COUNTER)) {
            var constant=BrainEnergyControls.known(BrainEnergyLab.Task.CONSTANT,false);
            var result=BrainEnergyLab.evaluate(constant,BrainEnergyLab.data(task,34,true),task);
            assertFalse(result.solved(task));assertTrue(result.accuracy()<.7);
        }
    }
    @Test void delayNeedsHistoryAndResetClearsIt() {
        var network=BrainEnergyControls.known(BrainEnergyLab.Task.DELAY,false);
        assertTrue(network.calcProcess(new double[]{1,0})[0]<.001);
        assertTrue(network.calcProcess(new double[]{0,0})[0]>.999);
        network.rnnClearPreviousState();assertTrue(network.calcProcess(new double[]{0,0})[0]<.001);
        var data=BrainEnergyLab.data(BrainEnergyLab.Task.DELAY,43,true);
        assertEquals(BrainEnergyLab.evaluate(network,data,BrainEnergyLab.Task.DELAY),BrainEnergyLab.evaluate(network,data,BrainEnergyLab.Task.DELAY));
    }
    @Test void priceMatchesActualCellEnergyModelAtSixtyDecisionsPerSecond() {
        var network=BrainEnergyControls.known(BrainEnergyLab.Task.XOR,false);
        network.calcProcess(new double[]{1,0});
        Cell cell=new Cell(new Vector2D(100,100),20,new CellBrain(network));cell.setEnergy(1);
        double expected=new BrainEnergyLab.Price(1,1,.003).cost(cell.getBrain().getSynapseCount(),cell.getBrain().getProccessedSynapses());
        EnergyCellCalcService.decayEnergy(cell,1.0/60,true);
        assertEquals(expected,(1-cell.getEnergy())*60-SimulationConfig.ENERGY_DECAY_RATE,1e-12);
    }
    @Test void booleanConstantBaselinesAreAccountedFor() {
        assertEquals(.1875,BrainEnergyLab.data(BrainEnergyLab.Task.AND,3,false).baselineLoss());
        assertEquals(.25,BrainEnergyLab.data(BrainEnergyLab.Task.XOR,3,false).baselineLoss());
        assertEquals(0,BrainEnergyLab.data(BrainEnergyLab.Task.CONSTANT,3,false).baselineLoss());
    }
    @Test void allEvolutionRandomnessIsReproducible() {
        var price=new BrainEnergyLab.Price(100,1,.003);
        var a=BrainEnergyLab.evolve(BrainEnergyLab.Task.DELAY,price,65,5,8,null,null);
        var b=BrainEnergyLab.evolve(BrainEnergyLab.Task.DELAY,price,65,5,8,null,null);
        assertEquals(a.metrics(),b.metrics());assertEquals(a.champion().rate(),b.champion().rate());
        assertEquals(a.champion().strength(),b.champion().strength());
    }
}
