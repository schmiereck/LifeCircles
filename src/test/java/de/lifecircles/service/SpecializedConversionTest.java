package de.lifecircles.service;

import de.lifecircles.model.*;
import de.lifecircles.model.neural.*;
import de.lifecircles.service.partitioningStrategy.SpatialGridPartitioningStrategy;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class SpecializedConversionTest {
    static EnergyConversionProgram program() {
        EnergyConversion[] routes=new EnergyConversion[8];
        for(int i=0;i<8;i++)routes[i]=new EnergyConversion(EnergyType.ALL.get((i+1)%8),EnergyType.ALL.get(i),.2+.05*i);
        return new EnergyConversionProgram(routes);
    }
    Cell cell() { return new Cell(new Vector2D(100,100),10,new PhysicsRegressionTest.ConstantBrain()); }
    @ParameterizedTest @ValueSource(ints={0,1,2,3,4,5,6,7})
    void onlySpecializationsOwnRouteRunsAndSwitchingStateDoesNotMoveEnergy(int state) {
        Cell cell=cell();EnergyConversionProgram dna=program();cell.setEnergyConversionProgram(dna);
        for(EnergyType type:EnergyType.ALL)cell.setEnergy(type,.5);
        cell.setCellState(state);assertEquals(dna.route(state),cell.getEnergyConversion());
        for(EnergyType type:EnergyType.ALL)assertEquals(.5,cell.getEnergy(type));
        EnergyConversion active=dna.route(state);double consumed=.1*active.capacity()*.5;
        double loss=EnergyConversionService.convert(cell,.5);
        for(EnergyType type:EnergyType.ALL)assertEquals(.5+(type==active.source()?-consumed:type==active.target()?consumed*.8:0),cell.getEnergy(type),1e-14);
        assertEquals(4,cell.getTotalEnergy()+loss,1e-14);
        cell.setCellState((state+1)%8);assertSame(dna,cell.getEnergyConversionProgram());
    }
    @Test void conversionUsesSameThreeStateBitsAsExistingSensors() {
        Cell cell=cell();cell.setEnergyConversionProgram(program());cell.setCellState(13);
        assertEquals(5,cell.getSpecializationIndex());assertEquals(program().route(5),cell.getEnergyConversion());
        assertArrayEquals(new double[]{1,0,1},cell.getNormalizedCellState());
    }
    @Test void programsAreImmutableAndMutateAllRolesIncludingUnexpressedOnes() {
        EnergyConversion[] routes=new EnergyConversion[8];Arrays.fill(routes,EnergyConversion.NONE);
        EnergyConversionProgram original=new EnergyConversionProgram(routes);routes[0]=new EnergyConversion(EnergyType.PRIMARY,EnergyType.SECONDARY,1);
        assertEquals(EnergyConversion.NONE,original.route(0));
        EnergyConversionProgram edited=original.withRoute(7,routes[0]);
        assertEquals(EnergyConversion.NONE,original.route(7));assertEquals(routes[0],edited.route(7));
        Random fixed=new Random(){
            public double nextDouble(){return .1;}
            public double nextDouble(double low,double high){return .5;}
            public int nextInt(int bound){return 0;}
        };
        EnergyConversionProgram mutated=original.mutate(fixed,1,.2);
        for(int state=0;state<8;state++) {assertEquals(.1,mutated.route(state).capacity());assertEquals(EnergyConversion.NONE,original.route(state));}
        assertSame(original,original.mutate(new Random(1),0,1));
        assertThrows(IllegalArgumentException.class,()->new EnergyConversionProgram(new EnergyConversion[7]));
        assertThrows(NullPointerException.class,()->new EnergyConversionProgram(new EnergyConversion[8]));
        assertThrows(IndexOutOfBoundsException.class,()->original.route(8));
    }
    @Test void biologicalClonesAndTrainingElitesInheritEveryRoleAndChildStateSelectsItsOwnEnzyme() {
        var config=SimulationConfig.getInstance();Random previous=NeuralNetwork.getRandom();NeuralNetwork.setRandom(new Random(517));
        try {
            Environment world=new Environment(300,300);Cell parent=CellFactory.createCell(new Vector2D(100,100),10);
            parent.setEnergyConversionProgram(program());parent.setCellState(1);world.addCell(parent);
            var actor=parent.getSensorActors().getFirst();actor.setReproductionDesire(1);actor.setReproductionState(6);
            actor.setReproductionEnergyShareOutput(.25);
            Cell child=ReproductionManagerService.reproduceClonally(config,world,parent);assertNotNull(child);
            assertEquals(6,child.getCellState());assertEquals(program(),child.getEnergyConversionProgram());
            assertEquals(program().route(6),child.getEnergyConversion());assertEquals(program().route(1),parent.getEnergyConversion());
            Cell elite=ReproductionManagerService.createTrainingFounder(config,world,parent,new Vector2D(200,100),3,false);
            assertEquals(program(),elite.getEnergyConversionProgram());assertEquals(program().route(3),elite.getEnergyConversion());
            child.setEnergyConversionProgram(child.getEnergyConversionProgram().withRoute(1,EnergyConversion.NONE));
            assertEquals(program(),parent.getEnergyConversionProgram());
        } finally {NeuralNetwork.setRandom(previous);}
    }
    @Test void genuineSingleEnzymeSaveKeepsItsFormerReactionInEverySpecialization() throws Exception {
        byte[] fixture;
        try(var input=Objects.requireNonNull(getClass().getResourceAsStream("/energy/single-conversion-v1.cells"))) {fixture=input.readAllBytes();}
        assertFalse(new String(fixture,StandardCharsets.ISO_8859_1).contains("energyConversionProgram"));
        try(var input=new ObjectInputStream(new ByteArrayInputStream(fixture))) {
            List<?> loaded=(List<?>)input.readObject();
            EnergyConversion[] expected={new EnergyConversion(EnergyType.RGB_111,EnergyType.PRIMARY,.37),new EnergyConversion(EnergyType.SECONDARY,EnergyType.RGB_010,.8)};
            for(int i=0;i<2;i++) {
                Cell cell=(Cell)loaded.get(i);
                for(int state=0;state<8;state++){cell.setCellState(state);assertEquals(expected[i],cell.getEnergyConversion());}
            }
            assertEquals(.2,((Cell)loaded.get(0)).getEnergy());assertEquals(.6,((Cell)loaded.get(0)).getEnergy(EnergyType.RGB_111));
        }
    }
    @Test void serializationRetainsAllRolesAndAnInertCorpseDoesNotExpressAnyOfThem() throws Exception {
        Cell original=CellFactory.createCell(new Vector2D(100,100),10);original.setEnergyConversionProgram(program());original.setCellState(7);
        var bytes=new ByteArrayOutputStream();try(var output=new ObjectOutputStream(bytes)){output.writeObject(original);}
        try(var input=new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            Cell copy=(Cell)input.readObject();assertEquals(7,copy.getCellState());assertEquals(program(),copy.getEnergyConversionProgram());
            copy.setEnergy(EnergyType.PRIMARY,.4);copy.becomeRemains();double before=copy.getTotalEnergy();
            assertEquals(0,EnergyConversionService.convert(copy,10));assertEquals(before,copy.getTotalEnergy());
        }
    }
    /** Same fixed controller/DNA in each cell; specialization controls which cells pass products rightward. */
    private static class ChainBrain extends PhysicsRegressionTest.ConstantBrain {
        int state;
        public void setInputs(double[] inputs) {
            state=(int)(inputs[GlobalInputFeature.MY_CELL_STATE_0.ordinal()]+2*inputs[GlobalInputFeature.MY_CELL_STATE_1.ordinal()]
                    +4*inputs[GlobalInputFeature.MY_CELL_STATE_2.ordinal()]);
        }
        public double[] process() {
            double[] output=super.process();
            if(state!=2)output[GlobalOutputFeature.values().length+ActorOutputFeature.ENERGY_DELIVERY.ordinal()]=1;
            return output;
        }
    }
    record ChainResult(double output,double total,double loss) {}
    private ChainResult chain(boolean middleEnzyme,boolean reverse,boolean seam) {
        Environment world=new Environment(300,300);
        EnergyConversionProgram dna=EnergyConversionProgram.NONE
                .withRoute(0,new EnergyConversion(EnergyType.RGB_111,EnergyType.RGB_010,1))
                .withRoute(1,new EnergyConversion(EnergyType.RGB_010,EnergyType.SECONDARY,1))
                .withRoute(2,new EnergyConversion(EnergyType.SECONDARY,EnergyType.PRIMARY,1));
        List<Cell> cells=new ArrayList<>();
        for(int state=0;state<3;state++) {
            Cell cell=new Cell(new Vector2D(Vector2D.wrapCoordinate((seam?278:100)+22*state,300),100),10,new ChainBrain());
            cell.setEnergyConversionProgram(dna);cell.setCellState(state==1&&!middleEnzyme?3:state);
            cell.setType(new CellType(0,1,0));cell.setEnergy(.1);cells.add(cell);world.addCell(cell);
            CellBrainService.think(cell,CellBrainService.generateInputs(cell,world));
        }
        Cell a=cells.get(0),b=cells.get(1),c=cells.get(2);
        // Delivery protects 0.1 per form: reserve baselines keep intermediates moving without exporting source/maintenance.
        a.setEnergy(EnergyType.RGB_111,.1);a.setEnergy(EnergyType.RGB_010,.1);b.setEnergy(EnergyType.SECONDARY,.1);
        if(reverse)Collections.reverse(world.getCellList());
        var grid=new SpatialGridPartitioningStrategy(300,300,60);
        for(int i=0;i<60;i++){grid.build(world.getCellList());world.update(1.0/60,grid);}
        assertEquals(3,world.getCellList().size());
        for(Cell cell:cells)assertSame(dna,cell.getEnergyConversionProgram());
        double first=.1-a.getEnergy(EnergyType.RGB_111);
        double second=.8*first-(a.getEnergy(EnergyType.RGB_010)-.1)-b.getEnergy(EnergyType.RGB_010);
        double third=.8*second-(b.getEnergy(EnergyType.SECONDARY)-.1)-c.getEnergy(EnergyType.SECONDARY);
        double loss=.2*(first+second+third);
        double total=cells.stream().mapToDouble(Cell::getTotalEnergy).sum();
        assertEquals(.6,total+loss+3*SimulationConfig.ENERGY_DECAY_RATE,1e-12);
        return new ChainResult(c.getEnergy(),total,loss);
    }
    @ParameterizedTest @ValueSource(booleans={false,true})
    void sameGenotypeAndCellColorCanRunAThreeCellMetabolicChainThroughNormalDelivery(boolean seam) {
        var config=SimulationConfig.getInstance();double sun=config.getSunRayRate();config.setSunRayRate(0);
        try {
            ChainResult working=chain(true,false,seam),reversed=chain(true,true,seam),interrupted=chain(false,false,seam);
            assertEquals(working.output(),reversed.output(),1e-12);assertEquals(working.total(),reversed.total(),1e-12);
            assertTrue(working.output()>.14,working.toString());
            assertEquals(.097,interrupted.output(),1e-12);
            assertTrue(working.loss()>interrupted.loss());
            System.out.printf(Locale.ROOT,"Specialized chain: final-cell primary %.6f, missing middle specialization %.6f%n",working.output(),interrupted.output());
        } finally {config.setSunRayRate(sun);}
    }
}
