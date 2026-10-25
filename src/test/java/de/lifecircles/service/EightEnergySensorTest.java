package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.model.neural.*;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class EightEnergySensorTest {
    private Cell cell(double x){return CellFactory.createCell(new Vector2D(x,100),10,.02,0,0);}
    @Test void allEightCodesAreExactlyTheRgbCubeCorners() {
        assertEquals(8,EnergyType.ALL.size());Set<String> codes=new HashSet<>();
        for(EnergyType type:EnergyType.ALL) {
            assertSame(type,EnergyType.fromRgb(type.getRed(),type.getGreen(),type.getBlue()));
            assertTrue(codes.add(type.getRgbCode()));
            assertEquals(Integer.parseInt(type.getRgbCode(),2),type.getCode());
        }
        for(double invalid:new double[]{.5,-1,2,Double.NaN,Double.POSITIVE_INFINITY})
            assertThrows(IllegalArgumentException.class,()->EnergyType.fromRgb(invalid,0,1));
    }
    @Test void sensorsKeepEveryOwnAndNeighborEnergySeparateAtAllTwelvePorts() {
        Environment world=new Environment(300,300);Cell observer=cell(100);world.addCell(observer);
        for(EnergyType type:EnergyType.ALL)observer.setEnergy(type,.05+.1*type.getCode());
        for(int port=0;port<12;port++) {
            Cell neighbor=cell(122);observer.getSensorActors().get(port).setSensedCell(neighbor);
            for(EnergyType type:EnergyType.ALL)neighbor.setEnergy(type,(port+1)*.01+.08*type.getCode());
        }
        double[] inputs=CellBrainService.generateInputs(observer,world);
        assertEquals(328,inputs.length);
        for(EnergyType type:EnergyType.ALL) {
            assertEquals(observer.getEnergy(type),inputs[EnergyInputLayout.ownEnergy(12,type)]);
            for(int port=0;port<12;port++)assertEquals((port+1)*.01+.08*type.getCode(),inputs[EnergyInputLayout.neighborEnergy(12,port,type)],1e-14);
        }
        observer.getSensorActors().get(4).setSensedCell(null);
        observer.getSensorActors().get(7).setSensedCell(new Blocker(new Vector2D(100,100),10,10,Color.GRAY,Blocker.BlockerType.WALL));
        inputs=CellBrainService.generateInputs(observer,world);
        for(EnergyType type:EnergyType.ALL) {
            assertEquals(0,inputs[EnergyInputLayout.neighborEnergy(12,4,type)]);
            assertEquals(0,inputs[EnergyInputLayout.neighborEnergy(12,7,type)]);
        }
        Cell dead=cell(122);dead.setEnergy(.5);dead.becomeRemains();observer.getSensorActors().get(6).setSensedCell(dead);
        inputs=CellBrainService.generateInputs(observer,world);
        assertEquals(1,inputs[EnergyInputLayout.neighborRemains(12,6)]);
        assertEquals(0,inputs[EnergyInputLayout.neighborRemains(12,4)]);
        assertEquals(0,inputs[EnergyInputLayout.neighborRemains(12,7)]);
    }
    @Test void oldInputIndicesRemainUnmovedAndNewChannelsDoNotOverlap() {
        Set<Integer> indices=new HashSet<>();
        for(int i=0;i<213;i++)indices.add(i);
        assertEquals(3,EnergyInputLayout.ownEnergy(12,EnergyType.PRIMARY));
        for(EnergyType type:EnergyType.ALL)if(type!=EnergyType.PRIMARY) {
            assertTrue(indices.add(EnergyInputLayout.ownEnergy(12,type)));
            for(int port=0;port<12;port++)assertTrue(indices.add(EnergyInputLayout.neighborEnergy(12,port,type)));
        }
        for(int actor=0;actor<12;actor++)assertTrue(indices.add(EnergyInputLayout.neighborRemains(12,actor)));
        assertEquals(316,indices.size());assertEquals(315,Collections.max(indices));
        for(int clock=0;clock<EnergyInputLayout.CLOCKS;clock++) {
            assertTrue(indices.add(EnergyInputLayout.clockSine(12,clock)));assertTrue(indices.add(EnergyInputLayout.clockCosine(12,clock)));
        }
        assertEquals(328,indices.size());assertEquals(327,Collections.max(indices));
    }
    @ParameterizedTest @ValueSource(ints={213,304,316})
    void migrationAppendsDisconnectedInputsWithoutChangingOutputsWorkOrEvolutionRandomStream(int oldCount) throws Exception {
        var previous=NeuralNetwork.getRandom();
        try {
            NeuralNetwork.setRandom(new Random(482));
            var old=new NeuralNetwork(new ValuesNeuronValueFunctionFactory(),oldCount,new int[]{2,3,2,2},133,.05,3);
            var add=NeuralNet.class.getDeclaredMethod("addSynapse",Synapse.class);add.setAccessible(true);
            add.invoke(old.getNeuralNet(),new Synapse(old.getOutputNeuronArr()[0],0,old.getHiddenLayerArr()[0].getNeuron(1),0,.15));
            var reference=new NeuralNetwork(old);var migrated=new NeuralNetwork(old);int stored=old.getSynapseCount();
            for(int step=0;step<120;step++) {
                double[] warm=new double[oldCount];java.util.Arrays.fill(warm,(step%7)*.1);
                assertArrayEquals(reference.calcProcess(warm),migrated.calcProcess(warm));
            }
            Random migrationRandom=new Random(992);NeuralNetwork.setRandom(migrationRandom);
            CellBrain target=new CellBrain(migrated);target.ensureEnergyInputs(12);
            assertEquals(new Random(992).nextLong(),migrationRandom.nextLong());
            assertEquals(328,target.getInputCount());assertEquals(stored,migrated.getSynapseCount());
            Random inputs=new Random(17);
            for(int step=0;step<1200;step++) {
                double[] oldInput=new double[oldCount],newInput=new double[328];
                for(int i=0;i<newInput.length;i++)newInput[i]=inputs.nextDouble();
                System.arraycopy(newInput,0,oldInput,0,oldCount);
                assertArrayEquals(reference.calcProcess(oldInput),migrated.calcProcess(newInput));
                assertEquals(reference.getProccessedSynapses(),migrated.getProccessedSynapses());
            }
        } finally {NeuralNetwork.setRandom(previous);}
    }
}
