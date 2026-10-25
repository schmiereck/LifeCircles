package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.model.neural.*;
import de.lifecircles.service.partitioningStrategy.SpatialGridPartitioningStrategy;
import java.io.ObjectInputStream;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/** Appended neural output throttles the expressed enzyme; migrated and new genomes keep full activity. */
class EnzymeThrottleTest {
    final SimulationConfig config=SimulationConfig.getInstance();
    double sun,price;Random previous;
    @BeforeEach void save(){sun=config.getSunRayRate();price=config.getBrainEnergyCosts().work();previous=NeuralNetwork.getRandom();config.setSunRayRate(0);}
    @AfterEach void restore(){config.setSunRayRate(sun);config.setBrainEnergyMultiplier(price);NeuralNetwork.setRandom(previous);}

    @Test void outputMapsHalfAndAboveToFullActivityAndThrottlesLinearlyBelow() {
        assertEquals(1,OutputLayout.activityFromOutput(.5));assertEquals(1,OutputLayout.activityFromOutput(1));assertEquals(1,OutputLayout.activityFromOutput(2));
        assertEquals(.5,OutputLayout.activityFromOutput(.25));assertEquals(0,OutputLayout.activityFromOutput(0));assertEquals(0,OutputLayout.activityFromOutput(-1));
        assertEquals(1,OutputLayout.activityFromOutput(Double.NaN));
        assertEquals(133,OutputLayout.legacyOutputCount(12));assertEquals(134,OutputLayout.outputCount(12));assertEquals(133,OutputLayout.enzymeActivity(12));
    }
    @Test void migrationAppendsADisconnectedOutputAndLeavesAllOldOutputsBitIdentical() {
        NeuralNetwork.setRandom(new Random(77));
        CellBrain old=new CellBrain(new ValuesNeuronValueFunctionFactory(),316,133,1.4,.005,.025);
        old.applyCellState(0); // what the Cell constructor does for the migrated twin
        NeuralNetwork.setRandom(new Random(77));
        CellBrain migrated=new CellBrain(new ValuesNeuronValueFunctionFactory(),316,133,1.4,.005,.025);
        int synapses=migrated.getSynapseCount();
        Cell cell=new Cell(new Vector2D(100,100),10,migrated);
        NeuralNet net=migrated.getNeuralNetwork().getNeuralNet();
        assertEquals(134,net.getOutputNeuronArr().length);assertEquals(synapses,migrated.getSynapseCount());
        assertEquals(0,net.getOutputNeuronArr()[133].getInputSynapseArr(0).length);
        Random inputs=new Random(3);
        for(int round=0;round<5;round++) {
            double[] in=new double[316];for(int i=0;i<in.length;i++)in[i]=inputs.nextDouble();
            old.setInputs(in);double[] expected=old.process();
            // The cell also appends the disconnected clock inputs.
            migrated.setInputs(Arrays.copyOf(in,328));double[] actual=migrated.process();
            assertArrayEquals(expected,Arrays.copyOf(actual,133));assertEquals(.5,actual[133]);
        }
        CellBrainService.think(cell);assertEquals(1,cell.getEnzymeActivity());
        new Cell(new Vector2D(1,1),10,migrated);assertEquals(134,net.getOutputNeuronArr().length);
    }
    @Test void newFoundersStartAtFullActivityAndBiasEvolutionCanThrottle() {
        NeuralNetwork.setRandom(new Random(5));
        Cell cell=CellFactory.createCell(new Vector2D(100,100),10);
        CellBrain brain=(CellBrain)cell.getBrain();var outputs=brain.getNeuralNetwork().getNeuralNet().getOutputNeuronArr();
        assertEquals(134,outputs.length);
        CellBrainService.think(cell);assertEquals(1,cell.getEnzymeActivity());
        outputs[133].setBias(0,Math.log(.2/.8)); // sigmoid = 0.2
        CellBrainService.think(cell);assertEquals(.4,cell.getEnzymeActivity(),1e-12);
    }
    @Test void oldSaveLoadsWithFullActivityAndMigratedOutput() throws Exception {
        try(var input=new ObjectInputStream(Objects.requireNonNull(getClass().getResourceAsStream("/energy/single-conversion-v1.cells")))) {
            Cell old=(Cell)((List<?>)input.readObject()).get(0);
            assertEquals(1,old.getEnzymeActivity());
            if(old.getBrain() instanceof CellBrain brain)
                assertEquals(OutputLayout.outputCount(old.getSensorActors().size()),brain.getNeuralNetwork().getNeuralNet().getOutputNeuronArr().length);
        }
    }
    @Test void conversionThroughputScalesWithActivityAndStopsAtZero() {
        for(double activity:new double[]{1,.25,0}) {
            Cell cell=new Cell(new Vector2D(100,100),10,new PhysicsRegressionTest.ConstantBrain());
            cell.setEnergy(.5);cell.setEnergy(EnergyType.SECONDARY,0);
            cell.setEnergyConversion(new EnergyConversion(EnergyType.PRIMARY,EnergyType.SECONDARY,1));
            cell.setEnzymeActivity(activity);
            double lost=EnergyConversionService.convert(cell,.5);
            double consumed=EnergyConversionService.MAX_INPUT_PER_SECOND*activity*.5;
            assertEquals(.5-consumed,cell.getEnergy(),1e-15);
            assertEquals(consumed*EnergyConversionService.EFFICIENCY,cell.getEnergy(EnergyType.SECONDARY),1e-15);
            assertEquals(consumed*(1-EnergyConversionService.EFFICIENCY),lost,1e-15);
        }
        assertThrows(IllegalArgumentException.class,()->new Cell(new Vector2D(1,1),10,new PhysicsRegressionTest.ConstantBrain()).setEnzymeActivity(1.5));
    }
    @Test void legacyLengthControllersLeaveActivityAndPausedBrainsKeepTheLastDecision() {
        Cell cell=new Cell(new Vector2D(100,100),10,new PhysicsRegressionTest.ConstantBrain());cell.setEnzymeActivity(.3);
        CellBrainService.applyOutputs(cell,new double[OutputLayout.legacyOutputCount(12)]);assertEquals(.3,cell.getEnzymeActivity(),1e-15);
        double[] outputs=new double[OutputLayout.outputCount(12)];outputs[133]=.1;
        CellBrainService.applyOutputs(cell,outputs);assertEquals(.2,cell.getEnzymeActivity(),1e-15);
        // Paused (no 001, no respiration): the enzyme keeps running at the last decided activity.
        cell.setEnergy(.5);cell.setEnergy(EnergyType.SECONDARY,0);cell.setEnergyConversion(new EnergyConversion(EnergyType.PRIMARY,EnergyType.RGB_010,1));
        config.setBrainEnergyMultiplier(1);
        var brain=new BasalBrainRespirationTest.FixedBrain(10,5,3);Cell paused=new Cell(new Vector2D(100,100),10,brain);
        paused.setEnergy(.5);paused.setEnergy(EnergyType.SECONDARY,0);paused.setEnergyConversion(new EnergyConversion(EnergyType.PRIMARY,EnergyType.RGB_010,1));
        paused.setEnzymeActivity(.2);
        Environment world=new Environment(300,300);world.addCell(paused);
        var grid=new SpatialGridPartitioningStrategy(300,300,60);grid.build(world.getCellList());
        world.update(.5,grid,FunctionalBrainEnergyTest.NO_RESPIRATION);
        assertTrue(paused.isBrainPaused());assertEquals(.2,paused.getEnzymeActivity(),1e-15);
        assertEquals(EnergyConversionService.MAX_INPUT_PER_SECOND*.2*.5*EnergyConversionService.EFFICIENCY,paused.getEnergy(EnergyType.RGB_010),1e-15);
    }
}
