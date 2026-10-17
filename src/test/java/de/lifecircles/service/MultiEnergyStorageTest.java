package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.model.neural.*;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MultiEnergyStorageTest {
    private Cell cell() {return new Cell(new Vector2D(100,100),10,new CellBrain(
            new NeuralNetwork(new ValuesNeuronValueFunctionFactory(),CellFactory.calcInputCount(12),
                    new int[]{1,1,1},CellFactory.calcOutputCount(12),0,3)));}
    @Test void newCellsKeepTheOriginalPrimaryAndStartSecondaryEmpty() {
        Cell cell=cell();assertEquals(1,cell.getEnergy());assertEquals(1,cell.getEnergy(EnergyType.PRIMARY));
        assertEquals(0,cell.getEnergy(EnergyType.SECONDARY));assertEquals(1,cell.getTotalEnergy());
        cell.setEnergy(EnergyType.SECONDARY,.7);cell.setEnergy(.25);
        assertEquals(.25,cell.getEnergy(EnergyType.PRIMARY));assertEquals(.7,cell.getEnergy(EnergyType.SECONDARY));
        assertEquals(.95,cell.getTotalEnergy(),1e-14);
    }
    @Test void capacitiesAndLimitsAreIndependent() {
        Cell cell=cell();cell.setEnergy(.2);
        cell.setEnergy(EnergyType.SECONDARY,2);assertEquals(1,cell.getEnergy(EnergyType.SECONDARY));assertEquals(.2,cell.getEnergy());
        cell.setEnergy(EnergyType.SECONDARY,-1);assertEquals(0,cell.getEnergy(EnergyType.SECONDARY));
        for(EnergyType type:EnergyType.ALL) {
            assertEquals(1,cell.getMaxEnergy(type));
            assertThrows(IllegalArgumentException.class,()->cell.setEnergy(type,Double.NaN));
        }
    }
    @SuppressWarnings("unchecked")
    @Test void actualOldFilePreservesItsEnergyAndAddsNoNewReserve() throws Exception {
        try(var stream=getClass().getResourceAsStream("/energy/single-energy-v1.cells")) {
            assertNotNull(stream);
            try(var in=new ObjectInputStream(stream)) {
                List<Cell> loaded=(List<Cell>)in.readObject();assertEquals(2,loaded.size());
                assertEquals(.375,loaded.get(0).getEnergy());assertEquals(.8125,loaded.get(1).getEnergy());
                assertEquals(42,loaded.get(0).getAge());assertEquals(5,loaded.get(0).getCellState());
                for(Cell cell:loaded) {
                    assertEquals(0,cell.getEnergy(EnergyType.SECONDARY));
                    assertEquals(cell.getEnergy(),cell.getTotalEnergy());
                    cell.setEnergy(EnergyType.SECONDARY,.625);assertEquals(.625,cell.getEnergy(EnergyType.SECONDARY));
                    for(SensorActor actor:cell.getSensorActors())assertSame(cell,actor.getParentCell());
                }
            }
        }
    }
    @Test void saveAndLoadPreservesBothStoresAndLegacyAccessStillMeansPrimary() throws Exception {
        Cell source=cell();source.setEnergy(.3125);source.setEnergy(EnergyType.SECONDARY,.6875);
        var bytes=new ByteArrayOutputStream();try(var out=new ObjectOutputStream(bytes)){out.writeObject(source);}
        Cell loaded;try(var in=new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))){loaded=(Cell)in.readObject();}
        assertEquals(.3125,loaded.getEnergy());assertEquals(.6875,loaded.getEnergy(EnergyType.SECONDARY));
        loaded.setEnergy(EnergyType.SECONDARY,.5);assertEquals(.6875,source.getEnergy(EnergyType.SECONDARY));
    }
}
