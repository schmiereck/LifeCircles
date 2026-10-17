package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.model.neural.*;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MultiEnergyLifecycleTest {
    private final SimulationConfig config=SimulationConfig.getInstance();
    private Random previous;
    @BeforeEach void seed(){previous=NeuralNetwork.getRandom();NeuralNetwork.setRandom(new Random(711));}
    @AfterEach void restore(){NeuralNetwork.setRandom(previous);}
    private Cell parent(Environment world,double share) {
        var net=new NeuralNetwork(new ValuesNeuronValueFunctionFactory(),CellFactory.calcInputCount(12),new int[]{1,1,1},CellFactory.calcOutputCount(12),.1,3);
        Cell c=new Cell(new Vector2D(100,100),10,new CellBrain(net));c.setEnergy(.8);c.setEnergy(EnergyType.SECONDARY,.6);c.setAge(9);
        c.getSensorActors().getFirst().setReproductionDesire(1);c.getSensorActors().getFirst().setReproductionEnergyShareOutput(share);
        world.addCell(c);return c;
    }
    @ParameterizedTest @ValueSource(doubles={.1,.25,.5})
    void biologicalDivisionPartitionsBothReservesWithoutCreatingEnergy(double share) {
        Environment world=new Environment(300,300);Cell p=parent(world,share);
        Cell child=ReproductionManagerService.reproduceClonally(config,world,p);assertNotNull(child);
        assertEquals(.8*share,child.getEnergy(),1e-14);assertEquals(.6*share,child.getEnergy(EnergyType.SECONDARY),1e-14);
        assertEquals(.8,p.getEnergy()+child.getEnergy(),1e-14);
        assertEquals(.6,p.getEnergy(EnergyType.SECONDARY)+child.getEnergy(EnergyType.SECONDARY),1e-14);
        assertEquals(0,child.getAge());assertEquals(9,p.getAge());
    }
    @Test void blockedDivisionDoesNotRemoveEitherReserve() {
        Environment world=new Environment(300,300);Cell p=parent(world,.25);
        world.addBlocker(new Blocker(new Vector2D(111,100),20,20,Color.GRAY,Blocker.BlockerType.WALL));
        assertNull(ReproductionManagerService.reproduceClonally(config,world,p));
        assertEquals(.8,p.getEnergy());assertEquals(.6,p.getEnergy(EnergyType.SECONDARY));
    }
    @Test void repeatedDivisionKeepsSeparateTypeBudgets() {
        Environment world=new Environment(300,300);Cell p=parent(world,.25);List<Cell> lineage=new ArrayList<>(List.of(p));
        for(int i=0;i<8;i++)lineage.add(Objects.requireNonNull(ReproductionManagerService.reproduceClonally(config,world,p)));
        assertEquals(.8,lineage.stream().mapToDouble(Cell::getEnergy).sum(),1e-13);
        assertEquals(.6,lineage.stream().mapToDouble(c->c.getEnergy(EnergyType.SECONDARY)).sum(),1e-13);
    }
    @Test void metabolismAndSurvivalRemainPrimaryOnlyWithoutRescuingPrimaryStarvation() {
        Environment world=new Environment(300,300);Cell p=parent(world,.25);
        var brain=(CellBrain)p.getBrain();brain.setInputs(new double[brain.getInputCount()]);brain.process();
        double cost=BrainEnergyCosts.DEFAULT.cost(brain.getSynapseCount(),brain.getProccessedSynapses(),.5);
        EnergyCellCalcService.decayEnergy(p,.5,true);
        assertEquals(.8-SimulationConfig.ENERGY_DECAY_RATE*.5-cost,p.getEnergy(),1e-14);
        assertEquals(.6,p.getEnergy(EnergyType.SECONDARY));
        p.setAge(0);p.setEnergy(0);assertTrue(SimulationStepPolicy.NONE.isDead(p));
        p.setEnergy(.5);p.setEnergy(EnergyType.SECONDARY,0);assertFalse(SimulationStepPolicy.NONE.isDead(p));
    }
    @Test void oldBrainInputsStillSensePrimaryAndKeepTheirShape() {
        Environment world=new Environment(300,300);Cell p=parent(world,.25);
        double[] inputs=CellBrainService.generateInputs(p,world);
        assertEquals(213,inputs.length);assertEquals(.8,inputs[GlobalInputFeature.MY_CELL_ENERGY.ordinal()]);
        assertEquals(133,CellFactory.calcOutputCount(12));
    }
    @Test void trainingFounderStartsAFreshEpisodeRatherThanDuplicatingParentalReserves() {
        Environment world=new Environment(300,300);Cell p=parent(world,.25);
        Cell founder=ReproductionManagerService.createTrainingFounder(config,world,p,new Vector2D(200,100),0,false);
        assertEquals(1,founder.getEnergy());assertEquals(0,founder.getEnergy(EnergyType.SECONDARY));
        assertEquals(.8,p.getEnergy());assertEquals(.6,p.getEnergy(EnergyType.SECONDARY));
    }
}
