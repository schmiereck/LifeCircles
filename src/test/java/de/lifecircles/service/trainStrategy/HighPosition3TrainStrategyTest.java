package de.lifecircles.service.trainStrategy;

import de.lifecircles.model.*;
import de.lifecircles.model.neural.*;
import de.lifecircles.service.*;
import de.lifecircles.service.partitioningStrategy.SpatialGridPartitioningStrategy;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class HighPosition3TrainStrategyTest {
    final SimulationConfig config=SimulationConfig.getInstance();
    double width,height,scale,rate,strength,age,division;
    Random previous;
    @BeforeEach void save() {
        width=config.getWidth();height=config.getHeight();scale=config.getScaleSimulation();
        rate=config.getMutationRate();strength=config.getMutationStrength();age=config.getReproductionAgeThreshold();division=config.getDivisionMutationRateMultiplier();
        config.setMutationRate(0);config.setMutationStrength(0);config.setReproductionAgeThreshold(0);
        previous=NeuralNetwork.getRandom();NeuralNetwork.setRandom(new Random(573));
    }
    @AfterEach void restore() {
        config.setWidth(width);config.setHeight(height);config.setScaleSimulation(scale);
        config.setMutationRate(rate);config.setMutationStrength(strength);config.setReproductionAgeThreshold(age);config.setDivisionMutationRateMultiplier(division);
        NeuralNetwork.setRandom(previous);
    }
    static Cell genome(double forceOutput, boolean births) {
        var network=new NeuralNetwork(new ValuesNeuronValueFunctionFactory(),CellFactory.calcInputCount(12),new int[0],CellFactory.calcOutputCount(12),0,0);
        var outputs=network.getNeuralNet().getOutputNeuronArr();
        for (var output:outputs) output.setBias(0, -12);
        outputs[GlobalOutputFeature.SIZE.ordinal()].setBias(0,12);
        for (int a=0;a<12;a++) {
            int b=GlobalOutputFeature.values().length+a*ActorOutputFeature.values().length;
            outputs[b+ActorOutputFeature.FORCE.ordinal()].setBias(0,Math.log(forceOutput/(1-forceOutput)));
            outputs[b+ActorOutputFeature.TYPE_GREEN.ordinal()].setBias(0,12);
            if (births && a==9) outputs[b+ActorOutputFeature.REPRODUCTION_DESIRE.ordinal()].setBias(0,12);
            outputs[b+ActorOutputFeature.REPRODUCTION_ENERGY_SHARE.ordinal()].setBias(0,0);
        }
        Cell cell=new Cell(new Vector2D(700,1130),20,new CellBrain(network));cell.setType(new CellType(0,1,0));return cell;
    }
    HighPosition3TrainStrategy strategy() { return new HighPosition3TrainStrategy(new Random(98)); }
    Environment setup(HighPosition3TrainStrategy strategy, Cell genome) {
        Environment world=strategy.initializeEnvironment();world.addGroundBlocker();
        strategy.startEpisode(world,Collections.nCopies(8,genome));return world;
    }
    void step(HighPosition3TrainStrategy strategy,Environment world,SpatialGridPartitioningStrategy grid,double dt) {
        strategy.beforeStep(world,dt);grid.build(world.getCellList());world.update(dt,grid,strategy.stepPolicy());strategy.selectAndMutate(world,dt);
    }
    void promote(HighPosition3TrainStrategy strategy,int stages) {
        for(int i=0;i<stages*3;i++) strategy.advanceStage(true);
    }
    @Test void requiresThreeConsecutiveSuccessesAtEachStage() {
        var strategy=strategy();strategy.advanceStage(true);strategy.advanceStage(true);strategy.advanceStage(false);
        strategy.advanceStage(true);strategy.advanceStage(true);assertEquals(HighPosition3TrainStrategy.Stage.HOLD,strategy.getStage());
        strategy.advanceStage(true);assertEquals(HighPosition3TrainStrategy.Stage.RECOVER,strategy.getStage());
        promote(strategy,2);assertEquals(HighPosition3TrainStrategy.Stage.BUILD,strategy.getStage());
        promote(strategy,1);assertEquals(HighPosition3TrainStrategy.Stage.BUILD,strategy.getStage());
    }
    @Test void masksActuatorsButAlwaysLeavesNetworkForceOutputLive() {
        var strategy=strategy();var cell=genome(.25,true);CellBrainService.think(cell);
        double force=cell.getSensorActors().getFirst().getForceStrength();
        cell.setRadiusSize(7);strategy.afterThinking(cell);
        assertEquals(20,cell.getTargetRadiusSize());assertEquals(force,cell.getSensorActors().getFirst().getForceStrength());
        assertEquals(0,cell.getMaxReproductionDesire());
        for(var actor:cell.getSensorActors()) {assertEquals(0,actor.getEnergyDelivery());assertEquals(0,actor.getEnergyAbsorption());}
        promote(strategy,2);CellBrainService.think(cell);strategy.afterThinking(cell);
        assertTrue(cell.getTargetRadiusSize()<20);assertEquals(0,cell.getMaxReproductionDesire());
        promote(strategy,1);CellBrainService.think(cell);strategy.afterThinking(cell);
        assertTrue(cell.getMaxReproductionDesire()>.9);
    }
    @Test void candidatesStartAsIndependentSixtyCellBodies() {
        var strategy=strategy();var original=genome(.001,false);original.setAge(200);original.setVelocity(new Vector2D(50,40));
        original.setEnergyConversion(new EnergyConversion(EnergyType.RGB_111,EnergyType.PRIMARY,.8));
        var world=setup(strategy,original);assertEquals(480,world.getCellList().size());
        Set<CellBrainInterface> brains=Collections.newSetFromMap(new IdentityHashMap<>());
        for(Cell cell:world.getCellList()) {
            brains.add(cell.getBrain());assertEquals(0,cell.getAge());assertEquals(0,cell.getVelocity().length());
            assertEquals(original.getEnergyConversion(),cell.getEnergyConversion());
            assertEquals(20,cell.getRadiusSize());assertEquals(.75,cell.getEnergy());assertFalse(cell.isGrowing());
        }
        assertEquals(480,brains.size());assertFalse(brains.contains(original.getBrain()));
        Cell first=world.getCellList().getFirst(),second=world.getCellList().get(1);
        var firstOutput=((CellBrain)first.getBrain()).getNeuralNetwork().getNeuralNet().getOutputNeuronArr()[0];
        var secondOutput=((CellBrain)second.getBrain()).getNeuralNetwork().getNeuralNet().getOutputNeuronArr()[0];
        double before=secondOutput.getBias(0);firstOutput.setBias(0,-100);assertEquals(before,secondOutput.getBias(0));
    }
    @Test void productionEpisodeSelectsNewGenomesAndKeepsOneUnmutatedElite() {
        var strategy=strategy();var good=genome(.001,false);good.setGeneration(7);
        var bad=genome(.5,false);bad.setGeneration(20);
        var world=setup(strategy,bad);
        List<Cell> genomes=new ArrayList<>(Collections.nCopies(8,bad));genomes.set(0,good);strategy.startEpisode(world,genomes);
        Set<Cell> previous=new HashSet<>(world.getCellList());
        var grid=new SpatialGridPartitioningStrategy(world.getWidth(),world.getHeight(),80);
        for(int i=0;i<1800;i++) step(strategy,world,grid,1.0/60);
        assertEquals(2,strategy.getGeneration());assertEquals(480,world.getCellList().size());
        assertTrue(world.getCellList().stream().noneMatch(previous::contains));
        assertEquals(7,world.getCellList().getFirst().getGeneration());
        assertTrue(strategy.getStatus().contains("Halten"));
        assertEquals(0,world.getCellList().getFirst().getAge());
    }
    @Test void buildStartsWithFiveCellsAndCapsBirthsPerBodyWithoutGlobalMutationChanges() {
        var strategy=strategy();promote(strategy,3);
        config.setDivisionMutationRateMultiplier(1);config.setMutationRate(1);config.setMutationStrength(1);
        var original=genome(.001,true);var world=setup(strategy,original);assertEquals(40,world.getCellList().size());
        var grid=new SpatialGridPartitioningStrategy(world.getWidth(),world.getHeight(),80);
        for(int i=0;i<20;i++) step(strategy,world,grid,1.0/60);
        assertEquals(480,world.getCellList().size());
        assertEquals(1,config.getDivisionMutationRateMultiplier());
        for(Cell cell:world.getCellList()) {
            assertEquals(original.getBrain().getSynapseCount(),cell.getBrain().getSynapseCount());
            assertEquals(original.getMutationRateFactor(),cell.getMutationRateFactor());
            assertEquals(original.getType(),cell.getType());
            double force=cell.getBrain().getOutputValue(GlobalOutputFeature.values().length+ActorOutputFeature.FORCE.ordinal());
            assertEquals(.001,force,1e-10);
        }
        for(int section=0;section<8;section++) {
            final int slot=section;
            assertEquals(60,world.getCellList().stream().filter(c->(int)(c.getPosition().getX()/1400)==slot).count());
        }
    }
    @Test void loadingRestartsHoldingWithImportedGenomesAndResetsCurriculum() {
        var strategy=strategy();promote(strategy,3);var world=setup(strategy,genome(.001,true));
        Cell imported=genome(.4,false);imported.setType(new CellType(.12,.34,.56));world.resetCells(List.of(imported));
        strategy.onPopulationImported(world);
        assertEquals(HighPosition3TrainStrategy.Stage.HOLD,strategy.getStage());assertEquals(1,strategy.getGeneration());
        assertEquals(480,world.getCellList().size());assertTrue(world.getCellList().stream().allMatch(c->c.getType().equals(imported.getType())));
    }
    @Test void mergeIntroducesImportedGenomeAndKeepsOtherCandidates() {
        var strategy=strategy();var world=setup(strategy,genome(.001,false));
        var imported=genome(.2,false);imported.setType(new CellType(.12,.34,.56));world.addCell(imported);
        strategy.onPopulationImported(world,List.of(imported),true);
        assertEquals(480,world.getCellList().size());assertEquals(imported.getType(),world.getCellList().getFirst().getType());
        assertEquals(new CellType(0,1,0),world.getCellList().get(60).getType());
    }
    @Test void emptyReplacementStartsFreshInsteadOfLeavingAnEmptyEpisode() {
        var strategy=strategy();var world=setup(strategy,genome(.001,false));world.resetCells();
        strategy.onPopulationImported(world,List.of(),false);
        assertEquals(480,world.getCellList().size());assertEquals(HighPosition3TrainStrategy.Stage.HOLD,strategy.getStage());
    }
    @Test void escapeInvalidatesBothDonorAndInvadedCandidate() {
        var strategy=strategy();var world=setup(strategy,genome(.001,false));
        List<Cell> genomes=new ArrayList<>();
        for(int i=0;i<8;i++) {Cell genome=genome(.001,false);genome.setType(new CellType(i*.1,.5,.5));genomes.add(genome);}
        strategy.startEpisode(world,genomes);world.getCellList().getFirst().setPosition(new Vector2D(1401,1130));
        strategy.beforeStep(world,1.0/60);
        for(int i=0;i<60;i++) strategy.selectAndMutate(world,.5);
        assertEquals(genomes.get(2).getType(),world.getCellList().getFirst().getType());
    }
    @Test void kickOccursOnceAtTenSecondsAndReversesAtTwenty() {
        var strategy=strategy();promote(strategy,1);var world=setup(strategy,genome(.001,false));
        for(int i=0;i<20;i++) strategy.selectAndMutate(world,.5);
        Cell top=world.getCellList().get(55),base=world.getCellList().getFirst();
        strategy.beforeStep(world,1.0/60);assertEquals(20,top.getVelocity().getX());assertEquals(0,base.getVelocity().getX());
        strategy.beforeStep(world,1.0/60);assertEquals(20,top.getVelocity().getX());
        for(int i=0;i<20;i++) strategy.selectAndMutate(world,.5);
        strategy.beforeStep(world,1.0/60);assertEquals(0,top.getVelocity().getX());
    }
    @Test void episodeDurationUsesSecondsNotNumberOfSteps() {
        var strategy=strategy();var world=setup(strategy,genome(.001,false));
        for(int i=0;i<3599;i++) strategy.selectAndMutate(world,1.0/120);
        assertEquals(1,strategy.getGeneration());strategy.selectAndMutate(world,1.0/120);assertEquals(2,strategy.getGeneration());
        assertThrows(IllegalArgumentException.class,()->strategy.beforeStep(world,0));
        assertThrows(IllegalArgumentException.class,()->strategy.beforeStep(world,Double.NaN));
    }
}
