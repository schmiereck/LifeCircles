package de.lifecircles.service.trainStrategy;

import de.lifecircles.model.*;
import de.lifecircles.model.neural.*;
import de.lifecircles.service.*;
import de.lifecircles.service.partitioningStrategy.SpatialGridPartitioningStrategy;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class CarrionRainTrainStrategyTest {
    final SimulationConfig config=SimulationConfig.getInstance();
    double width,height,scale,sun,death,mutation,strength;
    BrainEnergyCosts costs;
    Random previous;
    @BeforeEach void save() {
        width=config.getWidth();height=config.getHeight();scale=config.getScaleSimulation();sun=config.getSunRayRate();
        death=config.getCellDeathAge();mutation=config.getMutationRate();strength=config.getMutationStrength();costs=config.getBrainEnergyCosts();
        previous=NeuralNetwork.getRandom();NeuralNetwork.setRandom(new Random(371));
        config.setSunRayRate(100);config.setCellDeathAge(10000);config.setMutationRate(0);config.setMutationStrength(0);config.setBrainEnergyMultiplier(0);
    }
    @AfterEach void restore() {
        config.setWidth(width);config.setHeight(height);config.setScaleSimulation(scale);config.setSunRayRate(sun);
        config.setCellDeathAge(death);config.setMutationRate(mutation);config.setMutationStrength(strength);
        config.setBrainEnergyMultiplier(costs.work());NeuralNetwork.setRandom(previous);
    }
    static Cell genome(boolean eat) {
        var network=new NeuralNetwork(new ValuesNeuronValueFunctionFactory(),CellFactory.calcInputCount(12),new int[0],CellFactory.calcOutputCount(12),0,0);
        var outputs=network.getNeuralNet().getOutputNeuronArr();
        for(var output:outputs) output.setBias(0,-100);
        outputs[GlobalOutputFeature.SIZE.ordinal()].setBias(0,100);
        for(int i=0;i<12;i++) {
            int base=GlobalOutputFeature.values().length+i*ActorOutputFeature.values().length;
            outputs[base+ActorOutputFeature.FORCE.ordinal()].setBias(0,0);
            outputs[base+ActorOutputFeature.ENERGY_ABSORPTION.ordinal()].setBias(0,eat?100:-100);
            outputs[base+ActorOutputFeature.REPRODUCTION_DESIRE.ordinal()].setBias(0,100);
        }
        return new Cell(new Vector2D(200,530),20,new CellBrain(network));
    }
    Environment setup(CarrionRainTrainStrategy strategy, List<Cell> genomes) {
        Environment world=strategy.initializeEnvironment();strategy.initialize(world);
        strategy.onPopulationImported(world,genomes,false);return world;
    }
    void step(CarrionRainTrainStrategy strategy,Environment world,SpatialGridPartitioningStrategy grid,double dt) {
        strategy.beforeStep(world,dt);grid.build(world.getCellList());world.update(dt,grid,strategy);strategy.selectAndMutate(world,dt);
    }
    @Test void rainIsOrdinaryInertPrimaryEnergyWithEqualStartsAndNoSunOrDivision() {
        var strategy=new CarrionRainTrainStrategy(new Random(1));var world=setup(strategy,List.of(genome(true)));
        assertEquals(16,world.getCellList().size());assertEquals(10,world.getBlockerList().size());
        for(Cell cell:world.getCellList()) {
            assertEquals(cell.isRemains()?.12:.144,cell.getEnergy());assertEquals(cell.isRemains()?.12:.18,cell.getTotalEnergy());
            if(cell.isRemains()) assertSame(RemainsBrain.INSTANCE,cell.getBrain());
            else { assertFalse(strategy.allowReproduction(cell));assertEquals(.036,cell.getEnergy(EnergyType.SECONDARY));assertEquals(0,cell.getMaxReproductionDesire()); }
        }
        var grid=new SpatialGridPartitioningStrategy(world.getWidth(),world.getHeight(),80);
        step(strategy,world,grid,1.0/60);assertTrue(world.getSunRayList().isEmpty());assertEquals(100,config.getSunRayRate());
        assertTrue(strategy.getStatus().contains("Sonne AUS"));
    }
    @Test void passiveScavengerSurvivesFullEpisodeWhileNonEaterStarvesAndSelectionCopiesWinner() {
        var strategy=new CarrionRainTrainStrategy(new Random(2));
        List<Cell> seeds=new ArrayList<>(Collections.nCopies(8,genome(false)));seeds.set(1,genome(true));
        var world=setup(strategy,seeds);
        Cell nonEater=world.getCellList().get(0), eater=world.getCellList().get(1);
        var grid=new SpatialGridPartitioningStrategy(world.getWidth(),world.getHeight(),80);
        for(int i=0;i<3600;i++)step(strategy,world,grid,1.0/30);
        var evaluation=strategy.getLastEvaluation();assertNotNull(evaluation);
        assertEquals(120,evaluation.survivalSeconds(),.04);assertTrue(evaluation.harvested()>.18,evaluation.toString());
        assertEquals(evaluation.injected(),evaluation.expired()+evaluation.remainingFoodEnergy()+evaluation.totalHarvested(),1e-10);
        assertEquals(0,nonEater.getEnergy());assertTrue(eater.getEnergy()>0);
        assertFalse(world.getCellList().contains(eater));assertEquals(1,evaluation.generation());
        Cell elite=world.getCellList().getFirst();assertEquals(.144,elite.getEnergy());assertEquals(.036,elite.getEnergy(EnergyType.SECONDARY));assertEquals(0,elite.getAge());
        assertEquals(0,elite.getSensorActors().getFirst().getEnergyAbsorption()); // First decision is paid in the next episode step.
    }
    @Test void uneatenFoodExpiresAndPopulationIsBoundedWithoutMetabolicSubsidy() {
        var strategy=new CarrionRainTrainStrategy(new Random(3));var world=setup(strategy,List.of(genome(false)));
        var grid=new SpatialGridPartitioningStrategy(world.getWidth(),world.getHeight(),80);
        for(int i=0;i<1200;i++) {
            step(strategy,world,grid,.1);
            assertTrue(world.getCellList().size()<=8*(1+CarrionRainTrainStrategy.FOOD_LIMIT));
        }
        var evaluation=strategy.getLastEvaluation();assertNotNull(evaluation);
        assertEquals(48,evaluation.survivalSeconds(),.11);assertEquals(0,evaluation.harvested(),1e-12);
        assertTrue(evaluation.expired()>0);assertTrue(evaluation.injected()>evaluation.expired());
        assertEquals(evaluation.injected(),evaluation.expired()+evaluation.remainingFoodEnergy()+evaluation.totalHarvested(),1e-10);
    }
    @Test void escapingCandidatesAreRemovedAndCannotWinByRaidingOtherChambers() {
        var strategy=new CarrionRainTrainStrategy(new Random(5));var world=setup(strategy,List.of(genome(true)));
        List<Cell> living=world.getCellList().stream().filter(c->!c.isRemains()).toList();
        for(Cell cell:living)cell.setPosition(new Vector2D(-1,100));
        strategy.beforeStep(world,.1);
        assertTrue(living.stream().noneMatch(world.getCellList()::contains));
        strategy.selectAndMutate(world,120);
        assertEquals(0,strategy.getLastEvaluation().score());
    }
    @Test void corpseOnlyImportsAreRejectedAndInvalidStepSizesFail() {
        var strategy=new CarrionRainTrainStrategy(new Random(4));var world=setup(strategy,List.of(genome(false)));
        Cell corpse=genome(false);corpse.becomeRemains();
        assertThrows(IllegalArgumentException.class,()->strategy.onPopulationImported(world,List.of(corpse),true));
        assertThrows(IllegalArgumentException.class,()->strategy.beforeStep(world,Double.NaN));
        assertThrows(IllegalArgumentException.class,()->strategy.beforeStep(world,0));
    }
    @Test void secondaryCarrionRequiresBothAbsorptionAndTheRightConversionAndKeepsEliteGenes() {
        var strategy=new CarrionRainTrainStrategy(new Random(12),EnergyType.SECONDARY);
        Cell eater=genome(true),converter=genome(true),wrongConverter=genome(true),nonEater=genome(false);
        var enzyme=new EnergyConversion(EnergyType.SECONDARY,EnergyType.PRIMARY,.1);
        converter.setEnergyConversion(enzyme);nonEater.setEnergyConversion(enzyme);
        wrongConverter.setEnergyConversion(new EnergyConversion(EnergyType.RGB_111,EnergyType.PRIMARY,1));
        var world=setup(strategy,List.of(eater,converter,wrongConverter,nonEater,eater,converter,wrongConverter,nonEater));
        List<Cell> living=world.getCellList().stream().filter(c->!c.isRemains()).toList();
        for(Cell food:world.getCellList())if(food.isRemains()) {
            assertEquals(0,food.getEnergy());assertEquals(.12,food.getEnergy(EnergyType.SECONDARY));assertEquals(.12,food.getTotalEnergy());
        }
        var grid=new SpatialGridPartitioningStrategy(world.getWidth(),world.getHeight(),80);
        for(int i=0;i<3600;i++)step(strategy,world,grid,1.0/30);
        var result=strategy.getLastEvaluation();assertNotNull(result);assertEquals(120,result.survivalSeconds(),.04);
        assertEquals(0,living.get(0).getEnergy());assertTrue(living.get(0).getEnergy(EnergyType.SECONDARY)>0);
        assertEquals(0,living.get(2).getEnergy());assertEquals(0,living.get(3).getEnergy());
        assertTrue(living.get(1).getEnergy()>0);assertEquals(enzyme,world.getCellList().getFirst().getEnergyConversion());
        assertEquals(result.injected(),result.expired()+result.remainingFoodEnergy()+result.totalHarvested(),1e-10);
        assertTrue(strategy.getStatus().contains("Aasregen 001"));
    }
}
