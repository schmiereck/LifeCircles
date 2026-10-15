package de.lifecircles.service.trainStrategy;

import de.lifecircles.model.*;
import de.lifecircles.model.neural.*;
import de.lifecircles.service.*;
import de.lifecircles.service.partitioningStrategy.SpatialGridPartitioningStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Real evolution: ordinary founder resets, no additional energy grants, imposed bonds or culling. */
@EnabledIfSystemProperty(named="evolution.study",matches="true")
class HighPosition2EvolutionStudyTest {
    private static final double DT = 1.0/60;
    private static final String HEADER = "case,generation,seconds,cells,births,deaths,grounded,grounded_fraction,sustained_height,current_supported_height,instant_height,mean_energy,ready_to_divide,mean_radius,mean_genes,mean_generation,max_generation,mean_attraction,mean_speed,step_ms\n";
    private final SimulationConfig config = SimulationConfig.getInstance();

    @Test void measure() throws Exception {
        Path output = Path.of(System.getProperty("evolution.output","target/evolution")); Files.createDirectories(output);
        int generations = Integer.getInteger("evolution.generations",10);
        int seed = Integer.getInteger("evolution.seed",741);
        String cases = System.getProperty("evolution.cases","standard,more_sun,clonal_more_sun");
        Path initial = output.resolve("initial.cells");
        Random previous = NeuralNetwork.getRandom();
        double oldRate=config.getSunRayRate(), oldEnergy=config.getEnergyPerRay(), oldDivision=config.getDivisionMutationRateMultiplier(),
                oldAge=config.getReproductionAgeThreshold(), oldWidth=config.getWidth(), oldHeight=config.getHeight(), oldScale=config.getScaleSimulation();
        try {
            if (!Files.exists(initial)) {
                NeuralNetwork.setRandom(new Random(seed));
                var initializer = new HighPosition2TrainStrategy();
                Environment source = initializer.initializeEnvironment(); initializer.initialize(source);
                Random colors = new Random(seed+1);
                for(Cell cell:source.getCellList()) cell.setType(new CellType(colors.nextDouble(),colors.nextDouble(),colors.nextDouble()));
                FileService.getInstance().saveCellsToFile(initial.toString(),source.getCellList());
            }
            Files.writeString(output.resolve("settings.txt"),String.format(Locale.ROOT,
                    "seed=%d\ngenerations=%d\ncases=%s\ndt=1/60\nwidth=6400\nheight=1200\nenergy_per_ray=0.005\nray_spacing=%.6f\nmutation_rate=%.6f\nmutation_strength=%.6f\nrotation_damping=%.6f\ncontact=%.6f\nattraction=%.6f\nnormalization=%s\nInitial population is saved. Meta-mutation uses unseeded Random; complete trajectories are not bitwise reproducible.\n",
                    seed,generations,cases,config.getSunRaySpacingPx(),config.getMutationRate(),config.getMutationStrength(),
                    config.getRotationalFriction(),config.getCellRepulsionStrength(),config.getCellActorMaxAttractiveForceStrength(),config.isNormalizeBrainInputs()));
            for(String name:cases.split(",")) {
                assertTrue(Set.of("standard","more_sun","clonal_more_sun").contains(name));
                Path folder=output.resolve(name); Files.createDirectories(folder); Files.writeString(folder.resolve("generations.csv"),HEADER);
                Files.writeString(folder.resolve("timeline.csv"),"seconds,cells,births,deaths,mean_energy\n");
                int[] generation={0}; long[] births={0}, deaths={0}; long[] started={System.nanoTime()}; double[] best={-1};
                var strategy = new HighPosition2TrainStrategy(new Random(seed+2),(world,fitness,winners)-> {
                    try {
                        generation[0]++;
                        var ratings=fitness.current(world); List<Cell> cells=world.getCellList(); double floor=world.getHeight()-Environment.GroundBlockerHeight;
                        double persistent=winners.stream().mapToDouble(fitness::persistentScore).max().orElse(0)*floor;
                        double supported=ratings.values().stream().mapToDouble(GroundedColonyFitness.Rating::score).max().orElse(0)*floor;
                        double instant=cells.stream().mapToDouble(c->floor-c.getPosition().getY()+c.getRadiusSize()).max().orElse(0);
                        double meanEnergy=cells.stream().mapToDouble(Cell::getEnergy).average().orElse(0);
                        double attraction=cells.stream().flatMap(c->c.getSensorActors().stream()).mapToDouble(a->Math.max(0,-a.getForceStrength())).average().orElse(0);
                        double elapsed=(System.nanoTime()-started[0])/1e6/3000;
                        String line=String.format(Locale.ROOT,"%s,%d,%d,%d,%d,%d,%d,%.9f,%.9f,%.9f,%.9f,%.9f,%d,%.9f,%.9f,%.9f,%d,%.9f,%.9f,%.9f%n",
                                name,generation[0],generation[0]*50,cells.size(),births[0],deaths[0],ratings.size(),ratings.size()/(double)Math.max(1,cells.size()),
                                persistent,supported,instant,meanEnergy,cells.stream().filter(c->ReproductionManagerService.canReproduce(config,c)).count(),
                                cells.stream().mapToDouble(Cell::getRadiusSize).average().orElse(0),cells.stream().mapToDouble(c->c.getBrain().getSynapseCount()).average().orElse(0),
                                cells.stream().mapToInt(Cell::getGeneration).average().orElse(0),cells.stream().mapToInt(Cell::getGeneration).max().orElse(0),attraction,
                                cells.stream().mapToDouble(c->c.getVelocity().length()).average().orElse(0),elapsed);
                        Files.writeString(folder.resolve("generations.csv"),line,StandardOpenOption.APPEND); System.out.print(line);
                        if(persistent>best[0]) {
                            best[0]=persistent;
                            FileService.getInstance().saveCellsToFile(folder.resolve("best.cells").toString(),new ArrayList<>(cells));
                            FileService.getInstance().saveCellsToFile(folder.resolve("best-parents.cells").toString(),new ArrayList<>(winners));
                            Files.writeString(folder.resolve("best.txt"),line);
                        }
                        if(generation[0]==1 || generation[0]%10==0 || generation[0]==generations) {
                            FileService.getInstance().saveCellsToFile(folder.resolve("generation-"+generation[0]+".cells").toString(),new ArrayList<>(cells));
                            FileService.getInstance().saveCellsToFile(folder.resolve("parents-"+generation[0]+".cells").toString(),new ArrayList<>(winners));
                        }
                    } catch(Exception e) { throw new RuntimeException(e); }
                });
                NeuralNetwork.setRandom(new Random(seed));
                Environment world=strategy.initializeEnvironment(); strategy.initialize(world);
                world.resetCells(FileService.getInstance().loadCellsFromFile(initial.toString()));
                config.setSunRayRate(name.equals("standard")?10:20);
                config.setDivisionMutationRateMultiplier(name.equals("clonal_more_sun")?0:1);
                seedSun(world,seed+3);
                var reproductionRandom=ReproductionManagerService.class.getDeclaredField("random"); reproductionRandom.setAccessible(true);
                ((Random)reproductionRandom.get(null)).setSeed(seed+4);
                var grid = new SpatialGridPartitioningStrategy(6400,1200,
                        2*(config.getCellMaxRadiusSize()+config.getCellActorMaxFieldRadius()));
                long runStart=System.nanoTime(), lastProgress=runStart;
                for(int step=1;step<=generations*3000;step++) {
                    if((step-1)%3000==0) { births[0]=0; deaths[0]=0; started[0]=System.nanoTime(); }
                    int before=world.getCellList().size(); grid.build(world.getCellList()); world.update(DT,grid);
                    long born=world.getCellList().stream().filter(c->c.getAge()==0).count(); births[0]+=born; deaths[0]+=before+born-world.getCellList().size();
                    if(step%60==0) {
                        for(Cell cell:world.getCellList()) {
                            assertTrue(Double.isFinite(cell.getPosition().getX()) && Double.isFinite(cell.getPosition().getY())
                                    && Double.isFinite(cell.getEnergy()) && Double.isFinite(cell.getRadiusSize()),"Nonfinite simulation state");
                        }
                        double mean=world.getCellList().stream().mapToDouble(Cell::getEnergy).average().orElse(0);
                        Files.writeString(folder.resolve("timeline.csv"),String.format(Locale.ROOT,"%d,%d,%d,%d,%.9f%n",step/60,world.getCellList().size(),births[0],deaths[0],mean),StandardOpenOption.APPEND);
                    }
                    strategy.selectAndMutate(world);
                    if(step%3000==0) FileService.getInstance().saveCellsToFile(folder.resolve("founders-latest.cells").toString(),new ArrayList<>(world.getCellList()));
                    if(world.getCellList().isEmpty()) {
                        Files.writeString(folder.resolve("stopped.txt"),"Extinction at step "+step); break;
                    }
                    if(world.getCellList().size()>4096 || (System.nanoTime()-runStart)/1e9>Double.parseDouble(System.getProperty("evolution.max_wall_seconds","600"))) {
                        Files.writeString(folder.resolve("stopped.txt"),"Resource limit at step "+step+", cells="+world.getCellList().size()); break;
                    }
                    if((System.nanoTime()-lastProgress)/1e9>20) {
                        System.out.printf("PROGRESS %s step %d/%d cells %d wall %.1fs%n",name,step,generations*3000,world.getCellList().size(),(System.nanoTime()-runStart)/1e9); lastProgress=System.nanoTime();
                    }
                }
            }
        } finally {
            NeuralNetwork.setRandom(previous); config.setSunRayRate(oldRate); config.setEnergyPerRay(oldEnergy);
            config.setDivisionMutationRateMultiplier(oldDivision); config.setReproductionAgeThreshold(oldAge);
            config.setWidth(oldWidth); config.setHeight(oldHeight); config.setScaleSimulation(oldScale);
        }
    }

    private void seedSun(Environment world,long seed) throws Exception {
        var field=Environment.class.getDeclaredField("energySunCalcService"); field.setAccessible(true);
        var random=EnergySunCalcService.class.getDeclaredField("random"); random.setAccessible(true);
        ((Random)random.get(field.get(world))).setSeed(seed);
    }
}
