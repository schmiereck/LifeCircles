package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.model.neural.*;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.*;
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
        assertEquals(304,inputs.length);
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
    }
    @Test void oldInputIndicesRemainUnmovedAndNewChannelsDoNotOverlap() {
        Set<Integer> indices=new HashSet<>();
        for(int i=0;i<213;i++)indices.add(i);
        assertEquals(3,EnergyInputLayout.ownEnergy(12,EnergyType.PRIMARY));
        for(EnergyType type:EnergyType.ALL)if(type!=EnergyType.PRIMARY) {
            assertTrue(indices.add(EnergyInputLayout.ownEnergy(12,type)));
            for(int port=0;port<12;port++)assertTrue(indices.add(EnergyInputLayout.neighborEnergy(12,port,type)));
        }
        assertEquals(304,indices.size());assertEquals(303,Collections.max(indices));
    }
    @Test void migrationAppendsDisconnectedInputsWithoutChangingOutputsWorkOrEvolutionRandomStream() {
        var previous=NeuralNetwork.getRandom();
        try {
            NeuralNetwork.setRandom(new Random(482));
            var old=new NeuralNetwork(new ValuesNeuronValueFunctionFactory(),213,new int[]{2,3,2},133,.05,3);
            var reference=new NeuralNetwork(old);var migrated=new NeuralNetwork(old);int stored=old.getSynapseCount();
            Random migrationRandom=new Random(992);NeuralNetwork.setRandom(migrationRandom);
            Cell target=new Cell(new Vector2D(100,100),10,new CellBrain(migrated));
            target.setCellState(7); // Match the reference's initially active fixed layers.
            assertEquals(new Random(992).nextLong(),migrationRandom.nextLong());
            assertEquals(304,target.getBrain().getInputCount());assertEquals(stored,migrated.getSynapseCount());
            Random inputs=new Random(17);
            for(int step=0;step<1200;step++) {
                double[] oldInput=new double[213],newInput=new double[304];
                for(int i=0;i<newInput.length;i++)newInput[i]=inputs.nextDouble();
                System.arraycopy(newInput,0,oldInput,0,213);
                assertArrayEquals(reference.calcProcess(oldInput),migrated.calcProcess(newInput));
                assertEquals(reference.getProccessedSynapses(),migrated.getProccessedSynapses());
            }
        } finally {NeuralNetwork.setRandom(previous);}
    }
}
